#!/usr/bin/env python3
"""
Test script for calorie estimation functionality.
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from data import caldata
from research import Research


def test_fallback_estimation():
    """Test fallback calorie estimation without AI."""
    print("=== Testing Fallback Calorie Estimation ===")
    cd = caldata()
    
    test_items = [
        "apple",
        "slice of pizza", 
        "cup of rice",
        "chicken breast",
        "chocolate cake",
        "green salad",
        "banana",
        "hamburger",
        "unknown food item"
    ]
    
    for item in test_items:
        calories = cd.estimate_calories_for(item)
        print(f"{item:20} → {calories:3} calories")


def test_ai_estimation():
    """Test AI-powered calorie estimation (requires API keys)."""
    print("\n=== Testing AI Calorie Estimation ===")
    
    try:
        import toml
        config = toml.load("config.toml")
        research = Research(config["openai"]["api_key"], config["anthropic"]["api_key"])
        cd = caldata()
        
        test_items = [
            "medium apple",
            "slice of pepperoni pizza", 
            "cup of brown rice",
            "grilled chicken breast"
        ]
        
        for item in test_items:
            calories = cd.estimate_calories_for(item, research)
            print(f"{item:25} → {calories:3} calories (AI estimate)")
            
    except FileNotFoundError:
        print("No config.toml found - skipping AI tests")
        print("Create config.toml from config.toml.sample to test AI functionality")
    except Exception as e:
        print(f"Error testing AI estimation: {e}")


if __name__ == "__main__":
    test_fallback_estimation()
    test_ai_estimation()