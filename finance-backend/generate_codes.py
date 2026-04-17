"""
generate_codes.py — 管理员激活码生成工具
用法：
    python generate_codes.py --count 5
    python generate_codes.py --count 10 --max-uses 3 --note "beta testers"
    python generate_codes.py --count 1 --duration 30 --note "短期试用"
"""

import argparse
import sys
import os

# 确保能 import subscription.py（同目录运行）
sys.path.insert(0, os.path.dirname(__file__))

from subscription import init_db, create_codes

def main():
    parser = argparse.ArgumentParser(description="Generate SmartSpend activation codes")
    parser.add_argument("--count",     type=int, default=1,   help="Number of codes to generate (default: 1)")
    parser.add_argument("--max-uses",  type=int, default=1,   help="Max uses per code (default: 1, use >1 for test codes)")
    parser.add_argument("--duration",  type=int, default=90,  help="Pro duration in days (default: 90)")
    parser.add_argument("--note",      type=str, default="",  help="Optional note for this batch")
    args = parser.parse_args()

    init_db()
    codes = create_codes(
        count=args.count,
        max_uses=args.max_uses,
        duration_days=args.duration,
        note=args.note,
    )

    print(f"\n✅ Generated {len(codes)} code(s) "
          f"[max_uses={args.max_uses}, duration={args.duration}d, note='{args.note}']:\n")
    for code in codes:
        print(f"  {code}")
    print()

if __name__ == "__main__":
    main()
