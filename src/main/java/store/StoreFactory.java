package store;

import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;

public abstract class StoreFactory {
    @Getter
    public static ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
}
