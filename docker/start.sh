#!/bin/bash

JAR_PATH="/root/bmcshell/target/bmcshell-0.0.1-SNAPSHOT.jar"
PORT1=${PORT1:-8443}
PORT2=${PORT2:-}

# Ensure shared working directory exists
mkdir -p /bmcshellhome
cd /bmcshellhome

# Create detached tmux session
tmux new-session -d -s main -x 240 -y 60

if [ -n "$PORT2" ]; then
    # ── Dual instance: 2×2 grid ───────────────────────────
    # Step 1: split full window into left and right columns
    tmux split-window -h -t main:0
    # Step 2: split left column top/bottom  → pane 0 (bmcshell:P1), pane 2 (journal:P1)
    tmux split-window -v -t main:0.0
    # Step 3: split right column top/bottom → pane 1 (bmcshell:P2), pane 3 (journal:P2)
    tmux split-window -v -t main:0.1

    # Send commands in pane order
    tmux send-keys -t main:0.0 "java -Dserver.port=$PORT1 -jar $JAR_PATH" Enter
    tmux send-keys -t main:0.1 "java -Dserver.port=$PORT2 -jar $JAR_PATH" Enter
    tmux send-keys -t main:0.2 "curl -k -N https://localhost:$PORT1/sse/journal" Enter
    tmux send-keys -t main:0.3 "curl -k -N https://localhost:$PORT2/sse/journal" Enter
else
    # ── Single instance: left/right split ─────────────────
    # Pane 0 (top): bmcshell on PORT1
    tmux send-keys -t main:0.0 "java -Dserver.port=$PORT1 -jar $JAR_PATH" Enter
    # Pane 1 (bottom): SSE journal stream for PORT1
    tmux split-window -v -t main:0.0
    tmux send-keys -t main:0.1 "curl -k -N https://localhost:$PORT1/sse/journal" Enter
fi

# Focus on the first bmcshell pane
tmux select-pane -t main:0.0

tmux attach-session -t main
