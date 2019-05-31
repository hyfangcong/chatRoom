package net.qiujuer.library.clink.core;

/**
 * @author: fangcong
 * @date: 2019/5/27
 */

import java.io.Closeable;

/**
 * 公共类型的封装
 * 提供了类型及基本的长度定义
 */
public abstract class Packet<T> implements Closeable {
    protected  byte type;
    protected long length;

    public byte type(){
        return this.type;
    }

    public long length(){
        return this.length;
    }

    public abstract T open();
}
