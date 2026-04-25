import sqlite3
from datetime import datetime, timezone

import firebase_admin
from firebase_admin import credentials, firestore

DB_PATH = "/home/azureuser/finance-backend/subscriptions.db"
SERVICE_ACCOUNT = "/home/azureuser/finance-backend/firebase-service-account.json"
UIDS = [
    "u5Z7g06lSyapkVKRQyfFax6Wpvj2",  # 你自己
    "NqoKSJLPMEe76BPOymq9tOQPzRu1",
    "0IfYmz2E80btIva43uoFqhG7zss2",
]

cred = credentials.Certificate(SERVICE_ACCOUNT)
if not firebase_admin._apps:
    firebase_admin.initialize_app(cred)
db = firestore.client()


def conn():
    c = sqlite3.connect(DB_PATH)
    c.row_factory = sqlite3.Row
    return c


def migrate(UID):
    now_str = datetime.now(timezone.utc).isoformat()
    today = datetime.now(timezone.utc).strftime("%Y-%m-%d")

    print("Migrating check_ins...")
    check_ins = db.collection("users").document(UID).collection("check_ins").get()
    migrated_checkins = 0
    with conn() as c:
        for doc in check_ins:
            data = doc.to_dict()
            date = data.get("date", doc.id)
            if date == today:
                print(f"  Skipping today ({today})")
                continue
            streak = data.get("streak", 1)
            tokens = data.get("tokensEarned", 1)
            bonus = max(0, tokens - 1)
            try:
                c.execute(
                    "INSERT OR IGNORE INTO check_in_records (uid, date, streak, bonus, created_at) VALUES (?, ?, ?, ?, ?)",
                    (UID, date, streak, bonus, now_str),
                )
                migrated_checkins += 1
            except Exception as e:
                print(f"  Skip {date}: {e}")
    print(f"  Done: {migrated_checkins} check_ins migrated")

    print("Migrating achievements...")
    achievements = db.collection("users").document(UID).collection("achievements").get()
    migrated_achievements = 0
    with conn() as c:
        for doc in achievements:
            data = doc.to_dict()
            achievement_id = data.get("achievementId", doc.id)
            is_unlocked = data.get("isUnlocked", False)
            if not is_unlocked:
                continue
            unlocked_at = data.get("unlockedAt", now_str)
            if isinstance(unlocked_at, int):
                unlocked_at = datetime.fromtimestamp(
                    unlocked_at / 1000, tz=timezone.utc
                ).isoformat()
            try:
                c.execute(
                    "INSERT OR IGNORE INTO achievements (uid, achievement_id, unlocked_at) VALUES (?, ?, ?)",
                    (UID, achievement_id, str(unlocked_at)),
                )
                migrated_achievements += 1
            except Exception as e:
                print(f"  Skip {achievement_id}: {e}")
    print(f"  Done: {migrated_achievements} achievements migrated")

    print("Calculating token balance from token_transactions...")
    transactions = (
        db.collection("users").document(UID).collection("token_transactions").get()
    )
    historical_balance = 0
    for doc in transactions:
        data = doc.to_dict()
        amount = data.get("amount", 0)
        if isinstance(amount, (int, float)):
            historical_balance += int(amount)
    print(f"  Historical balance from Firestore: {historical_balance}")

    with conn() as c:
        row = c.execute(
            "SELECT balance FROM token_balances WHERE uid = ?", (UID,)
        ).fetchone()
        current_backend_balance = row["balance"] if row else 0
    print(f"  Current backend balance: {current_backend_balance}")

    total_balance = historical_balance + current_backend_balance
    print(f"  Total balance to set: {total_balance}")

    with conn() as c:
        c.execute(
            """
            INSERT INTO token_balances (uid, balance, updated_at)
            VALUES (?, ?, ?)
            ON CONFLICT(uid) DO UPDATE SET
                balance = excluded.balance,
                updated_at = excluded.updated_at
            """,
            (UID, total_balance, now_str),
        )
    print(f"  Done: token_balance set to {total_balance}")

    print("\n=== Migration Summary ===")
    with conn() as c:
        checkin_count = c.execute(
            "SELECT COUNT(*) as n FROM check_in_records WHERE uid = ?", (UID,)
        ).fetchone()["n"]
        achievement_count = c.execute(
            "SELECT COUNT(*) as n FROM achievements WHERE uid = ?", (UID,)
        ).fetchone()["n"]
        balance = c.execute(
            "SELECT balance FROM token_balances WHERE uid = ?", (UID,)
        ).fetchone()
        print(f"check_in_records: {checkin_count} rows")
        print(f"achievements:     {achievement_count} rows")
        print(f"token_balance:    {balance['balance'] if balance else 0}")


if __name__ == "__main__":
    for uid in UIDS:
        print(f"\n{'=' * 50}")
        print(f"Migrating UID: {uid}")
        print("=" * 50)
        migrate(uid)
