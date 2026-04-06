# C++ 撮合引擎兜底方案设计

## 1. 概述

本文档定义了 C++ 撮合引擎的全面兜底方案，包括高可用架构、故障恢复、数据一致性保障、性能降级和灾难恢复策略。撮合引擎作为交易系统的核心，必须保证极高的可用性和可靠性。

## 2. 高可用架构

### 2.1 主备架构

```cpp
// matching_engine_cluster.h
#pragma once

#include <memory>
#include <atomic>
#include <thread>
#include "matching_engine.h"
#include "state_replication.h"

enum class EngineRole {
    MASTER,
    SLAVE,
    STANDBY
};

enum class EngineStatus {
    ACTIVE,
    SYNCING,
    FAILED,
    RECOVERING
};

class MatchingEngineCluster {
public:
    MatchingEngineCluster(const std::string& cluster_id);
    
    // 启动集群
    void Start();
    
    // 停止集群
    void Stop();
    
    // 主备切换
    void Failover();
    
    // 健康检查
    bool IsHealthy() const;
    
    // 获取当前角色
    EngineRole GetRole() const { return role_; }
    
    // 获取当前状态
    EngineStatus GetStatus() const { return status_; }
    
private:
    // 心跳检测
    void HeartbeatLoop();
    
    // 状态同步
    void StateSyncLoop();
    
    // 选举新主节点
    void ElectMaster();
    
    // 提升为主节点
    void PromoteToMaster();
    
    // 降级为备节点
    void DemoteToSlave();
    
private:
    std::string cluster_id_;
    EngineRole role_;
    std::atomic<EngineStatus> status_;
    
    std::unique_ptr<MatchingEngine> engine_;
    std::unique_ptr<StateReplication> replicator_;
    
    std::thread heartbeat_thread_;
    std::thread sync_thread_;
    
    std::atomic<bool> running_;
    std::atomic<uint64_t> last_heartbeat_;
    
    static constexpr uint64_t HEARTBEAT_TIMEOUT_MS = 3000;
    static constexpr uint64_t HEARTBEAT_INTERVAL_MS = 1000;
};

// matching_engine_cluster.cpp
void MatchingEngineCluster::HeartbeatLoop() {
    while (running_) {
        if (role_ == EngineRole::MASTER) {
            // 主节点发送心跳
            SendHeartbeat();
        } else {
            // 备节点检查心跳
            uint64_t now = GetCurrentTimeMs();
            uint64_t last = last_heartbeat_.load();
            
            if (now - last > HEARTBEAT_TIMEOUT_MS) {
                LOG_ERROR("Master heartbeat timeout, initiating failover");
                Failover();
            }
        }
        
        std::this_thread::sleep_for(
            std::chrono::milliseconds(HEARTBEAT_INTERVAL_MS)
        );
    }
}

void MatchingEngineCluster::Failover() {
    LOG_WARN("Starting failover process");
    
    // 1. 停止接收新订单
    engine_->StopAcceptingOrders();
    
    // 2. 等待当前订单处理完成
    engine_->WaitForPendingOrders();
    
    // 3. 保存当前状态
    engine_->SaveState();
    
    // 4. 选举新主节点
    ElectMaster();
    
    // 5. 如果当前节点被选为主节点
    if (role_ == EngineRole::MASTER) {
        PromoteToMaster();
    }
    
    LOG_INFO("Failover completed, current role: {}", 
             role_ == EngineRole::MASTER ? "MASTER" : "SLAVE");
}

void MatchingEngineCluster::PromoteToMaster() {
    LOG_INFO("Promoting to master");
    
    // 1. 加载最新状态
    engine_->LoadState();
    
    // 2. 验证数据完整性
    if (!engine_->ValidateState()) {
        LOG_ERROR("State validation failed, cannot promote to master");
        status_ = EngineStatus::FAILED;
        return;
    }
    
    // 3. 切换角色
    role_ = EngineRole::MASTER;
    status_ = EngineStatus::ACTIVE;
    
    // 4. 开始接收订单
    engine_->StartAcceptingOrders();
    
    // 5. 通知 Java 服务
    NotifyJavaService("MASTER_CHANGED");
    
    LOG_INFO("Successfully promoted to master");
}
```

### 2.2 状态复制

