import os
import subprocess

# Adaptive icon specifications:
# Total canvas: 108dp x 108dp
# Safe zone: center 72dp x 72dp (inner 66.6%)
# Scales for mipmaps:
# mdpi: 108px (safe: 72px)
# hdpi: 162px (safe: 108px)
# xhdpi: 216px (safe: 144px)
# xxhdpi: 324px (safe: 216px)
# xxxhdpi: 432px (safe: 288px)

scales = {
    'mipmap-mdpi': (48, 108),
    'mipmap-hdpi': (72, 162),
    'mipmap-xhdpi': (96, 216),
    'mipmap-xxhdpi': (144, 324),
    'mipmap-xxxhdpi': (192, 432),
}

res_dir = 'app/src/main/res'
svg_master = 'brand/concept-b.svg'  # Radar Aperture logo
svg_tile = 'brand/production/wifi-analyzer-app-icon.svg'

# 1. Create a foreground SVG with 108x108 viewBox and the logo centered in the 72x72 safe zone
with open(svg_master, 'r', encoding='utf-8') as f:
    inner_svg = f.read()

# Extract inner contents of svg_master (strip <svg> and </svg>)
import re
match = re.search(r'<svg[^>]*>(.*)</svg>', inner_svg, re.DOTALL)
inner_content = match.group(1) if match else inner_svg

# Center the 512x512 logo into the 108x108 adaptive canvas (scale ~ 68/512 = 0.1328, translate to center)
# In 108x108: center is 54, safe circle r=36. Logo scaled to ~68x68 gives comfortable safe margin.
scale = 70.0 / 512.0
offset = (108.0 - 512.0 * scale) / 2.0  # (108 - 70) / 2 = 19.0

foreground_svg_content = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">
  <g transform="translate({offset:.2f}, {offset:.2f}) scale({scale:.4f})">
    {inner_content}
  </g>
</svg>'''

with open('brand/adaptive_foreground.svg', 'w', encoding='utf-8') as f:
    f.write(foreground_svg_content)

# Render foreground PNGs and legacy launcher PNGs
for folder, (legacy_size, adaptive_size) in scales.items():
    folder_path = os.path.join(res_dir, folder)
    os.makedirs(folder_path, exist_ok=True)
    
    # Legacy flat icons (full tile with rounded square background)
    legacy_file = os.path.join(folder_path, 'ic_launcher.png')
    legacy_round = os.path.join(folder_path, 'ic_launcher_round.png')
    subprocess.run(['rsvg-convert', '-w', str(legacy_size), '-h', str(legacy_size), svg_tile, '-o', legacy_file], check=True)
    subprocess.run(['rsvg-convert', '-w', str(legacy_size), '-h', str(legacy_size), svg_tile, '-o', legacy_round], check=True)
    
    # Adaptive foreground (transparent background, logo in safe zone)
    fg_file = os.path.join(folder_path, 'ic_launcher_foreground.png')
    subprocess.run(['rsvg-convert', '-w', str(adaptive_size), '-h', str(adaptive_size), 'brand/adaptive_foreground.svg', '-o', fg_file], check=True)
    print(f"Generated {folder}: legacy={legacy_size}px, foreground={adaptive_size}px")

# 2. Create res/mipmap-anydpi-v26/ic_launcher.xml and ic_launcher_round.xml
anydpi_dir = os.path.join(res_dir, 'mipmap-anydpi-v26')
os.makedirs(anydpi_dir, exist_ok=True)

adaptive_xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
'''

with open(os.path.join(anydpi_dir, 'ic_launcher.xml'), 'w') as f:
    f.write(adaptive_xml)

with open(os.path.join(anydpi_dir, 'ic_launcher_round.xml'), 'w') as f:
    f.write(adaptive_xml)

# 3. Add background color to colors/values
values_dir = os.path.join(res_dir, 'values')
colors_xml_path = os.path.join(values_dir, 'colors.xml')
colors_xml = '''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#08111F</color>
</resources>
'''
with open(colors_xml_path, 'w') as f:
    f.write(colors_xml)

print("Created adaptive icon XMLs and colors.xml successfully.")
