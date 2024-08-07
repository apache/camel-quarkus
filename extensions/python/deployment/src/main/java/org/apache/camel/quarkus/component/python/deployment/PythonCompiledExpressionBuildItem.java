package org.apache.camel.quarkus.component.python.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import org.python.core.PyCode;

public final class PythonCompiledExpressionBuildItem extends MultiBuildItem {

    private final String sourceCode;

    private final PyCode compiledCode;

    public PythonCompiledExpressionBuildItem(String sourceCode, PyCode compiledCode) {
        this.sourceCode = sourceCode;
        this.compiledCode = compiledCode;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public PyCode getCompiledCode() {
        return compiledCode;
    }
}