package net.qiujuer.library.clink.box;

import net.qiujuer.library.clink.core.SendPacket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 * @author: fangcong
 * @date: 2019/5/27
 */
public class StringSendPacket extends SendPacket<ByteArrayInputStream> {
    private final byte[] bytes;

    public StringSendPacket(String msg) {
        this.bytes = msg.getBytes();
        this.length = bytes.length;
    }

    @Override
    protected ByteArrayInputStream createStream() {
        return new ByteArrayInputStream(bytes);
    }
}
