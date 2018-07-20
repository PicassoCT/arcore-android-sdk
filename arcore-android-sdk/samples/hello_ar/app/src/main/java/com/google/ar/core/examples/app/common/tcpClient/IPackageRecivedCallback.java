package com.google.ar.core.examples.app.common.tcpClient;

import android.graphics.Bitmap;

public interface IPackageRecivedCallback {
    void callback(byte [] array, int length );
}
