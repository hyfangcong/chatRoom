package net.qiujuer.lesson.sample.foo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author: fangcong
 * @date: 2019/5/27
 */
public class Test {
    public static void main(String[] args) {
//        ConcurrentHashMap<Integer,Integer> conMap = new ConcurrentHashMap<>();
        Map<Integer, Integer> conMap = new HashMap<>();
        for(int i = 1; i < 1000000 ; i ++){
            conMap.put(i,i);
        }

//        Thread thread1 = new Thread(() -> {
//            while(true) {
//                for (Map.Entry<Integer, Integer> entry : conMap.entrySet()) {
//                    entry.getKey();
//                }
//            }
//        });
//        thread1.start();
//
//        Thread thread2 = new Thread( () ->{
//            for (Map.Entry<Integer, Integer> entry : conMap.entrySet()) {
//                conMap.remove(entry.getKey());
//            }
//        });
//        thread2.start();

        Thread thread3 = new Thread( () ->{
//            for (Map.Entry<Integer, Integer> entry : conMap.entrySet()) {
//                conMap.remove(entry.getKey());
//            }
            Iterator<Map.Entry<Integer,Integer>> iterator = conMap.entrySet().iterator();
            while(iterator.hasNext()){
                iterator.next();
                iterator.remove();
            }
        });
        thread3.start();
    }
}
