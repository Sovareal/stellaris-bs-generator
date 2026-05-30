# Phase 70: Dynamic Port Allocation + IPC Crash Detection

**Status:** DONE
**Session:** 52

## Tasks

1. [x] `lib.rs`: allocate free port with `TcpListener::bind("127.0.0.1:0")`
2. [x] `lib.rs`: pass `--server.port=PORT` to Java process
3. [x] `lib.rs`: expose port via `get_backend_port` Tauri command (`State<BackendPort>`)
4. [x] `lib.rs`: spawn `spawn_crash_monitor` thread using `try_wait` in 500ms loop
5. [x] `lib.rs`: emit `backend-crashed` event if child exits while `ShuttingDown` flag is false
6. [x] `lib.rs`: set `ShuttingDown` before killing child in `RunEvent::Exit`
7. [x] `api.ts`: replace hardcoded `BASE_URL` with `backendPortPromise` from `invoke("get_backend_port")`
8. [x] `useBackendReady.ts`: listen for `backend-crashed` Tauri event; update error state
9. [x] `knip.json`: remove `@tauri-apps/api` from `ignoreDependencies` (now directly imported)

## Key Decisions

- **Port state as `Mutex<u16>`**: `manage()` is called before `setup()`, so `BackendPort(Mutex::new(8080))` is registered as placeholder and updated in `setup()` after allocation.
- **`ShuttingDown(AtomicBool)`**: set to `true` in `RunEvent::Exit` before `child.kill()` so the monitor thread does not emit `backend-crashed` on intentional shutdown.
- **Monitor thread via `try_wait`**: avoids blocking the main thread; 500ms sleep interval; exits loop after detecting process end.
- **`backendPortPromise` in `api.ts`**: module-level lazy promise initialized once, cached; `request()` awaits it. Falls back to 8080 outside Tauri context (standalone `npm run dev`).
- **`isTauri` guard**: `"__TAURI__" in window` check prevents `invoke` and `listen` calls outside Tauri WebView.
- **`Emitter` trait**: must be imported explicitly -- `use tauri::Emitter` -- to call `app_handle.emit()`.
