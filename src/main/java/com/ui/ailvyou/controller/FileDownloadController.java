package com.ui.ailvyou.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.ui.ailvyou.constant.FileConstant;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/")
public class FileDownloadController {

    private final String uploadDir = FileConstant.FILE_DIR;

    /**
     * 文件下载接口 - 支持多种方式
     * 方式1: 通过filename参数下载
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam String filename,
            @RequestParam(required = false) String type,
            HttpServletRequest request) {

        try {
            // 安全验证
            if (StrUtil.isBlank(filename) || filename.contains("..")) {
                return ResponseEntity.badRequest().build();
            }

            // 解码文件名
            String decodedFilename = filename;
            try {
                decodedFilename = java.net.URLDecoder.decode(filename, StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                log.warn("文件名解码失败: {}", filename);
            }

            // 根据文件类型确定文件路径
            String filePath = getFilePath(decodedFilename, type);
            log.info("尝试下载文件: {}", filePath);

            File file = new File(filePath);

            if (!file.exists() || !file.isFile()) {
                log.error("文件不存在: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encodeFilename(decodedFilename, request) + "\"");

            // 设置Content-Type
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = getContentTypeByExtension(decodedFilename);
            }

            // 支持断点续传
            long fileLength = file.length();
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileLength));

            // 处理Range请求（断点续传）
            String rangeHeader = request.getHeader(HttpHeaders.RANGE);
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                return handleRangeRequest(file, rangeHeader, contentType, headers);
            }

            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileLength)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            log.error("文件下载失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 方式2: 通过路径参数下载
     */
    @GetMapping("/download/{type}/{filename:.+}")
    public ResponseEntity<Resource> downloadFileByPath(
            @PathVariable String type,
            @PathVariable String filename,
            HttpServletRequest request) {

        return downloadFile(filename, type, request);
    }

