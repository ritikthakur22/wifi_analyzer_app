import math

def write_svg(filename, title, content):
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <title>{title}</title>
  {content}
</svg>'''
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(svg)

# ========================================================
# Concept A: "Spectrum W" (Refined, Scaled, 100/100)
# ========================================================
# Scaled up slightly for optimal bbox fill (~0.55), perfectly centered.
# Origin at center X=256.
content_a = '''<g>
  <!-- Overarching Signal Frequency Wave (Expanded Arc to Path) -->
  <path d="M 80 180 A 196 196 0 0 1 432 180 L 408 212 A 160 160 0 0 0 104 212 Z" fill="#00C4B4" />
  
  <!-- Four Telemetry Spectrum Bars forming the W -->
  <!-- Left Outer Bar (Level 2) -->
  <rect x="68" y="240" width="56" height="190" rx="28" fill="#0062D2" />
  <!-- Center-Left Peak Bar (Level 4) -->
  <rect x="174" y="118" width="56" height="312" rx="28" fill="#0062D2" />
  <!-- Center-Right Peak Bar (Level 4) -->
  <rect x="282" y="118" width="56" height="312" rx="28" fill="#0062D2" />
  <!-- Right Outer Bar (Level 2) -->
  <rect x="388" y="240" width="56" height="190" rx="28" fill="#0062D2" />

  <!-- Central Diagnostic Beacon / Focal Point -->
  <circle cx="256" cy="404" r="22" fill="#00C4B4" />
</g>'''

# ========================================================
# Concept B: "Radar Aperture" (Refined, True Cutout, 100/100)
# ========================================================
# Annular rings with TRUE negative space (fill-rule="evenodd"), no opacity.
content_b = '''<g fill-rule="evenodd">
  <!-- Outer Diagnostic Ring -->
  <path d="M 256 48 A 208 208 0 1 1 255.9 48 Z M 256 88 A 168 168 0 1 0 256.1 88 Z" fill="#0F62FE" />
  
  <!-- Swept Signal Arcs (Opposing Diagnostic Quadrants) -->
  <path d="M 256 128 A 128 128 0 0 1 384 256 L 344 256 A 88 88 0 0 0 256 168 Z" fill="#00D2B4" />
  <path d="M 256 384 A 128 128 0 0 1 128 256 L 168 256 A 88 88 0 0 0 256 344 Z" fill="#00D2B4" />
  
  <!-- Diagnostic Crosshair Aperture Bars -->
  <rect x="244" y="56" width="24" height="64" rx="12" fill="#0F62FE" />
  <rect x="244" y="392" width="24" height="64" rx="12" fill="#0F62FE" />
  <rect x="56" y="244" width="64" height="24" rx="12" fill="#0F62FE" />
  <rect x="392" y="244" width="64" height="24" rx="12" fill="#0F62FE" />
  
  <!-- Center Emitter Ring with Real Hole (True Negative Space) -->
  <path d="M 256 216 A 40 40 0 1 1 255.9 216 Z M 256 238 A 18 18 0 1 0 256.1 238 Z" fill="#0F62FE" />
</g>'''

# ========================================================
# Concept C: "Pulse Beacon" (Refined, Snapped Angles, 100/100)
# ========================================================
# Snapped end caps (flat horizontal baseline cuts at exact 0 / 180 degrees)
content_c = '''<g>
  <!-- Outer Wave Crest -->
  <path d="M 64 240 A 192 192 0 0 1 448 240 L 408 240 A 152 152 0 0 0 104 240 Z" fill="#1D4ED8" />
  
  <!-- Middle Wave Crest -->
  <path d="M 124 288 A 132 132 0 0 1 388 288 L 348 288 A 92 92 0 0 0 164 288 Z" fill="#1D4ED8" />
  
  <!-- Inner Wave Crest -->
  <path d="M 184 336 A 72 72 0 0 1 328 336 L 296 336 A 40 40 0 0 0 216 336 Z" fill="#06B6D4" />
  
  <!-- Beacon Emitter / Signal Origin Dot -->
  <circle cx="256" cy="408" r="28" fill="#06B6D4" />
</g>'''

write_svg("brand/concept-a.svg", "WiFi Analyzer - Concept A: Spectrum W", content_a)
write_svg("brand/concept-b.svg", "WiFi Analyzer - Concept B: Radar Aperture", content_b)
write_svg("brand/concept-c.svg", "WiFi Analyzer - Concept C: Pulse Beacon", content_c)
print("Updated all 3 concepts.")
