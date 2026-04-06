#!/bin/bash

# 为所有服务创建完整的 POM 文件结构

set -e

# 服务列表（不包括 user-service，已经创建）
services=("account" "order" "risk" "settlement" "notification")

for service in "${services[@]}"; do
  echo "Creating POMs for ${service}-service..."
  
  service_dir="trading-business/${service}-service"
  
  # 创建 interfaces 层 POM
  cat > "$service_dir/${service}-interfaces/pom.xml" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.trading</groupId>
        <artifactId>SERVICE_NAME-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>SERVICE_NAME-interfaces</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.trading</groupId>
            <artifactId>SERVICE_NAME-application</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.trading</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
EOF
  sed -i '' "s/SERVICE_NAME/${service}/g" "$service_dir/${service}-interfaces/pom.xml"
  
  # 创建 application 层 POM
  cat > "$service_dir/${service}-application/pom.xml" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.trading</groupId>
        <artifactId>SERVICE_NAME-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>SERVICE_NAME-application</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.trading</groupId>
            <artifactId>SERVICE_NAME-domain</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.trading</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
    </dependencies>

</project>
EOF
  sed -i '' "s/SERVICE_NAME/${service}/g" "$service_dir/${service}-application/pom.xml"
  
  # 创建 domain 层 POM
  cat > "$service_dir/${service}-domain/pom.xml" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.trading</groupId>
        <artifactId>SERVICE_NAME-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>SERVICE_NAME-domain</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.trading</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
        </dependency>
    </dependencies>

</project>
EOF
  sed -i '' "s/SERVICE_NAME/${service}/g" "$service_dir/${service}-domain/pom.xml"
  
  # 创建 infrastructure 层 POM
  cat > "$service_dir/${service}-infrastructure/pom.xml" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.trading</groupId>
        <artifactId>SERVICE_NAME-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>SERVICE_NAME-infrastructure</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.trading</groupId>
            <artifactId>SERVICE_NAME-domain</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.trading</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

</project>
EOF
  sed -i '' "s/SERVICE_NAME/${service}/g" "$service_dir/${service}-infrastructure/pom.xml"
  
  echo "✅ ${service}-service POMs created"
done

echo ""
echo "🎉 All service POMs created successfully!"
