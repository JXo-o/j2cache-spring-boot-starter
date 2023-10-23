package net.oschina.j2cache.service.cache.impl.lettuce;

import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;

/**
 * ClassName: LettuceByteCodec
 * Package: net.oschina.j2cache.service.cache.impl.lettuce
 * Description: 使用字节编码
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:25
 */
public class LettuceByteCodec implements RedisCodec<String, byte[]> {

    private static final byte[] EMPTY = new byte[0];

    @Override
    public String decodeKey(ByteBuffer byteBuffer) {
        return new String(getBytes(byteBuffer));
    }

    @Override
    public byte[] decodeValue(ByteBuffer byteBuffer) {
        return getBytes(byteBuffer);
    }

    @Override
    public ByteBuffer encodeKey(String s) {
        return ByteBuffer.wrap(s.getBytes());
    }

    @Override
    public ByteBuffer encodeValue(byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }


    private static byte[] getBytes(ByteBuffer buffer) {
        int remaining = buffer.remaining();

        if (remaining == 0) {
            return EMPTY;
        }

        byte[] b = new byte[remaining];
        buffer.get(b);
        return b;
    }
}
