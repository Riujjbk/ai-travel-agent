package com.ui.ailvyou.tools;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TerminalOperationToolTest {

    @Resource
    private TerminalOperationTool tool;

    @Test
    public void testExecuteTerminalCommand() {
        // 在 Windows 上使用 dir，在 Linux/Mac 上使用 ls
        String os = System.getProperty("os.name").toLowerCase();
        String command = os.contains("win") ? "dir" : "ls -l";
        
        String result = tool.executeTerminalCommand(command);
        System.out.println("Command Output:\n" + result);
        
        assertNotNull(result);
        assertFalse(result.isEmpty(), "命令执行结果不应为空");
    }
}