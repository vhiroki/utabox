#!/usr/bin/env python3
"""
Convert videoke_list.csv to a pre-populated Room SQLite database.
Run this script once to generate songs.db, then place it in app/src/main/assets/database/
"""

import csv
import sqlite3
import os

CSV_PATH = "/Users/vhiroki/Downloads/videoke_list.csv"
DB_PATH = "/Users/vhiroki/AndroidStudioProjects/UtaBox/app/src/main/assets/database/songs.db"

def main():
    # Remove existing database if it exists
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)

    # Create new database
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    # Create songs table matching Room entity
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS songs (
            musicId TEXT PRIMARY KEY NOT NULL,
            artista TEXT NOT NULL,
            musica TEXT NOT NULL,
            inicio TEXT NOT NULL
        )
    ''')

    # Read CSV and insert songs
    with open(CSV_PATH, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        songs = []
        for row in reader:
            songs.append((
                row['music_id'],
                row['artista'],
                row['musica'],
                row['inicio']
            ))

    cursor.executemany('''
        INSERT INTO songs (musicId, artista, musica, inicio) VALUES (?, ?, ?, ?)
    ''', songs)

    conn.commit()

    # Verify
    cursor.execute('SELECT COUNT(*) FROM songs')
    count = cursor.fetchone()[0]
    print(f"Successfully created database with {count} songs at {DB_PATH}")

    conn.close()

if __name__ == "__main__":
    main()
