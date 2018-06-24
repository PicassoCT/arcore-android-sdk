
package com.google.ar.core.examples.app.common.rendering;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.google.ar.core.examples.app.springar.R;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

// Draws the Spring Overlay as a Texture
public class SpringOverlayRenderer implements IPackageRecivedCallback {
    private static final String TAG = SpringOverlayRenderer.class.getSimpleName();

    //Config Variables
    private static final String VERTEX_SHADER_NAME = "shaders/overlay.vert";
    private static final String FRAGMENT_SHADER_NAME = "shaders/overlay.frag";
    private int vertexShader;
    private int fragmentShader;
    private int overlayProgram;

    //Shader Uniform Handles
    int mPositionHandle;
    int mMVPMatrixHandle;
    int uTextureHandle;
    int textureCoordHandle;

    // Server tcpConnection;
    Server tcpConnection = null;
    Context context;

    //Texture data
    private int[] textures = new int[2];
    private static final float[] texture = new float[]{
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
    };
    private FloatBuffer textureBuffer;
    private final int textureStride = COORDS_PER_VERTEX * 4;

    //Vertex data
    //Number of Floats per Vertex in the ByteBuffer
    static final int COORDS_PER_VERTEX = 3;
    private FloatBuffer vertexBuffer;
    static float vertices[] = {   // in counterclockwise order:
            -1.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
    };
    private final short [] drawOrder = new short[]{0, 1, 2, 2, 3, 0};
    private ShortBuffer drawListBuffer;
    ByteBuffer dlb;
    private final int vertexStride = COORDS_PER_VERTEX * 3;
    private final int vertexCount = vertices.length / COORDS_PER_VERTEX;


    //Takes a loaded bitmap from a callback,
    // inserts itself into the OpenGL_Thread
    // updates the Uniform Texture Object
    private void bindTexture(int textureID, Context context, Bitmap bitmap) {
        Log.d(TAG, "Spring OverlayRender bindTexture called");
    /*
         //TODO DelMe after Tests
        Bitmap textureBitmap = null;
        try {
            textureBitmap =
                    BitmapFactory.decodeStream(context.getAssets().open("models/trigrid.png"));
        }catch (IOException i) {}

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(textures.length, textures, 0);
        if (textures[0] == GLES20.GL_FALSE)
            throw new RuntimeException("Error loading texture");

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0); //
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);



        textureBitmap.recycle();
*/

    }

