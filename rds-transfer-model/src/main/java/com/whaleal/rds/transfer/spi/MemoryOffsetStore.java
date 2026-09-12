package com.whaleal.rds.transfer.spi;

import java.util.concurrent.ConcurrentHashMap;

/** 进程内位点；默认实现。文件 / DB 存储后续可插拔。 */
public final class MemoryOffsetStore implements OffsetStore {

    private final ConcurrentHashMap<String, String> offsets = new ConcurrentHashMap<String, String>();

    @Override
    public void save(String namespace, String offset) {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("namespace is required");
        }
        if (offset == null) {
            offsets.remove(namespace);
            return;
        }
        offsets.put(namespace, offset);
    }

    @Override
    public String load(String namespace) {
        if (namespace == null) {
            return null;
        }
        return offsets.get(namespace);
    }
}
