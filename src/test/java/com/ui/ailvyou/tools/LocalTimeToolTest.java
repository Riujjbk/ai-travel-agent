package com.ui.ailvyou.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalTimeToolTest {

    @Test
    void test(){
        LocalTimeTool localTimeTool = new LocalTimeTool();
        String localTime = localTimeTool.getLocalTime("伦敦");
        System.out.println(localTime);
    }

}