from pathlib import Path
import sqlite3


class MonuMemory:

    def __init__(self, database_path):
        self.database_path = Path(database_path)
        self.database_path.parent.mkdir(
            parents=True,
            exist_ok=True
        )

        self._initialize()

    def _connect(self):
        return sqlite3.connect(
            self.database_path
        )

    def _initialize(self):

        con = self._connect()

        con.execute("""
        CREATE TABLE IF NOT EXISTS memories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            category TEXT NOT NULL,
            content TEXT NOT NULL,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP
        )
        """)

        con.commit()
        con.close()

    def remember(self, category, content):

        con = self._connect()

        con.execute(
            """
            INSERT INTO memories(category, content)
            VALUES (?, ?)
            """,
            (category, content)
        )

        con.commit()
        con.close()

    def recall(self, limit=20):

        con = self._connect()

        rows = con.execute(
            """
            SELECT category, content, created_at
            FROM memories
            ORDER BY id DESC
            LIMIT ?
            """,
            (limit,)
        ).fetchall()

        con.close()

        return [
            {
                "category": row[0],
                "content": row[1],
                "created_at": row[2]
            }
            for row in rows
        ]
