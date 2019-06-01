package net.qiujuer.library.clink.frames;

import net.qiujuer.library.clink.core.IoArgs;

import java.io.IOException;

/**
 * @author: fangcong
 * @date: 2019/6/1
 */
public class CancelReceiveFrame extends AbsReceiveFrame {
    CancelReceiveFrame(byte[] header){
        super(header);
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        return 0;
    }
}
