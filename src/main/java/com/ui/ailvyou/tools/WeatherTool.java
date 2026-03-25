package com.ui.ailvyou.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class WeatherTool {


    @Tool(description = "Get the weather in a city")
    public String getWeather(@ToolParam (description = "City name") String city){


        return "The weather in "+city+" is sunny";
    }


}
