package org.apache.camel.quarkus.component.tika.graalvm;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.ContentHandler;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.component.tika.TikaConfiguration;
import org.apache.camel.component.tika.TikaParseOutputFormat;
import org.apache.camel.component.tika.TikaProducer;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ExpandedTitleContentHandler;

// TODO: Remove this when Camel Tika & Quarkus Tika versions are aligned
// https://github.com/apache/camel-quarkus/issues/3599
@TargetClass(TikaProducer.class)
public final class TikaProducerSubstitutions {

    @Alias
    private String encoding;

    // Removes problematic textMain switch case since it's covered in the custom TikaProducer in TikaRecorder
    @Substitute
    private ContentHandler getContentHandler(TikaConfiguration configuration, OutputStream outputStream)
            throws TransformerConfigurationException, UnsupportedEncodingException {

        ContentHandler result = null;

        TikaParseOutputFormat outputFormat = configuration.getTikaParseOutputFormat();
        switch (outputFormat) {
        case xml:
            result = getTransformerHandler(outputStream, "xml", true);
            break;
        case text:
            result = new BodyContentHandler(new OutputStreamWriter(outputStream, this.encoding));
            break;
        case html:
            result = new ExpandedTitleContentHandler(getTransformerHandler(outputStream, "html", true));
            break;
        default:
            throw new IllegalArgumentException(
                    String.format("Unknown format %s", configuration.getTikaParseOutputFormat()));
        }
        return result;
    }

    @Alias
    private TransformerHandler getTransformerHandler(
            OutputStream output, String method,
            boolean prettyPrint)
            throws TransformerConfigurationException, UnsupportedEncodingException {
        return null;
    }
}
