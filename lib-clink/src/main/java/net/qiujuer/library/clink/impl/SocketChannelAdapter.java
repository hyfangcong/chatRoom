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
    private IoArgs.IoArgsListener receiveIoEventListener;
    private IoArgs.IoArgsListener sendIoEventListener;
    private IoArgs receiveIoArgsTemp;

    public SocketChannelAdapter(SocketChannel socketChannel, IoProvider ioProvider,
                                OnChannelStatusChangedListener onChannelStatusChangedListener) throws IOException {
        this.socketChannel = socketChannel;
        this.ioProvider = ioProvider;
        this.onChannelStatusChangedListener = onChannelStatusChangedListener;
        socketChannel.configureBlocking(false);
    }

    @Override
    public boolean sendAsync(IoArgs ioArgs, IoArgs.IoArgsListener listener) throws IOException {
        if(isClosed.get()){
            throw new IOException("current channel is closed!");
        }
        this.sendIoEventListener = listener;
        //当前的数据附加到回调函数中
        outputCallback.setAttach(ioArgs);
        return ioProvider.registerOutput(socketChannel, outputCallback);
    }

    @Override
    public void setReceiveListener(IoArgs.IoArgsListener listener) {
        this.receiveIoEventListener = listener;
    }

    @Override
    public boolean receiveAsync(IoArgs ioArgs) throws IOException {
        if(isClosed.get()){
            throw new IOException("current channel is closed!");
        }
        this.receiveIoArgsTemp = ioArgs;
        return ioProvider.registerInput(socketChannel, inputCallback);
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
            IoArgs ioArgs = receiveIoArgsTemp;
            IoArgs.IoArgsListener listener = SocketChannelAdapter.this.receiveIoEventListener;
            listener.onStart(ioArgs);
            try{
                //具体的读取操作
                int len;
                if(listener != null &&  (len = ioArgs.readFrom(socketChannel)) > 0){
                    //读取完成后回调
                    System.out.println(Thread.currentThread().getName() + "从channel中读取数据， len：" + len);
                    listener.onComplement(ioArgs);
                }else{
                    throw new IOException("cannot readFrom any data!");
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
            IoArgs.IoArgsListener listener = sendIoEventListener;
            IoArgs ioArgs = getAttach();
            System.out.println(Thread.currentThread().getName() + "write to channel attach hashcode ---" + ioArgs.hashCode()) ;
            try{
                //具体的写操作
                if(listener != null){
                    int len = ioArgs.writeTo(socketChannel);
                    System.out.println(Thread.currentThread().getName() + " 写数据到channel--len:" + len );
                    //完成后回调
                    listener.onComplement(ioArgs);
                }else{
                    System.out.println("listener is null");
                    throw new IOException("cannot write any data!");
                }
            }
            catch (IOException e){
                e.printStackTrace();
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };
}
