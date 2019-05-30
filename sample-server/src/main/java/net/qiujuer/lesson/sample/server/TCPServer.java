package net.qiujuer.lesson.sample.server;

import net.qiujuer.lesson.sample.server.handle.ClientHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandler.ClientHandlerCallback {
    private final int port;
    private ClientListener listener;
    private List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final ExecutorService forwardingThreadPoolExecutor;
    private Selector selector;
    private  String serverInfo;

    public TCPServer(int port) {
        this.port = port;
        forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            selector = Selector.open();
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            //设置为非阻塞
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            //注册serverSocketChannel到selector
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
          //  this.serverSocketChannel = serverSocketChannel;
            this.serverInfo = serverSocketChannel.getLocalAddress().toString();
            System.out.println("服务器信息：" + serverInfo);
            //启动客户端监听
            ClientListener clientListener = this.listener = new ClientListener();
            clientListener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public synchronized void stop() {
        if (listener != null) {
            listener.exit();
        }

        for (ClientHandler clientHandler : clientHandlerList) {
            clientHandler.exit();
        }

        clientHandlerList.clear();
    }

    public void broadcast(String str) {
        synchronized (TCPServer.this) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.send(str);
            }
        }
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler handler) {
        clientHandlerList.remove(handler);
    }

    @Override
    public void onNewMessageArrived(ClientHandler handler, String msg) {
        synchronized (TCPServer.this){
            for(ClientHandler it : clientHandlerList){
                if(it.equals(handler)) continue;
                //给其他客户端发送信息
                forwardingThreadPoolExecutor.execute(() -> {
                    it.send(msg);
                });
            }
        }
    }

    private class ClientListener extends Thread {
        private boolean done = false;
        @Override
        public void run() {
            super.run();
            Selector selector = TCPServer.this.selector;
            System.out.println("服务器准备就绪～");
            // 等待客户端连接
            do {
                // 得到客户端
                SocketChannel client = null;
                try {
                    if(selector.select() > 0){
                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        Iterator<SelectionKey> iterator = selectionKeys.iterator();
                        while(iterator.hasNext()){
                            SelectionKey key = iterator.next();
                            iterator.remove();
                            ServerSocketChannel server = (ServerSocketChannel) key.channel();
                            client = server.accept();
                        }
                    }
                } catch (IOException e) {
                    continue;
                }
                try {
                    // 客户端构建异步线程
                    ClientHandler clientHandler = new ClientHandler(client, TCPServer.this);
                    synchronized (TCPServer.this) {
                        clientHandlerList.add(clientHandler);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("客户端连接异常：" + e.getMessage());
                }
            } while (!done);

            System.out.println("服务器已关闭！");
        }

        void exit() {
            done = true;
            //唤醒当前阻塞
            selector.wakeup();
        }
    }
}
