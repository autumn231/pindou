#!/usr/bin/env python3
# 从 maxcleme/beadcolors (MIT) 拉取色卡 CSV 并转为 JSON
import csv
import json
import os
import urllib.request

BASE = 'https://raw.githubusercontent.com/maxcleme/beadcolors/master/raw/'
BRANDS = [
    ('artkal_c', 'artkal_c.csv', 'Artkal C', '2.6mm'),
    ('artkal_s', 'artkal_s.csv', 'Artkal S', '5mm'),
    ('mard', 'mard.csv', 'MARD', '5mm'),
]
OUT_DIR = 'app/src/main/assets/palettes'


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for key, fname, brand, spec in BRANDS:
        url = BASE + fname
        print(f'Downloading {url}')
        resp = urllib.request.urlopen(url, timeout=30)
        text = resp.read().decode('utf-8')
        reader = csv.reader(text.splitlines())
        colors = []
        for row in reader:
            if len(row) < 5:
                continue
            try:
                code = row[0].strip()
                name = row[1].strip()
                r = int(row[2])
                g = int(row[3])
                b = int(row[4])
            except (ValueError, IndexError):
                continue
            if not code:
                continue
            colors.append({
                'code': code,
                'name': name,
                'rgb': [r, g, b],
                'effect': 'solid'
            })
        palette = {
            'brand': brand,
            'spec': spec,
            'version': '2024.1',
            'source': 'maxcleme/beadcolors (MIT)',
            'colors': colors
        }
        out_path = os.path.join(OUT_DIR, f'{key}.json')
        with open(out_path, 'w', encoding='utf-8') as f:
            json.dump(palette, f, ensure_ascii=False, indent=2)
        print(f'  -> {len(colors)} colors -> {out_path}')


if __name__ == '__main__':
    main()
