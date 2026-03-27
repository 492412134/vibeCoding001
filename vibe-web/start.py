#!/usr/bin/env python3
"""
前端开发服务器 - 托管静态页面并代理 API 请求到 Gateway
"""

import http.server
import socketserver
import urllib.request
import urllib.error
import os
import sys

PORT = 10990
GATEWAY_URL = "http://localhost:8081"

class ProxyHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, format, *args):
        # 简化日志输出
        if '/api/' in args[0]:
            print(f"[API] {args[0]}")
        else:
            print(f"[WEB] {args[0]}")

    def do_GET(self):
        if self.path.startswith('/api/') or self.path.startswith('/actuator/'):
            self._proxy_to_gateway()
        else:
            super().do_GET()

    def do_POST(self):
        if self.path.startswith('/api/') or self.path.startswith('/actuator/'):
            self._proxy_to_gateway()
        else:
            super().do_POST()

    def do_PUT(self):
        if self.path.startswith('/api/') or self.path.startswith('/actuator/'):
            self._proxy_to_gateway()
        else:
            super().do_PUT()

    def do_DELETE(self):
        if self.path.startswith('/api/') or self.path.startswith('/actuator/'):
            self._proxy_to_gateway()
        else:
            super().do_DELETE()

    def do_OPTIONS(self):
        # 处理 CORS 预检请求
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        self.end_headers()

    def _proxy_to_gateway(self):
        try:
            target_url = GATEWAY_URL + self.path
            
            # 构建请求头
            headers = {}
            for key, value in self.headers.items():
                if key.lower() not in ['host', 'connection']:
                    headers[key] = value

            # 读取请求体
            body = None
            content_length = self.headers.get('Content-Length')
            if content_length:
                body = self.rfile.read(int(content_length))

            # 创建并发送请求
            req = urllib.request.Request(
                target_url,
                data=body,
                method=self.command,
                headers=headers
            )

            with urllib.request.urlopen(req, timeout=30) as resp:
                # 发送状态码
                self.send_response(resp.status)
                
                # 发送响应头
                for header, value in resp.headers.items():
                    if header.lower() not in ['transfer-encoding', 'content-encoding', 'connection']:
                        self.send_header(header, value)
                
                # 添加 CORS 头
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                
                # 发送响应体
                self.wfile.write(resp.read())

        except urllib.error.HTTPError as e:
            self.send_response(e.code)
            for header, value in e.headers.items():
                self.send_header(header, value)
            self.end_headers()
            self.wfile.write(e.read())
        except Exception as e:
            print(f"[ERROR] 代理失败: {e}", file=sys.stderr)
            self.send_response(502)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(f'{{"error": "Gateway unavailable: {str(e)}"}}'.encode())

if __name__ == "__main__":
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    
    print("=" * 50)
    print("🌐 Vibe Web 前端开发服务器")
    print("=" * 50)
    print(f"📍 前端页面: http://localhost:{PORT}")
    print(f"🔗 API 代理: {GATEWAY_URL}")
    print("-" * 50)
    print("⚡ 按 Ctrl+C 停止服务")
    print("")
    
    with socketserver.TCPServer(("", PORT), ProxyHandler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n\n👋 服务器已停止")