```cpp
// state_replication.h
#pragma once

#include <memory>
#include <queue>
#include <mutex>
#include "order_book.h"

struct ReplicationLog {
    uint64_t sequence_id;
    uint64_t timestamp;
    std::string operation;  // ADD_ORDER, CANCEL_ORDER, TRADE
    std::string data;
    
    std::string Serialize() const;
    static ReplicationLog Deserialize(const std::string& data);
};

class StateReplication {
public:
    StateReplication();
    
    // 记录操作日志
    void LogOperation(const std::string& operation, const std::string& data);
    
    // 同步到备节点
    void SyncToSlaves();
    
    // 从主节点同步
    void SyncFromMaster();
    
    // 重放日志
    void ReplayLogs(uint64_t from_sequence);
    
    // 获取当前序列号
    uint64_t GetCurrentSequence() const { return current_sequence_; }
    
private:
    // 持久化日志
    void PersistLog(const ReplicationLog& log);
    
    // 发送日志到备节点
    void SendLogToSlave(const std::string& slave_addr, const ReplicationLog& log);
    
private:
    std::atomic<uint64_t> current_sequence_;
    std::queue<ReplicationLog> pending_logs_;
    std::mutex logs_mutex_;
    
    // 日志文件
    std::ofstream log_file_;
    std::string log_path_;
};

// state_replication.cpp
void StateReplication::LogOperation(
    const std::string& operation, 
    const std::string& data) {
    
    ReplicationLog log;
    log.sequence_id = ++current_sequence_;
    log.timestamp = GetCurrentTimeUs();
    log.operation = operation;
    log.data = data;
    
    // 1. 持久化到本地
    PersistLog(log);
    
    // 2. 加入待同步队列
    {
        std::lock_guard<std::mutex> lock(logs_mutex_);
        pending_logs_.push(log);
    }
    
    // 3. 异步同步到备节点
    std::thread([this]() {
        SyncToSlaves();
    }).detach();
}

void StateReplication::PersistLog(const ReplicationLog& log) {
    std::string serialized = log.Serialize();
    
    // 写入日志文件
    log_file_ << serialized << std::endl;
    log_file_.flush();
    
    // 每1000条日志轮转一次
    if (log.sequence_id % 1000 == 0) {
        RotateLogFile();
    }
}

void StateReplication::ReplayLogs(uint64_t from_sequence) {
    LOG_INFO("Replaying logs from sequence {}", from_sequence);
    
    // 1. 读取日志文件
    std::ifstream log_file(log_path_);
    std::string line;
    
    while (std::getline(log_file, line)) {
        ReplicationLog log = ReplicationLog::Deserialize(line);
        
        if (log.sequence_id <= from_sequence) {
            continue;
        }
        
        // 2. 重放操作
        if (log.operation == "ADD_ORDER") {
            ReplayAddOrder(log.data);
        } else if (log.operation == "CANCEL_ORDER") {
            ReplayCancelOrder(log.data);
        } else if (log.operation == "TRADE") {
            ReplayTrade(log.data);
        }
    }
    
    LOG_INFO("Log replay completed");
}
```

### 2.3 快照机制

