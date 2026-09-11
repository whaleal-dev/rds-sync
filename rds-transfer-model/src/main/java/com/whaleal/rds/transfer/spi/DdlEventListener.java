package com.whaleal.rds.transfer.spi;

import com.whaleal.rds.transfer.model.DdlEvent;

public interface DdlEventListener {
    void onDdl(DdlEvent event);
}
