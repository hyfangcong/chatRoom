package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.SendDispatcher;
import net.qiujuer.library.clink.core.SendPacket;
import net.qiujuer.library.clink.core.Sender;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: fangcong
 * @date: 2019/5/27
 */
public class AsyncSendDispatcher implements SendDispatcher {
    private final AtomicBoolean isClosed = new AtomicBoolean();
    private final Sender sender;
    private final Queue<SendPacket> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final Object queueLock = new Object();

    private IoArgs ioArgs = new IoArgs();
    private SendPacket packetTemp;

    //当前大小以及进度
    private int total;
    private int position;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
    }

    @Override
    public void send(SendPacket packet) {
        synchronized (queueLock) {
            queue.offer(packet);
            if (isSending.compareAndSet(false, true)) ;
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
            SendPacket packet = this.packetTemp;
            if(packet != null){
                this.packetTemp = null;
                CloseUtils.close(packet);
            }
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
        IoArgs ioArgs = new IoArgs();
        ioArgs.startWriting();
        if(position >= total){
            sendNextMessage();
            return;
        }else if(position == 0){
            //发送首包,需要携带长度信息
            ioArgs.writeLength(total);
        }

        byte[] bytes = packetTemp.bytes();
        //把bytes中的数据写到ioArgs
        int count = ioArgs.readFrom(bytes, position);
        position += count;
        //完成封装
        ioArgs.finishedWrite();

        try {
            System.out.println("异步发送-ioArgs hashcode-" + ioArgs.hashCode());
            sender.sendAsync(ioArgs, ioArgsListener);
        } catch (IOException e) {
            System.out.println("IO异常");
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    private final IoArgs.IoArgsListener ioArgsListener = new IoArgs.IoArgsListener() {
        @Override
        public void onStart(IoArgs ioArgs) {

        }

        @Override
        public void onComplement(IoArgs ioArgs) {
            //继续发送当前包
            sendCurrentPacket();
        }
    };

}
