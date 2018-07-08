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

    public  TwinBuffer(Context context, Server tcpConnection) {

        myConnection = tcpConnection;
        try {
            Buffers[0] = BitmapFactory.decodeStream(context.getAssets().open("models/springoverlayrawA.png"));
            Buffers[1] = BitmapFactory.decodeStream(context.getAssets().open("models/springoverlayrawB.png"));
        }catch (IOException i){
            i.printStackTrace();
        }
        }

    public Bitmap Buffers[] = new Bitmap[2];

    private int drawIndex = 1;
    private int writeIndex = 0;

    public Bitmap getDrawBuffer() {
        return Buffers[drawIndex];
    }

    Bitmap getWriteBuffer() {
        return Buffers[writeIndex];
    }


    public void switchBuffer() {
        int temp = drawIndex;
        drawIndex = writeIndex;
        writeIndex = temp;
    }

    public void loadTexture(Context context, int bufferID, String assetName) {
            if ( Buffers[bufferID] != null) {
                Buffers[bufferID].recycle();
            }

            android.graphics.Matrix flip = new android.graphics.Matrix();
            flip.postScale(-1f, -1f);
            try {
                Buffers[bufferID] =
                        BitmapFactory.decodeStream(context.getAssets().open(assetName));
            }catch (IOException i) {
                Log.e(TAG, "Error in loadTexture" );
                i.printStackTrace();

            }

            myConnection.packageRecipient.callback(Buffers[bufferID]);


    }

}
