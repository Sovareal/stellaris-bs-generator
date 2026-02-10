use std::process::{Child, Command};
use std::sync::Mutex;
use tauri::Manager;

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

fn spawn_backend(jar_path: &std::path::Path) -> Result<Child, String> {
    Command::new("java")
        .args(["-jar", &jar_path.to_string_lossy()])
        .spawn()
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
            match find_backend_jar(app) {
                Some(jar_path) => {
                    log::info!("Starting backend from: {}", jar_path.display());
                    match spawn_backend(&jar_path) {
                        Ok(child) => {
                            let state = app.state::<BackendProcess>();
                            *state.0.lock().unwrap() = Some(child);
                            log::info!("Backend process started");
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
        .on_window_event(|window, event| {
            if let tauri::WindowEvent::Destroyed = event {
                // Kill the backend process when the window closes
                if let Some(state) = window.try_state::<BackendProcess>() {
                    if let Ok(mut guard) = state.0.lock() {
                        if let Some(ref mut child) = *guard {
                            let _ = child.kill();
                            log::info!("Backend process terminated");
                        }
                    }
                }
            }
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
