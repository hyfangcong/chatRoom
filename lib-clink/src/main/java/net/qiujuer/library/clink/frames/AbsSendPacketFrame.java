package net.qiujuer.library.clink.frames;

import net.qiujuer.library.clink.core.Frame;
import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.SendPacket;

import java.io.IOException;

/**
 * @author: fangcong
 * @date: 2019/6/1
 */
public abstract class AbsSendPacketFrame extends AbsSendFrame {
    protected volatile SendPacket<?> sendPacket;

    public AbsSendPacketFrame(int length, byte type, byte flag, short identifier, SendPacket sendPacket) {
        super(length, type, flag, identifier);
        this.sendPacket = sendPacket;
    }

    /**
     * 获取当前发送的packet
     * @return SendPacket
     */
    public synchronized SendPacket getPacket(){
        return sendPacket;
    }

    @Override
    public synchronized boolean handle(IoArgs ioArgs) throws IOException {
        if(sendPacket == null && !isSending()){
            //已取消，并且没有发送任何数据，直接返回结束，发送下一帧
            return true;
        }
        return super.handle(ioArgs);
    }

    @Override
    public final synchronized Frame nextFrame() {
        return sendPacket == null ? null : buildNextFrame();
    }

    protected abstract Frame buildNextFrame();

    /**
     * true: 当前帧没有发送任何数据
     * false：当前帧已经发送部分数据
     * @return
     */
    public synchronized boolean abort(){
        boolean isSending = isSending();
        if(isSending){
            fillDirtyDataOnAbort();
        }
        sendPacket = null;
        return !isSending;
    }

    @Override
    public int getConsumeLength() {
        return headerRemaining + bodyRemain;
    }

    /**
     * 可以由子类重写
     */
    protected void fillDirtyDataOnAbort(){

    }
}
