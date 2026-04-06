# 业务流程定义文档

## 1. 核心业务流程

### 1.1 用户注册和认证流程

```mermaid
sequenceDiagram
    participant User
    participant WebUI
    participant APIGateway
    participant UserService
    participant Database
    participant EmailService

    User->>WebUI: 填写注册信息
    WebUI->>APIGateway: POST /api/v1/users/register
    APIGateway->>UserService: 注册请求
    UserService->>UserService: 验证信息格式
    UserService->>Database: 检查用户名/邮箱是否存在
    Database-->>UserService: 查询结果
    
    alt 用户已存在
        UserService-->>APIGateway: 返回错误
        APIGateway-->>WebUI: 用户已存在
        WebUI-->>User: 显示错误信息
    else 用户不存在
        UserService->>Database: 创建用户记录
        UserService->>EmailService: 发送验证邮件
        UserService-->>APIGateway: 注册成功
        APIGateway-->>WebUI: 返回用户信息
        WebUI-->>User: 显示注册成功
    end
```

**流程说明**:
1. 用户填写注册信息（用户名、邮箱、密码）
2. 前端验证基本格式
3. 后端验证用户名和邮箱唯一性
4. 密码加密存储（BCrypt）
5. 发送邮箱验证链接
6. 用户点击链接完成邮箱验证

**关键节点**:
- 用户名和邮箱唯一性检查
- 密码强度验证（至少 8 位，包含大小写字母和数字）
- 邮箱验证链接有效期 24 小时

### 1.2 订单提交流程

```mermaid
sequenceDiagram
    participant User
    participant OrderService
    participant RiskService
    participant AccountService
    participant Kafka
    participant MatchEngine
    participant NotificationService

    User->>OrderService: 提交订单
    OrderService->>OrderService: 参数校验
    
    OrderService->>RiskService: 风控检查
    RiskService->>RiskService: 执行风控规则
    RiskService-->>OrderService: 风控结果
    
    alt 风控不通过
        OrderService-->>User: 返回拒绝原因
    else 风控通过
        OrderService->>AccountService: 冻结资金（买单）
        AccountService-->>OrderService: 冻结成功
        
        OrderService->>OrderService: 保存订单（状态：已提交）
        OrderService->>Kafka: 发布 OrderCreated 事件
        OrderService-->>User: 返回订单ID
        
        Kafka->>MatchEngine: 订单事件
        MatchEngine->>MatchEngine: 加入订单簿
        MatchEngine->>Kafka: 发布 OrderConfirmed 事件
        
        Kafka->>OrderService: 订单确认事件
        OrderService->>OrderService: 更新状态（已确认）
        
        Kafka->>NotificationService: 订单事件
        NotificationService->>User: 推送订单通知
    end
```

**流程说明**:
1. 用户提交订单（品种、方向、价格、数量）
2. 参数校验（价格、数量合法性）
3. 风控检查（限额、持仓、自成交）
4. 买单冻结资金
5. 保存订单到数据库
6. 发送订单到撮合引擎
7. 撮合引擎确认订单
8. 推送订单状态通知

**关键节点**:
- 风控检查必须通过
- 买单必须先冻结资金
- 订单状态：已提交 → 已确认 → 部分成交/全部成交
- 异步处理，快速返回订单ID

### 1.3 订单撮合流程

```mermaid
sequenceDiagram
    participant Kafka
    participant MatchEngine
    participant OrderBook
    participant SettlementService
    participant QueryService

    Kafka->>MatchEngine: 接收订单事件
    MatchEngine->>MatchEngine: 解析订单
    
    MatchEngine->>OrderBook: 尝试撮合
    OrderBook->>OrderBook: 查找对手盘
    
    alt 有对手盘且价格匹配
        OrderBook->>OrderBook: 生成成交
        OrderBook-->>MatchEngine: 返回成交列表
        
        loop 每笔成交
            MatchEngine->>Kafka: 发布 TradeExecuted 事件
            MatchEngine->>SettlementService: 实时清算
            SettlementService->>SettlementService: 更新买方账户
            SettlementService->>SettlementService: 更新卖方账户
        end
        
        MatchEngine->>Kafka: 发布 OrderUpdated 事件
        Kafka->>QueryService: 同步成交数据
        
    else 无对手盘或价格不匹配
        OrderBook->>OrderBook: 加入订单簿
        OrderBook-->>MatchEngine: 订单挂单
        MatchEngine->>Kafka: 发布 OrderConfirmed 事件
    end
```

