package com.google.ar.core.examples.java.common.tcpClient;

import android.graphics.Bitmap;

public class TwinBuffer {
    Bitmap Buffers[];
    private int drawIndex = 1;
    private int writeIndex = 0;

    Bitmap getDrawBuffer(){return Buffers[drawIndex];};

    Bitmap getWriteBuffer(){ return Buffers[writeIndex];};


    public void switchBuffer() {
        int temp = drawIndex;
        drawIndex = writeIndex;
        writeIndex = temp;
    }


}
