#!/bin/sh
# Show I2C address to device mapping for all buses

echo "I2C Address Map:"
echo

cd /sys/bus/i2c/devices || exit 1

for dev in *-*; do
    [ -d "$dev" ] || continue
    
    bus=$(echo "$dev" | cut -d- -f1)
    addr=$(echo "$dev" | cut -d- -f2)
    
    name="N/A"
    if [ -f "$dev/name" ]; then
        name=$(cat "$dev/name")
    fi
    
    driver="none"
    if [ -L "$dev/driver" ]; then
        driver_path=$(readlink "$dev/driver" 2>/dev/null)
        if [ -n "$driver_path" ]; then
            driver=$(basename "$driver_path")
        fi
    fi
    
    printf "Bus %-3s | Addr 0x%-4s | %-30s | Driver: %s\n" "$bus" "$addr" "$name" "$driver"
done | sort

