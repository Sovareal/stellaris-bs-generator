use std::fs::File;
use std::net::TcpListener;
use std::process::{Child, Command, Stdio};
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Mutex;
use tauri::{Emitter, Manager, State};

#[cfg(windows)]
use std::os::windows::process::CommandExt;

struct BackendProcess(Mutex<Option<Child>>);
struct BackendPort(Mutex<u16>);
struct ShuttingDown(AtomicBool);

fn find_backend_jar(app: &tauri::App) -> Option<std::path::PathBuf> {
    // Dev mode only: look relative to project root (CARGO_MANIFEST_DIR is baked in at compile time).
    // Skipped in release builds so the installed backend.jar is always used.
    if cfg!(debug_assertions) {
        let dev_path = std::path::PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .parent()
            .and_then(|p| p.parent())
            .map(|root| root.join(format!("backend/build/libs/backend-{}.jar", env!("CARGO_PKG_VERSION"))));

        if let Some(ref path) = dev_path {
            if path.exists() {
                return dev_path;
            }
        }
    }

    // Production (and dev fallback): look in the resource directory next to the binary
    if let Ok(resource_dir) = app.path().resource_dir() {
        let prod_path = resource_dir.join("backend.jar");
        if prod_path.exists() {
            return Some(prod_path);
        }
    }

    None
}

fn find_java_executable(app: &tauri::App) -> String {
    // In production: use bundled JRE
    if !cfg!(debug_assertions) {
        if let Ok(resource_dir) = app.path().resource_dir() {
            let java_bin = if cfg!(windows) { "java.exe" } else { "java" };
            let bundled_java = resource_dir.join("jre").join("bin").join(java_bin);
            if bundled_java.exists() {
                log::info!("Using bundled JRE: {}", bundled_java.display());
                return bundled_java.to_string_lossy().into_owned();
            }
            log::warn!("Bundled JRE not found at {}, falling back to system java", bundled_java.display());
        }
    }
    // In dev mode or fallback: use system java
    "java".to_string()
}

fn get_log_path(app: &tauri::App) -> Option<std::path::PathBuf> {
    app.path().app_log_dir().ok().map(|dir| {
        let _ = std::fs::create_dir_all(&dir);
        dir.join("backend.log")
    })
}

fn allocate_port() -> Result<u16, String> {
    let listener = TcpListener::bind("127.0.0.1:0")
        .map_err(|e| format!("Failed to allocate port: {e}"))?;
    let port = listener
        .local_addr()
        .map_err(|e| format!("Failed to read allocated port: {e}"))?
        .port();
    drop(listener);
    Ok(port)
}

fn spawn_backend(
    java_path: &str,
    jar_path: &std::path::Path,
    port: u16,
    log_path: Option<&std::path::Path>,
) -> Result<Child, String> {
    let mut cmd = Command::new(java_path);
    cmd.args([
        "-jar",
        &jar_path.to_string_lossy(),
        &format!("--server.port={port}"),
    ]);

    // Redirect stdout/stderr to log file for diagnostics
    if let Some(path) = log_path {
        if let Ok(file) = File::create(path) {
            let stderr_file = file.try_clone().unwrap_or_else(|_| File::create(path).unwrap());
            cmd.stdout(Stdio::from(file));
            cmd.stderr(Stdio::from(stderr_file));
        }
    }

    // Hide the console window on Windows in production
    #[cfg(windows)]
    if !cfg!(debug_assertions) {
        const CREATE_NO_WINDOW: u32 = 0x08000000;
        cmd.creation_flags(CREATE_NO_WINDOW);
    }

    cmd.spawn()
        .map_err(|e| format!("Failed to start backend: {e}"))
}

fn spawn_crash_monitor(app_handle: tauri::AppHandle) {
    std::thread::spawn(move || {
        loop {
            std::thread::sleep(std::time::Duration::from_millis(500));

            let exited = {
                let state = app_handle.state::<BackendProcess>();
                let mut guard = state.0.lock().unwrap();
                match guard.as_mut() {
                    Some(child) => match child.try_wait() {
                        Ok(Some(_)) => true,
                        Ok(None) => false,
                        Err(_) => false,
                    },
                    None => return, // No child -- nothing to monitor
                }
            };

            if exited {
                let shutting_down = app_handle.state::<ShuttingDown>();
                if !shutting_down.0.load(Ordering::Relaxed) {
                    log::warn!("Backend process exited unexpectedly -- emitting backend-crashed");
                    let _ = app_handle.emit("backend-crashed", ());
                }
                break;
            }
        }
    });
}

