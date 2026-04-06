# 贡献指南

感谢您对多语言分布式交易系统项目的关注！

## 开发流程

### 1. Fork 项目

点击右上角的 Fork 按钮，将项目 fork 到您的账号下。

### 2. 克隆仓库

```bash
git clone https://github.com/YOUR_USERNAME/trading-system.git
cd trading-system
```

### 3. 创建分支

```bash
git checkout -b feature/your-feature-name
# 或
git checkout -b fix/your-bug-fix
```

### 4. 进行开发

按照代码规范进行开发，确保：
- 代码符合项目规范
- 添加必要的单元测试
- 更新相关文档

### 5. 提交代码

```bash
git add .
git commit -m "feat: add your feature description"
git push origin feature/your-feature-name
```

### 6. 创建 Pull Request

在 GitHub 上创建 Pull Request，描述您的更改。

## 代码规范

### Java 代码规范

- 遵循 Google Java Style Guide
- 使用 4 空格缩进
- 类名使用 PascalCase
- 方法名和变量名使用 camelCase
- 常量使用 UPPER_SNAKE_CASE
- 每行最多 120 个字符

### C++ 代码规范

- 遵循 Google C++ Style Guide
- 使用 4 空格缩进
- 类名使用 PascalCase
- 函数名使用 camelCase
- 变量名使用 camelCase
- 常量使用 kPascalCase
- 每行最多 100 个字符

### Golang 代码规范

- 遵循 Effective Go
- 使用 tab 缩进
- 使用 gofmt 格式化代码
- 导出的标识符使用 PascalCase
- 未导出的标识符使用 camelCase
- 运行 golangci-lint 检查

## 提交信息规范

使用 Conventional Commits 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式调整（不影响代码运行）
- `refactor`: 代码重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动
- `ci`: CI 配置文件和脚本的变动

### Scope 范围

- `user`: 用户服务
- `account`: 账户服务
- `order`: 订单服务
- `risk`: 风控服务
- `settlement`: 清算服务
- `notification`: 通知服务
- `engine`: C++ 撮合引擎
- `query`: Golang 查询服务
- `infra`: 基础设施

### 示例

```
feat(order): add market order support

- Implement market order validation
- Add market order matching logic
- Update order status handling

Closes #123
```

## 测试要求

### 单元测试

- 所有新功能必须包含单元测试
- 单元测试覆盖率应 > 80%
- 测试命名：`test{MethodName}_{Scenario}_{ExpectedResult}`

### 集成测试

- 关键业务流程需要集成测试
- 使用 TestContainers 进行数据库和中间件测试

### 运行测试

```bash
# Java
cd trading-business/order-service
mvn test

# C++
cd trading-engine/build
ctest --output-on-failure

# Golang
cd trading-query
go test -v ./...
```

## 代码审查

所有 Pull Request 需要至少一位维护者审查通过才能合并。

审查重点：
- 代码质量和可读性
- 测试覆盖率
- 性能影响
- 安全性
- 文档完整性

## 问题反馈

如果您发现 bug 或有功能建议，请：

1. 在 Issues 中搜索是否已有相关问题
2. 如果没有，创建新的 Issue
3. 清晰描述问题或建议
4. 提供复现步骤（如果是 bug）

## 联系方式

- 项目主页: https://github.com/your-org/trading-system
- 问题反馈: https://github.com/your-org/trading-system/issues
- 邮件: support@trading.com

## 行为准则

请遵守项目的行为准则，保持友好和专业的交流。

---

再次感谢您的贡献！
