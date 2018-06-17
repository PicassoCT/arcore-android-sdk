package com.google.ar.core.examples.java.common.tcpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class TwinBuffer {

  public void TwinBuffer() {
      Buffers[0] = BitmapFactory.decodeFile("/models/springoverlayraw.png");
      Buffers[1] = BitmapFactory.decodeFile("/models/springoverlayraw.png");

    }

    Bitmap Buffers[] = new Bitmap[2];

    private int drawIndex = 1;
    private int writeIndex = 0;

   public Bitmap getDrawBuffer(){return Buffers[drawIndex];};

    Bitmap getWriteBuffer(){ return Buffers[writeIndex];};


    public void switchBuffer() {
        int temp = drawIndex;
        drawIndex = writeIndex;
        writeIndex = temp;
    }


}