/// POST /api/system/shutdown via a raw TCP socket.
/// Returns true if the server acknowledged with a 2xx response.
/// Uses no external HTTP crate -- just stdlib TCP.
fn try_graceful_shutdown(port: u16) -> bool {
    use std::io::{Read, Write};
    use std::net::TcpStream;
    use std::time::Duration;

    let addr_str = format!("127.0.0.1:{port}");
    let Ok(addr) = addr_str.parse::<std::net::SocketAddr>() else {
        return false;
    };
    let Ok(mut stream) = TcpStream::connect_timeout(&addr, Duration::from_millis(500)) else {
        return false;
    };

    let request = format!(
        "POST /api/system/shutdown HTTP/1.1\r\nHost: 127.0.0.1:{port}\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
    );
    if stream.write_all(request.as_bytes()).is_err() {
        return false;
    }

    let _ = stream.set_read_timeout(Some(Duration::from_millis(1500)));
    let mut response = Vec::with_capacity(64);
    let _ = stream.read_to_end(&mut response);

    response.starts_with(b"HTTP/1.1 2")
}

#[tauri::command]
fn get_backend_port(state: State<BackendPort>) -> u16 {
    *state.0.lock().unwrap()
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .manage(BackendProcess(Mutex::new(None)))
        .manage(BackendPort(Mutex::new(8080)))
        .manage(ShuttingDown(AtomicBool::new(false)))
        .invoke_handler(tauri::generate_handler![get_backend_port])
        .setup(|app| {
            let log_level = if cfg!(debug_assertions) {
                log::LevelFilter::Info
            } else {
                log::LevelFilter::Warn
            };
            app.handle().plugin(
                tauri_plugin_log::Builder::default()
                    .level(log_level)
                    .build(),
            )?;

            // Allocate a free port for the Spring Boot backend
            let port = match allocate_port() {
                Ok(p) => {
                    log::info!("Allocated backend port: {p}");
                    p
                }
                Err(e) => {
                    log::error!("{e} -- falling back to port 8080");
                    8080
                }
            };
            *app.state::<BackendPort>().0.lock().unwrap() = port;

            let java_path = find_java_executable(app);
            let log_path = get_log_path(app);
            if let Some(ref p) = log_path {
                log::info!("Backend log file: {}", p.display());
            }

            match find_backend_jar(app) {
                Some(jar_path) => {
                    log::info!("Starting backend: java={}, jar={}, port={port}", java_path, jar_path.display());
                    match spawn_backend(&java_path, &jar_path, port, log_path.as_deref()) {
                        Ok(child) => {
                            let state = app.state::<BackendProcess>();
                            *state.0.lock().unwrap() = Some(child);
                            log::info!(
                                "Backend process started (pid={})",
                                state.0.lock().unwrap().as_ref().map(|c| c.id()).unwrap_or(0)
                            );
                            spawn_crash_monitor(app.handle().clone());
                        }
                        Err(e) => {
                            log::error!("{e}");
                        }
                    }
                }
                None => {
                    log::warn!(
                        "Backend JAR not found. Start backend manually: gradle :backend:bootRun"
                    );
                }
            }

            Ok(())
        })
        .build(tauri::generate_context!())
        .expect("error while building tauri application")
        .run(|app_handle, event| {
            if let tauri::RunEvent::Exit = event {
                // Signal the crash monitor that we are shutting down intentionally
                if let Some(sd) = app_handle.try_state::<ShuttingDown>() {
                    sd.0.store(true, Ordering::Relaxed);
                }

                let port = app_handle
                    .try_state::<BackendPort>()
                    .map(|s| *s.0.lock().unwrap())
                    .unwrap_or(8080);

                // Try graceful shutdown: POST /api/system/shutdown, then poll up to 5 s
                if try_graceful_shutdown(port) {
                    let process_state = app_handle.try_state::<BackendProcess>();
                    let mut gracefully_exited = false;
                    for _ in 0..50 {
                        if let Some(ref state) = process_state {
                            if let Ok(mut guard) = state.0.lock() {
                                if let Some(ref mut child) = *guard {
                                    if matches!(child.try_wait(), Ok(Some(_))) {
                                        gracefully_exited = true;
                                        break;
                                    }
                                }
                            }
                        }
                        std::thread::sleep(std::time::Duration::from_millis(100));
                    }
                    if gracefully_exited {
                        log::info!("Backend shut down gracefully");
                        return;
                    }
                    log::warn!("Backend did not exit within 5 s -- force-killing");
                }

                // Fallback: force kill (graceful unavailable or timed out)
                if let Some(state) = app_handle.try_state::<BackendProcess>() {
                    if let Ok(mut guard) = state.0.lock() {
                        if let Some(ref mut child) = *guard {
                            let _ = child.kill();
                            log::info!("Backend process terminated");
                        }
                    }
                }
            }
        });
}
