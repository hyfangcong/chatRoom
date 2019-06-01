package net.qiujuer.library.clink.core;

import net.qiujuer.library.clink.box.*;
import net.qiujuer.library.clink.impl.SocketChannelAdapter;
import net.qiujuer.library.clink.impl.async.AsyncReceiveDispatcher;
import net.qiujuer.library.clink.impl.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * @author: fangcong
 * @date: 2019/5/26
 */
public abstract class Connector implements SocketChannelAdapter.OnChannelStatusChangedListener, Closeable {
    protected UUID key = UUID.randomUUID();
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

    public void send(FileSendPacket packet) {
        sendDispatcher.send(packet);
    }

    @Override
    public void onChannelClosed(SocketChannel socketChannel) {

    }

    @Override
    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        sender.close();
        receiver.close();
        socketChannel.close();
    }

    protected void onReceivePacket(ReceivePacket packet){
        System.out.println(key.toString() + ":[New Packet]-Type:" + packet.type() + ", length:" + packet.length);
    }

    protected abstract File createNewReceiveFile();

    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length) {
           switch (type){
               case Packet.TYPE_MEMORY_BYTES:
                   return new BytesReceivePacket(length);
               case Packet.TYPE_MEMORY_STRING:
                   return new StringReceivePacket(length);
               case Packet.TYPE_STREAM_FILE:
                   return new FileReceivePacket(length, createNewReceiveFile());
               case Packet.TYPE_STREAM_DIRECT:
                   return new BytesReceivePacket(length);
               default:
                   throw new UnsupportedOperationException("Unsupported packet type:" + type);
           }
        }

        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
           onReceivePacket(packet);
        }
    };
}
