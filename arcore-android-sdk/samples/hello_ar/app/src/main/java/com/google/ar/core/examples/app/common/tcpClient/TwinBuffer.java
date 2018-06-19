package com.google.ar.core.examples.app.common.tcpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;

public class TwinBuffer {

  public void TwinBuffer( ) {


      Buffers[0] = BitmapFactory.decodeFile("/mipmap-mdpi/springoverlayraw.png");
      Buffers[1] = BitmapFactory.decodeFile("/mipmap-mdpi/springoverlayraw.png");

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

    public Bitmap loadTexture(Context context, int bufferID, int ressourceID) {
        InputStream imagestream = context.getResources().openRawResource(ressourceID);
        Buffers[bufferID] = null;
        android.graphics.Matrix flip = new  android.graphics.Matrix();
        flip.postScale(-1f,-1f);
        try {
            Buffers[bufferID] = BitmapFactory.decodeStream(imagestream);
            imagestream.close();
            imagestream=null;
        }catch (Exception e){



        }
        return Buffers[bufferID];

    }

}
