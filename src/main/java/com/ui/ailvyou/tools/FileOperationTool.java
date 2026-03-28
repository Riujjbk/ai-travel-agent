package com.ui.ailvyou.tools;

import cn.hutool.core.io.FileUtil;
import com.ui.ailvyou.constant.FileConstant;
import com.ui.ailvyou.util.FileDownloadHelper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileOperationTool {

    @Autowired
    private FileDownloadHelper fileDownloadHelper;

    private final String FILE_DIR = FileConstant.FILE_DIR + "file";

    @Tool(description = "read content from a file")
    public String readFile(@ToolParam(description = "Name of file to read") String fileName) {
        String filePath = FILE_DIR + "/" + fileName;

        try {
            return FileUtil.readUtf8String(filePath);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }

    }

    @Tool(description = "write content to a file")
    public String writeFile(@ToolParam(description = "Name of file to write") String fileName,
            @ToolParam(description = "Content to write") String content) {

        String filePath = FILE_DIR + "/" + fileName;
        try {
            FileUtil.mkdir(FILE_DIR);
            FileUtil.writeUtf8String(content, filePath);
            return "文件保存成功，下载地址：" + fileDownloadHelper.buildDownloadUrl(filePath, "file");
        } catch (Exception e) {
            return "Error writing file: " + e.getMessage();
        }
    }

}
