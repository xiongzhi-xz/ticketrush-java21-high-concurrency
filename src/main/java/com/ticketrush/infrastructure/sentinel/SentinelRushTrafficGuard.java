package com.ticketrush.infrastructure.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.ticketrush.application.service.RushTrafficGuard;
import com.ticketrush.common.api.ErrorCode;
import com.ticketrush.common.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * Sentinel 抢票流量保护。
 *
 * <p>普通资源控制整体抢票入口流量；热点参数资源按 skuId 控制热门票档流量。</p>
 */
@Component
public class SentinelRushTrafficGuard implements RushTrafficGuard {

    @Override
    public Permit enter(Long skuId) {
        Entry rushEntry = null;
        Entry hotSkuEntry = null;
        try {
            rushEntry = SphU.entry(SentinelResourceNames.RUSH_TICKET, EntryType.IN);
            hotSkuEntry = SphU.entry(SentinelResourceNames.RUSH_TICKET_SKU, EntryType.IN, 1, skuId);
            return new SentinelPermit(rushEntry, hotSkuEntry);
        } catch (BlockException exception) {
            closeQuietly(hotSkuEntry);
            closeQuietly(rushEntry);
            throw new BusinessException(ErrorCode.RATE_LIMITED, "抢票请求过于频繁，请稍后再试");
        }
    }

    private void closeQuietly(Entry entry) {
        if (entry != null) {
            entry.close();
        }
    }

    private record SentinelPermit(Entry rushEntry, Entry hotSkuEntry) implements Permit {

        @Override
        public void close() {
            hotSkuEntry.close();
            rushEntry.close();
        }
    }
}
