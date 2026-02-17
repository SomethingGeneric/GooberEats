# GooberEats
Minimal calorie tracking solution with AI-powered calorie estimation

## Features

- **Calorie Tracking**: Log food items and their calorie counts
- **AI Calorie Estimation**: Get intelligent calorie estimates for food items using AI with automatic caching
- **Android App**: Mobile app for on-the-go calorie tracking
- **Backend API**: RESTful API powering every client
- **Docker Support**: Easy deployment with Docker and Docker Compose

## Quick Start

### Running the Backend with Docker

The easiest way to get started:

```bash
cd backend
cp config.toml.sample config.toml
# Edit config.toml with your API keys
docker-compose up -d
```

The backend will be available at `http://localhost:5000`.

For more deployment options, see [backend/README.md](backend/README.md).

### CI/CD and DockerHub

Automatic Docker image builds are configured via GitHub Actions. To set up:
- See [docs/DOCKERHUB_SETUP.md](docs/DOCKERHUB_SETUP.md) for detailed instructions

### Building the Android APK for Testing

Generate a fresh debug APK (saved to `dist/GooberEats-debug.apk`) with:

```bash
./scripts/build-apk.sh
```

Need a fully reproducible build without the Android SDK locally? Run the Docker-based build (requires Docker + BuildKit):

```bash
./scripts/build-apk.sh --docker
```

> BuildKit must be enabled (`DOCKER_BUILDKIT=1`) for the `--output` flag to work.

Customize the Docker build by setting `DOCKER_BUILD_ARGS`. Example:

```bash
DOCKER_BUILD_ARGS="--build-arg ANDROID_PLATFORM=android-35 --build-arg ANDROID_BUILD_TOOLS=35.0.0" \
  ./scripts/build-apk.sh --docker
```

### Smoke-Testing the Android App in an Emulator

With the Android SDK + emulator tools installed (`ANDROID_SDK_ROOT` set), you can bootstrap an AVD, install the latest debug build, and launch the app automatically:

```bash
./test.sh
```

Pass `--docker-build` to build the APK inside Docker before running the emulator, or `--skip-build` if you already have an APK in `dist/`.

## AI Calorie Estimation

The system now includes AI-powered calorie estimation that can provide accurate calorie estimates for food items based on their description. The feature includes:

- **Smart Estimation**: Uses OpenAI/Anthropic AI models when available
- **Automatic Caching**: Repeated queries for the same food items are cached to minimize API costs
- **Fallback Logic**: Provides reasonable estimates based on food categories when AI is unavailable
- **API Endpoint**: `/api/estimate` endpoint for programmatic access

### Usage

1. Provide a food description (e.g., "apple", "slice of pizza", "chicken breast") via the Android app or a POST to `/api/estimate`.
2. The system returns an estimated calorie count in the response payload.
3. Clients can use the estimate to pre-fill calorie fields for easy logging.

### Caching Benefits

The caching system:
- Stores AI calorie estimates in SQLite database
- Serves cached results instantly without API calls
- Normalizes descriptions for consistent cache hits (case-insensitive)
- Significantly reduces API costs for repeated queries
