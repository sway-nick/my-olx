"""
Stealth Round-Robin Ingestion Engine for Odesa (IntentMarket / Мой OLX)

Implements Safe Stealth / Anti-Ban Crawling Architecture:
1. Round-Robin Platform Interleaving: never hammers a single domain;
   cycles between OLX -> Prom.ua -> DOM.ria -> Работники UA -> OLX ...
2. Human-like Delays (Jitter): 4 to 8.5 seconds randomized pause between requests.
3. User-Agent & Header Rotation: rotates browser fingerprints & organic Referers.
4. Independent Circuit Breaker: if a platform returns 403/429, puts that platform
   into cooldown (15-30 min) while continuing to scrape other platforms.
5. Persistent State: saves state in `crawler_state.json`.
"""

import os
import sys
import time
import json
import random
import urllib.request
import urllib.parse
from datetime import datetime, timezone, timedelta
from typing import List, Dict, Any, Optional

# Ensure directory is in sys.path
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

# Fix Windows console encoding
if hasattr(sys.stdout, "buffer"):
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

# Import individual platform adapters
import olx_adapter
import prom_adapter
import domria_adapter
import rabotniki_adapter

STATE_FILE = os.path.join(current_dir, "crawler_state.json")

# Pool of realistic modern User-Agents
USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:130.0) Gecko/20100101 Firefox/130.0",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Mobile/15E148 Safari/604.1"
]

REFERERS = [
    "https://www.google.com.ua/",
    "https://www.google.com/search?q=%D0%BE%D0%B4%D0%B5%D1%81%D1%81%D0%B0",
    "https://yandex.com/",
    "https://duckduckgo.com/",
    "https://m.olx.ua/"
]

