# GooberEats
Minimal calorie tracking solution with AI-powered calorie estimation

## Features

- **Calorie Tracking**: Log food items and their calorie counts
- **AI Calorie Estimation**: Get intelligent calorie estimates for food items using AI with automatic caching
- **Web Interface**: Easy-to-use web UI for managing calorie entries
- **Android App**: Mobile app for on-the-go calorie tracking
- **Backend API**: RESTful API for all functionality
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

## AI Calorie Estimation

The system now includes AI-powered calorie estimation that can provide accurate calorie estimates for food items based on their description. The feature includes:

- **Smart Estimation**: Uses OpenAI/Anthropic AI models when available
- **Automatic Caching**: Repeated queries for the same food items are cached to minimize API costs
- **Fallback Logic**: Provides reasonable estimates based on food categories when AI is unavailable
- **Web UI Integration**: Easy-to-use "🤖 Estimate Calories" button in the web interface
- **API Endpoint**: `/api/estimate` endpoint for programmatic access

### Usage

1. Enter a food description (e.g., "apple", "slice of pizza", "chicken breast")
2. Click the "🤖 Estimate Calories" button
3. The system will provide an estimated calorie count
4. The estimate automatically fills the calories field for easy logging

### Caching Benefits

The caching system:
- Stores AI calorie estimates in SQLite database
- Serves cached results instantly without API calls
- Normalizes descriptions for consistent cache hits (case-insensitive)
- Significantly reduces API costs for repeated queries

