package org.apache.camel.quarkus.core.runtime.support;

import java.util.function.Supplier;

import org.apache.camel.CamelContext;
import org.apache.camel.quarkus.core.runtime.support.FastCamelContext;

public class FastCamelContextSupplier implements Supplier<CamelContext> {

    @Override
    public CamelContext get() {
        return new FastCamelContext();
    }
}
