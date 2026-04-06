-- 创建账户交易流水表(分区表)
CREATE TABLE account_transactions (
    id BIGSERIAL,
    account_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(20, 8) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    balance_before DECIMAL(20, 8) NOT NULL,
    balance_after DECIMAL(20, 8) NOT NULL,
    description VARCHAR(500),
    reference_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 创建索引
CREATE INDEX idx_transactions_account_id ON account_transactions(account_id);
CREATE INDEX idx_transactions_created_at ON account_transactions(created_at);
CREATE INDEX idx_transactions_account_date ON account_transactions(account_id, created_at DESC);
CREATE INDEX idx_transactions_reference_id ON account_transactions(reference_id);
CREATE INDEX idx_transactions_type ON account_transactions(transaction_type);

-- 添加注释
COMMENT ON TABLE account_transactions IS '账户交易流水表';
COMMENT ON COLUMN account_transactions.id IS '流水ID';
COMMENT ON COLUMN account_transactions.account_id IS '账户ID';
COMMENT ON COLUMN account_transactions.transaction_type IS '交易类型';
COMMENT ON COLUMN account_transactions.amount IS '交易金额';
COMMENT ON COLUMN account_transactions.currency IS '货币类型';
COMMENT ON COLUMN account_transactions.balance_before IS '交易前余额';
COMMENT ON COLUMN account_transactions.balance_after IS '交易后余额';
COMMENT ON COLUMN account_transactions.description IS '描述';
COMMENT ON COLUMN account_transactions.reference_id IS '关联ID(订单ID等)';
COMMENT ON COLUMN account_transactions.created_at IS '创建时间';

-- 创建分区表(按月分区,提高查询性能)
-- 注意: PostgreSQL 10+ 支持声明式分区
CREATE TABLE account_transactions_y2024m01 PARTITION OF account_transactions
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE account_transactions_y2024m02 PARTITION OF account_transactions
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

CREATE TABLE account_transactions_y2024m03 PARTITION OF account_transactions
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');

CREATE TABLE account_transactions_y2024m04 PARTITION OF account_transactions
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');

CREATE TABLE account_transactions_y2024m05 PARTITION OF account_transactions
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');

CREATE TABLE account_transactions_y2024m06 PARTITION OF account_transactions
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');

CREATE TABLE account_transactions_y2024m07 PARTITION OF account_transactions
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');

CREATE TABLE account_transactions_y2024m08 PARTITION OF account_transactions
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');

CREATE TABLE account_transactions_y2024m09 PARTITION OF account_transactions
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');

CREATE TABLE account_transactions_y2024m10 PARTITION OF account_transactions
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');

CREATE TABLE account_transactions_y2024m11 PARTITION OF account_transactions
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE TABLE account_transactions_y2024m12 PARTITION OF account_transactions
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- 2025年分区
CREATE TABLE account_transactions_y2025m01 PARTITION OF account_transactions
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE account_transactions_y2025m02 PARTITION OF account_transactions
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE account_transactions_y2025m03 PARTITION OF account_transactions
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE TABLE account_transactions_y2025m04 PARTITION OF account_transactions
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');

CREATE TABLE account_transactions_y2025m05 PARTITION OF account_transactions
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');

CREATE TABLE account_transactions_y2025m06 PARTITION OF account_transactions
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');

CREATE TABLE account_transactions_y2025m07 PARTITION OF account_transactions
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');

CREATE TABLE account_transactions_y2025m08 PARTITION OF account_transactions
    FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');

CREATE TABLE account_transactions_y2025m09 PARTITION OF account_transactions
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');

CREATE TABLE account_transactions_y2025m10 PARTITION OF account_transactions
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');

CREATE TABLE account_transactions_y2025m11 PARTITION OF account_transactions
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

CREATE TABLE account_transactions_y2025m12 PARTITION OF account_transactions
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- 2026年分区
CREATE TABLE account_transactions_y2026m01 PARTITION OF account_transactions
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

CREATE TABLE account_transactions_y2026m02 PARTITION OF account_transactions
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');

CREATE TABLE account_transactions_y2026m03 PARTITION OF account_transactions
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE TABLE account_transactions_y2026m04 PARTITION OF account_transactions
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

CREATE TABLE account_transactions_y2026m05 PARTITION OF account_transactions
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

CREATE TABLE account_transactions_y2026m06 PARTITION OF account_transactions
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

CREATE TABLE account_transactions_y2026m07 PARTITION OF account_transactions
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');

CREATE TABLE account_transactions_y2026m08 PARTITION OF account_transactions
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

CREATE TABLE account_transactions_y2026m09 PARTITION OF account_transactions
    FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');

CREATE TABLE account_transactions_y2026m10 PARTITION OF account_transactions
    FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');

CREATE TABLE account_transactions_y2026m11 PARTITION OF account_transactions
    FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');

CREATE TABLE account_transactions_y2026m12 PARTITION OF account_transactions
    FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');

-- 创建默认分区(用于未来日期)
CREATE TABLE account_transactions_default PARTITION OF account_transactions DEFAULT;
