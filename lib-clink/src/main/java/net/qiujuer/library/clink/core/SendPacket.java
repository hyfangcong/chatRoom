package net.qiujuer.library.clink.core;

/**
 * @author: fangcong
 * @date: 2019/5/27
 */

/**
 * 发送包的定义
 */
public abstract class SendPacket extends Packet{
    private boolean isCanceled;

    public abstract byte[] bytes();

    public boolean isCanceled(){
        return this.isCanceled;
    }
}
