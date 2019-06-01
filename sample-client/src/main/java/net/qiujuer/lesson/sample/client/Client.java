package net.qiujuer.lesson.sample.client;


import net.qiujuer.lesson.sample.client.bean.ServerInfo;
import net.qiujuer.lesson.sample.foo.Foo;
import net.qiujuer.library.clink.box.FileSendPacket;
import net.qiujuer.library.clink.core.IoContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client {
    public static void main(String[] args) throws IOException {
        File cachePath = Foo.getCacheDir("client");
        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + info);

        if (info != null) {
            try {
                TCPClient tcpClient = TCPClient.startWith(info, cachePath);

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
                do{
                    String str = bufferedReader.readLine();
                    if("00bey00".equals(str)){
                        break;
                    }

                    //--f url表示发送文件
                    if(str.startsWith("--f")){
                        String[] array = str.split(" ");
                        if(array.length >= 2){
                            String filePath = array[1];
                            File file = new File(filePath);
                            if(file.exists() && file.isFile()){
                                FileSendPacket packet = new FileSendPacket(file);
                                tcpClient.send(packet);
                                continue;
                            }
                        }
                    }
                    //发送字符串
                    tcpClient.send(str);
                }while (true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        IoContext.close();
    }

//    private static void mutilClient(int number, ServerInfo info) throws IOException {
//        for(int i = 0 ; i < number ; i++){
//            TCPClient.startWith(info, );
//            try {
//                Thread.sleep(20);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
