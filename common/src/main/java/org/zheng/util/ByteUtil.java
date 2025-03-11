package org.zheng.util;

import java.util.HexFormat;

public class ByteUtil {
    public static String toHexString(byte[] json) {
        return HexFormat.of().formatHex(json);
    }
    public static String toHex(byte b){
        return toHexString(new byte[]{b});
    }
}
