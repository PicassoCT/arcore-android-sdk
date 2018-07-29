
package com.google.ar.core.examples.app.common.rendering;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.app.common.helpers.SpringAR;
import com.google.ar.core.examples.app.common.tcpClient.IPackageRecivedCallback;
import com.google.ar.core.examples.app.common.tcpClient.Server;
import com.google.ar.core.examples.app.common.tcpClient.TwinBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

// Draws the Spring Overlay as a Texture
public class SpringOverlayRenderer implements IPackageRecivedCallback {
    private static final String TAG = SpringOverlayRenderer.class.getSimpleName();

    //Config Variables
    private static final String VERTEX_SHADER_NAME = "shaders/overlay.vert";
    private static final String FRAGMENT_SHADER_NAME = "shaders/overlay.frag";
    private int vertexShader;
    private int fragmentShader;
    private int overlayProgram;
    TwinBuffer buffers;

    //Shader Uniform Handles
    int mPositionHandle;
    int mMVPMatrixHandle;
    int uTextureHandle;


    // Server tcpConnection;
    Server tcpConnection = null;
    Context context;

    //Texture data
    private int[] textures = new int[1];
    private static final float[] uvwTex = new float[]{
            0.0f, -1.00f,
           0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, -1.0f,
    };
    private FloatBuffer uvwTexBuffer;
    private int uvwTextureCoord;
    private int COORDS_PER_TEXTURE = 2;
    private final int textureStride = COORDS_PER_TEXTURE * 4;

    //Vertex data
    //Number of Floats per Vertex in the ByteBuffer
    static final int COORDS_PER_VERTEX = 4;
    private FloatBuffer vertexBuffer;
    static float vertices[] = {   // in counterclockwise order:
            -1.0f, 1.0f, 0.0f,1.0f,
            -1.00f, -1.0f, 0.0f,1.0f,
            1.0f, -1.0f, 0.0f,1.0f,
            1.0f, 1.0f, 0.0f,1.0f,
    };

    private final short [] drawOrder = new short[]{0, 1, 2, 2, 3, 0};
    private ShortBuffer drawListBuffer;
    ByteBuffer dlb;
    private final int vertexStride = COORDS_PER_VERTEX * 4;
    private final int vertexCount = vertices.length / COORDS_PER_VERTEX;
    private boolean boolHoloStyle =false;


    //Takes a loaded bitmap from a callback,
    // inserts itself into the OpenGL_Thread
    // updates the Uniform Texture Object
    private void bindTexture(int textureID, Context context, Bitmap bitmap) {
        Log.d(TAG, "Spring OverlayRender bindTexture called");

        android.graphics.Matrix flip = new android.graphics.Matrix();
        flip.postScale(-1f,-1f);
        Bitmap b = null;

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        GLES20.glGenTextures(textures.length, textures, 0);

        if (textures[0] == GLES20.GL_FALSE)
            throw new RuntimeException("Error loading uvwTex");

        // bind the uvwTex and set parameters

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, b, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        //Load first instance of the spring overlay
        // since we're using a PNG file with transparency, enable alpha blending.
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        bitmap.recycle();



    }

