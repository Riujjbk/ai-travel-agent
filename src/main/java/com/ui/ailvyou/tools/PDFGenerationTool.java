package com.ui.ailvyou.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.ui.ailvyou.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class PDFGenerationTool {

    @Tool(description = "生成一个 PDF 文件，可以只包含文本，也可以包含文本和图片。")
    public String generatePdf(
            @ToolParam(description = "PDF 的文件名，必须以 .pdf 结尾，例如 '旅行报告.pdf'") String fileName,
            @ToolParam(description = "要写入 PDF 的文本内容。如果只插入图片，此参数可以为空字符串。") String content,
            @ToolParam(description = "一个包含本地图片完整路径的列表。如果不需要图片，则忽略此参数。例如：[\"F:/downloads/image1.png\", \"F:/downloads/image2.jpg\"]") List<String> imagePaths) {

        String fileDir = FileConstant.FILE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;

        try {
            // 确保目录存在
            FileUtil.mkdir(fileDir);
            log.info("准备生成 PDF: {}", filePath);

            try (PdfWriter writer = new PdfWriter(filePath);
                    PdfDocument pdf = new PdfDocument(writer);
                    Document document = new Document(pdf)) {

                // 设置中文字体
                PdfFont font = null;
                try {
                    // 优先尝试使用系统自带的中文字体 (Windows 常用)
                    String[] fontPaths = {
                            "C:/Windows/Fonts/msyh.ttc,0", // 微软雅黑
                            "C:/Windows/Fonts/simsun.ttc,0", // 宋体
                            "C:/Windows/Fonts/msyh.ttf", // 微软雅黑 (某些版本)
                            "C:/Windows/Fonts/simsun.ttf", // 宋体 (某些版本)
                            "C:/Windows/Fonts/msyhl.ttc,0", // 微软雅黑 Light
                            "C:/Windows/Fonts/stsong.ttf" // 华文宋体
                    };

                    for (String path : fontPaths) {
                        try {
                            String actualPath = path.split(",")[0];
                            if (new java.io.File(actualPath).exists()) {
                                font = PdfFontFactory.createFont(path, "Identity-H");
                                log.info("成功加载系统字体: {}", path);
                                break;
                            }
                        } catch (Exception e) {
                            log.warn("尝试加载字体 {} 失败: {}", path, e.getMessage());
                        }
                    }

                    // 如果系统字体加载失败，再尝试使用 STSongStd-Light (依赖 itext-asian)
                    if (font == null) {
                        try {
                            font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
                            log.info("使用内置字体: STSongStd-Light");
                        } catch (Exception e) {
                            log.warn("无法加载 STSongStd-Light，请确保 itext-asian 依赖已正确引入。");
                        }
                    }
                } catch (Exception fontEx) {
                    log.warn("字体处理过程中发生异常: {}", fontEx.getMessage());
                }

                if (font != null) {
                    document.setFont(font);
                } else {
                    log.error("警告：未能加载任何中文字体，中文内容可能无法显示！");
                }

                boolean hasContent = false;
                // 1. 添加文本内容
                if (StrUtil.isNotBlank(content)) {
                    Paragraph p = new Paragraph(content);
                    if (font != null)
                        p.setFont(font);
                    document.add(p);
                    hasContent = true;
                    log.info("已添加文本内容，长度: {}", content.length());
                }

                // 2. 添加图片
                if (imagePaths != null && !imagePaths.isEmpty()) {
                    log.info("开始向 PDF 添加 {} 张图片...", imagePaths.size());
                    for (String imagePath : imagePaths) {
                        try {
                            if (FileUtil.exist(imagePath)) {
                                Image image = new Image(ImageDataFactory.create(imagePath));
                                image.setAutoScale(true); // 自动缩放以适应页面宽度
                                image.setMarginTop(15f); // 图片上方留出间距
                                document.add(image);
                                hasContent = true;
                                log.info("成功添加图片到 PDF: {}", imagePath);
                            } else {
                                log.warn("图片文件不存在，已跳过: {}", imagePath);
                                Paragraph warnPara = new Paragraph("\n[图片文件未找到: " + imagePath + "]");
                                if (font != null)
                                    warnPara.setFont(font);
                                document.add(warnPara.setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED));
                                hasContent = true;
                            }
                        } catch (Exception imgEx) {
                            log.error("添加图片 {} 到 PDF 时失败", imagePath, imgEx);
                            Paragraph errorPara = new Paragraph("\n[图片插入失败: " + imagePath + "]");
                            if (font != null)
                                errorPara.setFont(font);
                            document.add(errorPara.setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED));
                            hasContent = true;
                        }
                    }
                }

                if (!hasContent) {
                    Paragraph emptyPara = new Paragraph("（无内容）");
                    if (font != null)
                        emptyPara.setFont(font);
                    document.add(emptyPara);
                    log.info("添加了空内容占位符");
                }

                // 显式刷新
                document.flush();
            }
            log.info("PDF 文件已生成并保存: {}", filePath);
            return "PDF 已成功生成，路径为: " + filePath;
        } catch (Exception e) {
            log.error("生成 PDF 时发生严重异常", e);
            return "生成 PDF 失败: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }
}

/**
 * @Tool(description = "Generate a PDF file with given content")
 *                   public String generatePDF(
 * @ToolParam(description = "Name of the file to save the generated PDF") String
 *                        fileName,
 * @ToolParam(description = "Content to be included in the PDF") String content)
 *                        {
 *                        String fileDir = FileConstant.FILE_DIR + "/pdf";
 *                        String filePath = fileDir + "/" + fileName;
 *                        try {
 *                        // 创建目录
 *                        FileUtil.mkdir(fileDir);
 *                        // 创建 PdfWriter 和 PdfDocument 对象
 *                        try (PdfWriter writer = new PdfWriter(filePath);
 *                        PdfDocument pdf = new PdfDocument(writer);
 *                        Document document = new Document(pdf)) {
 *                        // 自定义字体（需要人工下载字体文件到特定目录）
 *                        // String fontPath =
 *                        Paths.get("src/main/resources/static/fonts/simsun.ttf")
 *                        // .toAbsolutePath().toString();
 *                        // PdfFont font = PdfFontFactory.createFont(fontPath,
 *                        // PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
 *                        // 使用内置中文字体
 *                        PdfFont font =
 *                        PdfFontFactory.createFont("STSongStd-Light",
 *                        "UniGB-UCS2-H");
 *                        document.setFont(font);
 *                        // 创建段落
 *                        Paragraph paragraph = new Paragraph(content);
 *                        // 添加段落并关闭文档
 *                        document.add(paragraph);
 *                        }
 *                        return "PDF generated successfully to: " + filePath;
 *                        } catch (IOException e) {
 *                        return "Error generating PDF: " + e.getMessage();
 *                        }
 *                        }
 */
