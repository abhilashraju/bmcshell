SESSION_NAME="MySession"
NUM_WINDOWS=4
if [ $1 -eq -1 ]; then
    for i in $(seq 1 $NUM_WINDOWS); do
        tmux select-pane -t "$SESSION_NAME.$((i-1))"
        tmux send-keys -t "$SESSION_NAME.$((i-1))" "$2" C-m
    done
else
    tmux select-pane -t "${SESSION_NAME}.$1"
    tmux send-keys -t "${SESSION_NAME}.$1" "$2" C-m
fi