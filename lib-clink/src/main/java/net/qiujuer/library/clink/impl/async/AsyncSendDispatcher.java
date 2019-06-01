package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.SendDispatcher;
import net.qiujuer.library.clink.core.SendPacket;
import net.qiujuer.library.clink.core.Sender;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: fangcong
 * @date: 2019/5/27
 */
public class AsyncSendDispatcher implements SendDispatcher,
        IoArgs.IoArgsEventProcessor , AsyncPacketReader.PacketProvider {
    private final AtomicBoolean isClosed = new AtomicBoolean();
    private final Sender sender;
    private final Queue<SendPacket> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean();

    private final AsyncPacketReader packetReader = new AsyncPacketReader(this);
    private final Object queueLock = new Object();

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        sender.setSendListener(this);
    }

    @Override
    public void send(SendPacket packet) {
        synchronized (queueLock) {
            queue.offer(packet);
            if (isSending.compareAndSet(false, true))
                if(packetReader.requestTakePacket()){
                    requestSend();
                }
        }
    }

    @Override
    public void cancel(SendPacket packet) {
        boolean res;

        synchronized (queueLock){
            res = queue.remove(packet);
        }
        //如果成功从等待发送的队列中移除，设置该packet为cancel状态，返回
        if(res){
            packet.cancel();
            return;
        }
        //如果移除失败，packet正在发送中，交给AsyncPacketReader处理
        packetReader.cancel(packet);
    }

    @Override
    public void close() throws IOException {
        if(isClosed.compareAndSet(false, true)){
            isSending.set(false);
            //关闭packetReader
            packetReader.close();
        }
    }

    @Override
    public SendPacket takePacket(){
        SendPacket packet;
        synchronized (queueLock) {
            packet = queue.poll();
            if(packet == null){
                //队列为空，取消发送状态
                isSending.set(false);
                return null;
            }
        }
        if (packet.isCanceled()) {
            //已取消，不用发送
            takePacket();
        }
        return packet;
    }

    /**
     * 完成Packet发送
     * @param isSuccess  是否成功
     */
    @Override
    public void completedPacket(SendPacket packet, boolean isSuccess) {
        CloseUtils.close(packet);
    }

    /**
     * 请求网络进行发送
     */
    private void requestSend() {
        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public IoArgs provideIoArgs() {
        return packetReader.fillData();
    }

    @Override
    public void onConsumeFailed(IoArgs ioArgs, Exception e) {
        if(ioArgs != null){
            e.printStackTrace();
        }else{
            //todo
        }
    }

    @Override
    public void onConsumeCompleted(IoArgs ioArgs) {
        if(packetReader.requestTakePacket()){
            //继续发送当前包
            requestSend();
        }
    }
}
