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
package org.apache.camel.component.telegram.service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.MediaType;

import org.apache.camel.component.telegram.TelegramService;
import org.apache.camel.component.telegram.model.EditMessageLiveLocationMessage;
import org.apache.camel.component.telegram.model.MessageResult;
import org.apache.camel.component.telegram.model.OutgoingAudioMessage;
import org.apache.camel.component.telegram.model.OutgoingDocumentMessage;
import org.apache.camel.component.telegram.model.OutgoingMessage;
import org.apache.camel.component.telegram.model.OutgoingPhotoMessage;
import org.apache.camel.component.telegram.model.OutgoingTextMessage;
import org.apache.camel.component.telegram.model.OutgoingVideoMessage;
import org.apache.camel.component.telegram.model.SendLocationMessage;
import org.apache.camel.component.telegram.model.SendVenueMessage;
import org.apache.camel.component.telegram.model.StopMessageLiveLocationMessage;
import org.apache.camel.component.telegram.model.UpdateResult;
import org.apache.camel.component.telegram.model.WebhookInfo;
import org.apache.camel.component.telegram.model.WebhookResult;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

/**
 * Adapts the {@code RestBotAPI} to the {@code TelegramService} interface.
 */
public class TelegramServiceRestBotAPIAdapter implements TelegramService {

    private final Object lock = new Object();
    /** Access only via {@link #getOrCreateRestBotApi()} */
    private RestBotAPI api;

    public TelegramServiceRestBotAPIAdapter() {
    }

    public TelegramServiceRestBotAPIAdapter(RestBotAPI api) {
        this.api = api;
    }

