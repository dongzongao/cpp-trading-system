# C++ Trading System Starter

一个独立的 C++ 交易系统脚手架，包含订单簿、撮合引擎、风险校验、命令行示例和基础测试。

## Features

- 单品种限价单撮合
- 价格优先、时间优先
- 基础风险控制
- CMake 构建
- 命令行示例
- 无外部依赖测试

## Build

```bash
cmake -S . -B build
cmake --build build
ctest --test-dir build --output-on-failure
```

## Run

```bash
./build/trading_demo
```

## Next

- 增加撤单和改单
- 支持市价单
- 增加账户和持仓风控
- 增加行情回放和策略接口
- 接入日志和持久化
