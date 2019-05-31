package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.box.StringReceivePacket;
import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.ReceiveDispatcher;
import net.qiujuer.library.clink.core.ReceivePacket;
import net.qiujuer.library.clink.core.Receiver;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: fangcong
 * @date: 2019/5/28
 */
public class AsyncReceiveDispatcher implements ReceiveDispatcher, IoArgs.IoArgsEventProcessor {
    private final AtomicBoolean isClosed = new AtomicBoolean();

    private final Receiver receiver;
    private final ReceivePacketCallback callback;

    private IoArgs ioArgs = new IoArgs();
    private ReceivePacket packetTemp;

    private WritableByteChannel packetChannel;
    private long total;
    private long position;

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
            completePacket(false);
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

    //解析数据到packet
    private void assemblePacket(IoArgs ioArgs){
        if(packetTemp == null){
            int length = ioArgs.readLength();
            packetTemp = new StringReceivePacket(length);
            packetChannel = Channels.newChannel(packetTemp.open());

            total = length;
            position = 0;
        }
        try {
            int count = ioArgs.writeTo(packetChannel);
                position += count;
                //检查是否完成一份packet的接收
                if (position == total) {
                    completePacket(true);
                }
        }catch (IOException e){
            e.printStackTrace();
            completePacket(false);
        }
    }

    //完成数据接收操作
    private void completePacket(boolean isSuccess) {
        ReceivePacket packet = this.packetTemp;
        CloseUtils.close(packet);
        packetTemp = null;

        WritableByteChannel channel = this.packetChannel;
        CloseUtils.close(channel);
        packetChannel = null;

        //通知外层已经接收到了一份packet
        if(packet != null) {
            callback.onReceivePacketCompleted(packet);
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        IoArgs args = this.ioArgs;
        int receiveSize;
        if(packetTemp == null){
            receiveSize = 4;
        }else{
            receiveSize = (int)Math.min(total - position, ioArgs.capacity());
        }
        //设置本次的数据大小
        ioArgs.limit(receiveSize);
        return args;
    }

    @Override
    public void onConsumeFailed(IoArgs ioArgs, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IoArgs ioArgs) {
        assemblePacket(ioArgs);
        registerReceive();
    }
}