    /**
     * @return a lazily created {@link RestBotAPI}
     */
    private RestBotAPI getOrCreateRestBotApi() {
        synchronized (lock) {
            RestBotAPI result = this.api;
            if (result == null) {
                try {
                    this.api = result = RestClientBuilder.newBuilder().baseUri(new URI(RestBotAPI.BOT_API_DEFAULT_URL))
                            .build(RestBotAPI.class);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            return result;
        }
    }

    @Override
    public void setHttpProxy(String host, Integer port) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UpdateResult getUpdates(String authorizationToken, Long offset, Integer limit, Integer timeoutSeconds) {
        return getOrCreateRestBotApi().getUpdates(authorizationToken, offset, limit, timeoutSeconds);
    }

    @Override
    public boolean setWebhook(String authorizationToken, String url) {
        WebhookResult res = getOrCreateRestBotApi().setWebhook(authorizationToken, new WebhookInfo(url));
        return res.isOk() && res.isResult();
    }

    @Override
    public boolean removeWebhook(String authorizationToken) {
        WebhookResult res = getOrCreateRestBotApi().setWebhook(authorizationToken, new WebhookInfo(""));
        return res.isOk() && res.isResult();
    }

    @Override
    public Object sendMessage(String authorizationToken, OutgoingMessage message) {
        Object resultMessage;

        if (message instanceof OutgoingTextMessage) {
            resultMessage = this.sendMessage(authorizationToken, (OutgoingTextMessage) message);
        } else if (message instanceof OutgoingPhotoMessage) {
            resultMessage = this.sendMessage(authorizationToken, (OutgoingPhotoMessage) message);
        } else if (message instanceof OutgoingAudioMessage) {
            resultMessage = this.sendMessage(authorizationToken, (OutgoingAudioMessage) message);
        } else if (message instanceof OutgoingVideoMessage) {
            resultMessage = this.sendMessage(authorizationToken, (OutgoingVideoMessage) message);
        } else if (message instanceof OutgoingDocumentMessage) {
            resultMessage = this.sendMessage(authorizationToken, (OutgoingDocumentMessage) message);
        } else if (message instanceof SendLocationMessage) {
            resultMessage = getOrCreateRestBotApi().sendLocation(authorizationToken, (SendLocationMessage) message);
        } else if (message instanceof EditMessageLiveLocationMessage) {
            resultMessage = getOrCreateRestBotApi().editMessageLiveLocation(authorizationToken,
                    (EditMessageLiveLocationMessage) message);
        } else if (message instanceof StopMessageLiveLocationMessage) {
            resultMessage = getOrCreateRestBotApi().stopMessageLiveLocation(authorizationToken,
                    (StopMessageLiveLocationMessage) message);
        } else if (message instanceof SendVenueMessage) {
            resultMessage = getOrCreateRestBotApi().sendVenue(authorizationToken, (SendVenueMessage) message);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported message type " + (message != null ? message.getClass().getName() : null));
        }

        return resultMessage;
    }

    private MessageResult sendMessage(String authorizationToken, OutgoingTextMessage message) {
        return getOrCreateRestBotApi().sendMessage(authorizationToken, message);
    }

    private MessageResult sendMessage(String authorizationToken, OutgoingPhotoMessage message) {
        MultipartFormDataOutput mdo = new MultipartFormDataOutput();

        fillCommonMediaParts(mdo, message);

        buildMediaPart(mdo, "photo", message.getFilenameWithExtension(), message.getPhoto());
        if (message.getCaption() != null) {
            buildTextPart(mdo, "caption", message.getCaption());
        }

        return getOrCreateRestBotApi().sendPhoto(authorizationToken, mdo);
    }

    private MessageResult sendMessage(String authorizationToken, OutgoingAudioMessage message) {
        MultipartFormDataOutput parts = new MultipartFormDataOutput();

        fillCommonMediaParts(parts, message);

        buildMediaPart(parts, "audio", message.getFilenameWithExtension(), message.getAudio());
        if (message.getTitle() != null) {
            buildTextPart(parts, "title", message.getTitle());
        }
        if (message.getDurationSeconds() != null) {
            buildTextPart(parts, "duration", String.valueOf(message.getDurationSeconds()));
        }
        if (message.getPerformer() != null) {
            buildTextPart(parts, "performer", message.getPerformer());
        }

        return getOrCreateRestBotApi().sendAudio(authorizationToken, parts);
    }

    private MessageResult sendMessage(String authorizationToken, OutgoingVideoMessage message) {
        MultipartFormDataOutput parts = new MultipartFormDataOutput();

        fillCommonMediaParts(parts, message);

        buildMediaPart(parts, "video", message.getFilenameWithExtension(), message.getVideo());
        if (message.getCaption() != null) {
            buildTextPart(parts, "caption", message.getCaption());
        }
        if (message.getDurationSeconds() != null) {
            buildTextPart(parts, "duration", String.valueOf(message.getDurationSeconds()));
        }
        if (message.getWidth() != null) {
            buildTextPart(parts, "width", String.valueOf(message.getWidth()));
        }
        if (message.getHeight() != null) {
            buildTextPart(parts, "height", String.valueOf(message.getHeight()));
        }

        return getOrCreateRestBotApi().sendVideo(authorizationToken, parts);
    }

    private MessageResult sendMessage(String authorizationToken, OutgoingDocumentMessage message) {
        MultipartFormDataOutput parts = new MultipartFormDataOutput();

        fillCommonMediaParts(parts, message);

        buildMediaPart(parts, "document", message.getFilenameWithExtension(), message.getDocument());
        if (message.getCaption() != null) {
            buildTextPart(parts, "caption", message.getCaption());
        }

        return getOrCreateRestBotApi().sendDocument(authorizationToken, parts);
    }

    private void fillCommonMediaParts(MultipartFormDataOutput parts, OutgoingMessage message) {
        buildTextPart(parts, "chat_id", message.getChatId());

        if (message.getReplyToMessageId() != null) {
            buildTextPart(parts, "reply_to_message_id", String.valueOf(message.getReplyToMessageId()));
        }
        if (message.getDisableNotification() != null) {
            buildTextPart(parts, "disable_notification", String.valueOf(message.getDisableNotification()));
        }
    }

    private void buildTextPart(MultipartFormDataOutput parts, String name, String value) {
        parts.addFormData(name, value, MediaType.TEXT_PLAIN_TYPE);
    }

    private void buildMediaPart(MultipartFormDataOutput parts, String name, String fileNameWithExtension, byte[] value) {
        parts.addFormData(name, new ByteArrayInputStream(value), MediaType.APPLICATION_OCTET_STREAM_TYPE,
                fileNameWithExtension);
    }

}
