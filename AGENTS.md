# Agent Guidelines

## Backend Python
- Use `uv` to manage dependencies. Run `uv sync --frozen --extra dev --no-install-project` to install the locked runtime and dev toolchain without packaging the app.
- Format code with `.venv/bin/black backend` (CI runs `black --check`, so commits must already be formatted).
- Lint changes with `.venv/bin/pylint backend/server.py backend/data.py backend/research.py` (update paths if you add new modules).
- Keep `uv.lock` and `requirements.txt` in sync after dependency changes by running `uv lock` followed by `uv export --format requirements.txt --output-file requirements.txt --locked --no-emit-project`.
