package net.qiujuer.library.clink.frames;

import net.qiujuer.library.clink.core.Frame;
import net.qiujuer.library.clink.core.IoArgs;

/**
 * @author: fangcong
 * @date: 2019/6/1
 */
public class ReceiveFrameFactory {
    public static AbsReceiveFrame createInstance(IoArgs ioArgs){
        byte[] buffer = new byte[Frame.FRAME_HEADER_LENGTH];
        ioArgs.writeTo(buffer, 0);
        byte type = buffer[2];
        switch (type){
            case Frame.TYPE_COMMAND_SEND_CANNEL:
                return new CancelReceiveFrame(buffer);
            case Frame.TYPE_PACKET_HEADER:
                return new ReceiveHeaderFrame(buffer);
            case Frame.TYPE_PACKET_ENTITY:
                return new ReceiveEntityFrame(buffer);
            default:
                throw  new UnsupportedOperationException("unSupported frame type: " + type);

        }
    }
}
