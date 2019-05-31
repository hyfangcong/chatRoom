package net.qiujuer.library.clink.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author: fangcong
 * @date: 2019/5/26
 */
public class IoArgs {
    private int limit = 10;
    private byte[] byteBuffer = new byte[10];
    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    /**
     * 从bytes中读取数据到buffer
     * @param bytes
     * @param offset
     * @return
     */
    public int readFrom(byte[] bytes, int offset){
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.put(bytes, offset, size);
        return size;
    }

    /**
     * 从buffer中写数据到bytes
     * @param bytes
     * @param offset
     * @return
     */
    public int writeTo(byte[] bytes, int offset){
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset ,size);
        return size;
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

    public interface IoArgsListener {
        void onStart(IoArgs ioArgs);

        void onComplement(IoArgs ioArgs);
    }

    public void startWriting(){
        buffer.clear();
        //定义单次写操作容纳区间
        buffer.limit(limit);
    }

    public void writeLength(int total){
        buffer.putInt(total);
    }
    public int readLength(){
        return buffer.getInt();
    }

    public void finishedWrite(){
        buffer.flip();
    }

    public void limit(int limit){
        this.limit = limit;
    }
}
