-- 插入测试账户数据
-- 注意: 这些数据仅用于开发和测试环境

-- 用户1的USD现金账户
INSERT INTO accounts (user_id, type, currency, total_balance, available_balance, frozen_balance, status, created_at, updated_at)
VALUES (1, 'CASH', 'USD', 10000.00000000, 10000.00000000, 0.00000000, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 用户1的BTC账户
INSERT INTO accounts (user_id, type, currency, total_balance, available_balance, frozen_balance, status, created_at, updated_at)
VALUES (1, 'CASH', 'BTC', 1.00000000, 1.00000000, 0.00000000, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 用户2的USD保证金账户
INSERT INTO accounts (user_id, type, currency, total_balance, available_balance, frozen_balance, status, created_at, updated_at)
VALUES (2, 'MARGIN', 'USD', 50000.00000000, 45000.00000000, 5000.00000000, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试持仓数据
INSERT INTO positions (account_id, symbol, total_quantity, available_quantity, frozen_quantity, average_cost, currency, created_at, updated_at)
VALUES (1, 'AAPL', 100.00000000, 100.00000000, 0.00000000, 150.50000000, 'USD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO positions (account_id, symbol, total_quantity, available_quantity, frozen_quantity, average_cost, currency, created_at, updated_at)
VALUES (1, 'TSLA', 50.00000000, 30.00000000, 20.00000000, 250.75000000, 'USD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO positions (account_id, symbol, total_quantity, available_quantity, frozen_quantity, average_cost, currency, created_at, updated_at)
VALUES (3, 'BTCUSD', 0.50000000, 0.30000000, 0.20000000, 45000.00000000, 'USD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试交易流水
INSERT INTO account_transactions (account_id, transaction_type, amount, currency, balance_before, balance_after, description, created_at)
VALUES (1, 'DEPOSIT', 10000.00000000, 'USD', 0.00000000, 10000.00000000, 'Initial deposit', CURRENT_TIMESTAMP);

INSERT INTO account_transactions (account_id, transaction_type, amount, currency, balance_before, balance_after, description, created_at)
VALUES (2, 'DEPOSIT', 50000.00000000, 'USD', 0.00000000, 50000.00000000, 'Initial deposit', CURRENT_TIMESTAMP);

INSERT INTO account_transactions (account_id, transaction_type, amount, currency, balance_before, balance_after, description, reference_id, created_at)
VALUES (3, 'FREEZE', 5000.00000000, 'USD', 50000.00000000, 50000.00000000, 'Freeze for order', 'ORDER-001', CURRENT_TIMESTAMP);
