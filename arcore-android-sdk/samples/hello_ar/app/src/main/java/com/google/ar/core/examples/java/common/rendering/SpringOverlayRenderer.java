
package com.google.ar.core.examples.java.common.rendering;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.*;
import android.widget.ImageView;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.common.tcpClient.*;
import com.google.ar.core.examples.java.common.helpers.comonUtils;
import com.google.ar.core.examples.java.common.tcpClient.Server;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import javax.microedition.khronos.opengles.GL10;


public class SpringOverlayRenderer implements IPackageRecivedCallback {//  {
    public Anchor groundAnchor;
    public Camera camera;
    Pose MapCenterPose;
    Pose CameraPose;


    public boolean stillConnected = false;
    //Headers


   // Server tcpConnection;
    Paint ipAdressPaint = new Paint();
    Server tcpConnection;
    //Shader Variables
    //Buffers


    private static final String tag = SpringOverlayRenderer.class.getSimpleName();

    private static final String OVERLAY_BUFFER_PATH = "assets/models/springoverlayraw.png";


    private static final float[] VERTEX_COORDINATES = new float[]{
            -1.0f, +1.0f, 0.0f,
            +1.0f, +1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            +1.0f, -1.0f, 0.0f
    };

    private static final float[] TEXTURE_COORDINATES = new float[]{
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };

    private static final Buffer TEXCOORD_BUFFER = ByteBuffer.allocateDirect(TEXTURE_COORDINATES.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(TEXTURE_COORDINATES).rewind();
    private static final Buffer VERTEX_BUFFER = ByteBuffer.allocateDirect(VERTEX_COORDINATES.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(VERTEX_COORDINATES).rewind();
    private Pose cameraPose;


    public void createOnGlThread(GL10 gl) throws IOException {

        //Load the placeholder texture


    /*
    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    glOrtho(0, World.SCREEN_WIDTH, World.SCREEN_HEIGHT, 0, 1, -1);
    glMatrixMode(GL_MODELVIEW);
    glEnable(GL_TEXTURE_2D);

*/
    }


    /*Constructor*/
    void SpringOverlayRenderer() {
           tcpConnection = new Server();
    }


    /*Initalization of the Device as a AR Device*/
    boolean checkConnection() {


        if (!stillConnected)
            return restoreConnection();


        return stillConnected;
    }

    boolean restoreConnection() {
        return true; //TODO
    }

    /*Update a phone with change screensize*/
    void onScreenChange(int width, int heigth) {
    }

    /*updates the Data*/
    public void update(Context context, Camera camera, Anchor groundAnchor) {
        Log.e(tag, "Spring OverlayRender Update called");
        if (camera != null && groundAnchor != null) {

            if (camera.getTrackingState() == TrackingState.TRACKING) {
                //Get Camera Position relative to MapCenter
                tcpConnection.updateCam_GroundAnchor(camera.getPose(), getMapCenterFromAnchor(groundAnchor));

            } else {
                tcpConnection.updateCam_GroundAnchor(camera.getDisplayOrientedPose(), getMapCenterFromAnchor(groundAnchor));

            }
        }
    }

    /*Draw the buffer
     */
    public void draw(Context context, GL10 gl, ImageView view) {

        Log.e(tag, "Spring OverlayRender draw called");
/*
    overlay = TextureLoader.getTexture("PNG", new FileInputStream(new File(OVERLAY_BUFFER_PATH)));

    glBindTexture(GL_TEXTURE_2D, overlay.getTextureID());
    glPushMatrix();
    glBegin(GL_QUADS);
    glTexCoord2f(0, 0);
    glVertex2f(0, 0); // Upper left

    glTexCoord2f(1, 0);
    glVertex2f(World.SCREEN_WIDTH, 0); // Upper right

    glTexCoord2f(1, 1);
    glVertex2f(World.SCREEN_WIDTH, World.SCREEN_HEIGHT); // Lower right

    glTexCoord2f(0, 1);
    glVertex2f(0, World.SCREEN_HEIGHT); // Lower left
    glEnd();
    glPopMatrix();
*/
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
	
	/* Calculates the Position of the MapCenter for a single or none anchors
	TODO
	*/

    /*Send the MapCenter and  Data via USB from Spring on the X86
    TODO
    */
    boolean sendDataToSpring(Pose cameraPose, Pose MapCenterPose) {
        return true;
    }

    /*Lade den Spring Overlay shader
    TODO


    /*Draw the new Recived Data
    TODO
    */
    void drawBuffer(Context context, ImageView view) {
        Paint paint = new Paint();
        //TODO Remove

        //oldBuffer.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888)
        Canvas canvas = new Canvas();
        canvas.drawBitmap(tcpConnection.Buffer.getDrawBuffer(), view.getWidth(), view.getHeight(), paint);

        drawIPAdress(canvas);

        //Attach the canvas to the ImageView

        //Attach the canvas to the ImageView
        view.setImageDrawable(new BitmapDrawable(context.getResources(), tcpConnection.Buffer.getDrawBuffer()));


    }

    void drawIPAdress(Canvas canvas) {
        if (!stillConnected) {
            canvas.drawText("Ip-Address:" + comonUtils.getIPAddress(true), canvas.getWidth() / 2, canvas.getHeight() / 2, ipAdressPaint);
        }
    }

    @Override
    public void callback(Bitmap bitmap) {

    }

}


