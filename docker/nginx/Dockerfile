FROM nginx:alpine

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/nginx.conf

# 复制前端静态文件
COPY vibe-web /usr/share/nginx/html

EXPOSE 80
