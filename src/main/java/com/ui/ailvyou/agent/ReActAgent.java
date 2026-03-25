package com.ui.ailvyou.agent;


import com.ui.ailvyou.agent.exception.BusinessException;
import com.ui.ailvyou.agent.exception.ErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ReAct（）模式的代理抽象类
 * 实现了思考-行动的循环模式，
 */
//  是 Lombok 提供的一个注解，用于自动生成 equals() 和 hashCode() 方法。它的作用是：
// callSuper = true
    // 在继承结构中，如果需要比较父类的字段，必须设置 callSuper = true，否则父类字段会被忽略
//表示在生成的 equals() 和 hashCode() 方法中，会调用父类的 equals() 和 hashCode() 方法
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ReActAgent extends BaseAgent {


    /**
     * 处理当前状态并决定下一步行动
     * 用 ture 表示需要执行，false 表示不需要执行
     *
     */
    public abstract boolean think();


    /**
     * 执行决定的行动
     */
    public abstract String act();


    /**
     * 执行单个步骤，思考与行动
     */
    public  String step(){
        try {
            boolean shouldAct = think();
            if (!shouldAct){
                return "思考完成 - 无需行动";
            }
            return act();
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"系统异常"+e.getMessage());
        }

    }

}
