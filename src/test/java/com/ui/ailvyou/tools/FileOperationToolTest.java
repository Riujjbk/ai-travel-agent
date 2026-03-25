package com.ui.ailvyou.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileOperationToolTest {

    @Test
    public void testReadFile() {
        FileOperationTool tool = new FileOperationTool();
        String fileName = "北京7日游.txt";
        String result = tool.readFile(fileName);
        assertNotNull(result);
    }

    @Test
    public void testWriteFile() {
        FileOperationTool tool = new FileOperationTool();
        String fileName = "北京7日游.txt";
        String content = "天安门，故宫，胡同，车站等";
        String result = tool.writeFile(fileName, content);
        assertNotNull(result);
    }

}