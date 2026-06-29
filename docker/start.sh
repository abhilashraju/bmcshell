#!/bin/bash

JAR_PATH="/root/bmcshell/target/bmcshell-0.0.1-SNAPSHOT.jar"
PORT1=${PORT1:-8443}
PORT2=${PORT2:-}

# Ensure shared working directory exists
mkdir -p /bmcshellhome
cd /bmcshellhome

# Create detached tmux session
tmux new-session -d -s main -x 240 -y 60

# ── Instance 1 ────────────────────────────────────────────
# Pane 0 (top-left): bmcshell on PORT1
tmux send-keys -t main:0.0 "java -jar $JAR_PATH --server.port=$PORT1" Enter

# Pane 1 (top-right): SSE journal stream for PORT1
tmux split-window -h -t main:0.0
tmux send-keys -t main:0.1 "curl -k -N https://localhost:$PORT1/sse/journal" Enter

# ── Instance 2 (only if PORT2 is provided) ────────────────
if [ -n "$PORT2" ]; then
    # Pane 2 (bottom-left): bmcshell on PORT2
    tmux split-window -v -t main:0.0
    tmux send-keys -t main:0.2 "java -jar $JAR_PATH --server.port=$PORT2" Enter

    # Pane 3 (bottom-right): SSE journal stream for PORT2
    tmux split-window -v -t main:0.1
    tmux send-keys -t main:0.3 "curl -k -N https://localhost:$PORT2/sse/journal" Enter
fi

# Focus on the first bmcshell pane
tmux select-pane -t main:0.0

tmux attach-session -t main
