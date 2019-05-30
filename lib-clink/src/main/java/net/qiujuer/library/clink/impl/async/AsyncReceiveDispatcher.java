package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.box.StringReceivePacket;
import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.ReceiveDispatcher;
import net.qiujuer.library.clink.core.ReceivePacket;
import net.qiujuer.library.clink.core.Receiver;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: fangcong
 * @date: 2019/5/28
 */
public class AsyncReceiveDispatcher implements ReceiveDispatcher {
    private final AtomicBoolean isClosed = new AtomicBoolean();

    private final Receiver receiver;
    private final ReceivePacketCallback callback;

    private IoArgs ioArgs = new IoArgs();
    private ReceivePacket packetTemp;
    private byte[] buffer;
    private int total;
    private int position;

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback callback){
        this.receiver = receiver;
        receiver.setReceiveListener(listener);
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
            ReceivePacket packet = packetTemp;
            if(packet != null){
                packetTemp = null;
                CloseUtils.close(packet);
            }
        }
    }

    private void registerReceive() {
        try {
            receiver.receiveAsync(ioArgs);
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
            buffer = new byte[length];
            total = length;
            position = 0;
        }
        int count = ioArgs.writeTo(buffer, position);
        if(count > 0){
            packetTemp.save(buffer, count);
            position += count;
            //检查是否完成一份packet的接收
            if(position == total){
                completePacket();
                packetTemp = null;
            }
        }
    }

    private void completePacket() {
        ReceivePacket packet = this.packetTemp;
        CloseUtils.close(packet);
        //通知外层已经接收到了一份packet
        callback.onReceivePacketCompleted(packet);
    }

    private final IoArgs.IoArgsListener listener = new IoArgs.IoArgsListener() {
        @Override
        public void onStart(IoArgs ioArgs) {
            int receiveSize;
            if(packetTemp == null){
                receiveSize = 4;
            }else{
                receiveSize = Math.min(total - position, ioArgs.capacity());
            }

            //设置本次的数据大小
            ioArgs.limit(receiveSize);
        }

        @Override
        public void onComplement(IoArgs ioArgs) {
            assemblePacket(ioArgs);
            //继续接收下一条消息
            System.out.println("重新注册reader");
            registerReceive();
        }
    };

}
