package net.qiujuer.library.clink.core;

/**
 * @author: fangcong
 * @date: 2019/5/27
 */

import java.io.Closeable;

/**
 * 接收数据调度封装
 * 把一份或者多份IoArgs组合成以分packet
 */
public interface ReceiveDispatcher extends Closeable {
    void start();

    void stop();

    /**
     * 接收数据完成时回调
     */
    interface ReceivePacketCallback{
        void onReceivePacketCompleted(ReceivePacket packet);
    }
}
