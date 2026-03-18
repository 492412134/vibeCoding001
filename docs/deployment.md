# 部署和配置文档

> 本文档详细说明 VibeCoding001 项目的部署流程、配置说明和运维指南。

## 1. 环境要求

### 1.1 系统要求
| 组件 | 最低配置 | 推荐配置 |
|------|---------|---------|
| CPU | 4核 | 8核+ |
| 内存 | 8GB | 16GB+ |
| 磁盘 | 50GB | 100GB+ SSD |
| 网络 | 100Mbps | 1Gbps |

### 1.2 软件依赖
| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | Java运行环境 |
| MySQL | 8.0+ | 数据存储 |
| Maven | 3.8+ | 构建工具 |
| Linux | CentOS 7+ / Ubuntu 20.04+ | 操作系统 |

---

## 2. 环境搭建

### 2.1 JDK 安装
```bash
# CentOS/RHEL
sudo yum install -y java-17-openjdk java-17-openjdk-devel

# Ubuntu/Debian
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk

# 验证安装
java -version
```

### 2.2 MySQL 安装
```bash
# CentOS/RHEL
sudo yum install -y mysql-server
sudo systemctl start mysqld
sudo systemctl enable mysqld

# Ubuntu/Debian
sudo apt-get install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
```

### 2.3 Maven 安装（可选）
```bash
# 下载Maven
wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz

# 解压
tar -xzf apache-maven-3.9.6-bin.tar.gz -C /opt/

# 配置环境变量
echo 'export PATH=$PATH:/opt/apache-maven-3.9.6/bin' >> ~/.bashrc
source ~/.bashrc

# 验证安装
mvn -version
```

---

## 3. 数据库初始化

### 3.1 创建数据库
```bash
# 登录MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE IF NOT EXISTS rule_engine 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS payment_db 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

# 创建应用用户
CREATE USER 'vibecoding'@'localhost' IDENTIFIED BY 'sr123456';
GRANT ALL PRIVILEGES ON rule_engine.* TO 'vibecoding'@'localhost';
GRANT ALL PRIVILEGES ON payment_db.* TO 'vibecoding'@'localhost';
FLUSH PRIVILEGES;
```

### 3.2 执行DDL脚本
```bash
# 进入项目目录
cd /path/to/vibeCoding001

# 执行支付服务DDL
mysql -u vibecoding -p payment_db < src/main/resources/db/payment_ddl.sql

# 执行规则引擎DDL
mysql -u vibecoding -p rule_engine < src/main/resources/sql/rule.sql
mysql -u vibecoding -p rule_engine < src/main/resources/sql/rule_execution_log.sql
mysql -u vibecoding -p rule_engine < src/main/resources/sql/external_api_config.sql
```

---

## 4. 应用配置

### 4.1 配置文件位置
```
src/main/resources/application.properties
```

### 4.2 核心配置项
```properties
# ===========================================
# 应用基础配置
# ===========================================
spring.application.name=vibeCoding001
server.port=8080

# ===========================================
# 数据库配置
# ===========================================
spring.datasource.url=jdbc:mysql://localhost:3306/rule_engine?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=vibecoding
spring.datasource.password=sr123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# 连接池配置（HikariCP）
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# ===========================================
# MyBatis配置
# ===========================================
mybatis.mapper-locations=classpath:mapper/*.xml
mybatis.type-aliases-package=com.example.vibecoding001.entity
mybatis.configuration.map-underscore-to-camel-case=true
mybatis.configuration.default-fetch-size=100
mybatis.configuration.default-statement-timeout=30

# ===========================================
# 日志配置
# ===========================================
logging.level.root=info
logging.level.com.example.vibecoding001=info
logging.level.com.example.vibecoding001.mapper=debug
logging.level.org.mybatis=debug
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
logging.file.name=logs/application.log
logging.file.max-size=100MB
logging.file.max-history=30

# ===========================================
# 支付服务配置
# ===========================================
# 线程池配置
payment.threadPool.coreThreads=20
payment.threadPool.maxThreads=20
payment.threadPool.queueCapacity=1000

# 批次配置
payment.batch.size=10
payment.batch.timeout=1000

# 第三方接口配置
payment.thirdParty.minDelay=50
payment.thirdParty.maxDelay=200
payment.thirdParty.successRate=0.95

# ===========================================
# 规则引擎配置
# ===========================================
rule.engine.cacheSize=1000
rule.engine.timeout=5000
rule.engine.logEnabled=true

# ===========================================
# 外部API配置
# ===========================================
external.api.defaultTimeout=30
external.api.defaultRetryTimes=3
external.api.connectionPoolSize=50
```

