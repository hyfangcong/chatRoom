package net.qiujuer.library.clink.core;

/**
 * @author: fangcong
 * @date: 2019/5/27
 */

import java.io.IOException;
import java.io.InputStream;

/**
 * 发送包的定义
 */
public abstract class SendPacket<T extends InputStream> extends Packet<T>{
    private boolean isCanceled;
    private T inputStream;

    protected abstract T createStream();

    protected void closeStream() throws IOException {
        inputStream.close();
    }

    @Override
    public final T open() {
        if(inputStream == null){
            inputStream = createStream();
        }
        return inputStream;
    }

    @Override
    public final void close() throws IOException {
        if(inputStream != null){
            closeStream();
            inputStream = null;
        }
    }

    public boolean isCanceled(){
        return this.isCanceled;
    }
}
