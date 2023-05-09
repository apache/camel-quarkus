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

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

@ApplicationScoped
@Named("imageAwtService")
public class ImageService implements IImageService {

    public static final String MSG_SUCCESS = "Upload Successful";
    private static final Logger log = Logger.getLogger(ImageService.class);

    private final Map<String, Image> imageRepository = new ConcurrentHashMap<>();

    @Override
    public Image downloadImage(String name) {
        final Image image = imageRepository.get(name);
        if (image == null) {
            throw new IllegalStateException("Image with name " + name + " does not exist.");
        }
        return image;
    }

    @Override
    public String uploadImage(Image image, String imageName) {

        log.infof("Upload image: %s", imageName);

        if (image != null && imageName != null) {
            imageRepository.put(imageName, image);
            return MSG_SUCCESS;
        }
        throw new IllegalStateException("Illegal Data Format.");
    }

}
