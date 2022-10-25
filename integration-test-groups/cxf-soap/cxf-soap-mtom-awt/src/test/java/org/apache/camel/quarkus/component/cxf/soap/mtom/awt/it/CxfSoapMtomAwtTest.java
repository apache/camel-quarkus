/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.cxf.soap.mtom.awt.it;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class CxfSoapMtomAwtTest {

    @DisabledOnIntegrationTest("https://github.com/apache/camel-quarkus/issues/4208")
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void uploadDownloadMtom(boolean mtomEnabled) throws IOException {
        byte[] imageBytes = CxfSoapMtomAwtTest.class.getClassLoader().getResourceAsStream("linux-image.png").readAllBytes();
        String imageName = "linux-image-name";
        RestAssured.given()
                .contentType(ContentType.BINARY)
                .queryParam("imageName", imageName)
                .queryParam("mtomEnabled", mtomEnabled)
                .body(imageBytes)
                .post("/cxf-soap/mtom-awt/upload")
                .then()
                .statusCode(201)
                .body(CoreMatchers.equalTo(ImageService.MSG_SUCCESS));
        byte[] downloadedImageBytes = RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("imageName", imageName)
                .queryParam("mtomEnabled", mtomEnabled)
                .post("/cxf-soap/mtom-awt/download")
                .then()
                .statusCode(201)
                .extract().asByteArray();

        try (ByteArrayInputStream imageBais = new ByteArrayInputStream(
                imageBytes); ByteArrayInputStream downloadedImageBais = new ByteArrayInputStream(downloadedImageBytes)) {
            Assertions.assertTrue(bufferedImagesEqual(ImageIO.read(imageBais),
                    ImageIO.read(downloadedImageBais)), "Uploaded image should match downloaded");
        }
    }

    // copied from https://stackoverflow.com/a/15305092
    boolean bufferedImagesEqual(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight()) {
            for (int x = 0; x < img1.getWidth(); x++) {
                for (int y = 0; y < img1.getHeight(); y++) {
                    if (img1.getRGB(x, y) != img2.getRGB(x, y))
                        return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

}
