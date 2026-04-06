-- 创建账户表
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    total_balance DECIMAL(20, 8) NOT NULL DEFAULT 0,
    available_balance DECIMAL(20, 8) NOT NULL DEFAULT 0,
    frozen_balance DECIMAL(20, 8) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE UNIQUE INDEX idx_accounts_user_currency ON accounts(user_id, currency);
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_accounts_created_at ON accounts(created_at);

-- 添加注释
COMMENT ON TABLE accounts IS '账户表';
COMMENT ON COLUMN accounts.id IS '账户ID';
COMMENT ON COLUMN accounts.user_id IS '用户ID';
COMMENT ON COLUMN accounts.type IS '账户类型: CASH, MARGIN';
COMMENT ON COLUMN accounts.currency IS '货币类型';
COMMENT ON COLUMN accounts.total_balance IS '总余额';
COMMENT ON COLUMN accounts.available_balance IS '可用余额';
COMMENT ON COLUMN accounts.frozen_balance IS '冻结余额';
COMMENT ON COLUMN accounts.status IS '账户状态: ACTIVE, SUSPENDED, CLOSED';
COMMENT ON COLUMN accounts.created_at IS '创建时间';
COMMENT ON COLUMN accounts.updated_at IS '更新时间';

-- 添加约束检查
ALTER TABLE accounts ADD CONSTRAINT chk_balance_non_negative 
    CHECK (total_balance >= 0 AND available_balance >= 0 AND frozen_balance >= 0);
    
ALTER TABLE accounts ADD CONSTRAINT chk_balance_consistency 
    CHECK (total_balance = available_balance + frozen_balance);
