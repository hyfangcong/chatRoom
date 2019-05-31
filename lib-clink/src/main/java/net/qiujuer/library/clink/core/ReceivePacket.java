package net.qiujuer.library.clink.core;

/**
 * @author: fangcong
 * @date: 2019/5/27
 */

import java.io.IOException;
import java.io.OutputStream;

/**
 * 接收包的定义
 */
public abstract class ReceivePacket<T extends OutputStream> extends Packet<T>{
    private T outputStream;

    protected abstract T createStream();

    protected void closeStream(T outputStream) throws IOException {
        this.outputStream.close();
    }
    @Override
    public final T open() {
        if(outputStream == null){
            outputStream = createStream();
        }
        return outputStream;
    }

    @Override
    public final void close() throws IOException {
        if(outputStream != null){
            closeStream(outputStream);
            outputStream = null;
        }
    }
}