```cpp
// snapshot_manager.h
#pragma once

#include <string>
#include <memory>
#include "order_book.h"

struct Snapshot {
    uint64_t sequence_id;
    uint64_t timestamp;
    std::unordered_map<std::string, OrderBook> order_books;
    
    bool Save(const std::string& path) const;
    static std::unique_ptr<Snapshot> Load(const std::string& path);
};

class SnapshotManager {
public:
    SnapshotManager(const std::string& snapshot_dir);
    
    // 创建快照
    void CreateSnapshot(const MatchingEngine& engine);
    
    // 加载最新快照
    std::unique_ptr<Snapshot> LoadLatestSnapshot();
    
    // 清理旧快照
    void CleanupOldSnapshots(int keep_count = 3);
    
private:
    std::string GetSnapshotPath(uint64_t sequence_id) const;
    
private:
    std::string snapshot_dir_;
};

// snapshot_manager.cpp
void SnapshotManager::CreateSnapshot(const MatchingEngine& engine) {
    LOG_INFO("Creating snapshot");
    
    Snapshot snapshot;
    snapshot.sequence_id = engine.GetCurrentSequence();
    snapshot.timestamp = GetCurrentTimeUs();
    
    // 1. 复制所有订单簿
    for (const auto& [symbol, order_book] : engine.GetOrderBooks()) {
        snapshot.order_books[symbol] = order_book;
    }
    
    // 2. 保存到磁盘
    std::string path = GetSnapshotPath(snapshot.sequence_id);
    if (!snapshot.Save(path)) {
        LOG_ERROR("Failed to save snapshot");
        return;
    }
    
    // 3. 清理旧快照
    CleanupOldSnapshots();
    
    LOG_INFO("Snapshot created: sequence={}, path={}", 
             snapshot.sequence_id, path);
}

std::unique_ptr<Snapshot> SnapshotManager::LoadLatestSnapshot() {
    // 1. 查找最新的快照文件
    std::vector<std::string> snapshot_files = ListSnapshotFiles();
    
    if (snapshot_files.empty()) {
        LOG_WARN("No snapshot found");
        return nullptr;
    }
    
    // 2. 按序列号排序
    std::sort(snapshot_files.begin(), snapshot_files.end(), std::greater<>());
    
    // 3. 加载最新快照
    for (const auto& file : snapshot_files) {
        auto snapshot = Snapshot::Load(file);
        if (snapshot) {
            LOG_INFO("Loaded snapshot: sequence={}", snapshot->sequence_id);
            return snapshot;
        }
    }
    
    LOG_ERROR("Failed to load any snapshot");
    return nullptr;
}
```

## 3. 故障恢复

### 3.1 崩溃恢复

```cpp
// crash_recovery.h
#pragma once

#include "matching_engine.h"
#include "snapshot_manager.h"
#include "state_replication.h"

class CrashRecovery {
public:
    CrashRecovery(
        MatchingEngine* engine,
        SnapshotManager* snapshot_mgr,
        StateReplication* replicator
    );
    
    // 执行恢复
    bool Recover();
    
    // 验证恢复结果
    bool ValidateRecovery();
    
private:
    // 恢复步骤
    bool LoadSnapshot();
    bool ReplayLogs();
    bool ValidateState();
    bool RebuildIndexes();
    
private:
    MatchingEngine* engine_;
    SnapshotManager* snapshot_mgr_;
    StateReplication* replicator_;
};

// crash_recovery.cpp
bool CrashRecovery::Recover() {
    LOG_INFO("Starting crash recovery");
    
    // 1. 加载最新快照
    if (!LoadSnapshot()) {
        LOG_ERROR("Failed to load snapshot");
        return false;
    }
    
    // 2. 重放日志
    if (!ReplayLogs()) {
        LOG_ERROR("Failed to replay logs");
        return false;
    }
    
    // 3. 验证状态
    if (!ValidateState()) {
        LOG_ERROR("State validation failed");
        return false;
    }
    
    // 4. 重建索引
    if (!RebuildIndexes()) {
        LOG_ERROR("Failed to rebuild indexes");
        return false;
    }
    
    LOG_INFO("Crash recovery completed successfully");
    return true;
}

bool CrashRecovery::LoadSnapshot() {
    auto snapshot = snapshot_mgr_->LoadLatestSnapshot();
    
    if (!snapshot) {
        LOG_WARN("No snapshot available, starting from empty state");
        return true;
    }
    
    // 恢复订单簿
    for (const auto& [symbol, order_book] : snapshot->order_books) {
        engine_->RestoreOrderBook(symbol, order_book);
    }
    
    LOG_INFO("Snapshot loaded: sequence={}", snapshot->sequence_id);
    return true;
}

bool CrashRecovery::ReplayLogs() {
    uint64_t from_sequence = engine_->GetCurrentSequence();
    
    LOG_INFO("Replaying logs from sequence {}", from_sequence);
    
    replicator_->ReplayLogs(from_sequence);
    
    return true;
}

bool CrashRecovery::ValidateState() {
    LOG_INFO("Validating recovered state");
    
    // 1. 检查订单簿一致性
    for (const auto& [symbol, order_book] : engine_->GetOrderBooks()) {
        if (!order_book.Validate()) {
            LOG_ERROR("Order book validation failed for {}", symbol);
            return false;
        }
    }
    
    // 2. 检查订单索引
    if (!engine_->ValidateOrderIndex()) {
        LOG_ERROR("Order index validation failed");
        return false;
    }
    
    // 3. 检查价格级别
    if (!engine_->ValidatePriceLevels()) {
        LOG_ERROR("Price level validation failed");
        return false;
    }
    
    LOG_INFO("State validation passed");
    return true;
}
```