### 4.3 多环境配置
```
src/main/resources/
├── application.properties          # 默认配置
├── application-dev.properties      # 开发环境
├── application-test.properties     # 测试环境
└── application-prod.properties     # 生产环境
```

**激活指定环境**:
```bash
# 启动时指定环境
java -jar vibeCoding001.jar --spring.profiles.active=prod
```

---

## 5. 应用部署

### 5.1 本地开发部署
```bash
# 1. 克隆代码
git clone <repository-url>
cd vibeCoding001

# 2. 编译打包
./mvnw clean package -DskipTests

# 3. 运行应用
./mvnw spring-boot:run

# 或使用打包后的jar
java -jar target/vibeCoding001-0.0.1-SNAPSHOT.jar
```

### 5.2 生产环境部署

#### 5.2.1 打包应用
```bash
# 清理并打包
./mvnw clean package -DskipTests

# 生成的jar包位置
target/vibeCoding001-0.0.1-SNAPSHOT.jar
```

#### 5.2.2 部署脚本
```bash
#!/bin/bash
# deploy.sh

APP_NAME=vibeCoding001
JAR_FILE=vibeCoding001-0.0.1-SNAPSHOT.jar
LOG_FILE=logs/application.log
PID_FILE=app.pid

# 停止应用
stop() {
    if [ -f $PID_FILE ]; then
        PID=$(cat $PID_FILE)
        echo "Stopping $APP_NAME (PID: $PID)..."
        kill $PID
        rm $PID_FILE
        echo "Stopped"
    else
        echo "$APP_NAME is not running"
    fi
}

# 启动应用
start() {
    if [ -f $PID_FILE ]; then
        echo "$APP_NAME is already running"
        exit 1
    fi
    
    echo "Starting $APP_NAME..."
    nohup java -jar $JAR_FILE --spring.profiles.active=prod > $LOG_FILE 2>&1 &
    echo $! > $PID_FILE
    echo "Started with PID: $(cat $PID_FILE)"
}

# 重启应用
restart() {
    stop
    sleep 5
    start
}

# 查看状态
status() {
    if [ -f $PID_FILE ]; then
        PID=$(cat $PID_FILE)
        if ps -p $PID > /dev/null; then
            echo "$APP_NAME is running (PID: $PID)"
        else
            echo "$APP_NAME is not running (stale PID file)"
        fi
    else
        echo "$APP_NAME is not running"
    fi
}

# 主逻辑
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac
```

#### 5.2.3 使用Systemd管理
```bash
# 创建服务文件
sudo vim /etc/systemd/system/vibecoding.service
```

```ini
[Unit]
Description=VibeCoding001 Application
After=syslog.target network.target mysql.service

[Service]
User=appuser
Group=appuser
WorkingDirectory=/opt/vibecoding001
ExecStart=/usr/bin/java -jar /opt/vibecoding001/vibeCoding001-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
ExecStop=/bin/kill -15 $MAINPID
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# 启动服务
sudo systemctl daemon-reload
sudo systemctl enable vibecoding
sudo systemctl start vibecoding

# 查看状态
sudo systemctl status vibecoding

# 查看日志
sudo journalctl -u vibecoding -f
```

---

## 6. 监控和运维

### 6.1 健康检查
```bash
# 应用健康检查
curl http://localhost:8080/actuator/health

# 支付服务状态
curl http://localhost:8080/api/payment/status
```

### 6.2 日志管理
```bash
# 查看实时日志
tail -f logs/application.log

# 查看错误日志
grep "ERROR" logs/application.log

# 按日期查看日志
grep "2026-03-18" logs/application.log

# 日志轮转配置（logrotate）
sudo vim /etc/logrotate.d/vibecoding
```

```
/opt/vibecoding001/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 644 appuser appuser
    postrotate
        /bin/kill -HUP $(cat /opt/vibecoding001/app.pid 2>/dev/null) 2>/dev/null || true
    endscript
}
```

### 6.3 性能监控
```bash
# JVM监控
jps -lvm
jstat -gc <pid> 1000 10
jmap -heap <pid>

# 线程监控
jstack <pid> > thread_dump.txt

# 内存分析
jmap -dump:format=b,file=heap_dump.hprof <pid>
```

