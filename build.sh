#!/bin/bash

# Script to build and run the Docker container for compiling SimpleItemGenerator with shadowJar

IMAGE_NAME="simpleitemgenerator-builder"
OUTPUT_DIR="./docker-output"

echo "Building Docker image: $IMAGE_NAME"
docker build -t $IMAGE_NAME .

if [ $? -ne 0 ]; then
    echo "Failed to build Docker image"
    exit 1
fi

echo "Creating output directory: $OUTPUT_DIR"
mkdir -p $OUTPUT_DIR

echo "Running Docker container to build the project"
docker run --rm -v "$(pwd)/$OUTPUT_DIR:/app/build/libs" $IMAGE_NAME

if [ $? -eq 0 ]; then
    echo "Build completed successfully!"
    echo "Check the $OUTPUT_DIR directory for the built JAR files."
else
    echo "Build failed!"
    exit 1
fi
