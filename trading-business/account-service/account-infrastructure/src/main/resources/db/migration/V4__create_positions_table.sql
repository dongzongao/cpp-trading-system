-- 创建持仓表
CREATE TABLE positions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    total_quantity DECIMAL(20, 8) NOT NULL DEFAULT 0,
    available_quantity DECIMAL(20, 8) NOT NULL DEFAULT 0,
    frozen_quantity DECIMAL(20, 8) NOT NULL DEFAULT 0,
    average_cost DECIMAL(20, 8) NOT NULL DEFAULT 0,
    currency VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_positions_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

-- 创建唯一索引(一个账户对一个品种只能有一个持仓)
CREATE UNIQUE INDEX idx_positions_account_symbol ON positions(account_id, symbol);

-- 创建其他索引
CREATE INDEX idx_positions_account_id ON positions(account_id);
CREATE INDEX idx_positions_symbol ON positions(symbol);
CREATE INDEX idx_positions_created_at ON positions(created_at);

-- 添加注释
COMMENT ON TABLE positions IS '持仓表';
COMMENT ON COLUMN positions.id IS '持仓ID';
COMMENT ON COLUMN positions.account_id IS '账户ID';
COMMENT ON COLUMN positions.symbol IS '交易品种代码';
COMMENT ON COLUMN positions.total_quantity IS '总持仓数量';
COMMENT ON COLUMN positions.available_quantity IS '可用持仓数量';
COMMENT ON COLUMN positions.frozen_quantity IS '冻结持仓数量';
COMMENT ON COLUMN positions.average_cost IS '平均成本';
COMMENT ON COLUMN positions.currency IS '货币类型';
COMMENT ON COLUMN positions.created_at IS '创建时间';
COMMENT ON COLUMN positions.updated_at IS '更新时间';

-- 添加约束检查
ALTER TABLE positions ADD CONSTRAINT chk_positions_quantity_non_negative 
    CHECK (total_quantity >= 0 AND available_quantity >= 0 AND frozen_quantity >= 0);
    
ALTER TABLE positions ADD CONSTRAINT chk_positions_quantity_consistency 
    CHECK (total_quantity = available_quantity + frozen_quantity);

ALTER TABLE positions ADD CONSTRAINT chk_positions_average_cost_non_negative 
    CHECK (average_cost >= 0);
