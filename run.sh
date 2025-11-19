#!/bin/sh
if [ -z "$1" ]; then
  echo "Usage: sh run.sh <legv8-binary-file>"
  exit 1
fi
java Dissasembler "$1"