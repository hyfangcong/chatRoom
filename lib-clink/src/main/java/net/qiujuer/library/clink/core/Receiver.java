package net.qiujuer.library.clink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author: fangcong
 * @date: 2019/5/26
 */
public interface Receiver extends Closeable {
    void setReceiveListener(IoArgs.IoArgsListener listener);

    boolean receiveAsync(IoArgs ioArgs) throws IOException;
}
