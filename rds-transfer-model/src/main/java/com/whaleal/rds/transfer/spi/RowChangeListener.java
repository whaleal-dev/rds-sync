package com.whaleal.rds.transfer.spi;

import com.whaleal.rds.transfer.model.RowChange;

public interface RowChangeListener {
    void onEvent(RowChange event);
}
