package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.box.StringReceivePacket;
import net.qiujuer.library.clink.core.*;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: fangcong
 * @date: 2019/5/28
 */
public class AsyncReceiveDispatcher implements ReceiveDispatcher,
        IoArgs.IoArgsEventProcessor , AsyncPacketWriter.PacketProvider {
    private final AtomicBoolean isClosed = new AtomicBoolean();

    private final Receiver receiver;
    private final ReceivePacketCallback callback;

    private final AsyncPacketWriter packetWriter = new AsyncPacketWriter(this);

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback callback){
        this.receiver = receiver;
        receiver.setReceiveListener(this);
        this.callback = callback;
    }

    @Override
    public void start() {
        registerReceive();
    }


    @Override
    public void stop() {

    }

    @Override
    public void close() throws IOException {
        if(isClosed.compareAndSet(false, true)) {
            packetWriter.close();
        }
    }

    private void registerReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public IoArgs provideIoArgs() {
        return packetWriter.takeIoArgs();
    }

    @Override
    public void onConsumeFailed(IoArgs ioArgs, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IoArgs ioArgs) {
        //递归操作
        do {
            packetWriter.consumeIoArgs(ioArgs);
        }while(ioArgs.remained());
        registerReceive();
    }


    @Override
    public ReceivePacket takePacket(byte type, long length, byte[] headerInfo) {
        return callback.onArrivedNewPacket(type, length);
    }

    @Override
    public void completedPacket(ReceivePacket packet, boolean isSuccess) {
        CloseUtils.close(packet);
        callback.onReceivePacketCompleted(packet);
    }
}
