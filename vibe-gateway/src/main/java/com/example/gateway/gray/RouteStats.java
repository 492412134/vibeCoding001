package com.example.gateway.gray;

import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class RouteStats {
    private final Map<String, AtomicLong> versionCounts = new ConcurrentHashMap<>();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong grayRequests = new AtomicLong(0);
    private final AtomicLong normalRequests = new AtomicLong(0);
    
    public void incrementVersionCount(String version) {
        versionCounts.computeIfAbsent(version, k -> new AtomicLong(0));
        versionCounts.get(version).incrementAndGet();
    }
    
    public void incrementTotalRequests() {
        totalRequests.incrementAndGet();
    }
    
    public void incrementGrayRequests() {
        grayRequests.incrementAndGet();
    }
    
    public void incrementNormalRequests() {
        normalRequests.incrementAndGet();
    }
    
    public long getVersionCount(String version) {
        AtomicLong count = versionCounts.get(version);
        return count != null ? count.get() : 0;
    }
    
    public long getTotalRequests() {
        return totalRequests.get();
    }
    
    public long getGrayRequests() {
        return grayRequests.get();
    }
    
    public long getNormalRequests() {
        return normalRequests.get();
    }
    
    public Map<String, Long> getAllVersionCounts() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        versionCounts.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }
    
    public void reset() {
        versionCounts.clear();
        totalRequests.set(0);
        grayRequests.set(0);
        normalRequests.set(0);
    }
}
