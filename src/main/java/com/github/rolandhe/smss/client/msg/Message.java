package com.github.rolandhe.smss.client.msg;


import com.github.rolandhe.smss.client.bytes.BytesUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Message {
    public static final int DEFAULT_BUFFER_SIZE = 1024;
    private final byte[] SIZE_HOLD = {0,0,0,0};
    private final Map<String,Header> headerMap = new LinkedHashMap<>();

    @Setter
    @Getter
    private byte[] payload;

    public void addHeader(String name,String value){
        Header h = headerMap.get(name);
        if (h != null){
            h.setValue(value);
            return;
        }
        h = new Header(name,value);
        headerMap.put(name,h);
    }

    public byte[] toBytes(){
        if(headerMap.isEmpty() && (payload == null || payload.length == 0)){
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
        try {
            bos.write(SIZE_HOLD);
            BytesUtils.writeIntLittle(SIZE_HOLD,headerMap.size());
            bos.write(SIZE_HOLD);
            for(Entry<String, Header> entry:headerMap.entrySet()){
                writeVarString(bos,entry.getKey());
                writeVarString(bos,entry.getValue().getValue());
            }
            if(payload != null && payload.length > 0){
                bos.write(payload);
            }
        } catch (IOException e){
            throw new RuntimeException(e);
        }

        byte[] content = bos.toByteArray();
        BytesUtils.writeIntLittle(content,content.length - 8);
        return content;
    }

    private void writeVarString(ByteArrayOutputStream bos,String v) throws IOException {
        byte[] u8Buff = BytesUtils.toVarString(SIZE_HOLD,v);
        bos.write(SIZE_HOLD);
        if (u8Buff != null){
            bos.write(u8Buff);
        }
    }

}