    public void drawFirstTimeLogo(Context context) {
        // first, try to generate a uvwTex handle
        android.graphics.Matrix flip = new android.graphics.Matrix();
        flip.postScale(-1f,-1f);
        Bitmap b = null;
        try {
            b = BitmapFactory.decodeStream( context.getAssets().open("models/springoverlayraw.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        GLES20.glGenTextures(textures.length, textures, 0);

        if (textures[0] == GLES20.GL_FALSE)
            throw new RuntimeException("Error loading uvwTex");

        // bind the uvwTex and set parameters

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, b, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        //Load first instance of the spring overlay
        // since we're using a PNG file with transparency, enable alpha blending.
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        b.recycle();
    }


    // On Surface created
    public void createOnGlThread(Context context) throws IOException {
        //tearDown();


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

        //Prepare the vertex data
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

        //Prepare the uvw data
        byteBuf = ByteBuffer.allocateDirect(uvwTex.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        uvwTexBuffer = byteBuf.asFloatBuffer();
        uvwTexBuffer.put(uvwTex);
        uvwTexBuffer.position(0);


        //Load the Logo
        drawFirstTimeLogo(context);

        ShaderUtil.checkGLError(TAG, "Program parameters");

    }


    /*updates the Data*/
    public void update(Camera camera, Anchor groundAnchor) {
        //Log.d(TAG, "Spring OverlayRender Update called");
        if (tcpConnection == null) {
            tcpConnection = new Server(context,this);
        }

        if (tcpConnection.messageCounter == 0){
            tcpConnection.datagramReciever.setSendToSpringMessage(formConfigurationMessage());
        }

        if ((camera != null) && (groundAnchor != null) &&  tcpConnection.messageCounter != 0) {

            //Get Camera Position relative to MapCenter
            if (camera.getTrackingState() == TrackingState.TRACKING) {
                tcpConnection.datagramReciever.setSendToSpringMessage(
                                buildGroundAnchorMessage(camera.getPose(),
                                                    getMapCenterFromAnchor(groundAnchor)));
            } else {
                tcpConnection.datagramReciever.setSendToSpringMessage(
                            buildGroundAnchorMessage(camera.getDisplayOrientedPose(),
                                                     getMapCenterFromAnchor(groundAnchor)));
            }
        }
    }

    private String formConfigurationMessage() {

        Log.d(TAG, "Server formConfigurationMessage called");

        return  SpringAR.sendCFGHeader +
                "MODEL=" + Build.MODEL + SpringAR.seperator +//devicename
                "DISPLAYWIDTH=" + Resources.getSystem().getDisplayMetrics().widthPixels + SpringAR.seperator +// screen width
                "DISPLAYHEIGTH="+ Resources.getSystem().getDisplayMetrics().heightPixels + SpringAR.seperator +// screen heigth
                "DISPLAYDIVIDE="+ 50 + SpringAR.seperator;// divider


    }

    private String buildGroundAnchorMessage(Pose camPose, Pose anchorPose) {
        Log.e(TAG, "Server formCamMatriceMessage called");
        String message = SpringAR.sendCAMHeader + "MATRICE=";
        float mat4_4[] = new float[16];
        camPose.toMatrix(mat4_4, 0);

        for (int i = 0; i < 16; i++) {
            message += (mat4_4[i] + SpringAR.seperator);
        }

        return message;
    }


    public void tearDown()
    {
        if (overlayProgram != -1)
        {
            GLES20.glDeleteProgram(overlayProgram);
            GLES20.glDeleteShader(vertexShader);
            GLES20.glDeleteShader(fragmentShader);
            GLES20.glDeleteTextures(textures.length, textures, 0); // free the uvwTex!
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

        GLES20.glUseProgram(overlayProgram);

        GLES20.glEnable(GLES20.GL_BLEND);
        if (boolHoloStyle)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_CONSTANT_COLOR);
        else
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glBlendEquation(GLES20.GL_FUNC_ADD);



        //VertexPositions
        mPositionHandle = GLES20.glGetAttribLocation(overlayProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer);
        ShaderUtil.checkGLError(TAG, "Loading Position Handle");

        // Apply the projection and view transformation
        mMVPMatrixHandle = GLES20.glGetUniformLocation(overlayProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, modelViewProjection, 0);
        ShaderUtil.checkGLError(TAG, "Getting Camera Matrix Handle");

        //Bind  UVW-Texture Coordinates to Shader Variable Handle
        uvwTextureCoord = GLES20.glGetAttribLocation(overlayProgram,"uvwTextureCoord");
        if (uvwTextureCoord < 0)
            ShaderUtil.checkGLError(TAG, "Loading uvwTextureCoord Handle");

        GLES20.glVertexAttribPointer(
                uvwTextureCoord,
                COORDS_PER_TEXTURE ,
                GLES20.GL_FLOAT,
                false,
                textureStride,
                uvwTexBuffer);
        GLES20.glEnableVertexAttribArray(uvwTextureCoord);


        //Bind Sampler to texture[0]
        uTextureHandle = GLES20.glGetUniformLocation(overlayProgram,"TextureHandle");
        GLES20.glUniform1i(uTextureHandle, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT,
                drawListBuffer);



        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(uvwTextureCoord);
        GLES20.glDisable(GLES20.GL_BLEND);

        /////////////////////////////////////////////////////////////////////////////////////////////


        ShaderUtil.checkGLError(TAG, "Cleaning up after drawing planes");
    }


    /*Calculates the MapCenter
     */
    Pose getMapCenterFromAnchor(Anchor anchor) {
        return anchor.getPose();

    }



    @Override
    public void callback(byte [] array, int length ) {
        //Necessary to avoid  ConcurrentModification error,
        // which means you are trying to access the rendering pipeline from a different thread
        // than the OpenGL is rendered on.

        /*
         * glSurfaceView.queueEvent(
         *
         * */
        buffers.setTexture(buffers.getWriteBufferIndex(),array);

            int id = context.getResources().getIdentifier("glSurfaceView","id", context.getPackageName());
            if (id!= 0) {
                ((GLSurfaceView) ((Activity) context).findViewById(id)).queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        bindTexture(0, context, buffers.getDrawBuffer());
                    }
                });
            }
            buffers.switchBuffer();

    }


}


