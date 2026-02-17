#!/usr/bin/env python3
"""
Test the API endpoint for calorie estimation.
"""

import requests
import json
import time
import subprocess
import os
import signal
import threading


def start_server():
    """Start the Flask server in the background."""
    try:
        # Create minimal config for testing
        config_content = """[openai]
api_key = "dummy-key"

[anthropic]
api_key = "dummy-key"
"""
        with open("config.toml", "w") as f:
            f.write(config_content)

        # Start server
        process = subprocess.Popen(
            ["python", "server.py"], stdout=subprocess.PIPE, stderr=subprocess.PIPE
        )

        # Give server time to start
        time.sleep(3)

        return process
    except Exception as e:
        print(f"Failed to start server: {e}")
        return None


def test_estimation_endpoint():
    """Test the /api/estimate endpoint."""
    base_url = "http://127.0.0.1:5000"

    test_cases = [
        {"description": "apple"},
        {"description": "slice of pizza"},
        {"description": "cup of rice"},
        {"description": "chicken breast"},
        {"description": "chocolate cake"},
    ]

    print("=== Testing /api/estimate endpoint ===")

    for test_case in test_cases:
        try:
            response = requests.post(
                f"{base_url}/api/estimate", json=test_case, timeout=10
            )

            if response.status_code == 200:
                result = response.json()
                print(
                    f"{result['description']:20} → {result['estimated_calories']:3} calories"
                )
            else:
                print(f"Error {response.status_code}: {response.text}")

        except requests.exceptions.RequestException as e:
            print(f"Request failed for {test_case['description']}: {e}")

    # Test error cases
    print("\n=== Testing error cases ===")

    # Empty description
    try:
        response = requests.post(
            f"{base_url}/api/estimate", json={"description": ""}, timeout=10
        )
        print(f"Empty description: {response.status_code} - {response.json()}")
    except Exception as e:
        print(f"Empty description test failed: {e}")

    # Missing description
    try:
        response = requests.post(f"{base_url}/api/estimate", json={}, timeout=10)
        print(f"Missing description: {response.status_code} - {response.json()}")
    except Exception as e:
        print(f"Missing description test failed: {e}")


if __name__ == "__main__":
    os.chdir("/home/runner/work/GooberEats/GooberEats/backend")

    print("Starting server...")
    server_process = start_server()

    if server_process:
        try:
            test_estimation_endpoint()
        finally:
            print("\nShutting down server...")
            server_process.terminate()
            server_process.wait(timeout=5)

            # Clean up test config
            if os.path.exists("config.toml"):
                os.remove("config.toml")
    else:
        print("Failed to start server for testing")
