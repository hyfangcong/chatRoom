package net.qiujuer.library.clink.frames;

import net.qiujuer.library.clink.core.Frame;
import net.qiujuer.library.clink.core.IoArgs;

import java.io.IOException;

/**
 * @author: fangcong
 * @date: 2019/6/1
 */

/**
 * 取消发送的帧，用于取消数据的发送
 */
public class CancelSendFrame extends AbsSendFrame{
    public CancelSendFrame(short identifier) {
        super(0, Frame.TYPE_COMMAND_SEND_CANNEL, Frame.FLAG_NONE, identifier);
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        return 0;
    }

    @Override
    public Frame nextFrame() {
        return null;
    }
}
