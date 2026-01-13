#!/bin/sh

# Get Process Name from Service Script
# Reusable utility to determine the actual process name from a systemd service
# Usage: get_process_name_from_service <service_name> [debug_log]

# Function to get process name from service
get_process_name_from_service() {
    local service="$1"
    local debug_log="${2:-/dev/null}"
    
    echo "Attempting to find process name for service: $service" >> "$debug_log"
    
    # Priority 1: Get from currently running MainPID (most accurate)
    local main_pid=$(systemctl show "$service" -p MainPID --value 2>/dev/null)
    if [ -n "$main_pid" ] && [ "$main_pid" != "0" ]; then
        # BusyBox ps uses different format
        local proc_name=$(ps -o comm -p "$main_pid" 2>/dev/null | tail -n 1)
        if [ -n "$proc_name" ] && [ "$proc_name" != "COMMAND" ]; then
            echo "Found running process from MainPID $main_pid: $proc_name" >> "$debug_log"
            echo "$proc_name"
            return 0
        fi
    fi
    
    # Priority 2: Try to get ExecStart from service file
    local exec_start=$(systemctl cat "$service" 2>/dev/null | grep -E "^ExecStart=" | head -n 1 | sed 's/ExecStart=//' | awk '{print $1}')
    
    if [ -n "$exec_start" ]; then
        # Extract just the binary name
        local process_name=$(basename "$exec_start")
        echo "Found process from ExecStart: $process_name" >> "$debug_log"
        echo "$process_name"
        return 0
    fi
    
    # Priority 3: Try to find any process matching the service name pattern
    local service_base="${service%.service}"
    local found_pid=$(busybox pidof "$service_base" 2>/dev/null | awk '{print $1}')
    if [ -z "$found_pid" ]; then
        found_pid=$(ps | grep "$service_base" | grep -v grep | awk '{print $1}' | head -n 1)
    fi
    
    if [ -n "$found_pid" ]; then
        local proc_name=$(ps -o comm -p "$found_pid" 2>/dev/null | tail -n 1)
        if [ -n "$proc_name" ] && [ "$proc_name" != "COMMAND" ]; then
            echo "Found process by pattern match: $proc_name (PID: $found_pid)" >> "$debug_log"
            echo "$proc_name"
            return 0
        fi
    fi
    
    # Last resort: use service name without .service extension
    echo "Using fallback process name: $service_base" >> "$debug_log"
    echo "$service_base"
}

# If script is called directly (not sourced), execute the function
if [ "${0##*/}" = "get_process_name.sh" ]; then
    if [ -z "$1" ]; then
        echo "Usage: $0 <service_name> [debug_log]"
        exit 1
    fi
    get_process_name_from_service "$1" "$2"
fi

# Made with Bob
