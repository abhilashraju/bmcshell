#!/bin/sh
# Find I2C device by name pattern
# Usage: i2c_find_device.sh <pattern>

if [ -z "$1" ]; then
    echo "Usage: $0 <pattern>"
    exit 1
fi

pattern="$1"

cd /sys/bus/i2c/devices || exit 1

found=0
for dev in *-*; do
    [ -d "$dev" ] || continue
    
    if [ -f "$dev/name" ]; then
        name=$(cat "$dev/name")
        
        # Case-insensitive match
        if echo "$name" | grep -qi "$pattern"; then
            found=1
            bus=$(echo "$dev" | cut -d- -f1)
            addr=$(echo "$dev" | cut -d- -f2)
            
            echo "Found: $dev"
            echo "  Bus: $bus"
            echo "  Address: 0x$addr"
            echo "  Name: $name"
            echo "  Path: /sys/bus/i2c/devices/$dev"
            
            if [ -L "$dev/driver" ]; then
                driver_path=$(readlink "$dev/driver" 2>/dev/null)
                if [ -n "$driver_path" ]; then
                    echo "  Driver: $(basename "$driver_path")"
                fi
            fi
            
            echo
        fi
    fi
done

if [ $found -eq 0 ]; then
    echo "No devices found matching pattern: $pattern"
fi


