package net.qiujuer.library.clink.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 * @author: fangcong
 * @date: 2019/5/26
 */
public class IoArgs {
    private int limit = 512;
    private ByteBuffer buffer = ByteBuffer.allocate(512);

    /**
     * 从bytes数组中进行消费
     */
    public int readFrom(byte[] bytes, int offset, int count) {
        int size = Math.min(count, buffer.remaining());
        if(size <= 0){
            return 0;
        }

        buffer.put(bytes, offset, count);
        return size;
    }

    /**
     * 写数据到bytes数组中
     */
    public int writeTo(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
    }

    /**
     * 从channel中读取数据到buffer
     * @return
     */
    public int readFrom(ReadableByteChannel channel) throws IOException {
        int bytesProduced = 0;
        while(buffer.hasRemaining()){
            int len = channel.read(buffer);
            if(len < 0)
                throw new EOFException();
            else if(len == 0)
                //channel中的数据已经读完了
                break;
            bytesProduced += len;
        }
        return bytesProduced;
    }

    /**
     * 从buffer中写数据到channel中
     * @return
     */
    public int writeTo(WritableByteChannel channel) throws IOException {
        int bytesProduced = 0;
        while(buffer.hasRemaining()){
            int len = channel.write(buffer);
            if(len < 0)
                throw new EOFException();
            bytesProduced += len;
        }
        return bytesProduced;
    }

    /**
     * 从socketChannel中读取数据
     * @param socketChannel
     * @return
     * @throws IOException
     */
    public int readFrom(SocketChannel socketChannel) throws IOException {
        startWriting();
       int bytesProduced = 0;
       while(buffer.hasRemaining()){
           int len = socketChannel.read(buffer);
           if(len < 0)
               throw new EOFException();
           else if(len == 0)
               //channel中的数据已经读完了
               break;
           bytesProduced += len;
       }
       finishedWrite();
       return bytesProduced;
    }

    /**
     * 写数据到socketChannel
     * @param socketChannel
     * @return
     * @throws IOException
     */
    public int writeTo(SocketChannel socketChannel) throws IOException {
        int bytesProduced = 0;
        while(buffer.hasRemaining()){
            int len = socketChannel.write(buffer);
            if(len < 0)
                throw new EOFException();
            bytesProduced += len;
        }
        return bytesProduced;
    }

    public int capacity() {
        return buffer.capacity();
    }

    public void startWriting(){
        buffer.clear();
        //定义单次写操作容纳区间
        buffer.limit(limit);
    }

    public int readLength(){
        return buffer.getInt();
    }

    public void finishedWrite(){
        buffer.flip();
    }

    public void limit(int limit){
        this.limit = Math.min(limit, capacity());
    }

    public boolean remained() {
        return buffer.remaining() > 0;
    }

    /**
     * 填充空数据
     * @param size
     * @return
     */
    public int fillEmpty(int size) {
        int bufferSize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + bufferSize);
        return bufferSize;
    }

    /**
     * 清空数据
     * @param size
     * @return
     */
    public int setEmpty(int size) {
        int emptySize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + emptySize);
        return emptySize;
    }

    /**
     * IoArgs 提供者、处理者；数据的生产者、消费者
     */
    public interface IoArgsEventProcessor{
        /**
         * 提供一份可消费的IoArgs
         *
         * @return IoArgs
         */
        IoArgs provideIoArgs();

        /**
         * 消费失败时返回
         * @param ioArgs  IoArgs
         * @param e  异常信息
         */
        void onConsumeFailed(IoArgs ioArgs, Exception e);

        /**
         * 消费成功
         * @param ioArgs
         */
        void onConsumeCompleted(IoArgs ioArgs);
    }
}