**流程说明**:
1. 撮合引擎接收订单
2. 查找订单簿中的对手盘
3. 按价格-时间优先原则撮合
4. 生成成交记录
5. 实时清算买卖双方账户
6. 发布成交事件
7. 未成交部分加入订单簿

**关键节点**:
- 价格优先：买单价格 ≥ 卖单价格才能成交
- 时间优先：同价格订单按时间先后撮合
- 成交价格：使用挂单（maker）价格
- 实时清算：成交即清算

### 1.4 订单撤销流程

```mermaid
sequenceDiagram
    participant User
    participant OrderService
    participant Kafka
    participant MatchEngine
    participant AccountService

    User->>OrderService: 撤销订单
    OrderService->>OrderService: 检查订单状态
    
    alt 订单不可撤销
        OrderService-->>User: 返回错误（已成交/已撤销）
    else 订单可撤销
        OrderService->>Kafka: 发布 CancelOrderRequest 事件
        OrderService-->>User: 返回撤单请求已提交
        
        Kafka->>MatchEngine: 撤单请求
        MatchEngine->>MatchEngine: 从订单簿移除订单
        MatchEngine->>Kafka: 发布 OrderCancelled 事件
        
        Kafka->>OrderService: 订单已撤销事件
        OrderService->>OrderService: 更新订单状态
        
        Kafka->>AccountService: 订单已撤销事件
        AccountService->>AccountService: 解冻资金（买单）
        
        OrderService->>User: 推送撤单成功通知
    end
```

**流程说明**:
1. 用户请求撤销订单
2. 检查订单状态（只能撤销未成交或部分成交的订单）
3. 发送撤单请求到撮合引擎
4. 撮合引擎从订单簿移除订单
5. 解冻买单的冻结资金
6. 更新订单状态为已撤销
7. 推送撤单通知

**关键节点**:
- 只能撤销未完全成交的订单
- 撤单是异步操作
- 买单撤销后立即解冻资金
- 部分成交的订单撤销后，已成交部分不受影响

### 1.5 资金存取款流程

```mermaid
sequenceDiagram
    participant User
    participant AccountService
    participant PaymentGateway
    participant Database
    participant NotificationService

    User->>AccountService: 申请入金
    AccountService->>AccountService: 生成入金订单
    AccountService->>PaymentGateway: 创建支付订单
    PaymentGateway-->>AccountService: 返回支付链接
    AccountService-->>User: 返回支付链接
    
    User->>PaymentGateway: 完成支付
    PaymentGateway->>AccountService: 支付回调
    AccountService->>AccountService: 验证签名
    
    alt 验证成功
        AccountService->>Database: 更新账户余额
        AccountService->>Database: 记录资金流水
        AccountService->>NotificationService: 发送入金成功通知
        AccountService-->>PaymentGateway: 返回成功
        NotificationService->>User: 推送入金成功
    else 验证失败
        AccountService-->>PaymentGateway: 返回失败
    end
```

**流程说明**:

**入金流程**:
1. 用户申请入金
2. 生成入金订单
3. 调用支付网关
4. 用户完成支付
5. 接收支付回调
6. 验证回调签名
7. 更新账户余额
8. 记录资金流水
9. 推送入金成功通知

**出金流程**:
1. 用户申请出金
2. 检查可用余额
3. 冻结出金金额
4. 人工审核（可选）
5. 调用支付网关转账
6. 扣除账户余额
7. 记录资金流水
8. 推送出金成功通知

**关键节点**:
- 入金：支付回调验证签名
- 出金：检查可用余额（余额 - 冻结 - 保证金）
- 出金可能需要人工审核
- 所有资金变动记录流水

### 1.6 日终清算流程