### 6.4 告警配置
建议配置以下告警项：
| 告警项 | 阈值 | 级别 |
|--------|------|------|
| 应用宕机 | - | CRITICAL |
| 队列堆积 | > 5000 | HIGH |
| 处理耗时 | > 500ms | MEDIUM |
| 成功率 | < 90% | HIGH |
| 磁盘使用率 | > 80% | MEDIUM |
| 内存使用率 | > 80% | MEDIUM |

---

## 7. 扩容方案

### 7.1 垂直扩容
```bash
# 增加JVM内存
java -Xms4g -Xmx8g -jar vibeCoding001.jar

# 优化线程池配置
payment.threadPool.coreThreads=40
payment.threadPool.maxThreads=80
```

### 7.2 水平扩容
```
                ┌─────────────┐
                │  Load Balancer │
                │   (Nginx/HAProxy)  │
                └──────┬──────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
   ┌─────────┐   ┌─────────┐   ┌─────────┐
   │  App 1  │   │  App 2  │   │  App 3  │
   │ :8080   │   │ :8081   │   │ :8082   │
   └────┬────┘   └────┬────┘   └────┬────┘
        │              │              │
        └──────────────┼──────────────┘
                       │
                       ▼
                ┌─────────────┐
                │    MySQL    │
                │  (Master-Slave)  │
                └─────────────┘
```

---

## 8. 备份和恢复

### 8.1 数据库备份
```bash
#!/bin/bash
# backup.sh

BACKUP_DIR=/backup/mysql
DATE=$(date +%Y%m%d_%H%M%S)

# 备份支付数据库
mysqldump -u root -p payment_db > $BACKUP_DIR/payment_db_$DATE.sql

# 备份规则引擎数据库
mysqldump -u root -p rule_engine > $BACKUP_DIR/rule_engine_$DATE.sql

# 压缩备份文件
gzip $BACKUP_DIR/payment_db_$DATE.sql
gzip $BACKUP_DIR/rule_engine_$DATE.sql

# 删除7天前的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete
```

### 8.2 数据库恢复
```bash
# 恢复支付数据库
mysql -u root -p payment_db < payment_db_20260318_120000.sql

# 恢复规则引擎数据库
mysql -u root -p rule_engine < rule_engine_20260318_120000.sql
```

---

## 9. 安全加固

### 9.1 网络安全
```bash
# 配置防火墙
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload

# 限制MySQL访问
sudo firewall-cmd --permanent --remove-service=mysql
sudo firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="127.0.0.1" service name="mysql" accept'
```

### 9.2 应用安全
```properties
# 禁用敏感端点
management.endpoints.web.exposure.exclude=env,heapdump

# 配置HTTPS
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
```

### 9.3 数据库安全
```sql
-- 使用强密码
ALTER USER 'vibecoding'@'localhost' IDENTIFIED BY 'ComplexP@ssw0rd123!';

-- 限制用户权限
REVOKE ALL PRIVILEGES ON *.* FROM 'vibecoding'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON rule_engine.* TO 'vibecoding'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON payment_db.* TO 'vibecoding'@'localhost';
FLUSH PRIVILEGES;
```

---

## 10. 故障排查

### 10.1 常见问题

#### Q1: 应用启动失败
**排查步骤**:
1. 检查日志：`tail -f logs/application.log`
2. 检查端口占用：`netstat -tlnp | grep 8080`
3. 检查数据库连接：`mysql -u vibecoding -p -e "SELECT 1"`
4. 检查磁盘空间：`df -h`

#### Q2: 性能下降
**排查步骤**:
1. 查看JVM内存：`jmap -heap <pid>`
2. 查看线程状态：`jstack <pid>`
3. 查看数据库连接：`SHOW PROCESSLIST;`
4. 检查队列堆积：`curl http://localhost:8080/api/payment/status`

#### Q3: 数据库连接超时
**解决方案**:
```properties
# 增加连接池大小
spring.datasource.hikari.maximum-pool-size=50

# 增加连接超时时间
spring.datasource.hikari.connection-timeout=60000

# 检查MySQL超时设置
wait_timeout=28800
interactive_timeout=28800
```

### 10.2 紧急处理
```bash
# 快速重启
sudo systemctl restart vibecoding

# 清理日志
> logs/application.log

# 释放内存
echo 3 > /proc/sys/vm/drop_caches
```

---

**文档版本**: v1.0  
**最后更新**: 2026-03-18  
**维护人员**: VibeCoding Team
