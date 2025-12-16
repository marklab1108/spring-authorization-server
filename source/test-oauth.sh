#!/bin/bash

# OAuth2 Client Credentials Flow Test Script
# This script demonstrates how to obtain and use access tokens

BASE_URL="http://localhost:9000"
CLIENT_ID="messaging-client"
CLIENT_SECRET="secret"

echo "========================================"
echo "Spring Authorization Server Test Script"
echo "========================================"
echo ""

# Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Get Authorization Server Metadata
echo -e "${BLUE}1. Getting Authorization Server Metadata${NC}"
echo "URL: ${BASE_URL}/.well-known/oauth-authorization-server"
echo ""
curl -s ${BASE_URL}/.well-known/oauth-authorization-server | jq '.' || echo "Server not running or jq not installed"
echo ""
echo ""

# Test 2: Get JWKS (Public Keys)
echo -e "${BLUE}2. Getting JWKS (JSON Web Key Set)${NC}"
echo "URL: ${BASE_URL}/oauth2/jwks"
echo ""
curl -s ${BASE_URL}/oauth2/jwks | jq '.' || echo "Server not running or jq not installed"
echo ""
echo ""

# Test 3: Get Access Token using Client Credentials
echo -e "${BLUE}3. Getting Access Token (Client Credentials Flow)${NC}"
echo "Client ID: ${CLIENT_ID}"
echo "Client Secret: ${CLIENT_SECRET}"
echo "Scopes: message.read message.write"
echo ""

TOKEN_RESPONSE=$(curl -s -X POST ${BASE_URL}/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u ${CLIENT_ID}:${CLIENT_SECRET} \
  -d "grant_type=client_credentials&scope=message.read message.write")

echo "$TOKEN_RESPONSE" | jq '.' || echo "Server not running or jq not installed"

# Extract access token
ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')
echo ""
echo -e "${GREEN}Access Token obtained successfully!${NC}"
echo ""
echo ""

# Test 4: Call Protected Endpoint with Token
if [ "$ACCESS_TOKEN" != "null" ] && [ -n "$ACCESS_TOKEN" ]; then
    echo -e "${BLUE}4. Calling Protected Endpoint with Access Token${NC}"
    echo "URL: ${BASE_URL}/api/protected"
    echo ""
    curl -s ${BASE_URL}/api/protected \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" | jq '.' || echo "Server not running or jq not installed"
    echo ""
    echo ""

    # Test 5: Call Messages Endpoint
    echo -e "${BLUE}5. Calling Messages Endpoint${NC}"
    echo "URL: ${BASE_URL}/api/messages"
    echo ""
    curl -s ${BASE_URL}/api/messages \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" | jq '.' || echo "Server not running or jq not installed"
    echo ""
    echo ""
else
    echo -e "${YELLOW}Could not obtain access token. Make sure the server is running.${NC}"
fi

# Test 6: Try accessing protected endpoint without token (should fail)
echo -e "${BLUE}6. Trying to Access Protected Endpoint WITHOUT Token (Should Fail)${NC}"
echo "URL: ${BASE_URL}/api/protected"
echo ""
curl -s ${BASE_URL}/api/protected
echo ""
echo ""

echo "========================================"
echo "Test Complete!"
echo "========================================"
echo ""
echo -e "${YELLOW}Note: Install jq for better JSON formatting: brew install jq${NC}"
