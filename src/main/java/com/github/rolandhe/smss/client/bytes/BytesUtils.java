package com.github.rolandhe.smss.client.bytes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class BytesUtils {
    public static byte[] toVarString(byte[] lenBuf, String v){
        if(v == null || v.isEmpty()){
            writeIntLittle(lenBuf,0);
            return null;
        }
        byte[] u8Buff = v.getBytes(StandardCharsets.UTF_8);
        writeIntLittle(lenBuf,u8Buff.length);
        return u8Buff;
    }

    public static void writeIntLittle(byte[] buf,int value) {
        ByteBuffer bbuf = ByteBuffer.wrap(buf,0,4);
        bbuf.order(ByteOrder.LITTLE_ENDIAN);
        bbuf.putInt(value);
    }

    public static void putShortLittle(byte[] buf,int offset, int value){
        buf[offset] = (byte)(0xFF & value);
        buf[offset+1] = (byte)(0xFF & (value>>8));
    }

    public static short getShortLittle(byte[] buf){
       return getShortLittle(buf,0);
    }

    public static short getShortLittle(byte[] buf,int offset){
        int lv = buf[offset]&0xFF;
        int hv = buf[offset + 1] << 8;
        return (short)(hv | lv);
    }

    public static void putIntLittle(byte[] buf,int offset, int value){
        buf[offset] = (byte)(0xFF & value);
        buf[offset+1] = (byte)(0xFF & (value>>8));
        buf[offset+2] = (byte)(0xFF & (value>>16));
        buf[offset+3] = (byte)(0xFF & (value>>24));
    }

    public static int getIntLittle(byte[] buf,int offset){
        int v = buf[offset++]&0xFF;
        v |= (buf[offset++]&0xFF)<<8;
        v |= (buf[offset++]&0xFF)<<16;
        v |= buf[offset]<<24;

        return v;
    }

    public static void putLongLittle(byte[] buf,int offset, long value){
        buf[offset] = (byte)(0xFF & value);
        buf[offset+1] = (byte)(0xFF & (value>>8));
        buf[offset+2] = (byte)(0xFF & (value>>16));
        buf[offset+3] = (byte)(0xFF & (value>>24));
        buf[offset+4] = (byte)(0xFF & (value>>32));
        buf[offset+5] = (byte)(0xFF & (value>>40));
        buf[offset+6] = (byte)(0xFF & (value>>48));
        buf[offset+7] = (byte)(0xFF & (value>>56));
    }

    public static long getLongLittle(byte[] buf,int offset){
        long v = buf[offset++]&0xFFL;

        v |= (buf[offset++]&0xFFL)<<8;
        v |= (buf[offset++]&0xFFL)<<16;
        v |= (buf[offset++]&0xFFL)<<24;
        v |= (buf[offset++]&0xFFL)<<32;
        v |= (buf[offset++]&0xFFL)<<40;
        v |= (buf[offset++]&0xFFL)<<48;
        v |= (buf[offset]&0xFFL)<<56;
        return v;
    }

    public static byte[] long2BytesLittle(long value){
        byte[] buf = new byte[8];
        putLongLittle(buf,0,value);
        return buf;
    }

    public static byte[] int2BytesLittle(int value){
        byte[] buf = new byte[4];
        putIntLittle(buf,0,value);
        return buf;
    }



//    public static byte[] toVarString(byte[] lenBuf,int offset, String v){
//        if(v == null || v.isEmpty()){
//            writeIntLittle(lenBuf,offset,0);
//            return null;
//        }
//        byte[] u8Buff = v.getBytes(StandardCharsets.UTF_8);
//        writeIntLittle(lenBuf,offset,u8Buff.length);
//        return u8Buff;
//    }
//    public static void writeIntLittle(byte[] buf,int offset,,int value) {
//        ByteBuffer bbuf = ByteBuffer.wrap(buf,offset,4);
//        bbuf.order(ByteOrder.LITTLE_ENDIAN);
//        bbuf.putInt(value);
//    }
}
