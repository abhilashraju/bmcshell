#!/bin/bash
# Script to list all I2C devices with detailed information
# Usage: i2c_devices_detailed.sh

set -e

echo "[DEBUG] Starting I2C devices detailed scan..."
echo "[DEBUG] Changing to /sys/bus/i2c/devices directory..."

cd /sys/bus/i2c/devices 2>/dev/null || {
    echo "[ERROR] Failed to access /sys/bus/i2c/devices directory"
    exit 1
}

echo "[DEBUG] Successfully changed to /sys/bus/i2c/devices"
echo "[DEBUG] Listing directory contents..."
ls -la | head -20

device_count=0
processed_count=0

echo "[DEBUG] Starting device enumeration..."

for dev in *-*; do
    echo "[DEBUG] Processing entry: $dev"
    
    # Skip if not a directory
    if [ ! -d "$dev" ]; then
        echo "[DEBUG] Skipping $dev - not a directory"
        continue
    fi
    
    device_count=$((device_count + 1))
    echo "[DEBUG] Found device directory: $dev"
    
    # Extract bus and address
    bus=$(echo "$dev" | cut -d- -f1)
    addr=$(echo "$dev" | cut -d- -f2)
    
    echo "[DEBUG] Extracted - Bus: $bus, Address: $addr"
    
    # Display device information
    echo "Device: $dev (Bus: $bus, Address: 0x$addr)"
    
    # Check and display name
    if [ -f "$dev/name" ]; then
        name_content=$(cat "$dev/name" 2>/dev/null)
        echo "  Name: $name_content"
        echo "[DEBUG] Device name: $name_content"
    else
        echo "[DEBUG] No name file found for $dev"
    fi
    
    # Check and display driver
    if [ -L "$dev/driver" ]; then
        driver_link=$(readlink "$dev/driver" 2>/dev/null)
        driver_name=$(basename "$driver_link" 2>/dev/null)
        echo "  Driver: $driver_name"
        echo "[DEBUG] Driver: $driver_name (link: $driver_link)"
    else
        echo "[DEBUG] No driver symlink found for $dev"
    fi
    
    echo
    processed_count=$((processed_count + 1))
done

echo "[DEBUG] Scan complete"
echo "[DEBUG] Total device directories found: $device_count"
echo "[DEBUG] Total devices processed: $processed_count"

if [ $device_count -eq 0 ]; then
    echo "[WARNING] No I2C device directories found matching pattern *-*"
    echo "[DEBUG] Directory listing:"
    ls -la
fi