### 3.2 数据一致性检查

```cpp
// consistency_checker.h
#pragma once

#include <vector>
#include <string>
#include "order_book.h"

struct ConsistencyIssue {
    std::string type;
    std::string description;
    std::string symbol;
    uint64_t order_id;
};

class ConsistencyChecker {
public:
    // 检查订单簿一致性
    std::vector<ConsistencyIssue> CheckOrderBook(const OrderBook& book);
    
    // 检查订单索引一致性
    std::vector<ConsistencyIssue> CheckOrderIndex(const MatchingEngine& engine);
    
    // 检查价格级别一致性
    std::vector<ConsistencyIssue> CheckPriceLevels(const OrderBook& book);
    
    // 修复一致性问题
    bool FixIssues(const std::vector<ConsistencyIssue>& issues);
};

// consistency_checker.cpp
std::vector<ConsistencyIssue> ConsistencyChecker::CheckOrderBook(
    const OrderBook& book) {
    
    std::vector<ConsistencyIssue> issues;
    
    // 1. 检查买卖盘价格顺序
    if (!book.IsBidsSorted()) {
        issues.push_back({
            "PRICE_ORDER",
            "Bid prices not in descending order",
            book.GetSymbol(),
            0
        });
    }
    
    if (!book.IsAsksSorted()) {
        issues.push_back({
            "PRICE_ORDER",
            "Ask prices not in ascending order",
            book.GetSymbol(),
            0
        });
    }
    
    // 2. 检查订单数量
    for (const auto& order : book.GetAllOrders()) {
        if (order.remaining_quantity <= 0) {
            issues.push_back({
                "INVALID_QUANTITY",
                "Order has zero or negative quantity",
                book.GetSymbol(),
                order.order_id
            });
        }
    }
    
    // 3. 检查价格交叉
    if (book.HasCrossedPrices()) {
        issues.push_back({
            "CROSSED_PRICES",
            "Bid price >= Ask price",
            book.GetSymbol(),
            0
        });
    }
    
    return issues;
}
```

## 4. 性能降级策略

### 4.1 限流保护

```cpp
// rate_limiter.h
#pragma once

#include <atomic>
#include <chrono>
#include <deque>

class RateLimiter {
public:
    RateLimiter(int max_requests_per_second);
    
    // 检查是否允许请求
    bool AllowRequest();
    
    // 获取当前速率
    int GetCurrentRate() const;
    
private:
    void CleanupOldRequests();
    
private:
    int max_requests_per_second_;
    std::deque<std::chrono::steady_clock::time_point> requests_;
    mutable std::mutex mutex_;
};

// rate_limiter.cpp
bool RateLimiter::AllowRequest() {
    std::lock_guard<std::mutex> lock(mutex_);
    
    // 清理过期请求
    CleanupOldRequests();
    
    // 检查是否超过限制
    if (requests_.size() >= max_requests_per_second_) {
        return false;
    }
    
    // 记录请求
    requests_.push_back(std::chrono::steady_clock::now());
    
    return true;
}

void RateLimiter::CleanupOldRequests() {
    auto now = std::chrono::steady_clock::now();
    auto one_second_ago = now - std::chrono::seconds(1);
    
    while (!requests_.empty() && requests_.front() < one_second_ago) {
        requests_.pop_front();
    }
}
```

### 4.2 过载保护