```mermaid
sequenceDiagram
    participant Scheduler
    participant SettlementService
    participant Database
    participant MatchEngine
    participant NotificationService

    Scheduler->>SettlementService: 触发日终清算
    SettlementService->>SettlementService: 检查清算状态
    
    alt 已清算
        SettlementService-->>Scheduler: 返回已清算
    else 未清算
        SettlementService->>Database: 汇总当日成交数据
        SettlementService->>Database: 汇总资金变动
        SettlementService->>Database: 汇总持仓变动
        
        SettlementService->>SettlementService: 计算应收应付
        SettlementService->>SettlementService: 计算手续费
        SettlementService->>SettlementService: 计算盈亏
        
        SettlementService->>MatchEngine: 获取订单簿快照
        SettlementService->>SettlementService: 对账
        
        alt 对账成功
            SettlementService->>Database: 保存清算记录
            SettlementService->>Database: 更新账户状态
            SettlementService->>NotificationService: 发送清算报告
            SettlementService-->>Scheduler: 清算成功
        else 对账失败
            SettlementService->>NotificationService: 发送告警
            SettlementService-->>Scheduler: 清算失败
        end
    end
```

**流程说明**:
1. 定时任务触发日终清算（通常在交易日结束后）
2. 检查当日是否已清算
3. 汇总当日所有成交数据
4. 汇总资金变动（入金、出金、手续费）
5. 汇总持仓变动
6. 计算每个账户的应收应付
7. 计算手续费
8. 计算已实现盈亏和未实现盈亏
9. 与撮合引擎对账
10. 保存清算记录
11. 生成清算报告

**关键节点**:
- 清算时间：交易日结束后（如 23:00）
- 对账：确保业务系统和撮合引擎数据一致
- 清算失败：发送告警，人工介入
- 清算报告：发送给用户和管理员

## 2. 异常处理流程

### 2.1 订单提交失败处理

```mermaid
flowchart TD
    A[订单提交] --> B{参数校验}
    B -->|失败| C[返回参数错误]
    B -->|成功| D{风控检查}
    D -->|失败| E[返回风控拒绝]
    D -->|成功| F{冻结资金}
    F -->|失败| G[返回资金不足]
    F -->|成功| H[保存订单]
    H --> I{发送到Kafka}
    I -->|失败| J[解冻资金]
    J --> K[标记订单失败]
    K --> L[返回系统错误]
    I -->|成功| M[返回订单ID]
```

**处理策略**:
- 参数错误：立即返回，不记录订单
- 风控拒绝：记录风控事件，返回拒绝原因
- 资金不足：返回错误，不冻结资金
- Kafka 发送失败：解冻资金，标记订单失败，记录日志

### 2.2 撮合引擎故障处理

```mermaid
flowchart TD
    A[检测到撮合引擎故障] --> B[停止接收新订单]
    B --> C[保存订单簿快照]
    C --> D[切换到备用引擎]
    D --> E[恢复订单簿]
    E --> F[重放未处理订单]
    F --> G[恢复接收新订单]
    G --> H[发送告警通知]
```

**处理策略**:
- 主备切换：自动切换到备用撮合引擎
- 订单簿恢复：从快照恢复订单簿状态
- 订单重放：重放故障期间的订单
- 数据一致性：确保主备数据一致

### 2.3 数据库故障处理

```mermaid
flowchart TD
    A[检测到数据库故障] --> B{主库故障?}
    B -->|是| C[切换到从库]
    C --> D[从库升级为主库]
    D --> E[更新应用配置]
    E --> F[恢复服务]
    B -->|否| G[使用主库]
    G --> F
    F --> H[发送告警通知]
```

**处理策略**:
- 主从切换：自动切换到从库
- 读写分离：读操作使用从库，写操作使用主库
- 连接池：使用连接池自动重连
- 降级策略：数据库不可用时，使用缓存提供只读服务

### 2.4 消息队列故障处理

```mermaid
flowchart TD
    A[检测到Kafka故障] --> B[启用本地队列]
    B --> C[订单写入本地队列]
    C --> D{Kafka恢复?}
    D -->|否| C
    D -->|是| E[重放本地队列消息]
    E --> F[恢复正常流程]
    F --> G[发送告警通知]
```

**处理策略**:
- 本地队列：Kafka 不可用时，使用本地队列缓存
- 消息重放：Kafka 恢复后，重放本地队列消息
- 消息去重：使用消息ID去重，避免重复处理
- 降级策略：Kafka 长时间不可用，拒绝新订单

## 3. 数据同步流程

### 3.1 订单数据同步

```mermaid
sequenceDiagram
    participant OrderService
    participant Kafka
    participant QueryService
    participant Database
    participant Cache

    OrderService->>Kafka: 发布订单事件
    Kafka->>QueryService: 消费订单事件
    QueryService->>QueryService: 解析事件
    QueryService->>Database: 保存/更新订单
    QueryService->>Cache: 更新缓存
    QueryService->>QueryService: 记录消费位移
```

