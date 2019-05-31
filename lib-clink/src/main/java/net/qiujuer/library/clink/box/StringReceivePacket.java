package net.qiujuer.library.clink.box;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import net.qiujuer.library.clink.core.ReceivePacket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author: fangcong
 * @date: 2019/5/27
 */
public class StringReceivePacket extends ReceivePacket<ByteArrayOutputStream> {
    private String string;
    public StringReceivePacket(int len){
        this.length = len;
    }

    public String string(){
        return string;
    }

    @Override
    protected ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int) length);
    }

    @Override
    protected void closeStream(ByteArrayOutputStream outputStream) throws IOException {
        super.closeStream(outputStream);
        string = new String(outputStream.toByteArray());
    }
}
