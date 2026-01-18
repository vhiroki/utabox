#!/bin/bash

# UtaBox APK Build Script
# This script builds the debug APK for the UtaBox Karaoke app

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}  UtaBox APK Build Script${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# Check if gradlew exists and is executable
if [ ! -f "./gradlew" ]; then
    echo -e "${RED}Error: gradlew not found in project root${NC}"
    exit 1
fi

if [ ! -x "./gradlew" ]; then
    echo -e "${YELLOW}Making gradlew executable...${NC}"
    chmod +x ./gradlew
fi

# Parse arguments
CLEAN_BUILD=false
RELEASE_BUILD=false
INSTALL_APK=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --clean|-c)
            CLEAN_BUILD=true
            shift
            ;;
        --release|-r)
            RELEASE_BUILD=true
            shift
            ;;
        --install|-i)
            INSTALL_APK=true
            shift
            ;;
        --help|-h)
            echo "Usage: ./build-apk.sh [options]"
            echo ""
            echo "Options:"
            echo "  -c, --clean    Clean build (removes previous build artifacts)"
            echo "  -r, --release  Build release APK (requires signing config)"
            echo "  -i, --install  Install APK on connected device after build"
            echo "  -h, --help     Show this help message"
            echo ""
            echo "Examples:"
            echo "  ./build-apk.sh              # Build debug APK"
            echo "  ./build-apk.sh --clean      # Clean and build debug APK"
            echo "  ./build-apk.sh -c -i        # Clean build and install"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Determine build type
if [ "$RELEASE_BUILD" = true ]; then
    BUILD_TYPE="Release"
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
    ASSEMBLE_TASK="assembleRelease"
    INSTALL_TASK="installRelease"
else
    BUILD_TYPE="Debug"
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    ASSEMBLE_TASK="assembleDebug"
    INSTALL_TASK="installDebug"
fi

echo -e "Build type: ${GREEN}$BUILD_TYPE${NC}"
echo ""

# Clean if requested
if [ "$CLEAN_BUILD" = true ]; then
    echo -e "${YELLOW}Cleaning previous build...${NC}"
    ./gradlew clean
    echo -e "${GREEN}Clean complete!${NC}"
    echo ""
fi

# Build the APK
echo -e "${YELLOW}Building $BUILD_TYPE APK...${NC}"
./gradlew $ASSEMBLE_TASK

# Check if build was successful
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  Build Successful!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "APK location: ${YELLOW}$APK_PATH${NC}"

    # Show APK file size
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        echo -e "APK size: ${YELLOW}$APK_SIZE${NC}"
    fi
    echo ""

    # Install if requested
    if [ "$INSTALL_APK" = true ]; then
        echo -e "${YELLOW}Installing APK on connected device...${NC}"
        ./gradlew $INSTALL_TASK
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}Installation complete!${NC}"
        else
            echo -e "${RED}Installation failed. Make sure a device is connected.${NC}"
            exit 1
        fi
    fi
else
    echo ""
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}  Build Failed!${NC}"
    echo -e "${RED}========================================${NC}"
    exit 1
fi
