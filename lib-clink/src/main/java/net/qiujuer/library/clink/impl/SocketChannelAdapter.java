package net.qiujuer.library.clink.impl;

import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.IoProvider;
import net.qiujuer.library.clink.core.Receiver;
import net.qiujuer.library.clink.core.Sender;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: fangcong
 * @date: 2019/5/26
 */
public class SocketChannelAdapter implements Receiver, Sender, Closeable {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final SocketChannel socketChannel;
    private final IoProvider ioProvider;
    private final OnChannelStatusChangedListener onChannelStatusChangedListener;
    private IoArgs.IoArgsEventProcessor receiveIoEventProcessor;
    private IoArgs.IoArgsEventProcessor sendIoEventProcessor;

    public SocketChannelAdapter(SocketChannel socketChannel, IoProvider ioProvider,
                                OnChannelStatusChangedListener onChannelStatusChangedListener) throws IOException {
        this.socketChannel = socketChannel;
        this.ioProvider = ioProvider;
        this.onChannelStatusChangedListener = onChannelStatusChangedListener;
        socketChannel.configureBlocking(false);
    }

    @Override
    public void setReceiveListener(IoArgs.IoArgsEventProcessor processor) {
        this.receiveIoEventProcessor = processor;
    }

    @Override
    public boolean postReceiveAsync() throws IOException {
        if(isClosed.get()){
            throw new IOException("current channel is closed!");
        }
        return ioProvider.registerInput(socketChannel, inputCallback);
    }

    @Override
    public void setSendListener(IoArgs.IoArgsEventProcessor processor) {
        this.sendIoEventProcessor = processor;
    }

    @Override
    public boolean postSendAsync() throws IOException {
        if(isClosed.get()){
            throw new IOException("current channel is closed!");
        }
        return ioProvider.registerOutput(socketChannel, outputCallback);
    }

    @Override
    public void close() throws IOException {
        System.out.println("begin close");
        if(isClosed.compareAndSet(false, true)){
            System.out.println("close do");
            //解除注册回掉
            ioProvider.unRegisterInput(socketChannel);
            ioProvider.unRegisterOutput(socketChannel);
            //关闭
            CloseUtils.close(socketChannel);
            //回掉当前channel已关闭
            onChannelStatusChangedListener.onChannelClosed(socketChannel);
        }
    }

    public interface OnChannelStatusChangedListener{
        void onChannelClosed(SocketChannel socketChannel);
    }

    private final IoProvider.HandlerInputCallback inputCallback = new IoProvider.HandlerInputCallback() {
        @Override
        protected void canProvideInput () {
            if(isClosed.get()){
                return;
            }
            IoArgs.IoArgsEventProcessor processor = receiveIoEventProcessor;
            IoArgs ioArgs = processor.provideIoArgs();
            try{
                //具体的读取操作
                if(ioArgs.readFrom(socketChannel) > 0){
                    //读取完成后回调
                    processor.onConsumeCompleted(ioArgs);
                }else{
                    processor.onConsumeFailed(ioArgs, new IOException("cannot readFrom any data!"));
                }
            }
            catch (IOException e){
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    private final IoProvider.HandlerOutputCallback outputCallback = new IoProvider.HandlerOutputCallback() {
        @Override
        protected void canProvideOutput() {
            if(isClosed.get()){
                return;
            }
            IoArgs.IoArgsEventProcessor processor = sendIoEventProcessor;
            IoArgs ioArgs = processor.provideIoArgs();
            System.out.println(Thread.currentThread().getName() + "发送数据--ioargs hashcode --" + ioArgs.hashCode());
            try{
                //具体的写操作
                if(ioArgs.writeTo(socketChannel) > 0){
                    //完成后回调
                    processor.onConsumeCompleted(ioArgs);
                }else{
                    processor.onConsumeFailed(ioArgs, new IOException("cannot write any data!"));
                }
            }
            catch (IOException e){
                e.printStackTrace();
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };
}
