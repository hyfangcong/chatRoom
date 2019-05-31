package net.qiujuer.library.clink.core;

import net.qiujuer.library.clink.box.StringReceivePacket;
import net.qiujuer.library.clink.box.StringSendPacket;
import net.qiujuer.library.clink.impl.SocketChannelAdapter;
import net.qiujuer.library.clink.impl.async.AsyncReceiveDispatcher;
import net.qiujuer.library.clink.impl.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * @author: fangcong
 * @date: 2019/5/26
 */
public class Connector implements SocketChannelAdapter.OnChannelStatusChangedListener, Closeable {
    private UUID key = UUID.randomUUID();
    private SocketChannel socketChannel;
    private Sender sender;
    private Receiver receiver;
    private SendDispatcher sendDispatcher;
    private ReceiveDispatcher receiveDispatcher;

    public void setup(SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        //IoContext是单例模式
        SocketChannelAdapter socketChannelAdapter = new SocketChannelAdapter(socketChannel,IoContext.get().getIoProvider(), this);
        this.sender = socketChannelAdapter;
        this.receiver = socketChannelAdapter;
        this.sendDispatcher = new AsyncSendDispatcher(sender);
        this.receiveDispatcher = new AsyncReceiveDispatcher(receiver, receivePacketCallback);

        //启动接收
        receiveDispatcher.start();
    }

    public void send(String msg) {
        SendPacket packet = new StringSendPacket(msg);
        sendDispatcher.send(packet);
    }

    @Override
    public void onChannelClosed(SocketChannel socketChannel) {

    }
    protected void onReceiveNewMessage(String str){
        System.out.println(key.toString() + ":" + str);
    }

    @Override
    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        sender.close();
        receiver.close();
        socketChannel.close();
    }

    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
            if(packet instanceof StringReceivePacket){
                String msg = ((StringReceivePacket) packet).string();
                onReceiveNewMessage(msg);
            }
        }
    };

}
