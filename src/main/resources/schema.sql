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
    file_type VARCHAR(128),
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
    images JSONB,
    scene SMALLINT NOT NULL DEFAULT 0, -- 0考研 1求职
    status SMALLINT NOT NULL DEFAULT 0, -- 0正常 1已解决 2已下架
    is_pinned BOOLEAN NOT NULL DEFAULT false,
    view_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    collect_count INT NOT NULL DEFAULT 0,
    endorse_count INT NOT NULL DEFAULT 0,
    is_excellent BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_post_status_created ON post(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_post_author ON post(author_id);
CREATE INDEX IF NOT EXISTS idx_post_scene ON post(scene, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_post_title_trgm ON post USING GIN (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_post_content_trgm ON post USING GIN (content gin_trgm_ops);

-- 9. 点赞表
CREATE TABLE IF NOT EXISTS post_like (
    id BIGINT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_post_user_like UNIQUE (post_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_post_like_user ON post_like(user_id, created_at DESC);

-- 10. 收藏表
CREATE TABLE IF NOT EXISTS post_collect (
    id BIGINT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_post_user_collect UNIQUE (post_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_post_collect_user ON post_collect(user_id, created_at DESC);

-- 11. 评论表
CREATE TABLE IF NOT EXISTS comment (
    id BIGINT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    parent_id BIGINT,
    author_id BIGINT NOT NULL,
    content TEXT,
    images JSONB,
    status SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_comment_post ON comment(post_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comment_parent ON comment(parent_id);

-- 12. 帖子认可表
CREATE TABLE IF NOT EXISTS post_endorse (
    id BIGINT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_post_endorse_user UNIQUE (post_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_post_endorse_post ON post_endorse(post_id);
CREATE INDEX IF NOT EXISTS idx_post_endorse_user ON post_endorse(user_id);

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
    template_key VARCHAR(64),
    template_version VARCHAR(16) DEFAULT 'v1',
    status SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 16. 简历版本表
CREATE TABLE IF NOT EXISTS resume_version (
    id BIGINT PRIMARY KEY,
    resume_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    template_id BIGINT NOT NULL,
    content_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_resume_version UNIQUE (resume_id, version_no)
);
CREATE INDEX IF NOT EXISTS idx_resume_version_resume ON resume_version(resume_id, version_no DESC);

-- 17. 简历记录表
CREATE TABLE IF NOT EXISTS resume (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    title VARCHAR(128),
    content_json JSONB,
    version INT NOT NULL DEFAULT 1,
    current_version_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_resume_user ON resume(user_id, updated_at DESC);

-- 18. 简历导出记录表
CREATE TABLE IF NOT EXISTS resume_export (
    id BIGINT PRIMARY KEY,
    resume_id BIGINT NOT NULL,
    pdf_url VARCHAR(512),
    status SMALLINT NOT NULL DEFAULT 0,
    file_size BIGINT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_resume_export_status ON resume_export(status);

-- 19. 管理员操作日志表
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

-- 20. 用户收藏表
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

-- 插入示例简历模板（新 sections 数组结构，模板 HTML 已移至 classpath:/templates/resume/）
INSERT INTO resume_template (id, name, type, thumbnail_url, structure_json, template_key, template_version, status, created_at)
VALUES (1, '考研复试通用模板', 0, 'https://cdn.anzs.com/template_1.jpg',
'[{"key":"basic","title":"基本信息","layout":"basic","fields":["姓名","年龄","性别","籍贯","工作年限","电话","邮箱"],"sampleItems":[{"姓名":"全民简历","年龄":"27岁","性别":"男","籍贯":"上海","工作年限":"2年经验","电话":"15388888883","邮箱":"qmjianli@qq.com"}]},{"key":"exam_apply","title":"报考信息","layout":"kv","fields":["报考院校","报考专业","数学","英语","计算机综合","政治理论","总分"],"sampleItems":[{"报考院校":"云南大学","报考专业":"土木工程","数学":"86","英语":"89","计算机综合":"91","政治理论":"90","总分":"380"}]},{"key":"education","title":"教育背景","layout":"entry_headline_date","headlineFields":["学校","专业","学历"],"dateFields":["入学时间","毕业时间"],"moreFields":["补充说明"],"fields":["学校","专业","学历","入学时间","毕业时间","补充说明"],"sampleItems":[{"学校":"全民简历师范大学","专业":"工商管理","学历":"本科","入学时间":"2012-09","毕业时间":"2016-07","补充说明":"专业成绩：GPA 3.66/4 （专业前5%）\n主修课程：基础会计学、货币银行学、统计学、经济法概论、财务会计学、管理学原理、组织行为学、市场营销学、国际贸易理论、国际贸易实务、人力资源开发与管理、财务管理学、企业经营战略概论、质量管理学、西方经济学等等。"}]},{"key":"work","title":"工作经历","layout":"rich_entry","headlineFields":["公司名称"],"dateFields":["起止时间"],"sublineFields":["职位"],"bodyFields":["工作描述"],"fields":["公司名称","职位","起止时间","工作描述","要点"],"sampleItems":[{"公司名称":"全民简历科技有限公司","职位":"行政主管","起止时间":"2018-09 ~ 至今","工作描述":"","要点":"• 拥负责本部的行政人事管理和日常事务，协助总监搞好各部门之间的综合协调，落实公司规章制度，沟通内外联系，保证上情下达和下情上报，负责对会议文件决定的事项进行催办、查办和落实，负责全公司组织系统研讨和修订。\n• 编制公司人事管理制度，规避各项人事风险。"},{"公司名称":"上海斧掌网络科技有限公司","职位":"行政专员","起止时间":"2016-09 ~ 2018-08","工作描述":"","要点":"• 负责中心简单财务管理，资产管控；\n• 负责公司总部的来访客户接待工作，负责引导和介绍公司的分布情况；\n• 负责中心的行政事务，公司班车管理、负责建立员工归属感及前台管理；\n• 负责招聘工作，确保人才梯队发展和人才储备及培养。\n• 督导公司各项行政、人事制度、员工福利、生日以及公司各种宴会活动的执行。\n• 负责招聘工作，制定公司的人力资源发展计划，确保人才梯队发展和人才储备及培养。"}]},{"key":"honors","title":"荣誉证书","layout":"kv","fields":["说明"],"sampleItems":[{"说明":"• 英语四级，听说读写能力良好，能流利的用英语进行日常交流，能快速浏览英文文档和书籍；\n• 通过全国计算机二级考试，熟练运用office等常用的办公软件。"}]},{"key":"evaluation","title":"自我评价","layout":"kv","fields":["自我评价"],"sampleItems":[{"自我评价":"工作积极认真，细心负责，熟练运用办公自动化软件，善于在工作中提出问题、发现问题、解决问题，有较强的分析能力；勤奋好学，踏实肯干，动手能力强，认真负责，有很强的社会责任感；坚毅不拔，吃苦耐劳，喜欢迎接新挑战。"}]}]',
'classic_academic_v1', 'v1', 0, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO resume_template (id, name, type, thumbnail_url, structure_json, template_key, template_version, status, created_at)
VALUES (2, '互联网求职模板', 1, 'https://cdn.anzs.com/template_2.jpg',
'[{"key":"basic","title":"基本信息","layout":"basic","fields":["姓名","邮箱","电话","出生日期","所在地","求职意向"],"sampleItems":[{"姓名":"王芳","邮箱":"wangfang@example.com","电话":"139-0000-0000","出生日期":"1999.03","所在地":"上海市","求职意向":"Java 后端开发"}]},{"key":"education","title":"教育背景","layout":"entry_headline_date","headlineFields":["学校","专业","学历"],"dateFields":["入学时间","毕业时间"],"moreFields":["GPA"],"fields":["学校","专业","学历","入学时间","毕业时间","GPA"],"sampleItems":[{"学校":"上海理工大学","专业":"软件工程","学历":"本科","入学时间":"2018.09","毕业时间":"2022.06","GPA":"3.5/4.0"}]},{"key":"internship","title":"实习经历","layout":"rich_entry","headlineFields":["公司名称"],"dateFields":["起止时间"],"sublineFields":["职位"],"bodyFields":["工作描述"],"fields":["公司名称","职位","起止时间","工作描述","要点"],"sampleItems":[{"公司名称":"示例互联网科技有限公司","职位":"后端开发实习生","起止时间":"2024.06 - 2024.09","工作描述":"参与订单与支付相关接口开发与联调。","要点":"• 使用 Spring Boot 编写 REST 接口并完成单元测试\n• 配合前端完成接口文档与 Mock 数据"},{"公司名称":"示例云计算有限公司","职位":"研发实习生","起止时间":"2023.07 - 2023.09","工作描述":"参与内部运营后台的迭代与缺陷修复。","要点":"• 熟悉 Git 协作流程与 Code Review\n• 编写 SQL 与简单性能排查文档"}]},{"key":"projects","title":"项目经验","layout":"rich_entry","headlineFields":["项目名称"],"dateFields":[],"sublineFields":["担任角色","技术栈"],"bodyFields":["项目描述"],"fields":["项目名称","担任角色","技术栈","项目描述","要点"],"sampleItems":[{"项目名称":"校园二手交易平台","担任角色":"后端负责人","技术栈":"Spring Boot、MySQL、Redis","项目描述":"课程设计项目，负责用户、商品与订单模块设计与实现。","要点":"• 完成登录鉴权与接口幂等处理\n• 部署演示环境并撰写设计说明"}]},{"key":"skills","title":"技能特长","layout":"kv","fields":["技能名称","掌握程度","证书"],"sampleItems":[{"技能名称":"Java / Spring","掌握程度":"熟练","证书":"—"},{"技能名称":"MySQL","掌握程度":"熟练","证书":"—"},{"技能名称":"英语","掌握程度":"读写良好","证书":"CET-6"}]}]',
'internet_job_v1', 'v1', 0, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 中文印刷风简历（借鉴常见中文简历版式：左文右照、黑字章节线；证件照填 HTTPS 图片 URL）
INSERT INTO resume_template (id, name, type, thumbnail_url, structure_json, template_key, template_version, status, created_at)
VALUES (3, '中文简历（印刷风·求职）', 1, 'https://cdn.anzs.com/template_cn_print.jpg',
'[{"key":"basic","title":"基本信息","layout":"basic","fields":["姓名","电话","邮箱","籍贯","政治面貌","求职意向","证件照"],"sampleItems":[{"姓名":"李想","电话":"137-0000-0000","邮箱":"lixiang@example.com","籍贯":"江苏省南京市","政治面貌":"中共党员","求职意向":"行政专员 / 综合岗","证件照":""}]},{"key":"education","title":"教育背景","layout":"entry_headline_date","headlineFields":["学校","专业","学历"],"dateFields":["入学时间","毕业时间"],"moreFields":["GPA"],"fields":["学校","专业","学历","入学时间","毕业时间","GPA"],"sampleItems":[{"学校":"南京师范大学","专业":"工商管理","学历":"本科","入学时间":"2016.09","毕业时间":"2020.06","GPA":"3.6/4.0"}]},{"key":"internship","title":"实习经历","layout":"rich_entry","headlineFields":["公司名称"],"dateFields":["起止时间"],"sublineFields":["职位"],"bodyFields":["工作描述"],"fields":["公司名称","职位","起止时间","工作描述","要点"],"sampleItems":[{"公司名称":"示例科技有限公司","职位":"行政专员","起止时间":"2020.07 - 2021.06","工作描述":"协助部门完成日常行政、会议与档案管理。","要点":"• 负责来访接待与办公用品采购\n• 维护固定资产台账并参与年检材料整理"}]},{"key":"projects","title":"项目经历","layout":"rich_entry","headlineFields":["项目名称"],"dateFields":[],"sublineFields":["担任角色","技术栈"],"bodyFields":["项目描述"],"fields":["项目名称","担任角色","技术栈","项目描述","要点"],"sampleItems":[{"项目名称":"企业文化宣传周","担任角色":"策划组成员","技术栈":"海报设计、公众号排版","项目描述":"协助策划线下活动与线上推文排期。","要点":"• 协调场地与物料\n• 跟进活动反馈问卷"}]},{"key":"student","title":"学生工作","layout":"rich_entry","headlineFields":["组织名称"],"dateFields":["起止时间"],"sublineFields":["职务"],"bodyFields":["工作内容"],"fields":["组织名称","职务","起止时间","工作内容","要点"],"sampleItems":[{"组织名称":"校学生会","职务":"办公室主任","起止时间":"2017.09 - 2019.06","工作内容":"统筹例会、值班表与活动后勤。","要点":"• 组织校级文艺晚会后勤志愿队\n• 优化物资借用流程"}]},{"key":"evaluation","title":"个人评价","layout":"kv","fields":["自我评价"],"sampleItems":[{"自我评价":"工作认真细致，熟练使用 Office 办公软件；具备良好的沟通协调能力与文档整理习惯；学习能力强，能快速适应新环境并完成交办任务。"}]}]',
'resume_cn_print_v1', 'v1', 0, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 插入示例信息库数据
INSERT INTO info_entry (id, title, scene, category, tags, source_url, source_name, update_time, status, sort_order, admin_id, created_at, updated_at)
VALUES (1, '北京大学 2026 年硕士研究生招生简章', 0, '招生简章', '["北大", "计算机"]', 'https://grs.pku.edu.cn/', '北京大学研究生院', '2026-03-15', 0, 100, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO info_entry (id, title, scene, category, tags, source_url, source_name, update_time, status, sort_order, admin_id, created_at, updated_at)
VALUES (2, '阿里巴巴 2026 届校园招聘正式启动', 1, '校招信息', '["阿里", "互联网"]', 'https://talent.alibaba.com/', '阿里巴巴集团', '2026-03-20', 0, 90, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- === 升级兼容 SQL (对已有数据库增量添加新列) ===

ALTER TABLE resume_template ADD COLUMN IF NOT EXISTS template_key VARCHAR(64);
ALTER TABLE resume_template ADD COLUMN IF NOT EXISTS template_version VARCHAR(16) DEFAULT 'v1';

ALTER TABLE resume ADD COLUMN IF NOT EXISTS current_version_id BIGINT;

ALTER TABLE resume_export ADD COLUMN IF NOT EXISTS status SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE resume_export ADD COLUMN IF NOT EXISTS file_size BIGINT;
ALTER TABLE resume_export ADD COLUMN IF NOT EXISTS error_message TEXT;

-- 删除已废弃的 html_template 列（模板 HTML 已移至 classpath:/templates/resume/）
ALTER TABLE resume_template DROP COLUMN IF EXISTS html_template;

-- 更新已有模板的 template_key 和 structureJson（新 sections 数组格式）
UPDATE resume_template SET
  template_key = 'classic_academic_v1',
  template_version = 'v1',
  structure_json = '[{"key":"basic","title":"基本信息","layout":"basic","fields":["姓名","年龄","性别","籍贯","工作年限","电话","邮箱"],"sampleItems":[{"姓名":"全民简历","年龄":"27岁","性别":"男","籍贯":"上海","工作年限":"2年经验","电话":"15388888883","邮箱":"qmjianli@qq.com"}]},{"key":"exam_apply","title":"报考信息","layout":"kv","fields":["报考院校","报考专业","数学","英语","计算机综合","政治理论","总分"],"sampleItems":[{"报考院校":"云南大学","报考专业":"土木工程","数学":"86","英语":"89","计算机综合":"91","政治理论":"90","总分":"380"}]},{"key":"education","title":"教育背景","layout":"entry_headline_date","headlineFields":["学校","专业","学历"],"dateFields":["入学时间","毕业时间"],"moreFields":["补充说明"],"fields":["学校","专业","学历","入学时间","毕业时间","补充说明"],"sampleItems":[{"学校":"全民简历师范大学","专业":"工商管理","学历":"本科","入学时间":"2012-09","毕业时间":"2016-07","补充说明":"专业成绩：GPA 3.66/4 （专业前5%）\n主修课程：基础会计学、货币银行学、统计学、经济法概论、财务会计学、管理学原理、组织行为学、市场营销学、国际贸易理论、国际贸易实务、人力资源开发与管理、财务管理学、企业经营战略概论、质量管理学、西方经济学等等。"}]},{"key":"work","title":"工作经历","layout":"rich_entry","headlineFields":["公司名称"],"dateFields":["起止时间"],"sublineFields":["职位"],"bodyFields":["工作描述"],"fields":["公司名称","职位","起止时间","工作描述","要点"],"sampleItems":[{"公司名称":"全民简历科技有限公司","职位":"行政主管","起止时间":"2018-09 ~ 至今","工作描述":"","要点":"• 拥负责本部的行政人事管理和日常事务，协助总监搞好各部门之间的综合协调，落实公司规章制度，沟通内外联系，保证上情下达和下情上报，负责对会议文件决定的事项进行催办、查办和落实，负责全公司组织系统研讨和修订。\n• 编制公司人事管理制度，规避各项人事风险。"},{"公司名称":"上海斧掌网络科技有限公司","职位":"行政专员","起止时间":"2016-09 ~ 2018-08","工作描述":"","要点":"• 负责中心简单财务管理，资产管控；\n• 负责公司总部的来访客户接待工作，负责引导和介绍公司的分布情况；\n• 负责中心的行政事务，公司班车管理、负责建立员工归属感及前台管理；\n• 负责招聘工作，确保人才梯队发展和人才储备及培养。\n• 督导公司各项行政、人事制度、员工福利、生日以及公司各种宴会活动的执行。\n• 负责招聘工作，制定公司的人力资源发展计划，确保人才梯队发展和人才储备及培养。"}]},{"key":"honors","title":"荣誉证书","layout":"kv","fields":["说明"],"sampleItems":[{"说明":"• 英语四级，听说读写能力良好，能流利的用英语进行日常交流，能快速浏览英文文档和书籍；\n• 通过全国计算机二级考试，熟练运用office等常用的办公软件。"}]},{"key":"evaluation","title":"自我评价","layout":"kv","fields":["自我评价"],"sampleItems":[{"自我评价":"工作积极认真，细心负责，熟练运用办公自动化软件，善于在工作中提出问题、发现问题、解决问题，有较强的分析能力；勤奋好学，踏实肯干，动手能力强，认真负责，有很强的社会责任感；坚毅不拔，吃苦耐劳，喜欢迎接新挑战。"}]}]'
WHERE id = 1 AND template_key IS NULL;

UPDATE resume_template SET
  template_key = 'internet_job_v1',
  template_version = 'v1',
  structure_json = '[{"key":"basic","title":"基本信息","layout":"basic","fields":["姓名","邮箱","电话","出生日期","所在地","求职意向"],"sampleItems":[{"姓名":"王芳","邮箱":"wangfang@example.com","电话":"139-0000-0000","出生日期":"1999.03","所在地":"上海市","求职意向":"Java 后端开发"}]},{"key":"education","title":"教育背景","layout":"entry_headline_date","headlineFields":["学校","专业","学历"],"dateFields":["入学时间","毕业时间"],"moreFields":["GPA"],"fields":["学校","专业","学历","入学时间","毕业时间","GPA"],"sampleItems":[{"学校":"上海理工大学","专业":"软件工程","学历":"本科","入学时间":"2018.09","毕业时间":"2022.06","GPA":"3.5/4.0"}]},{"key":"internship","title":"实习经历","layout":"rich_entry","headlineFields":["公司名称"],"dateFields":["起止时间"],"sublineFields":["职位"],"bodyFields":["工作描述"],"fields":["公司名称","职位","起止时间","工作描述","要点"],"sampleItems":[{"公司名称":"示例互联网科技有限公司","职位":"后端开发实习生","起止时间":"2024.06 - 2024.09","工作描述":"参与订单与支付相关接口开发与联调。","要点":"• 使用 Spring Boot 编写 REST 接口并完成单元测试\n• 配合前端完成接口文档与 Mock 数据"},{"公司名称":"示例云计算有限公司","职位":"研发实习生","起止时间":"2023.07 - 2023.09","工作描述":"参与内部运营后台的迭代与缺陷修复。","要点":"• 熟悉 Git 协作流程与 Code Review\n• 编写 SQL 与简单性能排查文档"}]},{"key":"projects","title":"项目经验","layout":"rich_entry","headlineFields":["项目名称"],"dateFields":[],"sublineFields":["担任角色","技术栈"],"bodyFields":["项目描述"],"fields":["项目名称","担任角色","技术栈","项目描述","要点"],"sampleItems":[{"项目名称":"校园二手交易平台","担任角色":"后端负责人","技术栈":"Spring Boot、MySQL、Redis","项目描述":"课程设计项目，负责用户、商品与订单模块设计与实现。","要点":"• 完成登录鉴权与接口幂等处理\n• 部署演示环境并撰写设计说明"}]},{"key":"skills","title":"技能特长","layout":"kv","fields":["技能名称","掌握程度","证书"],"sampleItems":[{"技能名称":"Java / Spring","掌握程度":"熟练","证书":"—"},{"技能名称":"MySQL","掌握程度":"熟练","证书":"—"},{"技能名称":"英语","掌握程度":"读写良好","证书":"CET-6"}]}]'
WHERE id = 2 AND template_key IS NULL;

-- 已存在库：写入带 sampleItems 的范文结构（新建简历时预填；不覆盖用户已改 template_key）
UPDATE resume_template SET structure_json = '[{"key":"basic","title":"基本信息","layout":"basic","fields":["姓名","年龄","性别","籍贯","工作年限","电话","邮箱"],"sampleItems":[{"姓名":"全民简历","年龄":"27岁","性别":"男","籍贯":"上海","工作年限":"2年经验","电话":"15388888883","邮箱":"qmjianli@qq.com"}]},{"key":"exam_apply","title":"报考信息","layout":"kv","fields":["报考院校","报考专业","数学","英语","计算机综合","政治理论","总分"],"sampleItems":[{"报考院校":"云南大学","报考专业":"土木工程","数学":"86","英语":"89","计算机综合":"91","政治理论":"90","总分":"380"}]},{"key":"education","title":"教育背景","layout":"entry_headline_date","headlineFields":["学校","专业","学历"],"dateFields":["入学时间","毕业时间"],"moreFields":["补充说明"],"fields":["学校","专业","学历","入学时间","毕业时间","补充说明"],"sampleItems":[{"学校":"全民简历师范大学","专业":"工商管理","学历":"本科","入学时间":"2012-09","毕业时间":"2016-07","补充说明":"专业成绩：GPA 3.66/4 （专业前5%）\n主修课程：基础会计学、货币银行学、统计学、经济法概论、财务会计学、管理学原理、组织行为学、市场营销学、国际贸易理论、国际贸易实务、人力资源开发与管理、财务管理学、企业经营战略概论、质量管理学、西方经济学等等。"}]},{"key":"work","title":"工作经历","layout":"rich_entry","headlineFields":["公司名称"],"dateFields":["起止时间"],"sublineFields":["职位"],"bodyFields":["工作描述"],"fields":["公司名称","职位","起止时间","工作描述","要点"],"sampleItems":[{"公司名称":"全民简历科技有限公司","职位":"行政主管","起止时间":"2018-09 ~ 至今","工作描述":"","要点":"• 拥负责本部的行政人事管理和日常事务，协助总监搞好各部门之间的综合协调，落实公司规章制度，沟通内外联系，保证上情下达和下情上报，负责对会议文件决定的事项进行催办、查办和落实，负责全公司组织系统研讨和修订。\n• 编制公司人事管理制度，规避各项人事风险。"},{"公司名称":"上海斧掌网络科技有限公司","职位":"行政专员","起止时间":"2016-09 ~ 2018-08","工作描述":"","要点":"• 负责中心简单财务管理，资产管控；\n• 负责公司总部的来访客户接待工作，负责引导和介绍公司的分布情况；\n• 负责中心的行政事务，公司班车管理、负责建立员工归属感及前台管理；\n• 负责招聘工作，确保人才梯队发展和人才储备及培养。\n• 督导公司各项行政、人事制度、员工福利、生日以及公司各种宴会活动的执行。\n• 负责招聘工作，制定公司的人力资源发展计划，确保人才梯队发展和人才储备及培养。"}]},{"key":"honors","title":"荣誉证书","layout":"kv","fields":["说明"],"sampleItems":[{"说明":"• 英语四级，听说读写能力良好，能流利的用英语进行日常交流，能快速浏览英文文档和书籍；\n• 通过全国计算机二级考试，熟练运用office等常用的办公软件。"}]},{"key":"evaluation","title":"自我评价","layout":"kv","fields":["自我评价"],"sampleItems":[{"自我评价":"工作积极认真，细心负责，熟练运用办公自动化软件，善于在工作中提出问题、发现问题、解决问题，有较强的分析能力；勤奋好学，踏实肯干，动手能力强，认真负责，有很强的社会责任感；坚毅不拔，吃苦耐劳，喜欢迎接新挑战。"}]}]' WHERE id = 1;
UPDATE resume_template SET structure_json = '[{"key":"basic","title":"基本信息","layout":"basic","fields":["姓名","邮箱","电话","出生日期","所在地","求职意向"],"sampleItems":[{"姓名":"王芳","邮箱":"wangfang@example.com","电话":"139-0000-0000","出生日期":"1999.03","所在地":"上海市","求职意向":"Java 后端开发"}]},{"key":"education","title":"教育背景","layout":"entry_headline_date","headlineFields":["学校","专业","学历"],"dateFields":["入学时间","毕业时间"],"moreFields":["GPA"],"fields":["学校","专业","学历","入学时间","毕业时间","GPA"],"sampleItems":[{"学校":"上海理工大学","专业":"软件工程","学历":"本科","入学时间":"2018.09","毕业时间":"2022.06","GPA":"3.5/4.0"}]},{"key":"internship","title":"实习经历","layout":"rich_entry","headlineFields":["公司名称"],"dateFields":["起止时间"],"sublineFields":["职位"],"bodyFields":["工作描述"],"fields":["公司名称","职位","起止时间","工作描述","要点"],"sampleItems":[{"公司名称":"示例互联网科技有限公司","职位":"后端开发实习生","起止时间":"2024.06 - 2024.09","工作描述":"参与订单与支付相关接口开发与联调。","要点":"• 使用 Spring Boot 编写 REST 接口并完成单元测试\n• 配合前端完成接口文档与 Mock 数据"},{"公司名称":"示例云计算有限公司","职位":"研发实习生","起止时间":"2023.07 - 2023.09","工作描述":"参与内部运营后台的迭代与缺陷修复。","要点":"• 熟悉 Git 协作流程与 Code Review\n• 编写 SQL 与简单性能排查文档"}]},{"key":"projects","title":"项目经验","layout":"rich_entry","headlineFields":["项目名称"],"dateFields":[],"sublineFields":["担任角色","技术栈"],"bodyFields":["项目描述"],"fields":["项目名称","担任角色","技术栈","项目描述","要点"],"sampleItems":[{"项目名称":"校园二手交易平台","担任角色":"后端负责人","技术栈":"Spring Boot、MySQL、Redis","项目描述":"课程设计项目，负责用户、商品与订单模块设计与实现。","要点":"• 完成登录鉴权与接口幂等处理\n• 部署演示环境并撰写设计说明"}]},{"key":"skills","title":"技能特长","layout":"kv","fields":["技能名称","掌握程度","证书"],"sampleItems":[{"技能名称":"Java / Spring","掌握程度":"熟练","证书":"—"},{"技能名称":"MySQL","掌握程度":"熟练","证书":"—"},{"技能名称":"英语","掌握程度":"读写良好","证书":"CET-6"}]}]' WHERE id = 2;
UPDATE resume_template SET structure_json = '[{"key":"basic","title":"基本信息","layout":"basic","fields":["姓名","电话","邮箱","籍贯","政治面貌","求职意向","证件照"],"sampleItems":[{"姓名":"李想","电话":"137-0000-0000","邮箱":"lixiang@example.com","籍贯":"江苏省南京市","政治面貌":"中共党员","求职意向":"行政专员 / 综合岗","证件照":""}]},{"key":"education","title":"教育背景","layout":"entry_headline_date","headlineFields":["学校","专业","学历"],"dateFields":["入学时间","毕业时间"],"moreFields":["GPA"],"fields":["学校","专业","学历","入学时间","毕业时间","GPA"],"sampleItems":[{"学校":"南京师范大学","专业":"工商管理","学历":"本科","入学时间":"2016.09","毕业时间":"2020.06","GPA":"3.6/4.0"}]},{"key":"internship","title":"实习经历","layout":"rich_entry","headlineFields":["公司名称"],"dateFields":["起止时间"],"sublineFields":["职位"],"bodyFields":["工作描述"],"fields":["公司名称","职位","起止时间","工作描述","要点"],"sampleItems":[{"公司名称":"示例科技有限公司","职位":"行政专员","起止时间":"2020.07 - 2021.06","工作描述":"协助部门完成日常行政、会议与档案管理。","要点":"• 负责来访接待与办公用品采购\n• 维护固定资产台账并参与年检材料整理"}]},{"key":"projects","title":"项目经历","layout":"rich_entry","headlineFields":["项目名称"],"dateFields":[],"sublineFields":["担任角色","技术栈"],"bodyFields":["项目描述"],"fields":["项目名称","担任角色","技术栈","项目描述","要点"],"sampleItems":[{"项目名称":"企业文化宣传周","担任角色":"策划组成员","技术栈":"海报设计、公众号排版","项目描述":"协助策划线下活动与线上推文排期。","要点":"• 协调场地与物料\n• 跟进活动反馈问卷"}]},{"key":"student","title":"学生工作","layout":"rich_entry","headlineFields":["组织名称"],"dateFields":["起止时间"],"sublineFields":["职务"],"bodyFields":["工作内容"],"fields":["组织名称","职务","起止时间","工作内容","要点"],"sampleItems":[{"组织名称":"校学生会","职务":"办公室主任","起止时间":"2017.09 - 2019.06","工作内容":"统筹例会、值班表与活动后勤。","要点":"• 组织校级文艺晚会后勤志愿队\n• 优化物资借用流程"}]},{"key":"evaluation","title":"个人评价","layout":"kv","fields":["自我评价"],"sampleItems":[{"自我评价":"工作认真细致，熟练使用 Office 办公软件；具备良好的沟通协调能力与文档整理习惯；学习能力强，能快速适应新环境并完成交办任务。"}]}]' WHERE id = 3;