**同步策略**:
- 事件驱动：通过 Kafka 事件同步
- 幂等性：使用事件ID去重
- 顺序保证：同一订单的事件按顺序处理
- 延迟监控：监控消费延迟，超过阈值告警

### 3.2 行情数据同步

```mermaid
sequenceDiagram
    participant MatchEngine
    participant Kafka
    participant MarketDataService
    participant TimescaleDB
    participant Redis
    participant WebSocket

    MatchEngine->>Kafka: 发布成交事件
    Kafka->>MarketDataService: 消费成交事件
    MarketDataService->>MarketDataService: 生成Tick数据
    MarketDataService->>TimescaleDB: 保存Tick数据
    MarketDataService->>Redis: 更新实时行情
    MarketDataService->>WebSocket: 推送行情
```

**同步策略**:
- 高频数据：使用 Kafka 高吞吐传输
- 时序存储：使用 TimescaleDB 存储历史行情
- 实时缓存：使用 Redis 缓存最新行情
- 推送优化：批量推送，减少网络开销

## 4. 监控和告警流程

### 4.1 系统监控

**监控指标**:
- 订单提交率（TPS）
- 撮合延迟（P99）
- API 响应时间（P95）
- 数据库连接池使用率
- Kafka 消费延迟
- 缓存命中率
- 错误率

**告警规则**:
- 订单提交率异常（突增或突降）
- 撮合延迟超过 100 微秒
- API 响应时间超过 100 毫秒
- 数据库连接池使用率超过 90%
- Kafka 消费延迟超过 10 秒
- 错误率超过 1%

### 4.2 业务监控

**监控指标**:
- 活跃用户数
- 订单数量
- 成交量
- 成交金额
- 持仓总量
- 资金总量

**告警规则**:
- 异常交易检测（大额订单、频繁交易）
- 风控事件（超限、自成交）
- 清算失败
- 对账不一致

## 5. 运维流程

### 5.1 发布流程

```mermaid
flowchart LR
    A[代码提交] --> B[CI构建]
    B --> C[单元测试]
    C --> D[集成测试]
    D --> E[构建镜像]
    E --> F[推送镜像]
    F --> G[部署到测试环境]
    G --> H[测试验证]
    H --> I{测试通过?}
    I -->|否| J[回滚]
    I -->|是| K[部署到生产环境]
    K --> L[灰度发布]
    L --> M[全量发布]
```

**发布策略**:
- 灰度发布：先发布 10% 流量，观察无异常后全量发布
- 蓝绿部署：保留旧版本，新版本验证通过后切换
- 回滚机制：发现问题立即回滚到上一版本
- 发布窗口：选择低峰期发布（如凌晨）

### 5.2 故障处理流程

```mermaid
flowchart TD
    A[故障告警] --> B[确认故障]
    B --> C[评估影响范围]
    C --> D{紧急程度}
    D -->|P0| E[立即处理]
    D -->|P1| F[1小时内处理]
    D -->|P2| G[4小时内处理]
    E --> H[定位问题]
    F --> H
    G --> H
    H --> I[修复问题]
    I --> J[验证修复]
    J --> K{修复成功?}
    K -->|否| H
    K -->|是| L[恢复服务]
    L --> M[故障复盘]
```

**故障等级**:
- P0：核心功能不可用（如订单提交失败）
- P1：重要功能受影响（如查询缓慢）
- P2：次要功能异常（如报表生成失败）

**处理原则**:
- 快速响应：收到告警立即响应
- 先恢复后分析：优先恢复服务，再分析根因
- 记录日志：详细记录故障处理过程
- 故障复盘：总结经验，避免再次发生

## 6. 总结

本文档定义了交易系统的核心业务流程，包括：

1. **核心流程**: 用户注册、订单提交、订单撮合、订单撤销、资金存取、日终清算
2. **异常处理**: 订单失败、引擎故障、数据库故障、消息队列故障
3. **数据同步**: 订单同步、行情同步
4. **监控告警**: 系统监控、业务监控
5. **运维流程**: 发布流程、故障处理

这些流程确保系统的稳定性、可靠性和可维护性。
