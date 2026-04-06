#!/bin/bash

# iOS App Icon Generator Script
# This script generates all required iOS app icon sizes from the Android launcher icon

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🚀 iOS App Icon Generator"
echo "========================="

# Define paths
ANDROID_ICON_PATH="../app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"
IOS_ASSETS_PATH="ExchangeConvertApp/Assets.xcassets/AppIcon.appiconset"

# Check if source icon exists
if [ ! -f "$ANDROID_ICON_PATH" ]; then
    echo -e "${RED}❌ Error: Android launcher icon not found at $ANDROID_ICON_PATH${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Found source icon: $ANDROID_ICON_PATH${NC}"

# Check if sips (macOS) or ImageMagick is available
if command -v sips &> /dev/null; then
    CONVERT_TOOL="sips"
    echo -e "${GREEN}✅ Using macOS sips for image conversion${NC}"
elif command -v convert &> /dev/null; then
    CONVERT_TOOL="imagemagick"
    echo -e "${GREEN}✅ Using ImageMagick for image conversion${NC}"
else
    echo -e "${RED}❌ Error: Neither sips nor ImageMagick found.${NC}"
    echo "Please install ImageMagick: brew install imagemagick"
    exit 1
fi

# Create temporary directory for processing
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# Function to resize icon
resize_icon() {
    local size=$1
    local output=$2
    
    if [ "$CONVERT_TOOL" = "sips" ]; then
        sips -z "$size" "$size" "$ANDROID_ICON_PATH" --out "$output" 2>/dev/null
    else
        convert "$ANDROID_ICON_PATH" -resize "${size}x${size}" "$output" 2>/dev/null
    fi
}

echo ""
echo "📐 Generating icon sizes..."
echo "---------------------------"

# iOS Icon Sizes Reference:
# iPhone:
# - 20x20 @2x, @3x (Notification)
# - 29x29 @2x, @3x (Settings)
# - 40x40 @2x, @3x (Spotlight)
# - 60x60 @2x, @3x (App Icon)
# iPad:
# - 20x20 @1x, @2x (Notification)
# - 29x29 @1x, @2x (Settings)
# - 40x40 @1x, @2x (Spotlight)
# - 76x76 @1x, @2x (App Icon)
# - 83.5x83.5 @2x (iPad Pro)
# App Store:
# - 1024x1024 (App Store)

# Define all required sizes
declare -a SIZES=(
    "20"
    "29"
    "40"
    "60"
    "76"
    "83"
    "1024"
)

# Generate base sizes
for size in "${SIZES[@]}"; do
    output_file="$TEMP_DIR/icon-${size}.png"
    echo "  Generating ${size}x${size}..."
    resize_icon "$size" "$output_file"
done

echo ""
echo "💾 Copying icons to Assets.xcassets..."
echo "--------------------------------------"

# iPhone icons
cp "$TEMP_DIR/icon-20.png" "$IOS_ASSETS_PATH/icon-20@2x.png"
resize_icon "40" "$IOS_ASSETS_PATH/icon-20@3x.png"  # 20x20@3x = 60x60
cp "$TEMP_DIR/icon-20.png" "$IOS_ASSETS_PATH/icon-20@2x-1.png"

cp "$TEMP_DIR/icon-29.png" "$IOS_ASSETS_PATH/icon-29@2x.png"
resize_icon "87" "$IOS_ASSETS_PATH/icon-29@3x.png"  # 29x29@3x = 87x87

resize_icon "80" "$IOS_ASSETS_PATH/icon-40@2x.png"  # 40x40@2x = 80x80
resize_icon "120" "$IOS_ASSETS_PATH/icon-40@3x.png"  # 40x40@3x = 120x120

resize_icon "120" "$IOS_ASSETS_PATH/icon-60@2x.png"  # 60x60@2x = 120x120
resize_icon "180" "$IOS_ASSETS_PATH/icon-60@3x.png"  # 60x60@3x = 180x180

# iPad icons
cp "$TEMP_DIR/icon-20.png" "$IOS_ASSETS_PATH/icon-20~ipad.png"
resize_icon "40" "$IOS_ASSETS_PATH/icon-20@2x~ipad.png"  # 20x20@2x = 40x40

cp "$TEMP_DIR/icon-29.png" "$IOS_ASSETS_PATH/icon-29~ipad.png"
resize_icon "58" "$IOS_ASSETS_PATH/icon-29@2x~ipad.png"  # 29x29@2x = 58x58

resize_icon "80" "$IOS_ASSETS_PATH/icon-40~ipad.png"  # 40x40@1x = 40x40 (use 80 for better quality)
cp "$TEMP_DIR/icon-40.png" "$IOS_ASSETS_PATH/icon-40@2x~ipad.png"

cp "$TEMP_DIR/icon-76.png" "$IOS_ASSETS_PATH/icon-76~ipad.png"
resize_icon "152" "$IOS_ASSETS_PATH/icon-76@2x~ipad.png"  # 76x76@2x = 152x152

resize_icon "167" "$IOS_ASSETS_PATH/icon-83.5@2x~ipad.png"  # 83.5x83.5@2x = 167x167

# App Store icon
cp "$TEMP_DIR/icon-1024.png" "$IOS_ASSETS_PATH/icon-1024.png"

echo -e "${GREEN}✅ All icons generated successfully!${NC}"
echo ""
echo "📱 Generated icon files:"
echo "------------------------"
ls -la "$IOS_ASSETS_PATH"/*.png 2>/dev/null | awk '{print "  " $NF}'
echo ""
echo -e "${YELLOW}📝 Note: Make sure to update the Contents.json file if needed.${NC}"
echo -e "${GREEN}🎉 Done! You can now build your iOS app with the new icons.${NC}"
