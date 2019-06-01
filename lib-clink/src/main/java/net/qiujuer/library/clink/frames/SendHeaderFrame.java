package net.qiujuer.library.clink.frames;

import net.qiujuer.library.clink.core.Frame;
import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.SendPacket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * @author: fangcong
 * @date: 2019/6/1
 */
public  class SendHeaderFrame extends AbsSendPacketFrame{
    /**
     * 一个packet的首帧，至少包含6个字节
     *
     * 前5个字节表示packet的字节数
     *
     * 第6个字节是packet的唯一标识identifier。用于接收方将帧组装成一个packet
     */
    static final int PACKET_HEADER_FRAME_MIN_LENGTH = 6;
    private final  byte[] body;

    public SendHeaderFrame(short identifier, SendPacket sendPacket) {
        super(PACKET_HEADER_FRAME_MIN_LENGTH,
                Frame.TYPE_PACKET_HEADER,
                Frame.FLAG_NONE, identifier, sendPacket);

        final long packetLength = sendPacket.length();
        final byte packetType = sendPacket.type();
        final byte[] packetHeaderInfo = sendPacket.headerInfo();

        body = new byte[headerRemaining];

        body[0] = (byte) (packetLength >> 32);
        body[1] = (byte) (packetLength >> 24);
        body[2] = (byte) (packetLength >> 16);
        body[3] = (byte) (packetLength >> 8);
        body[4] = (byte) packetLength;

        body[5] = packetType;

        if(packetHeaderInfo != null){
            System.arraycopy(packetHeaderInfo, 0,
                    body, PACKET_HEADER_FRAME_MIN_LENGTH, packetHeaderInfo.length);
        }
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        int count = bodyRemain;
        int offset = body.length - count;
        return ioArgs.readFrom(body, offset, count);
    }

    @Override
    public Frame buildNextFrame() {
        InputStream stream = sendPacket.open();
        ReadableByteChannel channel = Channels.newChannel(stream);
        return new SendEntityFrame(getBodyIdentifier(), sendPacket.length(),channel, sendPacket);
    }
}
