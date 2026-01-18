-- 为category表添加type字段
-- type: 0=帖子分类, 1=小说分类

-- 添加type列，默认值为0（帖子分类）
ALTER TABLE category ADD COLUMN type TINYINT NOT NULL DEFAULT 0 COMMENT '分类类型：0=帖子分类，1=小说分类';

-- 为现有数据设置type值
-- 根据分类名称判断，将小说相关的分类设置为1
UPDATE category SET type = 1 WHERE name IN ('玄幻', '科幻', '武侠', '都市', '言情', '历史', '军事', '游戏', '体育', '其他');

-- 添加索引以提高查询性能
CREATE INDEX idx_category_type ON category(type);
CREATE INDEX idx_category_type_active ON category(type, is_active, is_deleted);