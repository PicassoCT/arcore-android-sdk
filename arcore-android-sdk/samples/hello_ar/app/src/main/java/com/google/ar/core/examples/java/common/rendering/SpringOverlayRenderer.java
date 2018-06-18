
package com.google.ar.core.examples.java.common.rendering;

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
import com.google.ar.core.examples.java.common.tcpClient.*;
import com.google.ar.core.examples.java.common.helpers.comonUtils;
import com.google.ar.core.examples.java.common.tcpClient.Server;

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
    Server tcpConnection;
    //Shader Variables
    //Buffers
    private static final String TAG = SpringOverlayRenderer.class.getSimpleName();



    private static final float[] texturecoordinates = new float[]{
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };
////////////////////////////////////////////////////////////////////////////////////////////////////////
private static final String VERTEX_SHADER_NAME = "shaders/overlay.vert";
    private static final String FRAGMENT_SHADER_NAME = "shaders/overlay.frag";

    private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
    private static final int BYTES_PER_SHORT = Short.SIZE / 8;
    private static final int COORDS_PER_VERTEX = 3; // x, z, alpha

    private static final int VERTS_PER_BOUNDARY_VERT = 2;
    private static final int INDICES_PER_BOUNDARY_VERT = 3;
    private static final int INITIAL_BUFFER_BOUNDARY_VERTS = 64;

    private static final int INITIAL_VERTEX_BUFFER_SIZE_BYTES =
            BYTES_PER_FLOAT * COORDS_PER_VERTEX * VERTS_PER_BOUNDARY_VERT * INITIAL_BUFFER_BOUNDARY_VERTS;

    private static final int INITIAL_INDEX_BUFFER_SIZE_BYTES =
            BYTES_PER_SHORT
                    * INDICES_PER_BOUNDARY_VERT
                    * INDICES_PER_BOUNDARY_VERT
                    * INITIAL_BUFFER_BOUNDARY_VERTS;


    // Using the "signed distance field" approach to render sharp lines and circles.
    // {dotThreshold, lineThreshold, lineFadeSpeed, occlusionScale}
    // dotThreshold/lineThreshold: red/green intensity above which dots/lines are present
    // lineFadeShrink:  lines will fade in between alpha = 1-(1/lineFadeShrink) and 1.0
    // occlusionShrink: occluded planes will fade out between alpha = 0 and 1/occlusionShrink
    private static final float[] GRID_CONTROL = {0.2f, 0.4f, 2.0f, 1.5f};

    private int overlayProgram;
    private final int[] textures = new int[1];

    private int planeXZPositionAlphaAttribute;

    private int planeModelUniform;
    private int planeModelViewProjectionUniform;
    private int textureUniform;
    private int lineColorUniform;
    private int dotColorUniform;
    private int gridControlUniform;
    private int texturecoordinatesUniform;

    private FloatBuffer vertexBuffer =
            ByteBuffer.allocateDirect(INITIAL_VERTEX_BUFFER_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
    private ShortBuffer indexBuffer =
            ByteBuffer.allocateDirect(INITIAL_INDEX_BUFFER_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer();

    // Temporary lists/matrices allocated here to reduce number of allocations for each frame.
    private final float[] modelMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];
 ///////////////////////////////////////////////////////////////////////////////////////////////////////

    public void createOnGlThread(Context context, GL10 gl) throws IOException {
     //Load the placeholder texture

        int vertexShader =
                ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
        int passthroughShader =
                ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);

        overlayProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(overlayProgram, vertexShader);
        GLES20.glAttachShader(overlayProgram, passthroughShader);
        GLES20.glLinkProgram(overlayProgram);
        GLES20.glUseProgram(overlayProgram);

        ShaderUtil.checkGLError(TAG, "Program creation");

        // Read the texture.
        Bitmap textureBitmap = tcpConnection.Buffer.getDrawBuffer();

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(textures.length, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        ShaderUtil.checkGLError(TAG, "Texture loading");

        planeXZPositionAlphaAttribute = GLES20.glGetAttribLocation(overlayProgram, "a_XZPositionAlpha");

        planeModelUniform = GLES20.glGetUniformLocation(overlayProgram, "u_Model");
        planeModelViewProjectionUniform =
                GLES20.glGetUniformLocation(overlayProgram, "u_ModelViewProjection");
        textureUniform = GLES20.glGetUniformLocation(overlayProgram, "u_Texture");
        lineColorUniform = GLES20.glGetUniformLocation(overlayProgram, "u_lineColor");
        dotColorUniform = GLES20.glGetUniformLocation(overlayProgram, "u_dotColor");
        gridControlUniform = GLES20.glGetUniformLocation(overlayProgram, "u_gridControl");
        texturecoordinatesUniform = GLES20.glGetUniformLocation(overlayProgram, "u_texturecoordinates");

        ShaderUtil.checkGLError(TAG, "Program parameters");
    }


    /*Constructor*/
    void SpringOverlayRenderer() {

        tcpConnection = new Server();
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
    public void drawOverlay(Collection<Plane> allPlanes, Pose cameraPose, float[] cameraPerspective) {
        // Planes must be sorted by distance from camera so that we draw closer planes first, and
        // they occlude the farther planes.
        List<PlaneRenderer.SortablePlane> sortedPlanes = new ArrayList<>();
        float[] normal = new float[3];
        float cameraX = cameraPose.tx();
        float cameraY = cameraPose.ty();
        float cameraZ = cameraPose.tz();


        float[] cameraView = new float[16];
        cameraPose.inverse().toMatrix(cameraView, 0);

        // Planes are drawn with additive blending, masked by the alpha channel for occlusion.

        // Start by clearing the alpha channel of the color buffer to 1.0.
        GLES20.glClearColor(1, 1, 1, 1);
        GLES20.glColorMask(false, false, false, true);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glColorMask(true, true, true, true);

        // Disable depth write.
        GLES20.glDepthMask(false);

        // Additive blending, masked by alpha channel, clearing alpha channel.
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFuncSeparate(
                GLES20.GL_DST_ALPHA, GLES20.GL_ONE, // RGB (src, dest)
                GLES20.GL_ZERO, GLES20.GL_ONE_MINUS_SRC_ALPHA); // ALPHA (src, dest)

        // Set up the shader.
        GLES20.glUseProgram(overlayProgram);

        // Attach the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glUniform1i(textureUniform, 0);

        // Shared fragment uniforms.
        GLES20.glUniform4fv(gridControlUniform, 1, GRID_CONTROL, 0);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(planeXZPositionAlphaAttribute);

        ShaderUtil.checkGLError(TAG, "Setting up to draw planes");
        GLES20.glUniformMatrix4fv(texturecoordinatesUniform, 1, false, texturecoordinates, 0);

        draw(cameraView, cameraPerspective);


        // Clean up the state we set
        GLES20.glDisableVertexAttribArray(planeXZPositionAlphaAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDepthMask(true);

        ShaderUtil.checkGLError(TAG, "Cleaning up after drawing planes");
    }

    /*Draw the buffer
     */
    public void draw( float[] cameraView, float[] cameraPerspective) {
        Log.e(TAG, "Spring OverlayRender draw called");
/*
        ImageView image = (ImageView) callingActivity.findViewById(R.id.overlay);
        image.setImageBitmap(tcpConnection.Buffer.getDrawBuffer());
*/
        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);

        // Set the position of the plane
        vertexBuffer.rewind();
        GLES20.glVertexAttribPointer(
                planeXZPositionAlphaAttribute,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                BYTES_PER_FLOAT * COORDS_PER_VERTEX,
                vertexBuffer);

        // Set the Model and ModelViewProjection matrices in the shader.
        GLES20.glUniformMatrix4fv(planeModelUniform, 1, false, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(
                planeModelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);

        indexBuffer.rewind();
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLE_STRIP, indexBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        ShaderUtil.checkGLError(TAG, "Drawing plane");
    }


    /*Checks every frame for the Trackable Objects on the picture
    @ Param gets a list of trackable markers
    */
    Collection<Anchor> identifyAnchorPoses(Trackable Cornermarkers) {
        return Cornermarkers.getAnchors();
    }

    ;

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


