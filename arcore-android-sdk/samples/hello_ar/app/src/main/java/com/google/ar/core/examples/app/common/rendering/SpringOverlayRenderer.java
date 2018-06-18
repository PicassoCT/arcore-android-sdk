
package com.google.ar.core.examples.app.common.rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.*;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.app.common.tcpClient.*;
import com.google.ar.core.examples.app.common.helpers.comonUtils;
import com.google.ar.core.examples.app.common.tcpClient.Server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;


public class SpringOverlayRenderer implements IPackageRecivedCallback {//  {

    // Server tcpConnection;
    Server tcpConnection = new  Server();
    //Shader Variables
    //Buffers
    private static final String VERTEX_SHADER_NAME = "shaders/overlay.vert";
    private static final String FRAGMENT_SHADER_NAME = "shaders/overlay.frag";
    private static final String TAG = SpringOverlayRenderer.class.getSimpleName();
    private final int[] textures = new int[1];
    private int vertexShader;
    private int fragmentShader;
    private FloatBuffer vertexBuffer;

    static final int COORDS_PER_VERTEX = 3;
    static float quadCoords[] = {   // in counterclockwise order:
            0.0f, 0.0f,0.0f,
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f,0.0f,
            1.0f, 1.0f,0.0f,
    };

    private void initalizeQuadBuffer(){
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                quadCoords.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(quadCoords);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);

    }



    private static final float[] texturecoordinates = new float[]{
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,

    };

    private int overlayProgram;
    private int mPositionHandle;
    private final int vertexCount = texturecoordinates.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    private void createShaderProgram(Context context) {
        //Load the shaders
        try {
            vertexShader =
                    ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
            fragmentShader =
                    ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);
        }catch (IOException i) {};

        //Attach Texturing Shaders
        overlayProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(overlayProgram, vertexShader);
        GLES20.glAttachShader(overlayProgram, fragmentShader);
        GLES20.glLinkProgram(overlayProgram);
        GLES20.glUseProgram(overlayProgram);


    }

    public void createOnGlThread(Context context, GL10 gl) throws IOException {
        initalizeQuadBuffer();
        createShaderProgram(context);

        ShaderUtil.checkGLError(TAG, "Program creation");

        //Bind the texture and uniforms
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        if (tcpConnection.Buffer.getDrawBuffer() != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, tcpConnection.Buffer.getDrawBuffer(), 0);
        }

        ShaderUtil.checkGLError(TAG, "Program parameters");
    }


    /*Constructor*/
    void SpringOverlayRenderer() {


    }


    /*updates the Data*/
    public void update(Camera camera, Anchor groundAnchor) {
        Log.e(TAG, "Spring OverlayRender Update called");
        if (camera != null && groundAnchor != null) {

            if (camera.getTrackingState() == TrackingState.TRACKING) {
                //Get Camera Position relative to MapCenter
                tcpConnection.updateCam_GroundAnchor(camera.getPose(), getMapCenterFromAnchor(groundAnchor));

            } else {
                tcpConnection.updateCam_GroundAnchor(camera.getDisplayOrientedPose(), getMapCenterFromAnchor(groundAnchor));

            }
        }
    }

    /**
     * Draws the collection of tracked planes, with closer planes hiding more distant ones.
     *
     * @param allPlanes The collection of planes to draw.
     * @param cameraPose The pose of the camera, as returned by {@link Camera#getPose()}
     * @param cameraPerspective The projection matrix, as returned by {@link
     *     Camera#getProjectionMatrix(float[], int, float, float)}
     */
    public void drawOverlay( Pose cameraPose, float[] cameraPerspective) {
        GLES20.glUseProgram(overlayProgram);

        mPositionHandle = GLES20.glGetAttribLocation(overlayProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);

        ShaderUtil.checkGLError(TAG, "Cleaning up after drawing planes");
    }





    /*Calculates the MapCenter
     */
    Pose getMapCenterFromAnchor(Anchor anchor) {
        return anchor.getPose();

    }



    void drawIPAdress() {
        if (!tcpConnection.stillConnected) {
            Paint ipPaint = new Paint();
            ipPaint.setColor(Color.GREEN);
            ipPaint.setTextSize(50);
            ipPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            Canvas canvas = new Canvas();
            canvas.drawPaint(ipPaint);
            canvas.drawText("Ip-Address:" + comonUtils.getIPAddress(true), canvas.getWidth() / 2, canvas.getHeight() / 2, ipPaint);
        }
    }

    @Override
    public void callback(Bitmap bitmap) {
        //any postprocessing of the buffer happens in here
    }

}


