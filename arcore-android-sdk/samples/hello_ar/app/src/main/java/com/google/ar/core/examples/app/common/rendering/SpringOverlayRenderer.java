
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
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.app.common.tcpClient.*;
import com.google.ar.core.examples.app.common.helpers.comonUtils;
import com.google.ar.core.examples.app.common.tcpClient.Server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;


public class SpringOverlayRenderer implements IPackageRecivedCallback {
    private  FloatBuffer textureBuffer;//  {

    // Server tcpConnection;
    Server tcpConnection = new  Server();
    Context context;



    //Shader Variables
    //Buffers
    private static final String VERTEX_SHADER_NAME = "shaders/overlay.vert";
    private static final String FRAGMENT_SHADER_NAME = "shaders/overlay.frag";
    private static final String TAG = SpringOverlayRenderer.class.getSimpleName();
     private int[] textures = new int[2];
    private int vertexShader;
    private int fragmentShader;
    private FloatBuffer vertexBuffer;
    private  ByteBuffer indexBuffer;
    private ShortBuffer drawListBuffer;
    static final int COORDS_PER_VERTEX = 3;
    static final int COORDS_PER_TEXTURE = 2;
    private final int vertexStride = COORDS_PER_VERTEX*4;
    private final int textureStride = COORDS_PER_VERTEX*4;
    private final int vertexCount = texture.length / COORDS_PER_VERTEX;


    private int overlayProgram;
    private int mPositionHandle;

    static float vertices[] = {   // in counterclockwise order:
            0.0f, 0.0f,0.0f,
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f,0.0f,
            1.0f, 1.0f,0.0f,
    };

    private final short drawOrder[] = {
            0,1,2,
            0,2,3   ,
    };
    private final byte drawOrderByte[] = {
            0,1,2,
            0,2,3   ,
    };

    private static final float[] texture = new float[]{
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
    };
    private int mMVPMatrixHandle;




    private void bindTexture(int textureID, Context context, Bitmap bitmap) {
        Log.d(TAG, "Spring OverlayRender bindTexture called");
        GLES20.glGenTextures(2, IntBuffer.wrap(textures));
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
    }

    public void createOnGlThread(Context context) throws IOException {
        Log.d(TAG, "Spring OverlayRender createOnGlThread called");
        this.context = context;

        ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length*4);
        byteBuf.order(ByteOrder.nativeOrder());
        vertexBuffer =  byteBuf.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
        byteBuf = ByteBuffer.allocateDirect(texture.length*4);

        byteBuf.order(ByteOrder.nativeOrder());
        textureBuffer = byteBuf.asFloatBuffer();
        textureBuffer.put(texture);
        textureBuffer.position(0);

        indexBuffer = ByteBuffer.allocateDirect(drawOrder.length);
        indexBuffer.order(ByteOrder.nativeOrder());
        indexBuffer.put(drawOrderByte);
        indexBuffer.position(0);

        drawListBuffer = indexBuffer.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);




        try {
            vertexShader =
                    ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
            fragmentShader =
                    ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);
        }catch (IOException i) {};

        GLES20.glCompileShader(vertexShader);
        GLES20.glCompileShader(fragmentShader);

        overlayProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(overlayProgram, vertexShader);
        GLES20.glAttachShader(overlayProgram, fragmentShader);
        GLES20.glLinkProgram(overlayProgram);
        GLES20.glUseProgram(overlayProgram);

        bindTexture(0,
                context,
                tcpConnection.Buffer.loadTexture( context,
                0,
                context.getResources().getIdentifier("/drawable-xxhdpi/springoverlayraw.png", "id",context.getPackageName())
                ));

        ShaderUtil.checkGLError(TAG, "Program parameters");

    }




    /*updates the Data*/
    public void update(Camera camera, Anchor groundAnchor) {
        Log.d(TAG, "Spring OverlayRender Update called");
        if (tcpConnection == null) {
            tcpConnection = new Server();
        }

        if ( (camera != null) && (groundAnchor != null)) {

            //Get Camera Position relative to MapCenter
            if (camera.getTrackingState() == TrackingState.TRACKING)
                tcpConnection.updateCam_GroundAnchor(camera.getPose(), getMapCenterFromAnchor(groundAnchor));
            else
                tcpConnection.updateCam_GroundAnchor(camera.getDisplayOrientedPose(), getMapCenterFromAnchor(groundAnchor));
        }


    }

    /**
     * Draws the collection of tracked planes, with closer planes hiding more distant ones.
     *
     *
     * @param cameraPose The pose of the camera, as returned by {@link Camera#getPose()}
     * @param cameraPerspective The projection matrix, as returned by {@link
     *     Camera#getProjectionMatrix(float[], int, float, float)}
     */
    public void drawOverlay( Pose cameraPose, float[] cameraPerspective) {
        Log.d(TAG, "Spring OverlayRender drawOverlay called");

        GLES20.glUseProgram(overlayProgram);
        mPositionHandle = GLES20.glGetAttribLocation(overlayProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        int vsTextureCoord = GLES20.glGetAttribLocation(overlayProgram,"TexCoordIn");
        GLES20.glVertexAttribPointer(mPositionHandle,
                                        COORDS_PER_VERTEX,
                                    GLES20.GL_FLOAT,
                                        false,
                                        vertexStride,
                                        vertexBuffer);
        GLES20.glVertexAttribPointer(vsTextureCoord,
                                    COORDS_PER_TEXTURE,
                                    GLES20.GL_FLOAT,
                                    false,
                                    textureStride,
                                    textureBuffer);

        GLES20.glEnableVertexAttribArray(vsTextureCoord);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        int fsTexture = GLES20.glGetUniformLocation(overlayProgram, "TexCoordOut");
        GLES20.glUniform1i(fsTexture,0);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(overlayProgram,"uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, cameraPerspective,0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT,
                drawListBuffer);
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
        bindTexture( 0,context, bitmap);
        //any postprocessing of the buffer happens in here
    }

}


