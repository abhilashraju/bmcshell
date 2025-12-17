#!/bin/sh

# Service Resource Monitor Script (BusyBox compatible)
# Usage: service_monitor.sh <service_name> <log_file> <interval_seconds> [pid_file]

SERVICE_NAME="$1"
LOG_FILE="$2"
INTERVAL="${3:-5}"
PID_FILE="${4:-/tmp/monitor_${SERVICE_NAME}.pid}"

if [ -z "$SERVICE_NAME" ] || [ -z "$LOG_FILE" ]; then
    echo "Usage: $0 <service_name> <log_file> [interval_seconds] [pid_file]"
    exit 1
fi

# Store the monitor script PID
echo $$ > "$PID_FILE"

# Create log file with header
echo "timestamp,process_name,pid,full_command_path,threads,mem_percent,mem_vsz_kb,unknown_field,cpu_percent" > "$LOG_FILE"

# Redirect stderr to a debug log for troubleshooting
DEBUG_LOG="/tmp/monitor_debug_$(basename $PID_FILE .pid).log"
exec 2>> "$DEBUG_LOG"

echo "Starting monitoring for service: $SERVICE_NAME" >> "$DEBUG_LOG"
echo "Logging to: $LOG_FILE" >> "$DEBUG_LOG"
echo "PID file: $PID_FILE" >> "$DEBUG_LOG"
echo "Interval: ${INTERVAL}s" >> "$DEBUG_LOG"
echo "Monitor PID: $$" >> "$DEBUG_LOG"

# Function to get process name from service
get_process_name_from_service() {
    local service="$1"
    
    echo "Attempting to find process name for service: $service" >> "$DEBUG_LOG"
    
    # Priority 1: Get from currently running MainPID (most accurate)
    local main_pid=$(systemctl show "$service" -p MainPID --value 2>/dev/null)
    if [ -n "$main_pid" ] && [ "$main_pid" != "0" ]; then
        # BusyBox ps uses different format
        local proc_name=$(ps -o comm -p "$main_pid" 2>/dev/null | tail -n 1)
        if [ -n "$proc_name" ] && [ "$proc_name" != "COMMAND" ]; then
            echo "Found running process from MainPID $main_pid: $proc_name" >> "$DEBUG_LOG"
            echo "$proc_name"
            return 0
        fi
    fi
    
    # Priority 2: Try to get ExecStart from service file
    local exec_start=$(systemctl cat "$service" 2>/dev/null | grep -E "^ExecStart=" | head -n 1 | sed 's/ExecStart=//' | awk '{print $1}')
    
    if [ -n "$exec_start" ]; then
        # Extract just the binary name
        local process_name=$(basename "$exec_start")
        echo "Found process from ExecStart: $process_name" >> "$DEBUG_LOG"
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
            echo "Found process by pattern match: $proc_name (PID: $found_pid)" >> "$DEBUG_LOG"
            echo "$proc_name"
            return 0
        fi
    fi
    
    # Last resort: use service name without .service extension
    echo "Using fallback process name: $service_base" >> "$DEBUG_LOG"
    echo "$service_base"
}

# Get the actual process name - will be updated dynamically if service starts
PROCESS_NAME=$(get_process_name_from_service "$SERVICE_NAME")
echo "Initial process name to monitor: $PROCESS_NAME" >> "$DEBUG_LOG"

# Trap to cleanup on exit
trap 'echo "Monitor script exiting at $(date)" >> "$DEBUG_LOG"; rm -f "$PID_FILE"' EXIT INT TERM

# Verify the script is running by checking if we can write to the log
if ! touch "$LOG_FILE" 2>/dev/null; then
    echo "ERROR: Cannot write to log file: $LOG_FILE" >> "$DEBUG_LOG"
    exit 1
fi

echo "Monitor loop starting at $(date)" >> "$DEBUG_LOG"

# Function to get process stats using top (BusyBox compatible)
get_process_stats_with_top() {
    local pid="$1"
    
    # Run top in batch mode for 1 iteration, filter for our PID
    # BusyBox top format: PID USER PR NI VIRT RES SHR S %CPU %MEM TIME+ COMMAND
    top -bn 1 | grep "^[[:space:]]*$pid" 2>/dev/null | head -n 1
}

# Function to parse top output
parse_top_output() {
    local top_line="$1"
    
    if [ -z "$top_line" ]; then
        echo "0% 0 0%"
        return
    fi
    
    # Extract fields from top output
    # BusyBox top format: PID PPID USER STAT VSZ %VSZ %CPU COMMAND
    # Standard top format: PID USER PR NI VIRT RES SHR S %CPU %MEM TIME+ COMMAND
    
    # Try to detect which field contains CPU% by looking for numeric values
    # Extract all fields and find the ones that look like percentages or the CPU field
    local field_count=$(echo "$top_line" | awk '{print NF}')
    
    # BusyBox typically has fewer fields (8-9) vs standard top (12+)
    if [ "$field_count" -lt 10 ]; then
        # BusyBox format: PID PPID USER STAT VSZ %VSZ %CPU COMMAND
        local vsz=$(echo "$top_line" | awk '{print $5}')
        local mem=$(echo "$top_line" | awk '{print $6}')
        local cpu=$(echo "$top_line" | awk '{print $7}')
    else
        # Standard top format: PID USER PR NI VIRT RES SHR S %CPU %MEM TIME+ COMMAND
        local vsz=$(echo "$top_line" | awk '{print $5}')
        local cpu=$(echo "$top_line" | awk '{print $9}')
        local mem=$(echo "$top_line" | awk '{print $10}')
    fi
    
    # Convert memory sizes (may have K/M/G suffix) to KB
    vsz=$(echo "$vsz" | sed 's/[KMG]$//')
    
    # Remove any non-numeric characters except decimal point from percentages
    cpu=$(echo "$cpu" | sed 's/[^0-9.]//g')
    mem=$(echo "$mem" | sed 's/[^0-9.]//g')
    
    # Default to 0 if empty
    cpu="${cpu:-0}"
    mem="${mem:-0}"
    vsz="${vsz:-0}"
    
    # Add % suffix for percentages
    cpu="${cpu}%"
    mem="${mem}%"
    
    echo "$mem $vsz $cpu"
}

