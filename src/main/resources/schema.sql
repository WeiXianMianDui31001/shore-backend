-- 岸上见 数据库初始化脚本 (PostgreSQL)
-- 运行前请确保已创建数据库 shore_db

-- 扩展（可选，用于数组操作与模糊搜索）
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    email VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    nickname VARCHAR(64),
    avatar_url VARCHAR(512),
    role SMALLINT NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 0,
    points_balance INT NOT NULL DEFAULT 0,
    student_email VARCHAR(128),
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_status ON sys_user(role, status);

-- 2. 积分规则表
CREATE TABLE IF NOT EXISTS points_rule (
    id BIGINT PRIMARY KEY,
    version INT NOT NULL,
    download_cost INT NOT NULL,
    upload_reward INT NOT NULL,
    share_ratio DECIMAL(3,2) NOT NULL,
    daily_limit INT NOT NULL,
    effective_time TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT false,
    change_note TEXT,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_points_rule_active ON points_rule(is_active, effective_time DESC);

-- 3. 积分流水表
CREATE TABLE IF NOT EXISTS points_transaction (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type SMALLINT NOT NULL, -- 1收入 2支出
    amount INT NOT NULL,
    balance_after INT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    biz_id BIGINT,
    rule_version INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_points_tx_user_created ON points_transaction(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_points_tx_source ON points_transaction(source_type, biz_id);

-- 4. 资源表
CREATE TABLE IF NOT EXISTS resource (
    id BIGINT PRIMARY KEY,
    uploader_id BIGINT NOT NULL,
    title VARCHAR(256) NOT NULL,
    category VARCHAR(64),
    tags JSONB,
    description TEXT,
    object_key VARCHAR(512),
    file_size BIGINT,
    file_type VARCHAR(32),
    status SMALLINT NOT NULL DEFAULT 0, -- 0待审 1通过 2驳回
    points_cost INT NOT NULL DEFAULT 0,
    download_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_resource_status_created ON resource(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_resource_tags ON resource USING GIN (tags);
CREATE INDEX IF NOT EXISTS idx_resource_uploader ON resource(uploader_id);

-- 5. 资源审核记录表
CREATE TABLE IF NOT EXISTS resource_audit (
    id BIGINT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    action SMALLINT NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. 下载记录表
CREATE TABLE IF NOT EXISTS download_record (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    cost_points INT NOT NULL,
    uploader_reward INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_download_user ON download_record(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_download_resource ON download_record(resource_id);

-- 7. 信息库条目表
CREATE TABLE IF NOT EXISTS info_entry (
    id BIGINT PRIMARY KEY,
    title VARCHAR(256) NOT NULL,
    scene SMALLINT NOT NULL DEFAULT 0,
    category VARCHAR(64),
    tags JSONB,
    source_url VARCHAR(512),
    source_name VARCHAR(128),
    update_time DATE,
    status SMALLINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    admin_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_info_scene_status ON info_entry(scene, status, sort_order DESC);
CREATE INDEX IF NOT EXISTS idx_info_tags ON info_entry USING GIN (tags);

-- 8. 帖子表
CREATE TABLE IF NOT EXISTS post (
    id BIGINT PRIMARY KEY,
    author_id BIGINT NOT NULL,
    title VARCHAR(256) NOT NULL,
    content TEXT,
    tags JSONB,
    status SMALLINT NOT NULL DEFAULT 0, -- 0正常 1已解决 2已下架
    is_pinned BOOLEAN NOT NULL DEFAULT false,
    view_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_post_status_created ON post(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_post_author ON post(author_id);

-- 9. 评论表
CREATE TABLE IF NOT EXISTS comment (
    id BIGINT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    parent_id BIGINT,
    author_id BIGINT NOT NULL,
    content TEXT,
    status SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_comment_post ON comment(post_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comment_parent ON comment(parent_id);

-- 10. 通知表
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type SMALLINT NOT NULL, -- 0系统 1互动 2积分
    title VARCHAR(128),
    content TEXT,
    biz_id BIGINT,
    biz_type VARCHAR(32),
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notify_user_read ON notification(user_id, is_read, created_at DESC);

-- 11. 讨论室表
CREATE TABLE IF NOT EXISTS discussion_room (
    id BIGINT PRIMARY KEY,
    creator_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    password_hash VARCHAR(256),
    max_members INT NOT NULL DEFAULT 50,
    expire_at TIMESTAMP,
    closed_at TIMESTAMP,
    status SMALLINT NOT NULL DEFAULT 0, -- 0开启 1关闭
    whiteboard_snapshot JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 12. 讨论室成员表
CREATE TABLE IF NOT EXISTS room_member (
    id BIGINT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role SMALLINT NOT NULL DEFAULT 0,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_room_user UNIQUE (room_id, user_id)
);

-- 13. 聊天消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    msg_type SMALLINT NOT NULL DEFAULT 0, -- 0文本 1图片 2文件 3表情
    content TEXT,
    client_msg_id VARCHAR(64),
    sequence_no BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_chat_room ON chat_message(room_id, sequence_no DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_client_id ON chat_message(client_msg_id);

-- 14. 画板操作表
CREATE TABLE IF NOT EXISTS whiteboard_operation (
    id BIGINT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    op_type VARCHAR(32) NOT NULL,
    op_data JSONB,
    sequence_no BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_wb_room_seq ON whiteboard_operation(room_id, sequence_no);

-- 15. 简历模板表
CREATE TABLE IF NOT EXISTS resume_template (
    id BIGINT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    type SMALLINT NOT NULL DEFAULT 0, -- 0考研 1求职
    thumbnail_url VARCHAR(512),
    structure_json JSONB,
    status SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 16. 简历记录表
CREATE TABLE IF NOT EXISTS resume (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    title VARCHAR(128),
    content_json JSONB,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_resume_user ON resume(user_id, updated_at DESC);

-- 17. 简历导出记录表
CREATE TABLE IF NOT EXISTS resume_export (
    id BIGINT PRIMARY KEY,
    resume_id BIGINT NOT NULL,
    pdf_url VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 18. 管理员操作日志表
CREATE TABLE IF NOT EXISTS admin_log (
    id BIGINT PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(32),
    target_id BIGINT,
    detail JSONB,
    ip INET,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_admin_log_admin ON admin_log(admin_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_log_target ON admin_log(target_type, target_id);

-- 19. 用户收藏表
CREATE TABLE IF NOT EXISTS user_favorite (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_favorite UNIQUE (user_id, target_type, target_id)
);

-- 插入默认管理员账号 (密码: admin123)
INSERT INTO sys_user (id, email, password_hash, nickname, role, status, points_balance, created_at, updated_at)
VALUES (1, 'admin@anzs.com', '$2b$10$e/C4UJ/GKPait4KxSYBtJORxmCva.wyUq8BQafrV1m8Ieni1Pk4pK', '系统管理员', 2, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- 插入默认积分规则
INSERT INTO points_rule (id, version, download_cost, upload_reward, share_ratio, daily_limit, effective_time, is_active, change_note, created_at)
VALUES (1, 1, 5, 10, 0.30, 50, CURRENT_TIMESTAMP, true, '初始规则', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 插入示例简历模板
INSERT INTO resume_template (id, name, type, thumbnail_url, structure_json, status, created_at)
VALUES (1, '考研复试通用模板', 0, 'https://cdn.anzs.com/template_1.jpg', '[{"section":"基本信息"},{"section":"教育背景"},{"section":"科研经历"},{"section":"获奖情况"}]', 0, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO resume_template (id, name, type, thumbnail_url, structure_json, status, created_at)
VALUES (2, '互联网求职模板', 1, 'https://cdn.anzs.com/template_2.jpg', '[{"section":"基本信息"},{"section":"教育背景"},{"section":"实习经历"},{"section":"项目经验"},{"section":"技能特长"}]', 0, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 插入示例信息库数据
INSERT INTO info_entry (id, title, scene, category, tags, source_url, source_name, update_time, status, sort_order, admin_id, created_at, updated_at)
VALUES (1, '北京大学 2026 年硕士研究生招生简章', 0, '招生简章', '["北大", "计算机"]', 'https://grs.pku.edu.cn/', '北京大学研究生院', '2026-03-15', 0, 100, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO info_entry (id, title, scene, category, tags, source_url, source_name, update_time, status, sort_order, admin_id, created_at, updated_at)
VALUES (2, '阿里巴巴 2026 届校园招聘正式启动', 1, '校招信息', '["阿里", "互联网"]', 'https://talent.alibaba.com/', '阿里巴巴集团', '2026-03-20', 0, 90, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
