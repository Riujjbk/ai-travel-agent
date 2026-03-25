package com.ui.ailvyou.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceDownloadToolTest {


    @Test
    public void testDownloadResource() {
        ResourceDownloadTool tool = new ResourceDownloadTool();
        String url = "https://www.baidu.com/img/bd_logo1.png";
        String fileName = "baidu.png";
        String result = tool.downloadResource(url, fileName);
        assertNotNull(result);
    }

}