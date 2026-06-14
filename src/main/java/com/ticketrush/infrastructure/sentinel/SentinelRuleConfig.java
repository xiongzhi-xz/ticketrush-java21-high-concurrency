package com.ticketrush.infrastructure.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Sentinel 本地规则加载。
 *
 * <p>生产环境可以把规则迁移到 Nacos 动态配置；本地规则便于项目启动后直接演示限流效果。</p>
 */
@Configuration
@EnableConfigurationProperties(SentinelRuleProperties.class)
public class SentinelRuleConfig {

    private final SentinelRuleProperties properties;

    public SentinelRuleConfig(SentinelRuleProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void loadRules() {
        if (!properties.enabled()) {
            return;
        }

        FlowRule rushRule = new FlowRule(SentinelResourceNames.RUSH_TICKET)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(properties.rushQps())
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);

        ParamFlowRule hotSkuRule = new ParamFlowRule(SentinelResourceNames.RUSH_TICKET_SKU)
                .setParamIdx(0)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(properties.hotspotSkuQps())
                .setDurationInSec(properties.hotspotDurationSeconds())
                .setBurstCount(properties.hotspotBurstCount());

        FlowRuleManager.loadRules(List.of(rushRule));
        ParamFlowRuleManager.loadRules(List.of(hotSkuRule));
    }
}
