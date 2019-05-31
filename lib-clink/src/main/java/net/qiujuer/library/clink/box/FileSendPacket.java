package net.qiujuer.library.clink.box;

import net.qiujuer.library.clink.core.SendPacket;

import java.io.*;

/**
 * @author: fangcong
 * @date: 2019/5/27
 */
public class FileSendPacket extends SendPacket<FileInputStream> {
    public FileSendPacket(File file) {
        this.length = file.length();
    }

    @Override
    protected FileInputStream createStream() {
        return null;
    }
}
