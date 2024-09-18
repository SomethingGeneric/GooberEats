import sqlite3, os, json
from datetime import datetime


class caldata:
    def __init__(self):
        if not os.path.exists("cal_data"):
            os.makedirs("cal_data")
        self.todaystamp = str(datetime.now().strftime("%Y%m%d"))
        self.create_table()

    def create_table(self):
        conn = sqlite3.connect("cal_data/calories.db")
        c = conn.cursor()
        c.execute(
            """CREATE TABLE IF NOT EXISTS calories
                     (user_id TEXT, datestamp TEXT, calorie_count INTEGER, description TEXT)"""
        )
        conn.commit()
        conn.close()

    def ensure_prof(self, id):
        if not os.path.exists(f"cal_data/{id}"):
            os.makedirs(f"cal_data/{id}")

    def get_current_kcount(self, id):
        self.ensure_prof(id)
        conn = sqlite3.connect("cal_data/calories.db")
        c = conn.cursor()
        c.execute(
            "SELECT SUM(calorie_count) FROM calories WHERE user_id = ? AND datestamp = ?",
            (id, self.todaystamp),
        )
        result = c.fetchone()
        conn.close()
        return result[0] if result[0] else 0

    def add_current_kcount(self, id, nkcal, desc="Not logged"):
        self.ensure_prof(id)
        conn = sqlite3.connect("cal_data/calories.db")
        c = conn.cursor()
        c.execute(
            "INSERT INTO calories VALUES (?, ?, ?, ?)",
            (id, self.todaystamp, nkcal, desc),
        )
        conn.commit()
        conn.close()

    def get_all_kcal(self, id):
        self.ensure_prof(id)
        conn = sqlite3.connect("cal_data/calories.db")
        c = conn.cursor()
        c.execute("SELECT * FROM calories WHERE user_id = ?", (id,))
        result = c.fetchall()
        conn.close()
        json_result = []
        for row in result:
            json_result.append(
                {
                    "user_id": row[0],
                    "datestamp": row[1],
                    "calorie_count": row[2],
                    "description": row[3],
                }
            )
        return json.dumps(json_result)

    def estimate_calories_for(self, item_desc):
        # TODO: Implement web search or generative AI integration
        # For now, return a placeholder value
        return 100  # Placeholder: assumes 100 calories for any item
