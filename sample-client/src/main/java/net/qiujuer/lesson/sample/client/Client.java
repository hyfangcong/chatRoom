package net.qiujuer.lesson.sample.client;


import net.qiujuer.lesson.sample.client.bean.ServerInfo;
import net.qiujuer.library.clink.core.IoContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client {
    public static void main(String[] args) throws IOException {
        IoContext context = IoContext.get();

        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + info);

        if (info != null) {
            try {
                TCPClient tcpClient = TCPClient.startWith(info);

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
                do{
                    String str = bufferedReader.readLine();
                    tcpClient.send(str);
//                    Thread.sleep(100);
                    tcpClient.send(str);
                    tcpClient.send(str);
                    tcpClient.send(str);
                }while (true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        IoContext.close();
    }

    private static void mutilClient(int number, ServerInfo info) throws IOException {
        for(int i = 0 ; i < number ; i++){
            TCPClient.startWith(info);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
