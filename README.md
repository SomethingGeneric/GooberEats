# GooberEats
Minimal calorie tracking solution with AI-powered calorie estimation

## Features

- **Calorie Tracking**: Log food items and their calorie counts
- **AI Calorie Estimation**: Get intelligent calorie estimates for food items using AI
- **Web Interface**: Easy-to-use web UI for managing calorie entries
- **Android App**: Mobile app for on-the-go calorie tracking
- **Backend API**: RESTful API for all functionality

## AI Calorie Estimation

The system now includes AI-powered calorie estimation that can provide accurate calorie estimates for food items based on their description. The feature includes:

- **Smart Estimation**: Uses OpenAI/Anthropic AI models when available
- **Fallback Logic**: Provides reasonable estimates based on food categories when AI is unavailable
- **Web UI Integration**: Easy-to-use "🤖 Estimate Calories" button in the web interface
- **API Endpoint**: `/api/estimate` endpoint for programmatic access

### Usage

1. Enter a food description (e.g., "apple", "slice of pizza", "chicken breast")
2. Click the "🤖 Estimate Calories" button
3. The system will provide an estimated calorie count
4. The estimate automatically fills the calories field for easy logging
