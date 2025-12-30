#!/bin/bash

# Mock External Server Startup Script
# Starts the Spring Boot Mock External Authentication Server

echo "========================================"
echo "🚀 Starting Mock External Server"
echo "========================================"
echo ""
echo "📍 Server: http://localhost:8888"
echo "📋 Health Check: http://localhost:8888/health"
echo ""

# Change to mock-external-server directory
cd "$(dirname "$0")"

# Check if the JAR file exists
if [ ! -f "target/mock-external-server-1.0.0.jar" ]; then
    echo "⚠️  JAR file not found. Building project..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "❌ Build failed!"
        exit 1
    fi
    echo "✅ Build successful!"
    echo ""
fi

# Start the server
echo "🔄 Starting server..."
echo ""
mvn spring-boot:run

