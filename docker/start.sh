#!/bin/bash

# The directory to check for in the root directory
BMSHELL_DIR="/bmcshellhome"

# The location of your JAR file
JAR_PATH="/root/bmcshell/target/bmcshell-0.0.1-SNAPSHOT.jar"

# Check if the directory exists. The -d flag tests for a directory.
if [ -d "$BMSHELL_DIR" ]; then
    echo "Directory '$BMSHELL_DIR' exists. Changing directory..."
    cd "$BMSHELL_DIR"
else
    echo "Directory '$BMSHELL_DIR' not found. Creating and changing directory..."
    mkdir -p "$BMSHELL_DIR" # -p creates parent directories if needed.
    cd "$BMSHELL_DIR"
fi

# Run the Java application from the new working directory
echo "Executing Java application..."
ollama serve &
java -jar "$JAR_PATH"
bash

