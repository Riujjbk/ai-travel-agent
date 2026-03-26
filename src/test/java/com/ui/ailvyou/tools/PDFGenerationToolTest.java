package com.ui.ailvyou.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

//@SpringBootTest
public class PDFGenerationToolTest {

    @Test
    public void testGeneratePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "7days旅游推荐2.pdf";
        String content = "我为你设计了一条经典的云南7日深度游线路，融合自然风光与人文体验：\n" +
                "\n" +
                "行程亮点：这条线路串联了丽江古城的世界文化遗产、玉龙雪山的冰川奇观、大理的苍洱风光和香格里拉的高原秘境，让你在7天内体验云南多元的民族文化和壮丽的自然景观。\n" +
                "\n" +
                "具体安排：\n" +
                "• 第1-2天：抵达丽江，游览束河古镇和大研古城，感受纳西族建筑和东巴文化，晚上可欣赏《丽江千古情》演出。\n" +
                "\n" +
                "• 第3天：前往玉龙雪山，乘坐缆车登顶欣赏冰川公园，下午游览蓝月谷和白水河。\n" +
                "\n" +
                "• 第4天：乘车至大理，环游洱海，参观喜洲古镇和白族民居，傍晚在双廊欣赏日落。\n" +
                "\n" +
                "• 第5天：游览崇圣寺三塔和大理古城，体验扎染等白族手工艺。\n" +
                "\n" +
                "• 第6-7天：前往香格里拉，参观松赞林寺，徒步普达措国家公园，感受藏族文化和高原生态。\n" +
                "\n" +
                "旅行建议：云南昼夜温差大，需准备保暖衣物；部分景点海拔较高，建议提前适应并备好抗高反药物；当地美食推荐过桥米线、野生菌火锅和鲜花饼。 ";
        String result = tool.generatePdf(fileName, content, null);

        assertNotNull(result);
        System.out.println(result);
    }
}
