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
package org.apache.camel.quarkus.component.netty;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.apache.camel.component.netty.ChannelHandlerFactories;
import org.apache.camel.component.netty.ChannelHandlerFactory;

public final class NettyCodecHelper {

    private NettyCodecHelper() {
        // Utility class
    }

    public static ChannelHandlerFactory createNullDelimitedHandler(String protocol) {
        ByteBuf delimiter = Unpooled.wrappedBuffer(new byte[] { 0 });
        ByteBuf[] delimiters = new ByteBuf[] { delimiter, delimiter };
        return ChannelHandlerFactories.newDelimiterBasedFrameDecoder(4096, delimiters, protocol);
    }

    public static BytesDecoder createBytesDecoder() {
        return new BytesDecoder();
    }

    public static BytesEncoder createBytesEncoder() {
        return new BytesEncoder();
    }

    @ChannelHandler.Sharable
    static class BytesDecoder extends MessageToMessageDecoder<ByteBuf> {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
            if (msg.isReadable()) {
                byte[] bytes = new byte[msg.readableBytes()];
                int readerIndex = msg.readerIndex();
                msg.getBytes(readerIndex, bytes);
                out.add(bytes);
            }
        }
    }

    @ChannelHandler.Sharable
    static class BytesEncoder extends MessageToMessageEncoder<byte[]> {

        @Override
        protected void encode(ChannelHandlerContext ctx, byte[] msg, List<Object> out) throws Exception {
            byte[] bytes = msg;
            ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(bytes.length);
            buf.writeBytes(bytes);
            out.add(buf);
        }
    }
}