    public void drawFirstTimeLogo(Context context) {
        // first, try to generate a texture handle
        GLES20.glGenTextures(1, textures, 0);

        if (textures[0] == GLES20.GL_FALSE)
            throw new RuntimeException("Error loading texture");

        // bind the texture and set parameters
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        //Load first instance of the spring overlay
        //Bitmap b = BitmapFactory.decodeResource(context.getResources(),R.drawable.springoverlayraw);
        Bitmap b = null;
        try {
            b = BitmapFactory.decodeStream( context.getAssets().open("models/trigrid.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, b, 0);
        b.recycle();
    }


    // On Surface created
    public void createOnGlThread(Context context) throws IOException {
        //tearDown();
       // drawFirstTimeLogo(context);

        Log.d(TAG, "Spring OverlayRender createOnGlThread called");
        this.context = context;
         //  this.tcpConnection = new Server(context,this);



        try {
            vertexShader =
                    ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
            fragmentShader =
                    ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);
        } catch (IOException i) {
            Log.e(TAG, "GLES20::Error" + i.toString());
            ShaderUtil.checkGLError(TAG, "GLES20::Error: Program parameters");
        }

        GLES20.glCompileShader(vertexShader);
        GLES20.glCompileShader(fragmentShader);

        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(fragmentShader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        if (compileStatus[0] == 0)
        {
            String error = GLES20.glGetShaderInfoLog(fragmentShader);
            GLES20.glDeleteShader(fragmentShader);
            throw new RuntimeException("Error compiling shader: " + error);
        }

        overlayProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(overlayProgram, vertexShader);
        GLES20.glAttachShader(overlayProgram, fragmentShader);
        GLES20.glLinkProgram(overlayProgram);
        GLES20.glUseProgram(overlayProgram);


        ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        vertexBuffer = byteBuf.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        dlb= ByteBuffer.allocateDirect (
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order (ByteOrder.nativeOrder ());
        drawListBuffer = dlb.asShortBuffer ();
        drawListBuffer.put (drawOrder);
        drawListBuffer.position (0);

/*
        byteBuf = ByteBuffer.allocateDirect(texture.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        textureBuffer = byteBuf.asFloatBuffer();
        textureBuffer.put(texture);
        textureBuffer.position(0);
*/

/*

        //Trette tcpConnection Buffer Initialisierung los

        tcpConnection.Buffer.loadTexture(context,
                0,

                "models/trigrid.png"
        );
        tcpConnection.Buffer.loadTexture(context,
                1,
                "models/trigrid.png"
        );
      //Load the Logo
        drawFirstTimeLogo(context);

        // Activate the first texture (GL_TEXTURE0) and bind it to our handle
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glUniform1i(uTextureHandle, 0);

        // set the viewport and a fixed, white background

        // since we're using a PNG file with transparency, enable alpha blending.
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);
*/

        ShaderUtil.checkGLError(TAG, "Program parameters");

    }


    /*updates the Data*/
    public void update(Camera camera, Anchor groundAnchor) {
        Log.d(TAG, "Spring OverlayRender Update called");
        if (tcpConnection == null) {
            tcpConnection = new Server(context,this);
        }

        if ((camera != null) && (groundAnchor != null)) {

            //Get Camera Position relative to MapCenter
            if (camera.getTrackingState() == TrackingState.TRACKING)
                tcpConnection.updateCam_GroundAnchor(camera.getPose(), getMapCenterFromAnchor(groundAnchor));
            else
                tcpConnection.updateCam_GroundAnchor(camera.getDisplayOrientedPose(), getMapCenterFromAnchor(groundAnchor));
        }


    }

    public void tearDown()
    {
        if (overlayProgram != -1)
        {
            GLES20.glDeleteProgram(overlayProgram);
            GLES20.glDeleteShader(vertexShader);
            GLES20.glDeleteShader(fragmentShader);
            GLES20.glDeleteTextures(textures.length, textures, 0); // free the texture!
        }
    }

    /**
     * Draws the collection of tracked planes, with closer planes hiding more distant ones.
     *
     * @param cameraView        The pose of the camera, as returned by {@link Camera#getPose()}
     * @param cameraPerspective The projection matrix, as returned by {@link
     *                          Camera#getProjectionMatrix(float[], int, float, float)}
     */
    public void drawOverlay(float[] cameraView, float[] cameraPerspective) {
        float[] modelViewProjection = new float[16];
        Matrix.multiplyMM(modelViewProjection, 0, cameraPerspective, 0, cameraView, 0);

        Log.d(TAG, "Spring OverlayRender drawOverlay called");

        GLES20.glUseProgram(overlayProgram);

        //VertexPositions
       mPositionHandle = GLES20.glGetAttribLocation(overlayProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer);

        // Apply the projection and view transformation
        mMVPMatrixHandle = GLES20.glGetUniformLocation(overlayProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, modelViewProjection, 0);
        ShaderUtil.checkGLError(TAG, "Getting Camera Matrix Handle");

        //Texturinput
        /*
        uTextureHandle = GLES20.glGetAttribLocation(overlayProgram, "uTexture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glUniform1i(uTextureHandle, 0);

        textureCoordHandle = GLES20.glGetUniformLocation(overlayProgram, "TexCoordOut");
        GLES20.glUniform1i(textureCoordHandle, 0);
        */


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
    public void callback(final Bitmap bitmap) {
        //Necessary to avoid  ConcurrentModification error,
        // which means you are trying to access the rendering pipeline from a different thread
        // than the OpenGL is rendered on.

        /*
        * glSurfaceView.queueEvent(
        *
        * */
        /*
            int id = context.getResources().getIdentifier("glSurfaceView","id", context.getPackageName());
            if (id!= 0) {
                ((GLSurfaceView) ((Activity) context).findViewById(id)).queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        bindTexture(0, context, bitmap);
                    }
                });
            }
            */
    }


}


