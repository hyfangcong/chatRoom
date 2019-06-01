package net.qiujuer.library.clink.core;

import java.io.IOException;

/**
 * @author: fangcong
 * @date: 2019/6/1
 */
public abstract class Frame {
    /**
     * 帧头部固定6个字节
     */
    public static final int FRAME_HEADER_LENGTH  =6;
    /**
     * 单帧最大容量 64KB
     */
    public static final int MAX_CAPACITY = 64 * 1024 - 1;

    // Packet头信息帧
    public static final byte TYPE_PACKET_HEADER = 11;
    // Packet数据分片信息帧
    public static final byte TYPE_PACKET_ENTITY = 12;
    // 指令-发送取消
    public static final byte TYPE_COMMAND_SEND_CANNEL = 41;
    // 指令-接受拒绝
    public static final byte TYPE_COMMAND_RECEIVE_REJECT = 42;

    // Flag标记
    public static final byte FLAG_NONE = 0;

    // 头部6字节固定
    protected final byte[] header = new byte[FRAME_HEADER_LENGTH];

    public Frame(int length, byte type, byte flag, short identifier) {
        if(length < 0 || length > MAX_CAPACITY){
            throw new RuntimeException("The Body length of a single frame should be between 0 and " + MAX_CAPACITY);
        }

        if(identifier < 1 || identifier > 255){
            throw new RuntimeException("The Body identifier of a single frame should be between 1 and 255");
        }

        header[0] = (byte) (length >> 8);
        header[1] = (byte) length;

        header[2] = type;
        header[3] = flag;

        header[4] =(byte) identifier;
        header[5] = 0;
    }

    public Frame(byte[] header) {
        System.arraycopy(header, 0, this.header, 0, FRAME_HEADER_LENGTH);
    }

    /**
     * 获取数据区的长度
     * @return length
     */
    public int getBodyLength(){
        //byte转换成int时高位会补上1
        return (((int)header[0] & 0xff) << 8) | (((int)header[1]) & 0xff);
    }

    /**
     * 获取帧类型
     * @return
     */
    public byte getBodyType(){
        return header[2];
    }

    /**
     * 获取帧flag
     * @return
     */
    public byte getBodyFlag(){
        return header[3];
    }

    /**
     * 获取帧的标识【1 - 255】
     * @return
     */
    public short getBodyIdentifier(){
        return (short) ((short) header[4] & 0xff);
    }

    /**
     * 进行数据读或写操作
     *
     * @param ioArgs 数据
     * @return 是否已消费完全， True：则无需再传递数据到Frame或从当前Frame读取数据
     */
    public abstract boolean handle(IoArgs ioArgs) throws IOException;

    /**
     * 基于当前帧尝试构建下一份待消费的帧
     *
     * @return NULL：没有待消费的帧
     */
    public abstract Frame nextFrame();


    public abstract int getConsumeLength();
}
