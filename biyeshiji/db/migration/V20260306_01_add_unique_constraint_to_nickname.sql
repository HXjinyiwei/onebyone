-- 为user表的nickname字段添加唯一约束
ALTER TABLE `user` ADD UNIQUE INDEX `uk_user_nickname` (`nickname`);