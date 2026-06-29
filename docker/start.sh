#!/bin/bash

JAR_PATH="/root/bmcshell/target/bmcshell-0.0.1-SNAPSHOT.jar"
PORT1=${PORT1:-8443}
PORT2=${PORT2:-}

# journal_cmd PORT  — inline script sent to each journal pane
journal_cmd() {
    local p=$1
    echo "echo 'Waiting for bmcshell on port $p...'; until curl -sk --max-time 2 https://localhost:$p/sse/journal >/dev/null 2>&1; do sleep 2; done; echo 'Streaming...'; while true; do curl -k -N https://localhost:$p/sse/journal; echo '[disconnected - retrying in 3s]'; sleep 3; done"
}

# Ensure shared working directory exists
mkdir -p /bmcshellhome
cd /bmcshellhome

if [ -n "$PORT2" ]; then
    # ── Dual instance: 2×2 grid ───────────────────────────
    # Build layout step by step, capturing each new pane's ID explicitly
    # so numbering surprises don't affect targeting.

    # Create session — initial pane is the left-top bmcshell:P1
    tmux new-session -d -s main -x 240 -y 60
    P_BM1=$(tmux display-message -p -t main:0.0 '#{pane_id}')

    # Split right of P_BM1 → right-top bmcshell:P2
    tmux split-window -h -t "$P_BM1"
    P_BM2=$(tmux display-message -p -t main:0 '#{pane_id}')

    # Split below P_BM1 → left-bottom journal:P1
    tmux split-window -v -t "$P_BM1"
    P_J1=$(tmux display-message -p -t main:0 '#{pane_id}')

    # Split below P_BM2 → right-bottom journal:P2
    tmux split-window -v -t "$P_BM2"
    P_J2=$(tmux display-message -p -t main:0 '#{pane_id}')

    # Send commands to the captured pane IDs — no numbering ambiguity
    tmux send-keys -t "$P_BM1" "java -Dserver.port=$PORT1 -jar $JAR_PATH" Enter
    tmux send-keys -t "$P_BM2" "java -Dserver.port=$PORT2 -jar $JAR_PATH" Enter
    tmux send-keys -t "$P_J1"  "bash -c \"$(journal_cmd $PORT1)\"" Enter
    tmux send-keys -t "$P_J2"  "bash -c \"$(journal_cmd $PORT2)\"" Enter

    # Apply tiled layout so all 4 panes get equal space
    tmux select-layout -t main:0 tiled

    # Focus on the first bmcshell pane
    tmux select-pane -t "$P_BM1"
else
    # ── Single instance: top/bottom split ─────────────────
    tmux new-session -d -s main -x 240 -y 60
    P_BM1=$(tmux display-message -p -t main:0.0 '#{pane_id}')

    tmux send-keys -t "$P_BM1" "java -Dserver.port=$PORT1 -jar $JAR_PATH" Enter

    tmux split-window -v -t "$P_BM1"
    P_J1=$(tmux display-message -p -t main:0 '#{pane_id}')
    tmux send-keys -t "$P_J1" "bash -c \"$(journal_cmd $PORT1)\"" Enter

    tmux select-pane -t "$P_BM1"
fi

tmux attach-session -t main
