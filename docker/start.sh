#!/bin/bash

JAR_PATH="/root/bmcshell/target/bmcshell-0.0.1-SNAPSHOT.jar"
PORT1=${PORT1:-8443}
PORT2=${PORT2:-}

# Ensure shared working directory exists
mkdir -p /bmcshellhome
cd /bmcshellhome

# Create detached tmux session
tmux new-session -d -s main -x 240 -y 60

# ── Single instance layout (1 port) ──────────────────────
# Pane 0 (left): bmcshell on PORT1
tmux send-keys -t main:0.0 "java -Dserver.port=$PORT1 -jar $JAR_PATH" Enter

# Pane 1 (below pane 0): SSE journal stream for PORT1
tmux split-window -v -t main:0.0
tmux send-keys -t main:0.1 "curl -k -N https://localhost:$PORT1/sse/journal" Enter

# ── Dual instance layout (2 ports) ───────────────────────
if [ -n "$PORT2" ]; then
    # Pane 2 (top-right): split right of pane 0 (bmcshell on PORT2)
    tmux split-window -h -t main:0.0
    tmux send-keys -t main:0.2 "java -Dserver.port=$PORT2 -jar $JAR_PATH" Enter

    # Pane 3 (bottom-right): split below pane 2 (journal for PORT2)
    tmux split-window -v -t main:0.2
    tmux send-keys -t main:0.3 "curl -k -N https://localhost:$PORT2/sse/journal" Enter
fi

# Focus on the first bmcshell pane
tmux select-pane -t main:0.0

tmux attach-session -t main
