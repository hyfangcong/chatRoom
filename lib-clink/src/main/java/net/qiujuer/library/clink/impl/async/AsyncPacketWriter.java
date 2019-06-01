package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.core.Frame;
import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.Packet;
import net.qiujuer.library.clink.core.ReceivePacket;
import net.qiujuer.library.clink.frames.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.HashMap;

/**
 * @author: fangcong
 * @date: 2019/6/1
 */
public class AsyncPacketWriter implements Closeable {
    private final PacketProvider provider;

    private final HashMap<Short, PacketModel> packetMap = new HashMap();
    private final IoArgs ioArgs = new IoArgs();
    private volatile Frame frameTemp;

    public AsyncPacketWriter(PacketProvider provider) {
        this.provider = provider;
    }

    /**
     * 构建一份数据容纳封装
     * 当前帧如果没有返回，则至少返回6字节的IoArgs
     * 如果当前帧有，返回当前帧为消费的区间
     */
    synchronized IoArgs takeIoArgs() {
        ioArgs.limit(frameTemp == null ? Frame.FRAME_HEADER_LENGTH
            :frameTemp.getConsumeLength()
        );
        return ioArgs;
    }


    /**
     * 消费IoArgs中的数据
     *
     * @param ioArgs IoArgs
     */
    synchronized void consumeIoArgs(IoArgs ioArgs) {
        if(frameTemp == null){
            Frame temp;
            do{
                temp = buildNewFrame(ioArgs);
            }while(temp == null && ioArgs.remained());

            if(temp == null){
                return;
            }
            frameTemp = temp;
            if(!ioArgs.remained()){
                return;
            }
        }

        Frame currentFrame = frameTemp;
        do{
            try {
                if(currentFrame.handle(ioArgs)){
                    if(currentFrame instanceof ReceiveHeaderFrame){
                        ReceiveHeaderFrame headerFrame = (ReceiveHeaderFrame) currentFrame;
                        ReceivePacket packet = provider.takePacket(headerFrame.getPacketType(),
                                headerFrame.getPacketLength(), headerFrame.getPacketHeaderInfo());
                        appendNewPacket(headerFrame.getBodyIdentifier(), packet);

                    }else if(currentFrame instanceof ReceiveEntityFrame){
                        completeEntityFrame((ReceiveEntityFrame) currentFrame);
                    }
                    frameTemp = null;
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }while(ioArgs.remained());
    }

    private void completeEntityFrame(ReceiveEntityFrame frame) {
        synchronized (packetMap) {
            short identifier = frame.getBodyIdentifier();
            int length = frame.getBodyLength();
            PacketModel packetModel = packetMap.get(identifier);
            packetModel.unReceivedLength -= length;
            if (packetModel.unReceivedLength <= 0) {
                provider.completedPacket(packetModel.receivePacket, true);
                packetMap.remove(identifier);
            }
        }
    }

    private void appendNewPacket(short bodyIdentifier, ReceivePacket packet) {
        synchronized (packetMap) {
            PacketModel packetModel = new PacketModel(packet);
            packetMap.put(bodyIdentifier, packetModel);
        }
    }

    private Frame buildNewFrame(IoArgs ioArgs) {
        AbsReceiveFrame frame = ReceiveFrameFactory.createInstance(ioArgs);
        if(frame instanceof CancelReceiveFrame){
            cancelReceivePacket(frame.getBodyIdentifier());
            return null;
        }else if(frame instanceof ReceiveEntityFrame){
            WritableByteChannel channel = getPacketChannel(frame.getBodyIdentifier());
            ((ReceiveEntityFrame) frame).bindPacketChannel(channel);
        }
        return frame;
    }

    private WritableByteChannel getPacketChannel(short bodyIdentifier) {
        synchronized (packetMap) {
            PacketModel packetModel = packetMap.get(bodyIdentifier);
            return packetModel == null ? null : packetModel.channel;
        }
    }

    private void cancelReceivePacket(short bodyIdentifier) {
        synchronized (packetMap) {
            PacketModel packetModel = packetMap.get(bodyIdentifier);
            if (packetModel != null) {
                ReceivePacket packet = packetModel.receivePacket;
                provider.completedPacket(packet, false);
            }
        }
    }

    /**
     * 关闭操作，如果当前还有正在接收的packet，则尝试停止对应的packet的接收
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        synchronized (packetMap) {
            Collection<PacketModel> values = packetMap.values();
            for (PacketModel model : values) {
                provider.completedPacket(model.receivePacket, false);
            }
            packetMap.clear();
        }
    }

    /**
     * packet提供者
     */
    interface PacketProvider{

        /**
         * 拿Packet操作
         *
         * @param type       Packet类型
         * @param length     Packet长度
         * @param headerInfo Packet headerInfo
         * @return 通过类型，长度，描述等信息得到一份接收Packet
         */

        ReceivePacket takePacket(byte type, long length, byte[] headerInfo);

        void completedPacket(ReceivePacket packet, boolean isSuccess);
    }

    static class PacketModel{
        final ReceivePacket receivePacket;
        final WritableByteChannel channel;
        volatile long unReceivedLength;

        public PacketModel(ReceivePacket<?,?> receivePacket) {
            this.receivePacket = receivePacket;
            this.channel = Channels.newChannel(receivePacket.open());
            this.unReceivedLength = receivePacket.length();
        }
    }
}
