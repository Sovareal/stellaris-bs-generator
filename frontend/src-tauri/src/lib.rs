use std::fs::File;
use std::process::{Child, Command, Stdio};
use std::sync::Mutex;
use tauri::Manager;

#[cfg(windows)]
use std::os::windows::process::CommandExt;

struct BackendProcess(Mutex<Option<Child>>);

fn find_backend_jar(app: &tauri::App) -> Option<std::path::PathBuf> {
    // In dev mode: look relative to project root
    let dev_path = std::path::PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .parent()
        .and_then(|p| p.parent())
        .map(|root| root.join("backend/build/libs/backend-0.1.0.jar"));

    if let Some(ref path) = dev_path {
        if path.exists() {
            return dev_path;
        }
    }

    // In production: look in the resource directory next to the binary
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
            let bundled_java = resource_dir.join("jre").join("bin").join("java.exe");
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

fn spawn_backend(java_path: &str, jar_path: &std::path::Path, log_path: Option<&std::path::Path>) -> Result<Child, String> {
    let mut cmd = Command::new(java_path);
    cmd.args(["-jar", &jar_path.to_string_lossy()]);

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

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .manage(BackendProcess(Mutex::new(None)))
        .setup(|app| {
            if cfg!(debug_assertions) {
                app.handle().plugin(
                    tauri_plugin_log::Builder::default()
                        .level(log::LevelFilter::Info)
                        .build(),
                )?;
            }

            // Launch the Spring Boot backend sidecar
            let java_path = find_java_executable(app);
            let log_path = get_log_path(app);
            if let Some(ref p) = log_path {
                log::info!("Backend log file: {}", p.display());
            }
            match find_backend_jar(app) {
                Some(jar_path) => {
                    log::info!("Starting backend: java={}, jar={}", java_path, jar_path.display());
                    match spawn_backend(&java_path, &jar_path, log_path.as_deref()) {
                        Ok(child) => {
                            let state = app.state::<BackendProcess>();
                            *state.0.lock().unwrap() = Some(child);
                            log::info!("Backend process started (pid={})",
                                state.0.lock().unwrap().as_ref().map(|c| c.id()).unwrap_or(0));
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
                // Kill the backend process on app exit (covers all close paths)
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
