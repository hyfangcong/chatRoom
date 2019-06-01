package net.qiujuer.library.clink.frames;

import net.qiujuer.library.clink.core.Frame;
import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.SendPacket;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * @author: fangcong
 * @date: 2019/6/1
 */
public class SendEntityFrame extends AbsSendPacketFrame{
    private long unConsumeEntityLenght;
    private final ReadableByteChannel channel;
    public SendEntityFrame(short identifier, long entityLength,
                           ReadableByteChannel channel,
                           SendPacket sendPacket) {
        super((int)Math.min(entityLength, Frame.MAX_CAPACITY),
                Frame.TYPE_PACKET_ENTITY, Frame.FLAG_NONE, identifier, sendPacket);

        this.channel = channel;
        this.unConsumeEntityLenght = entityLength - bodyRemain;
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        if(sendPacket == null){
            //已终止当前帧，填充假数据
            return ioArgs.fillEmpty(bodyRemain);
        }
        return ioArgs.readFrom(channel);
    }

    @Override
    public Frame buildNextFrame() {
        if(unConsumeEntityLenght == 0){
            return null;
        }
        return new SendEntityFrame(getBodyIdentifier(), unConsumeEntityLenght, channel, sendPacket);
    }
}