```cpp
// overload_protection.h
#pragma once

#include <atomic>
#include "rate_limiter.h"

enum class LoadLevel {
    NORMAL,
    MEDIUM,
    HIGH,
    CRITICAL
};

class OverloadProtection {
public:
    OverloadProtection();
    
    // 检查系统负载
    LoadLevel CheckLoad();
    
    // 是否应该拒绝请求
    bool ShouldReject(const Order& order);
    
    // 获取当前负载指标
    struct LoadMetrics {
        int pending_orders;
        int orders_per_second;
        double cpu_usage;
        double memory_usage;
        LoadLevel level;
    };
    
    LoadMetrics GetMetrics() const;
    
private:
    void UpdateMetrics();
    
private:
    std::atomic<int> pending_orders_;
    std::atomic<int> orders_per_second_;
    std::atomic<LoadLevel> current_level_;
    
    RateLimiter rate_limiter_;
    
    static constexpr int NORMAL_THRESHOLD = 1000;
    static constexpr int MEDIUM_THRESHOLD = 5000;
    static constexpr int HIGH_THRESHOLD = 10000;
};

// overload_protection.cpp
bool OverloadProtection::ShouldReject(const Order& order) {
    LoadLevel level = current_level_.load();
    
    switch (level) {
        case LoadLevel::NORMAL:
            return false;
            
        case LoadLevel::MEDIUM:
            // 拒绝低优先级订单
            return order.priority == OrderPriority::LOW;
            
        case LoadLevel::HIGH:
            // 只接受高优先级订单
            return order.priority != OrderPriority::HIGH;
            
        case LoadLevel::CRITICAL:
            // 拒绝所有新订单
            return true;
    }
    
    return false;
}

LoadLevel OverloadProtection::CheckLoad() {
    int pending = pending_orders_.load();
    
    if (pending >= HIGH_THRESHOLD) {
        current_level_ = LoadLevel::CRITICAL;
    } else if (pending >= MEDIUM_THRESHOLD) {
        current_level_ = LoadLevel::HIGH;
    } else if (pending >= NORMAL_THRESHOLD) {
        current_level_ = LoadLevel::MEDIUM;
    } else {
        current_level_ = LoadLevel::NORMAL;
    }
    
    return current_level_.load();
}
```

### 4.3 优雅降级

```cpp
// graceful_degradation.h
#pragma once

#include "matching_engine.h"

class GracefulDegradation {
public:
    GracefulDegradation(MatchingEngine* engine);
    
    // 进入降级模式
    void EnterDegradedMode();
    
    // 退出降级模式
    void ExitDegradedMode();
    
    // 是否处于降级模式
    bool IsDegraded() const { return degraded_; }
    
private:
    // 降级策略
    void DisableNonCriticalFeatures();
    void ReduceLoggingLevel();
    void IncreaseMatchingInterval();
    void DisableMarketData();
    
private:
    MatchingEngine* engine_;
    std::atomic<bool> degraded_;
};

// graceful_degradation.cpp
void GracefulDegradation::EnterDegradedMode() {
    LOG_WARN("Entering degraded mode");
    
    degraded_ = true;
    
    // 1. 禁用非关键功能
    DisableNonCriticalFeatures();
    
    // 2. 降低日志级别
    ReduceLoggingLevel();
    
    // 3. 增加撮合间隔
    IncreaseMatchingInterval();
    
    // 4. 禁用行情推送
    DisableMarketData();
    
    LOG_INFO("Degraded mode activated");
}

void GracefulDegradation::DisableNonCriticalFeatures() {
    // 禁用统计功能
    engine_->DisableStatistics();
    
    // 禁用审计日志
    engine_->DisableAuditLog();
    
    // 禁用性能监控
    engine_->DisablePerformanceMonitoring();
}
```

## 5. 内存管理

### 5.1 内存池

```cpp
// memory_pool.h
#pragma once

#include <memory>
#include <vector>
#include <mutex>

template<typename T>
class MemoryPool {
public:
    MemoryPool(size_t initial_size = 1000);
    ~MemoryPool();
    
    // 分配对象
    T* Allocate();
    
    // 释放对象
    void Deallocate(T* ptr);
    
    // 获取统计信息
    struct Stats {
        size_t total_allocated;
        size_t in_use;
        size_t available;
    };
    
    Stats GetStats() const;
    
private:
    void Grow();
    
private:
    std::vector<T*> pool_;
    std::vector<T*> free_list_;
    std::mutex mutex_;
    
    size_t total_allocated_;
    size_t in_use_;
};

// memory_pool.cpp
template<typename T>
T* MemoryPool<T>::Allocate() {
    std::lock_guard<std::mutex> lock(mutex_);
    
    if (free_list_.empty()) {
        Grow();
    }
    
    T* ptr = free_list_.back();
    free_list_.pop_back();
    
    ++in_use_;
    
    return ptr;
}

template<typename T>
void MemoryPool<T>::Deallocate(T* ptr) {
    std::lock_guard<std::mutex> lock(mutex_);
    
    // 重置对象
    ptr->~T();
    new (ptr) T();
    
    free_list_.push_back(ptr);
    --in_use_;
}

template<typename T>
void MemoryPool<T>::Grow() {
    size_t grow_size = pool_.size() / 2;
    if (grow_size < 100) grow_size = 100;
    
    for (size_t i = 0; i < grow_size; ++i) {
        T* ptr = new T();
        pool_.push_back(ptr);
        free_list_.push_back(ptr);
    }
    
    total_allocated_ += grow_size;
    
    LOG_INFO("Memory pool grown: new_size={}", pool_.size());
}
```

