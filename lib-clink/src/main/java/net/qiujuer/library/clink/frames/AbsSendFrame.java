package net.qiujuer.library.clink.frames;

import net.qiujuer.library.clink.core.Frame;
import net.qiujuer.library.clink.core.IoArgs;

import java.io.IOException;

/**
 * @author: fangcong
 * @date: 2019/6/1
 */
public abstract class AbsSendFrame extends Frame {
    /**
     * 帧头剩余还没有发送的字节数
     */
     volatile byte headerRemaining = Frame.FRAME_HEADER_LENGTH;
    /**
     *  帧体剩余还没有发送的字节数
     */
    volatile int bodyRemain;

    public AbsSendFrame(int length, byte type, byte flag, short identifier) {
        super(length, type, flag, identifier);
        bodyRemain = length;
    }

    @Override
    public synchronized boolean handle(IoArgs ioArgs) throws IOException {
        try {
            ioArgs.limit(headerRemaining + bodyRemain);
            ioArgs.startWriting();

            if (headerRemaining > 0 && ioArgs.remained()) {
                headerRemaining -= consumeHeader(ioArgs);
            }

            if (headerRemaining == 0 && ioArgs.remained() && bodyRemain > 0) {
                bodyRemain -= consumeBody(ioArgs);
            }
            return headerRemaining == 0 && bodyRemain == 0;
        }finally {
            ioArgs.finishedWrite();
        }
    }

    protected byte consumeHeader(IoArgs ioArgs){
        int count = headerRemaining;
        int offset = header.length - count;
        return (byte) ioArgs.readFrom(header, offset, count);
    }

    @Override
    public Frame nextFrame() {
        return null;
    }

    @Override
    public int getConsumeLength() {
        return bodyRemain;
    }

    protected abstract int consumeBody(IoArgs ioArgs) throws IOException;

    /**
     * 判断当前帧是否已经发送了
     * @return
     */
    protected  synchronized boolean isSending(){
        return headerRemaining < Frame.FRAME_HEADER_LENGTH;
    }
}
