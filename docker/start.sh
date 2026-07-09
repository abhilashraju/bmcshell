#!/bin/bash

JAR_PATH="/root/bmcshell/target/bmcshell-0.0.1-SNAPSHOT.jar"
PORT=${PORT1:-8443}

# Ensure shared working directory exists
mkdir -p /bmcshellhome
cd /bmcshellhome

exec java -Dserver.port=$PORT -jar $JAR_PATH
