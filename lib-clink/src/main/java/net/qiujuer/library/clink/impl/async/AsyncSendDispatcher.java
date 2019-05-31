package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.SendDispatcher;
import net.qiujuer.library.clink.core.SendPacket;
import net.qiujuer.library.clink.core.Sender;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: fangcong
 * @date: 2019/5/27
 */
public class AsyncSendDispatcher implements SendDispatcher, IoArgs.IoArgsEventProcessor {
    private final AtomicBoolean isClosed = new AtomicBoolean();
    private final Sender sender;
    private final Queue<SendPacket> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final Object queueLock = new Object();

    private SendPacket packetTemp;

    //当前大小以及进度
    private ReadableByteChannel packetChannel;
    private long total;
    private long position;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        sender.setSendListener(this);
    }

    @Override
    public void send(SendPacket packet) {
        synchronized (queueLock) {
            queue.offer(packet);
            if (isSending.compareAndSet(false, true))
                sendNextMessage();
        }
    }

    @Override
    public void cancel(SendPacket packet) {

    }

    @Override
    public void close() throws IOException {
        if(isClosed.compareAndSet(false, true)){
            isSending.set(false);
            //异常关闭导致的完成操作
            completePacket(false);
        }
    }

    private SendPacket takePacket(){
        synchronized (queueLock) {
            SendPacket packet = queue.poll();
            if (packet != null && packet.isCanceled()) {
                //已取消，不用发送
                takePacket();
            }
            return packet;
        }
    }
    private void sendNextMessage(){
        SendPacket temp = packetTemp;
        if(temp != null){
            CloseUtils.close(temp);
        }
        //拿一个新的包
        SendPacket packet = packetTemp = takePacket();
        if(packet == null){
            //队列为空，取消发送状态
            isSending.set(false);
            return;
        }
        this.total = packet.length();
        this.position = 0;
        sendCurrentPacket();
    }

    private void sendCurrentPacket() {
        if(position >= total){
            completePacket(position == total);
            sendNextMessage();
            return;
        }

        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    /**
     * 完成Packet发送
     * @param isSuccess  是否成功
     */
    private void completePacket(boolean isSuccess){
        SendPacket packet = this.packetTemp;
        if(packet == null)
            return;
        CloseUtils.close(packet);
        CloseUtils.close(packetChannel);

        packetTemp = null;
        packetChannel = null;
        total = 0;
        position = 0;
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public IoArgs provideIoArgs() {
        IoArgs ioArgs = new IoArgs();
        if (packetChannel == null) {
            packetChannel = Channels.newChannel(packetTemp.open());
            //发送一个包的首包,需要携带长度
            ioArgs.limit(4);
            ioArgs.writeLength((int) packetTemp.length());
        } else {
            ioArgs.limit((int) Math.min(ioArgs.capacity(), total - position));
            try {
                int count = ioArgs.readFrom(packetChannel);
                position += count;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return ioArgs;
    }

    @Override
    public void onConsumeFailed(IoArgs ioArgs, Exception e) {

    }

    @Override
    public void onConsumeCompleted(IoArgs ioArgs) {
        sendCurrentPacket();
    }
}
