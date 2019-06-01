package net.qiujuer.library.clink.box;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import net.qiujuer.library.clink.core.ReceivePacket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author: fangcong
 * @date: 2019/5/27
 */
public class StringReceivePacket extends AbsByteArrayReceivePacket<String>  {
    public StringReceivePacket(long len){
        super(len);
    }

    @Override
    protected String buildEntity(ByteArrayOutputStream stream) {
        return new String(stream.toByteArray());
    }


    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }
}
