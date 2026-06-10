#!/bin/sh
# Map all devices on a specific I2C bus
# Usage: i2c_map_bus.sh <bus_number>

if [ -z "$1" ]; then
    echo "Usage: $0 <bus_number>"
    exit 1
fi

bus="$1"

echo "Mapping I2C bus $bus..."
echo
echo "Registered devices on bus $bus:"

cd /sys/bus/i2c/devices || exit 1

found=0
for dev in ${bus}-*; do
    [ -d "$dev" ] || continue
    
    found=1
    addr=$(echo "$dev" | cut -d- -f2)
    
    echo "  0x$addr"
    
    if [ -f "$dev/name" ]; then
        echo "    Name: $(cat "$dev/name")"
    fi
    
    if [ -L "$dev/driver" ]; then
        driver_path=$(readlink "$dev/driver" 2>/dev/null)
        if [ -n "$driver_path" ]; then
            echo "    Driver: $(basename "$driver_path")"
        fi
    fi
done

if [ $found -eq 0 ]; then
    echo "  No registered devices"
fi

echo
echo "Detected devices (i2cdetect):"
if command -v i2cdetect >/dev/null 2>&1; then
    i2cdetect -y "$bus" 2>/dev/null || echo "  i2cdetect failed"
else
    echo "  i2cdetect not available"
fi


