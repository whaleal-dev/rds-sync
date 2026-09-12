package com.whaleal.rds.transfer.spi;

/**
 * 位点存储。推进与 Sink 形态无关：默认跟 Source 回调；严格场景可改为 Sink 落地后再记。
 */
public interface OffsetStore {

    void save(String namespace, String offset);

    /** 无记录时返回 {@code null}。 */
    String load(String namespace);
}
