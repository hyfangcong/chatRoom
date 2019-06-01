package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.core.Frame;
import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.SendPacket;
import net.qiujuer.library.clink.core.ds.BytePriorityNode;
import net.qiujuer.library.clink.frames.*;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author: fangcong
 * @date: 2019/6/1
 */
public class AsyncPacketReader implements Closeable {
    private final PacketProvider provider;
    private volatile IoArgs ioArgs = new IoArgs();

    //frame队列
    private volatile BytePriorityNode<Frame> node;
    private volatile int nodeSize;

    //帧标识1 ... 255
    private volatile short identifier;

    public AsyncPacketReader(PacketProvider provider) {
        this.provider = provider;
    }

    /**
     * 请求从packetProvider中拿出一个packet进行发送
     *
     * @return 如果当前reader中有可以用于网络发送的数据，则返回true
     */
    public boolean requestTakePacket() {
        synchronized (this) {
            //这里可以控制并发量
            if (nodeSize >= 1) {
                return true;
            }
        }
        SendPacket packet = provider.takePacket();
        if(packet != null){
            short identifier = generatorIdentifier();
            SendHeaderFrame frame = new SendHeaderFrame(identifier, packet);
            appendNewFrame(frame);
        }

        synchronized (this){
            return nodeSize != 0;
        }
    }

    /**
     * 填充数据到IoArgs中
     *
     * @return 如果当前有可用于发送的帧，填充数据返回， 如果填充失败则返回null
     */
    public IoArgs fillData() {
        Frame currentFrame = getCurrentFrame();
        if(currentFrame == null){
            return null;
        }

        try{
            if(currentFrame.handle(ioArgs)){
                //消费完本帧
                //尝试基于本帧构建后续帧
                Frame nextFrame = currentFrame.nextFrame();
                if(nextFrame != null){
                    appendNewFrame(nextFrame);
                }else if(currentFrame instanceof SendEntityFrame){
                    //末尾为实体帧
                    //通知完成
                    provider.completedPacket(((SendEntityFrame)currentFrame).getPacket(), true);
                }

                //从链头弹出
                popCurrentFrame();
            }
            return ioArgs;
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 取消packet对应帧的发送，如果当前packet已经发送部分数据（就算只是头帧）
     * 也应该在帧队列中添加一份终止帧{@link CancelSendFrame}
     *
     * @param packet 带取消的packet
     */
    public synchronized void cancel(SendPacket packet) {
            if(nodeSize == 0){
                return;
            }

            for(BytePriorityNode<Frame> x = node, before = null; x != null; before = x, x = x.next){
                Frame frame = x.item;
                if(frame instanceof AbsSendPacketFrame){
                    AbsSendPacketFrame packetFrame = (AbsSendPacketFrame) frame;
                    if(packetFrame.getPacket() == packet){
                        boolean removable = packetFrame.abort();
                        if(removable){
                            removeFrame(x, before);
                            if(packetFrame instanceof SendHeaderFrame){
                                //头帧，并且未被发送任何数据，直接取消后不需要发送取消发送帧
                                break;
                            }
                        }

                        //添加终止帧，通知接收方
                        CancelSendFrame cancelSendFrame = new CancelSendFrame(packetFrame.getBodyIdentifier());
                        appendNewFrame(cancelSendFrame);

                        //意外终止，失败返回
                        provider.completedPacket(packet, false);
                        break;
                    }
                }
            }
    }

    /**
     * 关闭当前Reader，关闭时应关闭所有Frame对应的packet
     * @throws IOException 关闭时出现异常
     */
    @Override
    public synchronized void close(){
        while(node != null){
            Frame frame = node.item;
            if(frame instanceof AbsSendPacketFrame){
                SendPacket packet = ((AbsSendPacketFrame) frame).getPacket();
                provider.completedPacket(packet, false);
            }
            node = node.next;
        }
        nodeSize = 0;
        node = null;
    }

    private synchronized void appendNewFrame(Frame frame) {
        BytePriorityNode<Frame> newNode = new BytePriorityNode<>(frame);
        if(node != null){
            //带优先级的添加
            node.appendWithPriority(newNode);
        }else{
            node = newNode;
        }
        nodeSize++;
    }

    private synchronized Frame getCurrentFrame() {
        if(node != null){
            return node.item;
        }else
            return null;
    }

    private synchronized void popCurrentFrame() {
        node = node.next;
        nodeSize--;
        if(node == null){
            requestTakePacket();
        }
    }

    private synchronized void removeFrame(BytePriorityNode<Frame> removeNode, BytePriorityNode<Frame> before) {
        if(before == null){
            //removeNode是头节点
            node = removeNode.next;
        }else{
            before.next = removeNode.next;
            removeNode = null;
        }
        nodeSize--;
        if(node == null){
            requestTakePacket();
        }
    }



    public short generatorIdentifier(){
        short identifier = ++this.identifier;
        if(identifier == 255){
            this.identifier = 0;
        }
        return identifier;
    }

    interface PacketProvider{
        SendPacket takePacket();

        void completedPacket(SendPacket packet, boolean isSuccess);
    }
}
