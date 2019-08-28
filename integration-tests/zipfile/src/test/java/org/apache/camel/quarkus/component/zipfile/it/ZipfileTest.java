package org.apache.camel.quarkus.component.zipfile.it;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.IOHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ZipfileTest {

    @Test
    public void test() throws Exception {
        byte[] body;

        ExtractableResponse response = RestAssured.given() //
            .contentType(ContentType.TEXT).body("Hello World").post("/zipfile/post") //
            .then().extract();

        body = response.body().asByteArray();
        Assertions.assertNotNull(body);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(body));
        ZipEntry entry = zis.getNextEntry();
        if (entry != null) {
            IOHelper.copy(zis, bos);
        }

        String str = bos.toString();
        Assertions.assertEquals("Hello World", str);
    }

}
