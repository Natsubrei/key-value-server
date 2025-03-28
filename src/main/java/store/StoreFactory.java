package store;

import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 工厂类，用于提供线程安全的键值存储容器
 */
public abstract class StoreFactory {
    @Getter
    public static ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
}