### 5.2 内存泄漏检测

```cpp
// memory_leak_detector.h
#pragma once

#include <unordered_map>
#include <mutex>

struct AllocationInfo {
    void* address;
    size_t size;
    const char* file;
    int line;
    uint64_t timestamp;
};

class MemoryLeakDetector {
public:
    static MemoryLeakDetector& Instance();
    
    // 记录分配
    void RecordAllocation(void* ptr, size_t size, const char* file, int line);
    
    // 记录释放
    void RecordDeallocation(void* ptr);
    
    // 检查泄漏
    void CheckLeaks();
    
    // 生成报告
    void GenerateReport(const std::string& filename);
    
private:
    MemoryLeakDetector() = default;
    
private:
    std::unordered_map<void*, AllocationInfo> allocations_;
    std::mutex mutex_;
};

// 重载 new/delete
void* operator new(size_t size, const char* file, int line) {
    void* ptr = malloc(size);
    MemoryLeakDetector::Instance().RecordAllocation(ptr, size, file, line);
    return ptr;
}

void operator delete(void* ptr) noexcept {
    MemoryLeakDetector::Instance().RecordDeallocation(ptr);
    free(ptr);
}

#define new new(__FILE__, __LINE__)
```

## 6. 监控和告警

### 6.1 性能监控

```cpp
// performance_monitor.h
#pragma once

#include <atomic>
#include <chrono>

class PerformanceMonitor {
public:
    // 记录订单处理延迟
    void RecordOrderLatency(uint64_t latency_us);
    
    // 记录撮合延迟
    void RecordMatchingLatency(uint64_t latency_us);
    
    // 获取统计信息
    struct Stats {
        uint64_t total_orders;
        uint64_t total_trades;
        double avg_order_latency_us;
        double p99_order_latency_us;
        double avg_matching_latency_us;
        double throughput_tps;
    };
    
    Stats GetStats() const;
    
    // 检查是否超过阈值
    bool IsLatencyExceeded() const;
    
private:
    std::atomic<uint64_t> total_orders_;
    std::atomic<uint64_t> total_trades_;
    
    // 延迟统计
    std::vector<uint64_t> order_latencies_;
    std::vector<uint64_t> matching_latencies_;
    
    mutable std::mutex mutex_;
    
    static constexpr uint64_t LATENCY_THRESHOLD_US = 100;
};
```

### 6.2 健康检查

```cpp
// health_checker.h
#pragma once

#include "matching_engine.h"

enum class HealthStatus {
    HEALTHY,
    DEGRADED,
    UNHEALTHY
};

class HealthChecker {
public:
    HealthChecker(MatchingEngine* engine);
    
    // 执行健康检查
    HealthStatus Check();
    
    // 获取健康报告
    struct HealthReport {
        HealthStatus status;
        std::string message;
        std::map<std::string, std::string> details;
    };
    
    HealthReport GetReport() const;
    
private:
    bool CheckMemory();
    bool CheckCPU();
    bool CheckDisk();
    bool CheckNetwork();
    bool CheckOrderBooks();
    
private:
    MatchingEngine* engine_;
    HealthReport last_report_;
};
```

## 7. 总结

C++ 撮合引擎的兜底方案涵盖：

1. **高可用架构**: 主备集群、状态复制、快照机制
2. **故障恢复**: 崩溃恢复、日志重放、数据验证
3. **性能降级**: 限流保护、过载保护、优雅降级
4. **内存管理**: 内存池、泄漏检测
5. **监控告警**: 性能监控、健康检查

这些策略确保撮合引擎在极端情况下仍能保持高可用性和数据一致性。
