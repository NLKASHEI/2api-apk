"""Chaquopy bridge: starts the pinned original FastAPI adapter in-process.

No credentials are embedded in the APK. Only loopback is exposed.
"""
import os
import threading
from pathlib import Path

_server = None
_lock = threading.Lock()


def start(files_dir: str, api_key: str, host: str = "127.0.0.1", port: int = 8050):
    global _server
    root = Path(files_dir)
    state = root / "state"
    state.mkdir(parents=True, exist_ok=True)
    os.chdir(root)
    # IMPORTANT: set before loading upstream config.py
    os.environ["API_KEY"] = api_key
    os.environ["STATE_DIR"] = str(state)
    os.environ.setdefault("PYTHONUNBUFFERED", "1")
    import uvicorn
    config = uvicorn.Config("main:app", host=host, port=int(port),
                            log_level="info", access_log=False)
    server = uvicorn.Server(config)
    with _lock:
        _server = server
    try:
        server.run()
    finally:
        with _lock:
            if _server is server:
                _server = None


def stop():
    with _lock:
        server = _server
    if server:
        server.should_exit = True
