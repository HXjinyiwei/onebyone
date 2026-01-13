package com.example.biyeshiji.controller;

import com.example.biyeshiji.common.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class UploadController {

    @Value("${file.upload-dir:static/uploads}")
    private String uploadDir;

    @PostMapping("/cover")
    public Response<String> uploadCover(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Response.error("文件为空");
        }
        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Response.error("仅支持图片文件");
        }
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;
        // 确保上传目录存在（相对于应用程序根目录）
        Path rootPath = Paths.get("").toAbsolutePath().normalize();
        Path uploadPath = rootPath.resolve(uploadDir);
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                e.printStackTrace();
                return Response.error("无法创建上传目录: " + e.getMessage());
            }
        }
        // 保存文件
        Path filePath = uploadPath.resolve(filename);
        try {
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            e.printStackTrace();
            return Response.error("文件保存失败: " + e.getMessage());
        }
        // 返回可访问的URL（相对路径）
        String url = "/uploads/" + filename;
        return Response.success("上传成功", url);
    }
}