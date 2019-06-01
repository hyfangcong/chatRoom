package net.qiujuer.library.clink.box;

/**
 * @author: fangcong
 * @date: 2019/5/27
 */
public class StringSendPacket extends BytesSendPacket {
    /**
     * 字符串发送时就是Byte数组，所以直接得到Byte数组，并按照Byte的发送方式发送即可
     *
     * @param msg 字符串
     */
    public StringSendPacket(String msg) {
        super(msg.getBytes());
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }
}
