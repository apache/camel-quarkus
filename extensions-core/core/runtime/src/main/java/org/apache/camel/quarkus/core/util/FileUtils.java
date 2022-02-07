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
package org.apache.camel.quarkus.core.util;

import java.io.File;
import java.nio.file.Path;

import org.apache.camel.util.FileUtil;

public final class FileUtils {

    private FileUtils() {
        // Utility class
    }

    /**
     * Converts non-*nix path separator characters to the *nix alternative
     * 
     * @param  path The {@link java.nio.file.Path} to convert
     * @return      String representation of the path with *nix separators
     */
    public static String nixifyPath(Path path) {
        return nixifyPath(path.toString());
    }

    /**
     * Converts non-*nix path separator characters to the *nix alternative
     * 
     * @param  path The path String to convert
     * @return      String representation of the path with *nix separators
     */
    public static String nixifyPath(String path) {
        if (FileUtil.isWindows()) {
            return path.replace(File.separatorChar, '/');
        }
        return path;
    }
}
