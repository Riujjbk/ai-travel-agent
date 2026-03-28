package com.ui.ailvyou.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.ui.ailvyou.constant.FileConstant;
import com.ui.ailvyou.util.FileDownloadHelper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResourceDownloadTool {

    @Autowired
    private FileDownloadHelper fileDownloadHelper;

    @Tool(description = "Download a resource from a URL")
    public String downloadResource(@ToolParam(description = "URL of the resource to download") String url,
            @ToolParam(description = "Name of the file to save the resource to") String fileName) {

        String fileDir = FileConstant.FILE_DIR + "/download";
        String filePath = fileDir + "/" + fileName;

        try {
            FileUtil.mkdir(fileDir);
            // 使用hutool工具类下载文件HttpUtil
            HttpUtil.downloadFile(url, filePath);
            return "资源下载成功，下载地址：" + fileDownloadHelper.buildDownloadUrl(filePath, "download");
        } catch (Exception e) {
            return "Error downloading resource: " + e.getMessage();
        }

    }

}
