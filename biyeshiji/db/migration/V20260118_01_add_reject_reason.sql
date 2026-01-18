-- 为novel、post、chapter表添加reject_reason字段
-- 用于存储审核拒绝原因

-- 为novel表添加reject_reason字段
ALTER TABLE novel ADD COLUMN reject_reason VARCHAR(500) NULL COMMENT '审核拒绝原因';

-- 为post表添加reject_reason字段
ALTER TABLE post ADD COLUMN reject_reason VARCHAR(500) NULL COMMENT '审核拒绝原因';

-- 为chapter表添加reject_reason字段
ALTER TABLE chapter ADD COLUMN reject_reason VARCHAR(500) NULL COMMENT '审核拒绝原因';

-- 添加索引以提高查询性能（可选）
CREATE INDEX idx_novel_reject_reason ON novel(reject_reason(100));
CREATE INDEX idx_post_reject_reason ON post(reject_reason(100));
CREATE INDEX idx_chapter_reject_reason ON chapter(reject_reason(100));