# Function to get full command path
get_full_command_path() {
    local pid="$1"
    local cmd_path=""
    
    # Try to get from /proc/pid/exe
    if [ -L "/proc/$pid/exe" ]; then
        cmd_path=$(readlink "/proc/$pid/exe" 2>/dev/null)
    fi
    
    # Fallback to cmdline if exe didn't work
    if [ -z "$cmd_path" ] && [ -f "/proc/$pid/cmdline" ]; then
        cmd_path=$(tr '\0' ' ' < "/proc/$pid/cmdline" 2>/dev/null | awk '{print $1}')
    fi
    
    # Return N/A if still empty
    if [ -z "$cmd_path" ]; then
        echo "N/A"
    else
        echo "$cmd_path"
    fi
}

# Function to get thread count from /proc
get_thread_count() {
    local pid="$1"
    
    if [ -d "/proc/$pid/task" ]; then
        ls -1 "/proc/$pid/task" 2>/dev/null | wc -l
    else
        echo "0"
    fi
}

while true; do
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Try multiple methods to find the process
    # Method 1: Get MainPID from systemd
    MAIN_PID=$(systemctl show "$SERVICE_NAME" -p MainPID --value 2>/dev/null)
    
    # Method 2: If MainPID is 0 or empty, try busybox pidof
    if [ -z "$MAIN_PID" ] || [ "$MAIN_PID" = "0" ]; then
        MAIN_PID=$(busybox pidof "$PROCESS_NAME" 2>/dev/null | awk '{print $1}')
    fi
    
    # Method 3: Try ps grep
    if [ -z "$MAIN_PID" ] || [ "$MAIN_PID" = "0" ]; then
        MAIN_PID=$(ps | grep "$PROCESS_NAME" | grep -v grep | grep -v "$0" | awk '{print $1}' | head -n 1)
    fi
    
    # Method 4: Try with service name
    if [ -z "$MAIN_PID" ] || [ "$MAIN_PID" = "0" ]; then
        local service_base="${SERVICE_NAME%.service}"
        MAIN_PID=$(busybox pidof "$service_base" 2>/dev/null | awk '{print $1}')
    fi
    
    if [ -n "$MAIN_PID" ] && [ "$MAIN_PID" != "0" ]; then
        # Get the actual command name for this PID
        ACTUAL_PROC=$(ps -o comm -p "$MAIN_PID" 2>/dev/null | tail -n 1)
        
        # Clean up the process name (remove COMMAND header if present)
        if [ "$ACTUAL_PROC" = "COMMAND" ] || [ -z "$ACTUAL_PROC" ]; then
            ACTUAL_PROC=$(cat "/proc/$MAIN_PID/comm" 2>/dev/null)
        fi
        
        # Update PROCESS_NAME if we found the actual running process
        if [ -n "$ACTUAL_PROC" ] && [ "$ACTUAL_PROC" != "$PROCESS_NAME" ]; then
            echo "Process name updated from '$PROCESS_NAME' to '$ACTUAL_PROC' (PID: $MAIN_PID)" >> "$DEBUG_LOG"
            PROCESS_NAME="$ACTUAL_PROC"
        fi
        
        # Get full command path
        FULL_CMD=$(get_full_command_path "$MAIN_PID")
        
        # Get stats using top
        TOP_OUTPUT=$(get_process_stats_with_top "$MAIN_PID")
        STATS=$(parse_top_output "$TOP_OUTPUT")
        
        if [ -n "$STATS" ]; then
            # Parse the stats: mem_percent vsz cpu_percent
            MEM=$(echo "$STATS" | awk '{print $1}')
            VSZ=$(echo "$STATS" | awk '{print $2}')
            CPU=$(echo "$STATS" | awk '{print $3}')
            THREADS=$(get_thread_count "$MAIN_PID")
            
            # Log the data in new format: timestamp,process_name,pid,full_command_path,threads,mem_percent,mem_vsz_kb,unknown_field,cpu_percent
            echo "$TIMESTAMP,${ACTUAL_PROC:-$PROCESS_NAME},$MAIN_PID,$FULL_CMD,$THREADS,$MEM,$VSZ,0,$CPU" >> "$LOG_FILE"
        else
            echo "$TIMESTAMP,$PROCESS_NAME,N/A,N/A,0,0%,0,0,0%" >> "$LOG_FILE"
        fi
    else
        # Service not running - try to update process name for next iteration
        if [ "$PROCESS_NAME" = "${SERVICE_NAME%.service}" ]; then
            # Still using fallback name, try to get better name from service definition
            NEW_NAME=$(get_process_name_from_service "$SERVICE_NAME")
            if [ -n "$NEW_NAME" ] && [ "$NEW_NAME" != "$PROCESS_NAME" ]; then
                echo "Updated process name to: $NEW_NAME (service not running yet)" >> "$DEBUG_LOG"
                PROCESS_NAME="$NEW_NAME"
            fi
        fi
        echo "$TIMESTAMP,$PROCESS_NAME,N/A,N/A,0,0%,0,0,0%" >> "$LOG_FILE"
    fi
    
    sleep "$INTERVAL"
done

# Made with Bob
