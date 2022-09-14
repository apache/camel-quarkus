package org.springframework.transaction.support;

import org.springframework.transaction.PlatformTransactionManager;

public class TransactionTemplate {
    public PlatformTransactionManager getTransactionManager() {
        throw new UnsupportedOperationException("getTransactionManager is not supported");
    }
}
