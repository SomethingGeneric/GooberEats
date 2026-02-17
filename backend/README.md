# GooberEats Backend Server
Simple Flask application that actually manages storing the data on behalf of the user(s)!

## Usage

### Running with Docker (Recommended)

The easiest way to run the backend is using Docker:

1. Clone repo
2. Copy `config.toml.sample` to `config.toml` and add your API keys (at least one of OpenAI or Anthropic is needed for AI calorie estimation; leave sections blank to disable a provider)
3. (Optional) export `GOOBEREATS_IMAGE_TAG` to pin a specific release published by CI (defaults to `latest`)
4. Start the container with Docker Compose:
   ```bash
   cd backend
   docker compose up -d
   ```

The server will be available at `http://localhost:5000` with data persisted in the `cal_data` directory.

To stop the server:
```bash
docker compose down
```

To update to the latest published image:
```bash
docker compose pull
docker compose up -d
```

> **Need to build locally instead of pulling from Docker Hub?** Use the provided `docker-compose.dev.yml`:
> ```bash
> docker compose -f docker-compose.dev.yml up -d --build
> ```
> This matches the main compose configuration but builds directly from your working tree instead of Docker Hub.

If no AI provider keys are configured, the backend continues to serve existing data endpoints, but `/api/estimate` will return a `503` response with a helpful error message until a key is supplied.

### Dependency Management

Dependencies are locked with [`uv`](https://github.com/astral-sh/uv). The Docker image installs packages directly from `uv.lock`, and the pinned `requirements.txt` is generated from the same lockfile—avoid editing either by hand.

To update dependencies:

1. Ensure `uv` is installed locally (see the uv docs for installer scripts).
2. Modify `pyproject.toml` as needed.
3. Run:
   ```bash
   uv lock
   uv export --format requirements.txt --output-file requirements.txt --locked --no-emit-project
   ```
4. Commit the updated `uv.lock` and `requirements.txt` so the Docker build picks up the changes.

## Features

### AI Calorie Estimation with Caching
The backend includes intelligent calorie estimation with automatic caching to reduce API costs:
- First request for a food item uses AI (OpenAI/Anthropic)
- Subsequent requests for the same item are served from cache
- Cache is stored persistently in SQLite database
- Normalized descriptions (case-insensitive) ensure cache hits