    /**
     * 方式3: 流式下载大文件
     */
    @GetMapping("/stream/download")
    public ResponseEntity<StreamingResponseBody> streamDownloadFile(
            @RequestParam String filename,
            @RequestParam(required = false) String type) {

        try {
            String filePath = getFilePath(filename, type);
            File file = new File(filePath);

            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            long fileSize = file.length();
            String contentType = getContentTypeByExtension(filename);

            StreamingResponseBody responseBody = outputStream -> {
                try (InputStream inputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytesRead = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        outputStream.flush();

                        // 添加进度监控（可选）
                        if (totalBytesRead % (1024 * 1024) == 0) { // 每1MB刷新一次
                            log.debug("已下载: {}/{} bytes", totalBytesRead, fileSize);
                        }
                    }
                } catch (IOException e) {
                    log.error("流式下载中断", e);
                }
            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name())
                                    + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(fileSize)
                    .body(responseBody);

        } catch (Exception e) {
            log.error("流式下载失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 文件预览接口（支持文本和图片）
     */
    @GetMapping("/preview")
    public ResponseEntity<?> previewFile(
            @RequestParam String filename,
            @RequestParam(required = false) String type) {

        try {
            String filePath = getFilePath(filename, type);
            File file = new File(filePath);

            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            String extension = getFileExtension(filename).toLowerCase();

            // 文本文件预览
            if (isTextFile(extension)) {
                String content = FileUtil.readUtf8String(file);
                Map<String, Object> response = new HashMap<>();
                response.put("type", "text");
                response.put("filename", filename);
                response.put("content", content);
                response.put("size", file.length());
                return ResponseEntity.ok(response);
            }

            // 图片文件预览
            if (isImageFile(extension)) {
                byte[] bytes = FileUtil.readBytes(file);
                String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                String mimeType = getContentTypeByExtension(filename);

                Map<String, Object> response = new HashMap<>();
                response.put("type", "image");
                response.put("filename", filename);
                response.put("content", "data:" + mimeType + ";base64," + base64);
                response.put("size", file.length());
                return ResponseEntity.ok(response);
            }

            // 其他文件类型
            Map<String, Object> response = new HashMap<>();
            response.put("type", "binary");
            response.put("filename", filename);
            response.put("message", "此文件类型不支持预览，请下载查看");
            response.put("size", file.length());
            response.put("downloadUrl", "/api/download?filename=" +
                    URLEncoder.encode(filename, StandardCharsets.UTF_8.name()) + "&type=" + type);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("文件预览失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "预览失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/file/info")
    public ResponseEntity<?> getFileInfo(
            @RequestParam String filename,
            @RequestParam(required = false) String type) {

        try {
            String filePath = getFilePath(filename, type);
            File file = new File(filePath);

            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> info = new HashMap<>();
            info.put("filename", filename);
            info.put("path", filePath);
            info.put("size", file.length());
            info.put("sizeFormatted", formatFileSize(file.length()));
            info.put("lastModified", file.lastModified());
            info.put("exists", true);
            info.put("readable", file.canRead());
            info.put("type", getFileExtension(filename));
            info.put("mimeType", getContentTypeByExtension(filename));
            info.put("downloadUrl", "/api/download?filename=" +
                    URLEncoder.encode(filename, StandardCharsets.UTF_8.name()) + "&type=" + type);
            info.put("previewUrl", "/api/preview?filename=" +
                    URLEncoder.encode(filename, StandardCharsets.UTF_8.name()) + "&type=" + type);

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            log.error("获取文件信息失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取文件信息失败: " + e.getMessage()));
        }
    }

    /**
     * 处理断点续传请求
     */
    private ResponseEntity<Resource> handleRangeRequest(
            File file, String rangeHeader, String contentType, HttpHeaders headers) throws IOException {

        long fileLength = file.length();

        // 使用临时变量解析 Range 请求
        long tempStart = 0;
        long tempEnd = fileLength - 1;
        String range = rangeHeader.substring(6);
        String[] ranges = range.split("-");

        if (ranges.length > 0) {
            tempStart = Long.parseLong(ranges[0]);
            if (ranges.length > 1) {
                tempEnd = Long.parseLong(ranges[1]);
            }
        }

        // 确保 end 不超过文件大小
        if (tempEnd >= fileLength) {
            tempEnd = fileLength - 1;
        }

        // 将解析结果赋值给 final 变量
        final long start = tempStart;
        final long end = tempEnd;
        final long contentLength = end - start + 1;

        headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength);
        headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));

        Resource resource = new FileSystemResource(file) {
            @Override
            public InputStream getInputStream() throws IOException {
                FileInputStream fis = new FileInputStream(file);
                fis.skip(start); // ✅ 此处访问 final 变量不会报错
                return fis;
            }
        };

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .contentLength(contentLength)
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    /**
     * 获取文件路径
     */
    private String getFilePath(String filename, String type) {
        String baseDir = uploadDir;

        if (StrUtil.isNotBlank(type)) {
            switch (type.toLowerCase()) {
                case "pdf":
                    baseDir += "/pdf";
                    break;
                case "image":
                    baseDir += "/images";
                    break;
                case "document":
                    baseDir += "/documents";
                    break;
                case "file":
                    baseDir += "/file";
                    break;
                case "download":
                    baseDir += "/download";
                    break;
                default:
                    baseDir += "/" + type;
                    break;
            }
        }

        // 确保目录存在
        FileUtil.mkdir(baseDir);

        return baseDir + "/" + filename;
    }

    /**
     * 根据扩展名获取Content-Type
     */
    private String getContentTypeByExtension(String filename) {
        String extension = getFileExtension(filename).toLowerCase();

        switch (extension) {
            case "txt":
                return "text/plain; charset=utf-8";
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "zip":
                return "application/zip";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "html":
            case "htm":
                return "text/html";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 编码文件名（处理中文和特殊字符）
     */
    private String encodeFilename(String filename, HttpServletRequest request) {
        try {
            return URLEncoder.encode(filename, StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");
        } catch (Exception e) {
            return filename;
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }

    /**
     * 判断是否是文本文件
     */
    private boolean isTextFile(String extension) {
        return extension.matches("(txt|md|json|xml|html|htm|css|js|java|properties|yml|yaml|sql)$");
    }

    /**
     * 判断是否是图片文件
     */
    private boolean isImageFile(String extension) {
        return extension.matches("(png|jpg|jpeg|gif|bmp|webp|svg)$");
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
