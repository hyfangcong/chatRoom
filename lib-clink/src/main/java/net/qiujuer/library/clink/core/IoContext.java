package net.qiujuer.library.clink.core;

import net.qiujuer.library.clink.impl.IoSelectorProvider;

import java.io.IOException;

/**
 * @author: fangcong
 * @date: 2019/5/26
 */
public class IoContext {
    //保证IoContext是单例模式，ioContext是io全局上下文
    //主要是保证IoProvider是全局唯一的。
    private final IoProvider ioProvider;

    private IoContext(IoProvider ioProvider){
        this.ioProvider = ioProvider;
    }
    public static IoContext get(){
        return InnerSingleton.instance;
    }

    public IoProvider getIoProvider() {
        return ioProvider;
    }

    public static void close() throws IOException {
        if(InnerSingleton.instance != null){
            InnerSingleton.instance.callClose();
        }
    }

    private void callClose() throws IOException{
        ioProvider.close();
    }

    private static class InnerSingleton{
        private static IoContext instance;

        static {
            try {
                instance = new IoContext(new IoSelectorProvider());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    //静态内部类实现单例模式， 这是错误的单例模式。无法做到单例
//    public static class StartBoot{
//        private IoProvider ioProvider;
//        public StartBoot(){
//
//        }
//        public StartBoot ioProvider(IoProvider ioProvider){
//            this.ioProvider = ioProvider;
//            return this;
//        }
//        public IoContext start(){
//            INSTANCE = new IoContext(ioProvider);
//            return INSTANCE;
//        }
//    }
}
