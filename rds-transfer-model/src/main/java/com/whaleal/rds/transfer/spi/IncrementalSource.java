package com.whaleal.rds.transfer.spi;

/**
 * 增量源：三方解析（Debezium / Canal 等）经适配器产出 {@code RowChange} / {@code DdlEvent}。
 * 换解析器不改 Pipeline / Sink。
 */
public interface IncrementalSource extends AutoCloseable {

    void start(RowChangeListener rows, DdlEventListener ddl);

    void pause();

    void resume();

    void stop();

    @Override
    void close();
}
