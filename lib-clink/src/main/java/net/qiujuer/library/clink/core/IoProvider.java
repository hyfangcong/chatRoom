package net.qiujuer.library.clink.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

/**
 * @author: fangcong
 * @date: 2019/5/26
 */
public interface IoProvider extends Closeable {
    boolean registerInput(SocketChannel socketChannel, HandlerInputCallback handlerInputCallback);
    boolean registerOutput(SocketChannel socketChannel, HandlerOutputCallback handlerOutputCallback);
    void unRegisterInput(SocketChannel socketChannel);
    void unRegisterOutput(SocketChannel socketChannel);

    abstract class HandlerInputCallback implements Runnable{
        @Override
        public void run(){
            canProvideInput();
        }
        protected abstract void canProvideInput();
    }

    abstract class HandlerOutputCallback implements Runnable{
        @Override
        public void run(){
            canProvideOutput();
        }
        protected abstract void canProvideOutput();
    }
}
