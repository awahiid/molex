#!/usr/bin/env bash
mvn clean package

TARGET_DIR="/usr/local/bin"
COMMAND_NAME="molex"

sudo cp molex.sh "$TARGET_DIR/$COMMAND_NAME"
sudo chmod +x "$TARGET_DIR/$COMMAND_NAME"

echo "Molex ready. Run with: $COMMAND_NAME"