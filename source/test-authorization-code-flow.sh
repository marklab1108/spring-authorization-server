#!/bin/bash

# OAuth2 Authorization Code Flow Test Script (with external login)
# Prerequisites:
#   1. Mock External Server running on port 8888
#   2. Authorization Server running on port 9000
#   3. PostgreSQL running (per app config)

set -e

BASE_URL="http://localhost:9000"
MOCK_SERVER_URL="http://localhost:8888"
CLIENT_ID="client-web"
CLIENT_SECRET="web-secret"
REDIRECT_URI="http://localhost:8080/callback"
SCOPE="profile email"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "========================================"
echo "OAuth2 Authorization Code Flow Test"
echo "========================================"
echo ""
echo "INFO  Using Spring Boot Mock External Server"
echo ""

# Check Mock Server
echo -e "${BLUE}Checking Mock Server status...${NC}"
if curl -s -f "${MOCK_SERVER_URL}/health" > /dev/null 2>&1; then
    SERVICE=$(curl -s "${MOCK_SERVER_URL}/health" 2>/dev/null | jq -r '.service' 2>/dev/null || echo "mock-external-server")
    echo -e "${GREEN}OK  Mock Server is running (port 8888)${NC}"
    echo "  Service: ${SERVICE}"
else
    echo -e "${RED}FAIL  Mock Server is NOT running${NC}"
    echo "Start it first:"
    echo "  cd source/mock-external-server && mvn spring-boot:run"
    exit 1
fi
echo ""

# Check Authorization Server
echo -e "${BLUE}Checking Authorization Server status...${NC}"
if curl -s -f "${BASE_URL}/.well-known/oauth-authorization-server" > /dev/null 2>&1; then
    ISSUER=$(curl -s "${BASE_URL}/.well-known/oauth-authorization-server" | jq -r '.issuer' 2>/dev/null || echo "${BASE_URL}")
    echo -e "${GREEN}OK  Authorization Server is running (port 9000)${NC}"
    echo "  Issuer: ${ISSUER}"
else
    echo -e "${RED}FAIL  Authorization Server is NOT running${NC}"
    echo "Start it first:"
    echo "  mvn spring-boot:run"
    exit 1
fi
echo ""

# Build authorization URL
STATE=$(openssl rand -hex 16)
ENCODED_SCOPE=${SCOPE// /%20}
AUTH_URL="${BASE_URL}/oauth2/authorize?response_type=code&client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&scope=${ENCODED_SCOPE}&state=${STATE}"

echo -e "${BLUE}Step 1: Build Authorization URL${NC}"
echo "Client ID: ${CLIENT_ID}"
echo "Redirect URI: ${REDIRECT_URI}"
echo "Scope: ${SCOPE}"
echo "State: ${STATE}"
echo ""
echo -e "${YELLOW}Authorization URL:${NC}"
echo "${AUTH_URL}"
echo ""
echo -e "${YELLOW}Manual Steps:${NC}"
echo "1) Open the URL above in your browser."
echo "2) 你會被導向 external-login 頁面，點擊按鈕前往外部認證。"
echo "3) 在外部 Mock Server 登入頁輸入身分證字號（如 A123456789）或使用快速登入。"
echo "4) 認證成功後回到條款頁，勾選同意並提交。"
echo "5) 瀏覽器會重導回 ${REDIRECT_URI}，URL 上會帶 code/state。"
echo ""

# echo -e "${BLUE}Opening browser...${NC}"
# if [[ "$OSTYPE" == "darwin"* ]]; then
#     open "${AUTH_URL}"
# elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
#     xdg-open "${AUTH_URL}" >/dev/null 2>&1 || echo "Please open the URL manually."
# else
#     echo "Please open the URL manually."
# fi
# echo ""

echo -e "${YELLOW}After completing the flow, paste the full callback URL:${NC}"
read -r -p "Callback URL: " CALLBACK_URL

# Extract code
if [[ $CALLBACK_URL =~ code=([^&]+) ]]; then
    AUTH_CODE="${BASH_REMATCH[1]}"
    echo -e "${GREEN}OK  Authorization code extracted: ${AUTH_CODE}${NC}"
else
    echo -e "${RED}FAIL  Could not extract authorization code${NC}"
    exit 1
fi

# Verify state
if [[ $CALLBACK_URL =~ state=([^&]+) ]]; then
    RETURNED_STATE="${BASH_REMATCH[1]}"
    if [[ "$STATE" == "$RETURNED_STATE" ]]; then
        echo -e "${GREEN}OK  State verified${NC}"
    else
        echo -e "${RED}FAIL  State mismatch${NC}"
        exit 1
    fi
fi
echo ""

# Exchange code for token
echo -e "${BLUE}Step 2: Exchange Authorization Code for Access Token${NC}"
TOKEN_RESPONSE=$(curl -s -X POST "${BASE_URL}/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "${CLIENT_ID}:${CLIENT_SECRET}" \
  -d "grant_type=authorization_code" \
  -d "code=${AUTH_CODE}" \
  -d "redirect_uri=${REDIRECT_URI}")

echo "$TOKEN_RESPONSE" | jq '.' 2>/dev/null || echo "$TOKEN_RESPONSE"
echo ""

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token' 2>/dev/null)

if [[ -n "$ACCESS_TOKEN" && "$ACCESS_TOKEN" != "null" ]]; then
    echo -e "${GREEN}OK  Access Token received${NC}"
    echo ""
    echo -e "${BLUE}Step 3: Decode JWT (header/payload)${NC}"
    HEADER=$(echo "$ACCESS_TOKEN" | cut -d. -f1 | base64 -d 2>/dev/null || echo "")
    PAYLOAD=$(echo "$ACCESS_TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null || echo "")
    echo "Header:";   echo "$HEADER"   | jq '.' 2>/dev/null || echo "$HEADER"
    echo ""
    echo "Payload:";  echo "$PAYLOAD"  | jq '.' 2>/dev/null || echo "$PAYLOAD"
    echo ""
    echo -e "${GREEN}========================================"
    echo "Authorization Code Flow Test Complete!"
    echo "========================================${NC}"
else
    echo -e "${RED}FAIL  Did not receive access token${NC}"
    exit 1
fi
