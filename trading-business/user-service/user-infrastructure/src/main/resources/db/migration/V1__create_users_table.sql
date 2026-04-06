-- 创建用户表
CREATE TABLE users (
    user_id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    status VARCHAR(30) NOT NULL,
    roles VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    
    -- KYC 信息
    kyc_first_name VARCHAR(50),
    kyc_last_name VARCHAR(50),
    kyc_id_type VARCHAR(20),
    kyc_id_number VARCHAR(30),
    kyc_country VARCHAR(50),
    kyc_province VARCHAR(50),
    kyc_city VARCHAR(50),
    kyc_street VARCHAR(200),
    kyc_postal_code VARCHAR(20),
    kyc_date_of_birth TIMESTAMP,
    kyc_status VARCHAR(20),
    kyc_rejection_reason VARCHAR(500),
    kyc_submitted_at TIMESTAMP,
    kyc_reviewed_at TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_status ON users(status);
CREATE INDEX idx_created_at ON users(created_at);
CREATE INDEX idx_kyc_status ON users(kyc_status);

-- 添加注释
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.user_id IS '用户ID';
COMMENT ON COLUMN users.username IS '用户名';
COMMENT ON COLUMN users.email IS '邮箱';
COMMENT ON COLUMN users.password IS '密码（BCrypt加密）';
COMMENT ON COLUMN users.phone IS '手机号';
COMMENT ON COLUMN users.status IS '用户状态：PENDING_VERIFICATION, ACTIVE, SUSPENDED, CLOSED';
COMMENT ON COLUMN users.roles IS '角色列表（逗号分隔）';
COMMENT ON COLUMN users.kyc_status IS 'KYC状态：PENDING, APPROVED, REJECTED';
