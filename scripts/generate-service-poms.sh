#!/bin/bash

# 为所有服务生成 POM 文件

set -e

# 服务定义：服务名|描述
services=(
  "order-service|Trading Context - Order management, matching coordination"
  "risk-service|Risk Context - Risk management, limits, monitoring"
  "settlement-service|Settlement Context - Settlement, reconciliation, netting"
  "notification-service|Notification Context - Multi-channel notifications"
)

for service_def in "${services[@]}"; do
  IFS='|' read -r service_name description <<< "$service_def"
  module_name="${service_name%-service}"
  
  echo "Generating POM for $service_name..."
  
  # 主 POM
  cat > "trading-business/$service_name/pom.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.trading</groupId>
        <artifactId>trading-business</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>$service_name</artifactId>
    <packaging>pom</packaging>

    <name>$(echo $module_name | sed 's/.*/\u&/' | sed 's/-/ /g' | sed 's/\b\(.\)/\u\1/g') Service</name>
    <description>$description</description>

    <modules>
        <module>${module_name}-interfaces</module>
        <module>${module_name}-application</module>
        <module>${module_name}-domain</module>
        <module>${module_name}-infrastructure</module>
    </modules>

</project>
EOF

done

echo "✅ All service POMs generated successfully!"
