import hashlib
import logging
import os
import json
import sqlite3
from datetime import datetime, timezone

from research import ProviderNotConfiguredError

# Configure logging
logger = logging.getLogger(__name__)


class caldata:
    def __init__(self):
        if not os.path.exists("cal_data"):
            os.makedirs("cal_data")
        self.create_table()

    def create_table(self):
        with sqlite3.connect("cal_data/calories.db") as conn:
            c = conn.cursor()
            c.execute(
                """CREATE TABLE IF NOT EXISTS calories
                         (user_id TEXT, datestamp TEXT, calorie_count INTEGER, description TEXT, recorded_at TEXT)"""
            )
            # Create cache table for AI calorie estimations
            c.execute(
                """CREATE TABLE IF NOT EXISTS calorie_cache
                         (description_hash TEXT PRIMARY KEY, description TEXT, estimated_calories INTEGER, timestamp TEXT)"""
            )
            self._ensure_recorded_at_column(c)
            conn.commit()

    def _ensure_recorded_at_column(self, cursor: sqlite3.Cursor) -> None:
        cursor.execute("PRAGMA table_info(calories)")
        columns = {row[1] for row in cursor.fetchall()}

        if "recorded_at" in columns:
            return

        logger.info("Adding recorded_at column to calories table for timestamp support")
        cursor.execute("ALTER TABLE calories ADD COLUMN recorded_at TEXT")

        # Backfill existing rows with midnight timestamps based on their datestamp if possible
        cursor.execute(
            "SELECT rowid, datestamp FROM calories WHERE recorded_at IS NULL"
        )
        rows = cursor.fetchall()
        for rowid, datestamp in rows:
            iso_ts = self._convert_datestamp_to_iso(datestamp)
            cursor.execute(
                "UPDATE calories SET recorded_at = ? WHERE rowid = ?", (iso_ts, rowid)
            )

    def _convert_datestamp_to_iso(self, datestamp: str) -> str:
        """
        Convert legacy YYYYMMDD datestamp values to ISO-8601 timestamps.

        Existing records prior to introducing recorded_at only stored a day stamp,
        so we normalise them to midnight in the server's local time to preserve ordering.
        """
        if not datestamp:
            return datetime.now(timezone.utc).isoformat(timespec="seconds")

        datestamp = datestamp.strip()
        try:
            parsed = datetime.strptime(datestamp, "%Y%m%d")
            return parsed.isoformat()
        except ValueError:
            # Already looks like an ISO timestamp, just return original value.
            return datestamp

    def ensure_prof(self, id):
        if not os.path.exists(f"cal_data/{id}"):
            os.makedirs(f"cal_data/{id}")

    def _todaystamp(self) -> str:
        return datetime.now().strftime("%Y%m%d")

    def get_current_kcount(self, id):
        self.ensure_prof(id)
        with sqlite3.connect("cal_data/calories.db") as conn:
            c = conn.cursor()
            c.execute(
                "SELECT SUM(calorie_count) FROM calories WHERE user_id = ? AND datestamp = ?",
                (id, self._todaystamp()),
            )
            result = c.fetchone()
            return result[0] if result[0] else 0

    def add_current_kcount(self, id, nkcal, desc="Not logged"):
        self.ensure_prof(id)
        with sqlite3.connect("cal_data/calories.db") as conn:
            c = conn.cursor()
            now = datetime.now(timezone.utc)
            c.execute(
                "INSERT INTO calories (user_id, datestamp, calorie_count, description, recorded_at) VALUES (?, ?, ?, ?, ?)",
                (
                    id,
                    self._todaystamp(),
                    nkcal,
                    desc,
                    now.isoformat(timespec="seconds"),
                ),
            )
            conn.commit()

    def get_all_kcal(self, id):
        self.ensure_prof(id)
        with sqlite3.connect("cal_data/calories.db") as conn:
            c = conn.cursor()
            c.execute(
                """
                SELECT user_id, datestamp, calorie_count, description, recorded_at
                FROM calories
                WHERE user_id = ?
                ORDER BY
                    CASE WHEN recorded_at IS NULL OR recorded_at = '' THEN 1 ELSE 0 END,
                    recorded_at DESC,
                    datestamp DESC
                """,
                (id,),
            )
            result = c.fetchall()
            json_result = []
            for row in result:
                json_result.append(
                    {
                        "user_id": row[0],
                        "datestamp": row[1],
                        "calorie_count": row[2],
                        "description": row[3],
                        "recorded_at": row[4],
                    }
                )
            return json.dumps(json_result)

    def _get_cache_key(self, item_desc):
        """
        Generate a cache key for a food item description.

        Args:
            item_desc (str): Description of the food item

        Returns:
            str: Hash of the normalized description
        """
        # Normalize the description (lowercase, strip whitespace) for consistent caching
        normalized = item_desc.strip().lower()
        # MD5 is used only for cache key generation, not security
        return hashlib.md5(normalized.encode(), usedforsecurity=False).hexdigest()

    def _get_cached_estimate(self, item_desc):
        """
        Get cached calorie estimate for a food item.

        Args:
            item_desc (str): Description of the food item

        Returns:
            int or None: Cached calorie estimate if found, None otherwise
        """
        cache_key = self._get_cache_key(item_desc)
        with sqlite3.connect("cal_data/calories.db") as conn:
            c = conn.cursor()
            c.execute(
                "SELECT estimated_calories FROM calorie_cache WHERE description_hash = ?",
                (cache_key,),
            )
            result = c.fetchone()

            if result:
                logger.info(f"Cache hit for '{item_desc}': {result[0]} calories")
                return result[0]
            return None

    def _cache_estimate(self, item_desc, calories):
        """
        Cache a calorie estimate for a food item.

        Args:
            item_desc (str): Description of the food item
            calories (int): Estimated calories
        """
        cache_key = self._get_cache_key(item_desc)
        timestamp = datetime.now().isoformat()

        with sqlite3.connect("cal_data/calories.db") as conn:
            c = conn.cursor()
            # Use INSERT OR REPLACE to update existing cache entries
            c.execute(
                "INSERT OR REPLACE INTO calorie_cache (description_hash, description, estimated_calories, timestamp) VALUES (?, ?, ?, ?)",
                (cache_key, item_desc.strip().lower(), calories, timestamp),
            )
            conn.commit()
        logger.info(f"Cached estimate for '{item_desc}': {calories} calories")

    def estimate_calories_for(self, item_desc, research_client=None):
        """
        Estimate calories for a food item using AI with caching.

        Args:
            item_desc (str): Description of the food item
            research_client: Optional Research client for AI queries

        Returns:
            int: Estimated calories for the item
        """
        # Check cache first
        cached_estimate = self._get_cached_estimate(item_desc)
        if cached_estimate is not None:
            return cached_estimate

        if not research_client:
            # Fallback to reasonable defaults if no AI client available
            calories = self._get_fallback_calories(item_desc)
            # Cache fallback estimates too
            self._cache_estimate(item_desc, calories)
            return calories

        # Create a detailed prompt for calorie estimation
        prompt = f"""
        Please estimate the calories for the following food item: "{item_desc}"
        
        Consider:
        - Common serving sizes for this type of food
        - Typical preparation methods
        - Average nutritional content
        
        Respond with ONLY a number representing the estimated calories for a typical serving.
        If the description is unclear or you're unsure, provide your best reasonable estimate.
        
        Examples:
        - "apple" → 80
        - "slice of pizza" → 285
        - "cup of rice" → 205
        - "banana" → 105
        """

        try:
            # Try to get estimate from AI
            response = research_client.estimate(prompt)
        except ProviderNotConfiguredError:
            raise
        except Exception as e:
            logger.error(f"Error estimating calories with AI: {e}")
            response = ""

        # Extract numeric value from response
        import re

        numbers = re.findall(r"\d+", response)
        if numbers:
            estimated_calories = int(numbers[0])
            # Sanity check: ensure reasonable range (10-2000 calories)
            if 10 <= estimated_calories <= 2000:
                # Cache the AI estimate
                self._cache_estimate(item_desc, estimated_calories)
                return estimated_calories

        # If AI response is invalid, fall back to heuristic
        calories = self._get_fallback_calories(item_desc)
        self._cache_estimate(item_desc, calories)
        return calories

    def _get_fallback_calories(self, item_desc):
        """
        Provide fallback calorie estimates based on common food categories.

        Args:
            item_desc (str): Description of the food item

        Returns:
            int: Estimated calories
        """
        item_lower = item_desc.lower()

        # Simple heuristic based on food categories
        if any(word in item_lower for word in ["apple", "orange", "banana", "fruit"]):
            return 80
        elif any(
            word in item_lower for word in ["salad", "lettuce", "spinach", "greens"]
        ):
            return 50
        elif any(word in item_lower for word in ["pizza", "burger", "fries"]):
            return 350
        elif any(word in item_lower for word in ["rice", "pasta", "bread"]):
            return 200
        elif any(word in item_lower for word in ["chicken", "beef", "meat"]):
            return 250
        elif any(word in item_lower for word in ["donut", "cake", "cookie", "dessert"]):
            return 300
        elif any(word in item_lower for word in ["soda", "juice", "drink"]):
            return 150
        else:
            return 150  # Default estimate for unknown items
