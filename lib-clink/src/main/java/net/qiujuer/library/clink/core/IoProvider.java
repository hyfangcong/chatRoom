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
        //缓冲区的数据写到attach中，等待网卡空闲的时候发送
        private Object attach;
        @Override
        public void run(){
            canProvideOutput();
        }
        public void setAttach(Object attach) {
            this.attach = attach;
        }

        public final <T> T getAttach(){
            T attach = (T) this.attach;
            return attach;
        }
        protected abstract void canProvideOutput();
    }
}
