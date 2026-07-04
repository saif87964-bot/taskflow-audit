# Generates all launcher icon densities from the IMH logo.
# Usage: powershell -File scripts\generate_icons.ps1 [path-to-logo.png]
param([string]$LogoPath = "C:\Users\saif8\Desktop\imh_logo.png")

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if (-not (Test-Path $LogoPath)) {
    Write-Error "Logo not found at $LogoPath. Save the IMH logo PNG there first."
    exit 1
}

$res = Join-Path $PSScriptRoot "..\app\src\main\res"
$logo = [System.Drawing.Image]::FromFile($LogoPath)

function New-Icon([int]$size, [double]$logoScale, [bool]$transparent, [string]$outPath) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    if (-not $transparent) { $g.Clear([System.Drawing.Color]::White) }
    else { $g.Clear([System.Drawing.Color]::Transparent) }

    # Fit logo inside logoScale * canvas, preserving aspect ratio, centered
    $target = [double]$size * $logoScale
    $ratio = [Math]::Min($target / $logo.Width, $target / $logo.Height)
    $w = [int]($logo.Width * $ratio); $h = [int]($logo.Height * $ratio)
    $x = [int](($size - $w) / 2); $y = [int](($size - $h) / 2)
    $g.DrawImage($logo, $x, $y, $w, $h)
    $g.Dispose()

    $dir = Split-Path $outPath
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force $dir | Out-Null }
    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "  + $outPath"
}

$densities = @(
    @{ name = "mdpi";    legacy = 48;  fg = 108 },
    @{ name = "hdpi";    legacy = 72;  fg = 162 },
    @{ name = "xhdpi";   legacy = 96;  fg = 216 },
    @{ name = "xxhdpi";  legacy = 144; fg = 324 },
    @{ name = "xxxhdpi"; legacy = 192; fg = 432 }
)

foreach ($d in $densities) {
    $dir = Join-Path $res ("mipmap-" + $d.name)
    # Legacy icons: logo on white, 82% of canvas
    New-Icon $d.legacy 0.82 $false (Join-Path $dir "ic_launcher.png")
    New-Icon $d.legacy 0.82 $false (Join-Path $dir "ic_launcher_round.png")
    # Adaptive foreground: transparent, logo within the 66/108 safe zone (~58%)
    New-Icon $d.fg 0.56 $true (Join-Path $dir "ic_launcher_fg.png")
}

# Adaptive icon XMLs → white background + logo foreground
$adaptive = @"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_bg" />
    <foreground android:drawable="@mipmap/ic_launcher_fg" />
</adaptive-icon>
"@
$anydpi = Join-Path $res "mipmap-anydpi-v26"
Set-Content -Path (Join-Path $anydpi "ic_launcher.xml") -Value $adaptive -Encoding utf8
Set-Content -Path (Join-Path $anydpi "ic_launcher_round.xml") -Value $adaptive -Encoding utf8

$bgColor = @"
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_bg">#FFFFFF</color>
</resources>
"@
Set-Content -Path (Join-Path $res "values\ic_launcher_bg.xml") -Value $bgColor -Encoding utf8

$logo.Dispose()
Write-Host "Done. Launcher icons generated from $LogoPath"
