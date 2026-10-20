import math

def create_svg(filename, content, viewbox="0 0 512 512"):
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="{viewbox}" width="512" height="512">
  <title>WiFi Analyzer Logo Concept</title>
  {content}
</svg>'''
    with open(filename, 'w') as f:
        f.write(svg)

# Concept A: "Spectrum Beam W" - A stylized geometric W formed by 3 ascending signal wave arches converging at the base, with cyan-to-blue telemetry accents.
# Let's build filled paths with clean coordinates:
# Outer arc, middle arc, inner arc, and anchor dot. But with a central chevron cut that forms a sharp W monogram.
content_a = '''<g fill="none" stroke-linecap="round">
    <!-- Broadcast Waves forming W crest -->
    <path d="M 96 220 A 180 180 0 0 1 416 220" stroke="#0069C9" stroke-width="36" />
    <path d="M 146 270 A 120 120 0 0 1 366 270" stroke="#0088FF" stroke-width="34" />
    <path d="M 196 320 A 60 60 0 0 1 316 320" stroke="#00C9C9" stroke-width="32" />
    <circle cx="256" cy="380" r="24" fill="#00C9C9" />
    <!-- Overlaid dynamic signal telemetry needle/cross-beam -->
    <path d="M 120 380 L 256 120 L 392 380" stroke="#0052A3" stroke-width="16" stroke-linejoin="round" opacity="0.35"/>
  </g>'''
# Actually let's make Concept A pure solid filled geometry so it works effortlessly in one-colour and 16px!

# Let's design Concept A as a precision geometric glyph:
# A bold, modern monogram 'W' where each stroke is a vertical Wi-Fi signal bar that arcs outward at the top.
# Bar 1: left (low signal)
# Bar 2: center-left peak
# Bar 3: center-right peak
# Bar 4: right (low signal)
# forming both a W and a frequency spectrum curve!

# Let's test Concept A: "Spectrum W"
content_a = '''<g fill="#0069C9">
  <!-- Bar 1 (Left): X=100..144, Y=260..400 -->
  <rect x="96" y="240" width="48" height="160" rx="24" fill="#005EAA" />
  <!-- Bar 2 (Mid-Left high): X=180..224, Y=140..400 -->
  <rect x="180" y="140" width="48" height="260" rx="24" fill="#0080FF" />
  <!-- Bar 3 (Mid-Right high): X=288..332, Y=140..400 -->
  <rect x="284" y="140" width="48" height="260" rx="24" fill="#0080FF" />
  <!-- Bar 4 (Right): X=368..412, Y=260..400 -->
  <rect x="368" y="240" width="48" height="160" rx="24" fill="#005EAA" />
  <!-- Central Pulse Diamond / Diagnostic Focal Dot -->
  <circle cx="256" cy="370" r="18" fill="#00C9C9" />
  <!-- Upper Wi-Fi diagnostic arc bridging the W -->
  <path d="M 120 180 A 160 160 0 0 1 392 180" fill="none" stroke="#00C9C9" stroke-width="24" stroke-linecap="round"/>
</g>'''

create_svg("brand/concept-a.svg", content_a)
print("Created concept-a.svg")
