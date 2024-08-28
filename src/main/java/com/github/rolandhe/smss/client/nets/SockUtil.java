package com.github.rolandhe.smss.client.nets;

import java.io.IOException;
import java.net.Socket;

public class SockUtil {
    public static void writeAll(Socket sock, byte[] content) throws IOException {
        sock.getOutputStream().write(content);
        sock.getOutputStream().flush();
    }

    public static boolean readAll(Socket sock, byte[] buf) throws IOException{
        int offset = 0;
        int needLen = buf.length;
        while(needLen > 0){
            int n = sock.getInputStream().read(buf,offset,needLen);
            if(n == -1){
                return false;
            }
            offset += n;
            needLen -= n;
        }
        return true;
    }
}
