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
package org.apache.camel.quarkus.component.aws2.ses.it;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.apache.camel.quarkus.test.support.aws2.Aws2Client;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.apache.camel.quarkus.test.support.aws2.BaseAWs2TestSupport;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.DeleteVerifiedEmailAddressRequest;
import software.amazon.awssdk.services.ses.model.VerifyEmailAddressRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/* Disabled on Localstack because Localstack does not send e-mails which we do assume in our tests
 * See https://github.com/localstack/localstack/issues/339#issuecomment-341727758 */
@EnabledIfEnvironmentVariable(named = "AWS_ACCESS_KEY", matches = "[a-zA-Z0-9]+")
@EnabledIfEnvironmentVariable(named = "MAILSLURP_API_KEY", matches = "[a-zA-Z0-9]+")
@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2SesTest extends BaseAWs2TestSupport {
    private static final Logger LOG = Logger.getLogger(Aws2SesTest.class);

    private static final Pattern VERIFCATION_LINK_PATTERN = Pattern.compile("https://email-verification[^\\s]+");

    @Aws2Client(Service.SES)
    SesClient sesClient;

    public Aws2SesTest() {
        super("/aws2-ses");
    }

    @Test
    public void test() throws InterruptedException, ExecutionException, TimeoutException {

        /* First create a test mailbox at mailslurp.com */
        final Config config = ConfigProvider.getConfig();
        final String mailSlurpApiKey = config.getValue("mailslurp.api.key", String.class);
        final JsonPath mailbox = RestAssured.given()
                .header("x-api-key", mailSlurpApiKey)
                .post("https://api.mailslurp.com:443/createInbox") //
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath();

        final String mailSlurpAddress = mailbox.get("emailAddress");
        final String mailboxId = mailbox.get("id");
        LOG.infof("Using mailslurp inbox id: %s", mailboxId);

        /* Second, verify the mailslurp.com mailbox address at SES */
        try {
            /* Send the verification e-mail to mailslurp.com */
            sesClient.verifyEmailAddress(
                    VerifyEmailAddressRequest.builder()
                            .emailAddress(mailSlurpAddress)
                            .build());

            /* Await the verification e-mail at mailslurp.com */
            List<String> messageIds = Awaitility.await()
                    .pollDelay(1, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .atMost(120, TimeUnit.SECONDS)
                    .until(
                            () -> {
                                final ExtractableResponse<Response> response = RestAssured.given()
                                        .header("x-api-key", mailSlurpApiKey)
                                        .get("https://api.mailslurp.com:443/inboxes/" + mailboxId + "/emails") //
                                        .then()
                                        .statusCode(200)
                                        .extract();
                                final List<String> ids = response.jsonPath().getList("id");
                                LOG.infof("Expected an identity verification message from SES; got %s",
                                        response.body().asString());
                                return ids;
                            },
                            Matchers.not(Matchers.empty()));

            /* Get the verification link out of the verification e-mail and "click" it */
            boolean verified = false;
            for (String id : messageIds) {
                final JsonPath email = RestAssured.given()
                        .header("x-api-key", mailSlurpApiKey)
                        .get("https://api.mailslurp.com:443/emails/" + id) //
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .jsonPath();
                String from = email.getString("from");
                String body = email.getString("body");
                LOG.debugf("Got e-mail from %s: %s", from, body);
                Matcher m = VERIFCATION_LINK_PATTERN.matcher(body);
                if (m.find()) {

                    final String link = m.group();
                    LOG.infof("Found verification link %s", link);

                    /*
                     * We use HtmlUnitDriver because RestAssured was not able to follow the labyrinth of redirects and
                     * other traps
                     */
                    WebDriver driver = new HtmlUnitDriver();
                    driver.get(link);
                    String source = driver.getPageSource();
                    assertThat(source).contains("You have successfully verified an email address");
                    verified = true;
                    break;
                }
            }
            assertThat(verified).isTrue();

            /* All prerequisites should be set up now, so we can send the message via SES */
            final String randomId = RandomStringUtils.randomAlphanumeric(16).toLowerCase(Locale.ROOT);
            final String subject = "Test " + randomId;
            final String body = "Hello " + randomId;
            LOG.infof("About to send message to %s with subject %s", mailSlurpAddress, subject);
            RestAssured.given() //
                    .contentType(ContentType.TEXT)
                    .header("x-from", mailSlurpAddress)
                    .header("x-to", mailSlurpAddress)
                    .header("x-subject", subject)
                    .header("x-returnPath", mailSlurpAddress)
                    .body(body)
                    .post("/aws2-ses/send") //
                    .then()
                    .statusCode(201);

            /* Check that the message sent via SES was received in our test mailbox at mailslurp.com */
            Awaitility.await()
                    .pollDelay(1, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .atMost(120, TimeUnit.SECONDS)
                    .until(
                            () -> {
                                final ExtractableResponse<Response> response = RestAssured.given()
                                        .header("x-api-key", mailSlurpApiKey)
                                        .get("https://api.mailslurp.com:443/inboxes/" + mailboxId + "/emails") //
                                        .then()
                                        .statusCode(200)
                                        .extract();
                                final List<String> subjects = response.jsonPath().getList("subject");
                                LOG.infof("Expected subject '%s'; got %s", subject, response.body().asString());
                                return subjects;
                            },
                            Matchers.hasItem(subject));

        } finally {

            sesClient.deleteVerifiedEmailAddress(
                    DeleteVerifiedEmailAddressRequest.builder()
                            .emailAddress(mailSlurpAddress)
                            .build());

            RestAssured.given()
                    .header("x-api-key", mailSlurpApiKey)
                    .delete("https://api.mailslurp.com:443/inboxes/" + mailboxId) //
                    .then()
                    .statusCode(204);
        }
    }

    @Override
    public void testMethodForDefaultCredentialsProvider() {
        try {
            test();
        } catch (Exception e) {
            fail(e);
        }
    }
}
