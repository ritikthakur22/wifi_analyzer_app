import os
import subprocess

# Android launcher icon dimensions
densities = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

res_dir = 'app/src/main/res'
svg_source = 'brand/production/wifi-analyzer-app-icon.svg'

for folder, size in densities.items():
    target_dir = os.path.join(res_dir, folder)
    os.makedirs(target_dir, exist_ok=True)
    target_file = os.path.join(target_dir, 'ic_launcher.png')
    target_round = os.path.join(target_dir, 'ic_launcher_round.png')
    # Render using rsvg-convert
    subprocess.run(['rsvg-convert', '-w', str(size), '-h', str(size), svg_source, '-o', target_file], check=True)
    subprocess.run(['rsvg-convert', '-w', str(size), '-h', str(size), svg_source, '-o', target_round], check=True)
    print(f"Generated {folder} ({size}x{size})")

