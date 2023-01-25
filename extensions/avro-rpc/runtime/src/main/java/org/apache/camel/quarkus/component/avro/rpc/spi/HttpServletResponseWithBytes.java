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
package org.apache.camel.quarkus.component.avro.rpc.spi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Empty implementation of {@link jakarta.servlet.http.HttpServletResponse}.
 * Serves as a wrapper to be used for {@link org.apache.avro.ipc.ResponderServlet}.
 */
public class HttpServletResponseWithBytes implements HttpServletResponse {

    private ServletOutputStreamWithBytes baos = new ServletOutputStreamWithBytes();

    public byte[] getBytes() {
        return baos.getBytes();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return baos;
    }

    @Override
    public void setContentType(String s) {
        //do nothing
    }

    @Override
    public void setContentLength(int i) {
        //do nothing
    }

    // --------------------- following methods are not implemented and not used ----------------------------

    @Override
    public void addCookie(Cookie cookie) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsHeader(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encodeURL(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encodeRedirectURL(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encodeUrl(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encodeRedirectUrl(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendError(int i, String s) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendError(int i) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendRedirect(String s) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDateHeader(String s, long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDateHeader(String s, long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHeader(String s, String s1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addHeader(String s, String s1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIntHeader(String s, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addIntHeader(String s, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStatus(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStatus(int i, String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHeader(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getHeaders(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getHeaderNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCharacterEncoding() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getContentType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCharacterEncoding(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContentLengthLong(long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBufferSize(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBufferSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flushBuffer() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resetBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCommitted() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLocale(Locale locale) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Locale getLocale() {
        throw new UnsupportedOperationException();
    }

    private class ServletOutputStreamWithBytes extends ServletOutputStream {

        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {

        }

        @Override
        public void write(int b) {
            baos.write(b);
        }

        public byte[] getBytes() {
            return baos.toByteArray();
        }
    }
}
