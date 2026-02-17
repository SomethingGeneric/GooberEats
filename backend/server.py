# stdlib
import os
import logging

# pip
from flask import Flask, request
import toml

# local
from data import caldata
from research import ProviderNotConfiguredError, Research

# Configure logging
logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

config = toml.load("config.toml")

app = Flask(__name__)
cd = caldata()
research = Research(config)


@app.route("/api/kcount", methods=["GET", "POST"])
def deal():
    """
    Handle GET and POST requests for '/api/kcount' endpoint.

    For GET requests, retrieves the current kcount value for a given 'id' parameter.
    For POST requests, adds calories to the current kcount for a given 'id' parameter.

    Returns:
        - For GET requests: The current global kcount value as a string. (clients deal with typecasting as needed)
        - For POST requests: "Success" if the calories were added successfully, or "Invalid request body!" if the request body is invalid.
    """
    if request.method == "GET":
        id = request.args.get("id")
        value = cd.get_current_kcount(id)
        logger.info(f"GET request for id={id}: {value}")
        return str(value)
    elif request.method == "POST":
        data = request.get_json()
        if "id" in data and "kcal" in data and isinstance(data["kcal"], int):
            id = data["id"]
            newcal = data["kcal"]
            desc = "Not logged"
            if "desc" in data:
                desc = data["desc"]
            logger.info(f"POST request - Adding calories id={id} by {newcal}")
            cd.add_current_kcount(id, newcal, desc)
            return "Success"
        else:
            return "Invalid request body!"


@app.route("/api/estimate", methods=["POST"])
def estimate_calories():
    """
    Estimate calories for a food item using AI.

    Expects JSON payload:
    {
        "description": "food item description"
    }

    Returns:
    {
        "description": "food item description",
        "estimated_calories": 150
    }
    """
    data = request.get_json()
    if not data or "description" not in data:
        return {"error": "Missing 'description' field in request body"}, 400

    description = data["description"].strip()
    if not description:
        return {"error": "Description cannot be empty"}, 400

    try:
        estimated_calories = cd.estimate_calories_for(description, research)
        return {"description": description, "estimated_calories": estimated_calories}
    except ProviderNotConfiguredError:
        return {
            "error": "AI calorie estimation is not configured. Provide an API key for OpenAI or Anthropic."
        }, 503
    except Exception as e:
        logger.error(f"Error estimating calories: {e}")
        return {"error": "Failed to estimate calories"}, 500


@app.route("/api/datafor", methods=["GET"])
def get_data():
    """
    Get all entries for a given id.

    Returns:
        str: The result of calling `cd.get_all_kcal(id)`, which is JSON.
    """
    id = request.args.get("id")
    logger.info(f"GET request for all data of id={id}")
    return str(cd.get_all_kcal(id))


if __name__ == "__main__":
    app.run(host="0.0.0.0", debug=True)