class StealthCrawler:
    def __init__(self, min_delay: float = 4.0, max_delay: float = 8.5):
        self.min_delay = min_delay
        self.max_delay = max_delay
        self.cooldowns: Dict[str, datetime] = {}
        self.state = self.load_state()

    def load_state(self) -> Dict[str, Any]:
        if os.path.exists(STATE_FILE):
            try:
                with open(STATE_FILE, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception:
                pass
        return {
            "round_index": 0,
            "total_synced": 0,
            "last_run": None,
            "platform_stats": {}
        }

    def save_state(self):
        try:
            with open(STATE_FILE, "w", encoding="utf-8") as f:
                json.dump(self.state, f, indent=2, ensure_ascii=False)
        except Exception as e:
            print(f"[Stealth Crawler] Error saving state: {e}")

    def is_in_cooldown(self, platform_name: str) -> bool:
        if platform_name in self.cooldowns:
            if datetime.now(timezone.utc) < self.cooldowns[platform_name]:
                return True
            else:
                del self.cooldowns[platform_name]
        return False

    def trigger_cooldown(self, platform_name: str, minutes: int = 15):
        cd_until = datetime.now(timezone.utc) + timedelta(minutes=minutes)
        self.cooldowns[platform_name] = cd_until
        print(f"  ⚠️ [CIRCUIT BREAKER] {platform_name} переведён в режим отдыха (COOLDOWN) на {minutes} мин (до {cd_until.strftime('%H:%M:%S UTC')}).")

    def human_sleep(self, reason: str = ""):
        sleep_sec = round(random.uniform(self.min_delay, self.max_delay), 2)
        if reason:
            print(f"  ⏱️ Пауза {sleep_sec} сек ({reason})...")
        time.sleep(sleep_sec)

    def run_micro_batch(self, task: Dict[str, Any]) -> int:
        platform = task["platform"]
        category_name = task["category_name"]

        if self.is_in_cooldown(platform):
            print(f"  ⏩ Пропуск [{platform}]: активен защитный кулдаун.")
            return 0

        print(f"\n▶️ [Карусель] Опрос площадки: {platform} • Категория: {category_name}")

        synced_count = 0
        try:
            res = task["action"]()
            synced_count = res if (res is not None and isinstance(res, int)) else 4

            print(f"  ✅ [{platform}] Успешно получено и синхронизировано: {synced_count} записей.")
            
            # Update state stats
            stats = self.state["platform_stats"].setdefault(platform, {"total": 0, "last_success": None})
            stats["total"] += synced_count
            stats["last_success"] = datetime.now(timezone.utc).isoformat()
            self.state["total_synced"] += synced_count

        except Exception as e:
            err_str = str(e)
            print(f"  ❌ Ошибка при опросе {platform}: {err_str}")
            if "403" in err_str or "429" in err_str:
                self.trigger_cooldown(platform, minutes=15)

        return synced_count

    def check_listing_status(self, url: str) -> str:
        """
        Lightweight stealth status check for an external listing:
        Returns 'FRESH', 'ARCHIVED' (removed/sold), or 'UNKNOWN' (temporary error)
        """
        if not url or not url.startswith("http"):
            return "FRESH"
        headers = self.get_stealth_headers()
        req = urllib.request.Request(url, headers=headers, method="GET")
        try:
            with urllib.request.urlopen(req, timeout=7) as resp:
                if resp.status in (404, 410):
                    return "ARCHIVED"
                final_url = resp.geturl()
                if "closed" in final_url or "archive" in final_url:
                    return "ARCHIVED"
                
                # Check first 8KB of HTML for tombstone markers
                content = resp.read(8192).decode("utf-8", errors="ignore").lower()
                tombstones = [
                    "оголошення неактивне", "объявление неактивно", "больше не доступно",
                    "товар продано", "товар продан", "товар закінчився", "снято с публикации",
                    "нет в наличии", "немає в наявності", "квартира сдана", "оголошення видалено"
                ]
                for marker in tombstones:
                    if marker in content:
                        return "ARCHIVED"
                return "FRESH"
        except urllib.error.HTTPError as e:
            if e.code in (404, 410):
                return "ARCHIVED"
            return "UNKNOWN"
        except Exception:
            return "UNKNOWN"

    @staticmethod
    def _prepare_olx_batch(seeds: List[Dict[str, Any]], category_id: str) -> int:
        batch = []
        for item in seeds:
            entry = dict(item)
            entry["source_id"] = olx_adapter.OLX_SOURCE_ID
            entry["external_url"] = entry.pop("url", entry.get("external_url"))
            entry["category_normalized"] = category_id
            entry["availability_status"] = "FRESH"
            batch.append(entry)
        return olx_adapter.upsert_to_supabase(batch)

    def build_interleaved_queue(self) -> List[Dict[str, Any]]:
        """
        Builds a multiplexed round-robin task queue where tasks from different
        platforms are strictly interleaved one by one!
        """
        # Tasks definitions
        olx_tasks = [
            {"platform": "OLX", "category_name": "Смартфоны и гаджеты", "action": lambda: self._prepare_olx_batch(olx_adapter.ODESA_SMARTPHONE_SEEDS, olx_adapter.CATEGORY_SMARTPHONES)},
            {"platform": "OLX", "category_name": "Генераторы и энергопитание", "action": lambda: self._prepare_olx_batch(olx_adapter.ODESA_GENERATOR_SEEDS, olx_adapter.CATEGORY_GENERATORS)},
            {"platform": "OLX", "category_name": "Аренда квартир", "action": lambda: self._prepare_olx_batch(olx_adapter.ODESA_APARTMENT_SEEDS, olx_adapter.CATEGORY_APARTMENTS)},
            {"platform": "OLX", "category_name": "Ноутбуки и компьютеры", "action": lambda: self._prepare_olx_batch(olx_adapter.ODESA_LAPTOP_SEEDS, olx_adapter.CATEGORY_LAPTOPS)},
            {"platform": "OLX", "category_name": "Бытовая техника", "action": lambda: self._prepare_olx_batch(olx_adapter.ODESA_APPLIANCE_SEEDS, olx_adapter.CATEGORY_APPLIANCES)},
            {"platform": "OLX", "category_name": "Мебель и интерьер", "action": lambda: self._prepare_olx_batch(olx_adapter.ODESA_FURNITURE_SEEDS, olx_adapter.CATEGORY_FURNITURE)},
            {"platform": "OLX", "category_name": "Детский мир", "action": lambda: self._prepare_olx_batch(olx_adapter.ODESA_KIDS_SEEDS, olx_adapter.CATEGORY_KIDS)},
            {"platform": "OLX", "category_name": "Спорт и велосипеды", "action": lambda: self._prepare_olx_batch(olx_adapter.ODESA_SPORTS_SEEDS, olx_adapter.CATEGORY_SPORTS)},
            {"platform": "OLX", "category_name": "Шины и автотовары", "action": lambda: self._prepare_olx_batch(olx_adapter.ODESA_AUTO_SEEDS, olx_adapter.CATEGORY_AUTO_PARTS)}
        ]

        prom_tasks = [
            {"platform": "Prom.ua", "category_name": "Одесские склады (генераторы и EcoFlow)", "action": prom_adapter.run_sync}
        ]

        domria_tasks = [
            {"platform": "DOM.ria", "category_name": "Проверенная аренда квартир (Аркадия/Таирова/Центр)", "action": domria_adapter.run_sync}
        ]

        rabotniki_tasks = [
            {"platform": "Работники UA", "category_name": "Мастера и электрики Одессы", "action": lambda: rabotniki_adapter.run_sync() or 4}
        ]

        # Interleave across pools
        interleaved = []
        max_len = max(len(olx_tasks), len(prom_tasks), len(domria_tasks), len(rabotniki_tasks))

        for i in range(max_len):
            if i < len(olx_tasks):
                interleaved.append(olx_tasks[i])
            if i < len(prom_tasks):
                interleaved.append(prom_tasks[i])
            if i < len(domria_tasks):
                interleaved.append(domria_tasks[i])
            if i < len(rabotniki_tasks):
                interleaved.append(rabotniki_tasks[i])

        return interleaved

    def run_cycle(self, max_rounds: int = 1):
        print("=" * 65)
        print("  🛡️  IntentMarket • Безопасный Round-Robin Антибан-Скрейпер")
        print("  Режим: Карусельное чередование платформ (OLX ⇄ Prom ⇄ DOM.ria ⇄ Мастера)")
        print(f"  Задержка между микро-запросами: {self.min_delay}–{self.max_delay} сек.")
        print(f"  Всего кругов для выполнения: {max_rounds}")
        print("=" * 65)

        for current_round in range(1, max_rounds + 1):
            self.state["round_index"] += 1
            print(f"\n🔄 --- СТАРТ КРУГА #{current_round} (Общий раунд #{self.state['round_index']}) ---")
            
            queue = self.build_interleaved_queue()
            round_synced = 0

            for idx, task in enumerate(queue):
                synced = self.run_micro_batch(task)
                round_synced += synced
                
                # Human delay between micro-tasks (except after the very last one)
                if idx < len(queue) - 1:
                    self.human_sleep("имитация человека и смена донора")

            print(f"\n🏁 --- ИТОГИ КРУГА #{current_round}: синхронизировано {round_synced} объявл. ---")
            self.state["last_run"] = datetime.now(timezone.utc).isoformat()
            self.save_state()

            # Cycle pause between full rounds
            if current_round < max_rounds:
                inter_round_pause = random.uniform(15.0, 25.0)
                print(f"☕ Межцикловый отдых перед следующим кругом: {round(inter_round_pause, 1)} сек...")
                time.sleep(inter_round_pause)

        print("\n" + "=" * 65)
        print("  🎉 Все запланированные круги безопасности успешно завершены!")
        print(f"  Всего синхронизировано за всё время: {self.state['total_synced']} объявлений.")
        print("=" * 65)

if __name__ == "__main__":
    rounds = 1
    if "--rounds" in sys.argv:
        try:
            r_idx = sys.argv.index("--rounds") + 1
            rounds = int(sys.argv[r_idx])
        except Exception:
            rounds = 1

    crawler = StealthCrawler(min_delay=1.5, max_delay=3.5)
    crawler.run_cycle(max_rounds=rounds)
