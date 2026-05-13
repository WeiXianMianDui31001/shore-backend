-- 已有库若只有「考研复试通用模板」一条，或模板 2/3 的 structure_json 缺少 sampleItems，可执行本脚本。
-- 在 psql 或 DBeaver 等工具中连到 shore 使用的 PostgreSQL 后执行；可重复执行（冲突时更新 structure 等字段）。

INSERT INTO resume_template (id, name, type, thumbnail_url, structure_json, template_key, template_version, status, created_at)
VALUES (2, '互联网求职模板', 1, 'https://cdn.anzs.com/template_2.jpg',
'[{"key":"basic","title":"基本信息","layout":"basic","fields":["姓名","邮箱","电话","出生日期","所在地","求职意向"],"sampleItems":[{"姓名":"王芳","邮箱":"wangfang@example.com","电话":"139-0000-0000","出生日期":"1999.03","所在地":"上海市","求职意向":"Java 后端开发"}]},{"key":"education","title":"教育背景","layout":"entry_headline_date","headlineFields":["学校","专业","学历"],"dateFields":["入学时间","毕业时间"],"moreFields":["GPA"],"fields":["学校","专业","学历","入学时间","毕业时间","GPA"],"sampleItems":[{"学校":"上海理工大学","专业":"软件工程","学历":"本科","入学时间":"2018.09","毕业时间":"2022.06","GPA":"3.5/4.0"}]},{"key":"internship","title":"实习经历","layout":"rich_entry","headlineFields":["公司名称"],"dateFields":["起止时间"],"sublineFields":["职位"],"bodyFields":["工作描述"],"fields":["公司名称","职位","起止时间","工作描述","要点"],"sampleItems":[{"公司名称":"示例互联网科技有限公司","职位":"后端开发实习生","起止时间":"2024.06 - 2024.09","工作描述":"参与订单与支付相关接口开发与联调。","要点":"• 使用 Spring Boot 编写 REST 接口并完成单元测试\n• 配合前端完成接口文档与 Mock 数据"},{"公司名称":"示例云计算有限公司","职位":"研发实习生","起止时间":"2023.07 - 2023.09","工作描述":"参与内部运营后台的迭代与缺陷修复。","要点":"• 熟悉 Git 协作流程与 Code Review\n• 编写 SQL 与简单性能排查文档"}]},{"key":"projects","title":"项目经验","layout":"rich_entry","headlineFields":["项目名称"],"dateFields":[],"sublineFields":["担任角色","技术栈"],"bodyFields":["项目描述"],"fields":["项目名称","担任角色","技术栈","项目描述","要点"],"sampleItems":[{"项目名称":"校园二手交易平台","担任角色":"后端负责人","技术栈":"Spring Boot、MySQL、Redis","项目描述":"课程设计项目，负责用户、商品与订单模块设计与实现。","要点":"• 完成登录鉴权与接口幂等处理\n• 部署演示环境并撰写设计说明"}]},{"key":"skills","title":"技能特长","layout":"kv","fields":["技能名称","掌握程度","证书"],"sampleItems":[{"技能名称":"Java / Spring","掌握程度":"熟练","证书":"—"},{"技能名称":"MySQL","掌握程度":"熟练","证书":"—"},{"技能名称":"英语","掌握程度":"读写良好","证书":"CET-6"}]}]',
'internet_job_v1', 'v1', 0, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  type = EXCLUDED.type,
  thumbnail_url = EXCLUDED.thumbnail_url,
  structure_json = EXCLUDED.structure_json,
  template_key = EXCLUDED.template_key,
  template_version = EXCLUDED.template_version,
  status = EXCLUDED.status;

INSERT INTO resume_template (id, name, type, thumbnail_url, structure_json, template_key, template_version, status, created_at)
VALUES (3, '中文简历（印刷风·求职）', 1, 'https://cdn.anzs.com/template_cn_print.jpg',
'[{"key":"basic","title":"基本信息","layout":"basic","fields":["姓名","电话","邮箱","籍贯","政治面貌","求职意向","证件照"],"sampleItems":[{"姓名":"李想","电话":"137-0000-0000","邮箱":"lixiang@example.com","籍贯":"江苏省南京市","政治面貌":"中共党员","求职意向":"行政专员 / 综合岗","证件照":""}]},{"key":"education","title":"教育背景","layout":"entry_headline_date","headlineFields":["学校","专业","学历"],"dateFields":["入学时间","毕业时间"],"moreFields":["GPA"],"fields":["学校","专业","学历","入学时间","毕业时间","GPA"],"sampleItems":[{"学校":"南京师范大学","专业":"工商管理","学历":"本科","入学时间":"2016.09","毕业时间":"2020.06","GPA":"3.6/4.0"}]},{"key":"internship","title":"实习经历","layout":"rich_entry","headlineFields":["公司名称"],"dateFields":["起止时间"],"sublineFields":["职位"],"bodyFields":["工作描述"],"fields":["公司名称","职位","起止时间","工作描述","要点"],"sampleItems":[{"公司名称":"示例科技有限公司","职位":"行政专员","起止时间":"2020.07 - 2021.06","工作描述":"协助部门完成日常行政、会议与档案管理。","要点":"• 负责来访接待与办公用品采购\n• 维护固定资产台账并参与年检材料整理"}]},{"key":"projects","title":"项目经历","layout":"rich_entry","headlineFields":["项目名称"],"dateFields":[],"sublineFields":["担任角色","技术栈"],"bodyFields":["项目描述"],"fields":["项目名称","担任角色","技术栈","项目描述","要点"],"sampleItems":[{"项目名称":"企业文化宣传周","担任角色":"策划组成员","技术栈":"海报设计、公众号排版","项目描述":"协助策划线下活动与线上推文排期。","要点":"• 协调场地与物料\n• 跟进活动反馈问卷"}]},{"key":"student","title":"学生工作","layout":"rich_entry","headlineFields":["组织名称"],"dateFields":["起止时间"],"sublineFields":["职务"],"bodyFields":["工作内容"],"fields":["组织名称","职务","起止时间","工作内容","要点"],"sampleItems":[{"组织名称":"校学生会","职务":"办公室主任","起止时间":"2017.09 - 2019.06","工作内容":"统筹例会、值班表与活动后勤。","要点":"• 组织校级文艺晚会后勤志愿队\n• 优化物资借用流程"}]},{"key":"evaluation","title":"个人评价","layout":"kv","fields":["自我评价"],"sampleItems":[{"自我评价":"工作认真细致，熟练使用 Office 办公软件；具备良好的沟通协调能力与文档整理习惯；学习能力强，能快速适应新环境并完成交办任务。"}]}]',
'resume_cn_print_v1', 'v1', 0, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  type = EXCLUDED.type,
  thumbnail_url = EXCLUDED.thumbnail_url,
  structure_json = EXCLUDED.structure_json,
  template_key = EXCLUDED.template_key,
  template_version = EXCLUDED.template_version,
  status = EXCLUDED.status;
