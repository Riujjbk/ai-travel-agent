package com.ui.ailvyou.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.ui.ailvyou.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class FileDownloadHelper {
    
    /**
     * 构建文件下载链接
     */
    public String buildDownloadUrl(String filePath, String type) {
        try {
            File file = new File(filePath);
            String filename = file.getName();
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.name());
            
            // 自动检测文件类型
            if (StrUtil.isBlank(type)) {
                type = detectFileType(file);
            }
            
            return String.format("/api/download?filename=%s&type=%s", 
                encodedFilename, 
                type);
        } catch (Exception e) {
            log.error("构建下载链接失败", e);
            return null;
        }
    }
    
    /**
     * 构建文件预览链接
     */
    public String buildPreviewUrl(String filePath, String type) {
        try {
            File file = new File(filePath);
            String filename = file.getName();
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.name());
            
            if (StrUtil.isBlank(type)) {
                type = detectFileType(file);
            }
            
            return String.format("/api/preview?filename=%s&type=%s", 
                encodedFilename, 
                type);
        } catch (Exception e) {
            log.error("构建预览链接失败", e);
            return null;
        }
    }
    
    /**
     * 构建完整的文件响应信息
     */
    public Map<String, Object> buildFileResponse(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return Map.of("success", false, "error", "文件不存在");
            }
            
            String filename = file.getName();
            String type = detectFileType(file);
            String downloadUrl = buildDownloadUrl(filePath, type);
            String previewUrl = buildPreviewUrl(filePath, type);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文件生成成功");
            response.put("filename", filename);
            response.put("filepath", filePath);
            response.put("size", file.length());
            response.put("sizeFormatted", formatFileSize(file.length()));
            response.put("type", type);
            response.put("downloadUrl", downloadUrl);
            response.put("previewUrl", previewUrl);
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return response;
        } catch (Exception e) {
            log.error("构建文件响应失败", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private String detectFileType(File file) {
        String path = file.getAbsolutePath().replace("\\", "/");
        if (path.contains("/pdf/")) return "pdf";
        if (path.contains("/images/")) return "image";
        if (path.contains("/documents/")) return "document";
        if (path.contains("/file/")) return "file";
        if (path.contains("/download/")) return "download";
        return "other";
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }
}
