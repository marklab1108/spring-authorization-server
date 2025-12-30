#!/bin/bash

# Spring Authorization Server Startup Script
# Starts the Spring Boot OAuth2 Authorization Server

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Change to project root directory
cd "$(dirname "$0")"

# Configuration
JAR_NAME="demo-0.0.1-SNAPSHOT.jar"
JAR_PATH="target/${JAR_NAME}"
SERVER_PORT="${SERVER_PORT:-9000}"

# Print banner
echo ""
echo -e "${BLUE}========================================"
echo "🔐 Spring Authorization Server"
echo -e "========================================${NC}"
echo ""

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -d, --dev       Run in development mode (mvn spring-boot:run)"
    echo "  -j, --jar       Run from JAR file (default)"
    echo "  -b, --build     Force rebuild before running"
    echo "  -h, --help      Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  SERVER_PORT                    Server port (default: 9000)"
    echo "  SPRING_DATASOURCE_URL          PostgreSQL URL"
    echo "  SPRING_DATASOURCE_USERNAME     Database username"
    echo "  SPRING_DATASOURCE_PASSWORD     Database password"
    echo "  AUTHORIZATION_SERVER_ISSUER    OAuth2 issuer URL"
    echo "  EXTERNAL_AUTH_SERVER_URL       External auth server URL"
    echo ""
}

# Function to build project
build_project() {
    echo -e "${YELLOW}🔨 Building project...${NC}"
    mvn clean package -DskipTests -q
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Build failed!${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ Build successful!${NC}"
    echo ""
}

# Function to check prerequisites
check_prerequisites() {
    # Check Java
    if ! command -v java &> /dev/null; then
        echo -e "${RED}❌ Java is not installed or not in PATH${NC}"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 21 ]; then
        echo -e "${RED}❌ Java 21 or higher is required (found: $JAVA_VERSION)${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ Java $JAVA_VERSION detected${NC}"
}

# Function to show server info
show_server_info() {
    echo ""
    echo -e "${BLUE}📍 Server Endpoints:${NC}"
    echo "   - Authorization Server: http://localhost:${SERVER_PORT}"
    echo "   - Authorization:        http://localhost:${SERVER_PORT}/oauth2/authorize"
    echo "   - Token:                http://localhost:${SERVER_PORT}/oauth2/token"
    echo "   - JWKS:                 http://localhost:${SERVER_PORT}/oauth2/jwks"
    echo "   - OpenID Config:        http://localhost:${SERVER_PORT}/.well-known/openid-configuration"
    echo ""
    echo -e "${YELLOW}💡 Tip: Make sure PostgreSQL is running and configured properly${NC}"
    echo ""
}

# Function to run in dev mode
run_dev() {
    show_server_info
    echo -e "${GREEN}🚀 Starting in development mode...${NC}"
    echo ""
    mvn spring-boot:run
}

# Function to run from JAR
run_jar() {
    # Check if JAR exists
    if [ ! -f "$JAR_PATH" ]; then
        echo -e "${YELLOW}⚠️  JAR file not found. Building project...${NC}"
        build_project
    fi
    
    show_server_info
    echo -e "${GREEN}🚀 Starting from JAR...${NC}"
    echo ""
    java -jar "$JAR_PATH"
}

# Parse arguments
MODE="jar"
FORCE_BUILD=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--dev)
            MODE="dev"
            shift
            ;;
        -j|--jar)
            MODE="jar"
            shift
            ;;
        -b|--build)
            FORCE_BUILD=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            show_usage
            exit 1
            ;;
    esac
done

# Main execution
check_prerequisites

if [ "$FORCE_BUILD" = true ]; then
    build_project
fi

case $MODE in
    dev)
        run_dev
        ;;
    jar)
        run_jar
        ;;
esac

