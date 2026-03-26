package com.ui.ailvyou.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取指定地点当地时间的工具类
 */
@Slf4j
@Component
public class LocalTimeTool {

    private static final Map<String, String> CITY_TIMEZONE_MAP = new HashMap<>();

    static {
        // 中国主要城市
        CITY_TIMEZONE_MAP.put("北京", "Asia/Shanghai");
        CITY_TIMEZONE_MAP.put("上海", "Asia/Shanghai");
        CITY_TIMEZONE_MAP.put("广州", "Asia/Shanghai");
        CITY_TIMEZONE_MAP.put("深圳", "Asia/Shanghai");
        CITY_TIMEZONE_MAP.put("香港", "Asia/Hong_Kong");
        CITY_TIMEZONE_MAP.put("台北", "Asia/Taipei");
        
        // 国际主要城市
        CITY_TIMEZONE_MAP.put("东京", "Asia/Tokyo");
        CITY_TIMEZONE_MAP.put("首尔", "Asia/Seoul");
        CITY_TIMEZONE_MAP.put("纽约", "America/New_York");
        CITY_TIMEZONE_MAP.put("伦敦", "Europe/London");
        CITY_TIMEZONE_MAP.put("巴黎", "Europe/Paris");
        CITY_TIMEZONE_MAP.put("柏林", "Europe/Berlin");
        CITY_TIMEZONE_MAP.put("莫斯科", "Europe/Moscow");
        CITY_TIMEZONE_MAP.put("悉尼", "Australia/Sydney");
        CITY_TIMEZONE_MAP.put("新加坡", "Asia/Singapore");
        CITY_TIMEZONE_MAP.put("曼谷", "Asia/Bangkok");
        CITY_TIMEZONE_MAP.put("迪拜", "Asia/Dubai");
        CITY_TIMEZONE_MAP.put("洛杉矶", "America/Los_Angeles");
        CITY_TIMEZONE_MAP.put("芝加哥", "America/Chicago");
    }

    @Tool(description = "获取指定地点的当前当地时间。支持国内主要城市及国际大都市。")
    public String getLocalTime(
            @ToolParam(description = "地点名称，例如：北京、东京、纽约、伦敦等") String location) {
        
        log.info("获取地点 [{}] 的当地时间", location);

        String timezoneId = CITY_TIMEZONE_MAP.get(location);
        
        // 如果没有直接匹配到，尝试模糊匹配或直接作为 ZoneId 尝试
        if (timezoneId == null) {
            try {
                // 尝试直接使用输入作为 ZoneId (例如 "Asia/Shanghai")
                ZoneId zoneId = ZoneId.of(location);
                return formatTime(zoneId, location);
            } catch (Exception e) {
                // 尝试简单的包含匹配
                for (Map.Entry<String, String> entry : CITY_TIMEZONE_MAP.entrySet()) {
                    if (location.contains(entry.getKey()) || entry.getKey().contains(location)) {
                        timezoneId = entry.getValue();
                        location = entry.getKey();
                        break;
                    }
                }
            }
        }

        if (timezoneId != null) {
            return formatTime(ZoneId.of(timezoneId), location);
        }

        return String.format("抱歉，暂未找到地点 [%s] 的时区信息。请尝试提供更具体的城市名称或标准的时区 ID（如 Asia/Shanghai）。", location);
    }

    private String formatTime(ZoneId zoneId, String location) {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = now.format(formatter);
        return String.format("[%s] 的当前当地时间为: %s (%s)", location, formattedTime, zoneId.getId());
    }
}
