package net.qiujuer.library.clink.core;

/**
 * @author: fangcong
 * @date: 2019/5/27
 */

/**
 * 接收包的定义
 */
public abstract class ReceivePacket extends Packet{
    public abstract void save(byte[] bytes, int count);
}
