#!/usr/bin/env python3
"""
OAuth2 Client Credentials Flow 測試腳本
功能：
1. 使用 messaging-client 取得 access_token
2. 從 JWKS 端點取得公鑰
3. 驗證 access_token 的簽名和內容
"""

import requests
import json
import jwt
from jwt import PyJWKClient
from datetime import datetime
import sys


class OAuth2Tester:
    """OAuth2 測試類別"""
    
    def __init__(self, 
                 auth_server_url="http://localhost:9000",
                 client_id="messaging-client",
                 client_secret="secret",
                 scopes="message.read message.write"):
        """
        初始化 OAuth2 測試器
        
        Args:
            auth_server_url: Authorization Server 的 URL
            client_id: OAuth2 Client ID
            client_secret: OAuth2 Client Secret
            scopes: 請求的授權範圍
        """
        self.auth_server_url = auth_server_url
        self.client_id = client_id
        self.client_secret = client_secret
        self.scopes = scopes
        self.token_endpoint = f"{auth_server_url}/oauth2/token"
        self.jwks_endpoint = f"{auth_server_url}/oauth2/jwks"
        self.access_token = None
        
    def get_access_token(self):
        """
        使用 Client Credentials Flow 取得 access token
        
        Returns:
            dict: Token 回應資料，包含 access_token, token_type, expires_in 等
        """
        print("=" * 80)
        print("步驟 1: 取得 Access Token")
        print("=" * 80)
        print(f"Token Endpoint: {self.token_endpoint}")
        print(f"Client ID: {self.client_id}")
        print(f"Client Secret: {'*' * len(self.client_secret)}")
        print(f"Scopes: {self.scopes}")
        print()
        
        # 準備請求參數
        data = {
            'grant_type': 'client_credentials',
            'scope': self.scopes
        }
        
        # 使用 HTTP Basic Authentication
        auth = (self.client_id, self.client_secret)
        
        try:
            # 發送 POST 請求
            response = requests.post(
                self.token_endpoint,
                data=data,
                auth=auth,
                headers={'Content-Type': 'application/x-www-form-urlencoded'}
            )
            
            # 檢查回應狀態
            response.raise_for_status()
            
            # 解析回應
            token_response = response.json()
            self.access_token = token_response.get('access_token')
            
            print("✅ 成功取得 Access Token")
            print(f"Token Type: {token_response.get('token_type')}")
            print(f"Expires In: {token_response.get('expires_in')} 秒")
            print(f"Scope: {token_response.get('scope')}")
            print(f"\nAccess Token (前 50 個字元): {self.access_token[:50]}...")
            print()
            
            return token_response
            
        except requests.exceptions.RequestException as e:
            print(f"❌ 取得 Access Token 失敗: {e}")
            if hasattr(e.response, 'text'):
                print(f"錯誤詳情: {e.response.text}")
            sys.exit(1)
    
    def get_jwks(self):
        """
        從 JWKS 端點取得公鑰
        
        Returns:
            dict: JWKS 資料
        """
        print("=" * 80)
        print("步驟 2: 取得 JWKS 公鑰")
        print("=" * 80)
        print(f"JWKS Endpoint: {self.jwks_endpoint}")
        print()
        
        try:
            response = requests.get(self.jwks_endpoint)
            response.raise_for_status()
            
            jwks_data = response.json()
            
            print("✅ 成功取得 JWKS")
            print(f"公鑰數量: {len(jwks_data.get('keys', []))}")
            
            # 顯示每個公鑰的資訊
            for i, key in enumerate(jwks_data.get('keys', []), 1):
                print(f"\n公鑰 #{i}:")
                print(f"  Key Type: {key.get('kty')}")
                print(f"  Key ID: {key.get('kid')}")
                print(f"  Algorithm: {key.get('alg', 'N/A')}")
                print(f"  Use: {key.get('use', 'N/A')}")
            
            print()
            return jwks_data
            
        except requests.exceptions.RequestException as e:
            print(f"❌ 取得 JWKS 失敗: {e}")
            sys.exit(1)
    
    def verify_token(self):
        """
        驗證 access token 的簽名和內容
        
        Returns:
            dict: 解碼後的 token payload
        """
        print("=" * 80)
        print("步驟 3: 驗證 Access Token")
        print("=" * 80)
        
        if not self.access_token:
            print("❌ 沒有 Access Token 可供驗證")
            sys.exit(1)
        
        try:
            # 使用 PyJWKClient 自動從 JWKS 端點取得公鑰並驗證
            jwks_client = PyJWKClient(self.jwks_endpoint)
            
            # 解碼 token header 以取得 kid
            signing_key = jwks_client.get_signing_key_from_jwt(self.access_token)
            
            # 驗證並解碼 token
            decoded_token = jwt.decode(
                self.access_token,
                signing_key.key,
                algorithms=["RS256"],
                audience=self.client_id,
                issuer=self.auth_server_url
            )
            
            print("✅ Token 驗證成功！")
            print("\nToken Header:")
            header = jwt.get_unverified_header(self.access_token)
            print(json.dumps(header, indent=2))
            
            print("\nToken Payload:")
            print(json.dumps(decoded_token, indent=2, default=str))
            
            # 顯示 token 的有效期限
            if 'exp' in decoded_token:
                exp_time = datetime.fromtimestamp(decoded_token['exp'])
                print(f"\n到期時間: {exp_time}")
            
            if 'iat' in decoded_token:
                iat_time = datetime.fromtimestamp(decoded_token['iat'])
                print(f"簽發時間: {iat_time}")
            
            print()
            return decoded_token
            
        except jwt.InvalidTokenError as e:
            print(f"❌ Token 驗證失敗: {e}")
            sys.exit(1)
        except Exception as e:
            print(f"❌ 驗證過程發生錯誤: {e}")
            sys.exit(1)
    
    def run_full_test(self):
        """執行完整的測試流程"""
        print("\n" + "=" * 80)
        print("開始 OAuth2 Client Credentials Flow 測試")
        print("=" * 80)
        print()
        
        # 步驟 1: 取得 Access Token
        token_response = self.get_access_token()
        
        # 步驟 2: 取得 JWKS
        jwks_data = self.get_jwks()
        
        # 步驟 3: 驗證 Token
        decoded_token = self.verify_token()
        
        # 顯示總結
        print("=" * 80)
        print("測試完成總結")
        print("=" * 80)
        print("✅ 1. 成功使用 Client Credentials Flow 取得 Access Token")
        print("✅ 2. 成功從 JWKS 端點取得公鑰")
        print("✅ 3. 成功驗證 Access Token 的簽名和內容")
        print()
        print("所有測試通過！🎉")
        print("=" * 80)
        print()


def main():
    """主函數"""
    # 可以透過命令列參數或環境變數自訂配置
    # 這裡使用預設值
    
    tester = OAuth2Tester(
        auth_server_url="http://localhost:9000",
        client_id="messaging-client",
        client_secret="secret",
        scopes="message.read message.write"
    )
    
    tester.run_full_test()


if __name__ == "__main__":
    main()

