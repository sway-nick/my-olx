"""
СМАРТ Маркет • Автоматический Ежедневный Парсер (Daily Scheduler & Task Automator)
Обеспечивает автоматическое ежедневное обновление базы предложений по Одессе:
1. Запуск полного цикла парсинга DOM.ria, AUTO.ria, Prom.ua, Работники UA.
2. Автоматическая регистрация в Планировщике Задач Windows (Task Scheduler).
3. Фоновый режим демона (Daemon Mode) с 24-часовым таймером.
4. Контроль квоты Supabase Free Tier (< 500 МБ, $0/мес).
"""

import os
import sys
import time
import argparse
import subprocess
from datetime import datetime, timezone

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

import real_market_aggregator
import smart_cache_worker

TASK_NAME = "SmartMarketOdesaDailyCrawler"

def register_windows_task(run_time: str = "04:00") -> bool:
    """Registers a scheduled daily job in Windows Task Scheduler using schtasks.exe."""
    python_exe = sys.executable
    script_path = os.path.abspath(__file__)
    cmd = f'"{python_exe}" "{script_path}" --run-now --pages 6'

    # Create schtasks command
    create_cmd = [
        "schtasks", "/create",
        "/tn", TASK_NAME,
        "/tr", cmd,
        "/sc", "daily",
        "/st", run_time,
        "/f"
    ]

    try:
        proc = subprocess.run(create_cmd, capture_output=True, text=True, check=True)
        print(f"✅ [Планировщик Windows] Задача '{TASK_NAME}' успешно зарегистрирована!")
        print(f"   • Расписание: каждый день в {run_time}")
        print(f"   • Команда: {cmd}")
        return True
    except subprocess.CalledProcessError as e:
        print(f"⚠️ [Планировщик Windows] Не удалось зарегистрировать через schtasks: {e.stderr or e.stdout}")
        return False
    except Exception as e:
        print(f"⚠️ Ошибка: {e}")
        return False

def run_daily_daemon(interval_hours: int = 24, pages: int = 5):
    """Runs a continuous background loop, sleeping between cycles."""
    print("=" * 75)
    print(f"  🕒 СМАРТ Маркет • Режим фонового демона (интервал: {interval_hours} ч.)")
    print(f"  Запущен: {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S UTC')}")
    print("=" * 75)

    while True:
        try:
            print(f"\n🚀 Запуск запланированного ежедневного сбора...")
            res = real_market_aggregator.run_full_scraping_cycle(domria_pages=pages, autoria_pages=pages)
            print(f"💤 Цикл завершен. Следующий сбор через {interval_hours} часов.")
        except Exception as e:
            print(f"❌ Ошибка в цикле сбора: {e}")

        # Sleep for specified interval
        time.sleep(interval_hours * 3600)

def main():
    parser = argparse.ArgumentParser(description="СМАРТ Маркет • Ежедневный автоматический парсер (Одесса)")
    parser.add_argument("--run-now", action="store_true", help="Запустить полный цикл парсинга прямо сейчас")
    parser.add_argument("--install-task", action="store_true", help="Зарегистрировать задачу в Планировщике Windows")
    parser.add_argument("--daemon", action="store_true", help="Запустить в режиме непрерывного демона")
    parser.add_argument("--time", type=str, default="04:00", help="Время ежедневного запуска (ЧЧ:ММ), по умолчанию 04:00")
    parser.add_argument("--pages", type=int, default=5, help="Количество страниц для парсинга на каждом ресурсе")

    args = parser.parse_args()

    if args.install_task:
        register_windows_task(args.time)

    if args.daemon:
        run_daily_daemon(interval_hours=24, pages=args.pages)
    elif args.run_now or not sys.argv[1:]:
        print(f"🚀 Запуск цикла реального парсинга (глубина: {args.pages} стр.)...")
        real_market_aggregator.run_full_scraping_cycle(domria_pages=args.pages, autoria_pages=args.pages)

if __name__ == "__main__":
    main()
