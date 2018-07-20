package com.google.ar.core.examples.app.common.tcpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class TwinBuffer {
    final String TAG = getClass().getSimpleName();
    private Server myConnection;

    public TwinBuffer(Context context, Server tcpConnection) {

        myConnection = tcpConnection;
        try {
            Buffers[0] = BitmapFactory.decodeStream(context.getAssets().open("models/springoverlayrawA.png"));
            Buffers[1] = BitmapFactory.decodeStream(context.getAssets().open("models/springoverlayrawB.png"));
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public Bitmap Buffers[] = new Bitmap[2];

    private int drawIndex = 1;
    private int writeIndex = 0;

    public Bitmap getDrawBuffer() {
        return Buffers[drawIndex];
    }
    public int getDrawBufferIndex() { return drawIndex;    }

    Bitmap getWriteBuffer() {
        return Buffers[writeIndex];
    }
    public int getWriteBufferIndex() { return writeIndex; }


    public void switchBuffer() {
        int temp = drawIndex;
        drawIndex = writeIndex;
        writeIndex = temp;
    }

    public Bitmap setTexture(int bufferID, byte[] rawImage) {
        if (Buffers[bufferID] != null) {
            Buffers[bufferID].recycle();
        }

        android.graphics.Matrix flip = new android.graphics.Matrix();
        flip.postScale(-1f, -1f);
        Buffers[bufferID] = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length);


        return Buffers[bufferID];


    }

}
