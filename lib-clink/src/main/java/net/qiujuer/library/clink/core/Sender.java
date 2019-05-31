package net.qiujuer.library.clink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author: fangcong
 * @date: 2019/5/26
 */
public interface Sender extends Closeable {
    void setSendListener(IoArgs.IoArgsEventProcessor processor);

    boolean postSendAsync() throws IOException;
}
