#!/bin/bash

echo "Deploying AI Secure Identity Verifier Backend..."

# Navigate to backend directory
cd ./backend

# Build the project
echo "Building the project..."
./mvnw clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo "Build successful!"

# Check if AWS credentials are set
if [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
    echo "Warning: AWS credentials not found in environment variables."
    echo "Please set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY before running the application."
    echo "Alternatively, ensure you have IAM roles configured if running on AWS infrastructure."
fi

# Run the application
echo "Starting the application..."
java -jar target/identity-verifier-0.0.1-SNAPSHOT.jar --spring.profiles.active=aws

echo "Backend deployment completed!"