
package com.google.ar.core.examples.java.common.rendering;

import android.app.Activity;
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
import com.google.ar.core.examples.java.helloar.R;
import com.google.ar.core.examples.java.helloar.SpringARActivity;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import javax.microedition.khronos.opengles.GL10;


public class SpringOverlayRenderer implements IPackageRecivedCallback {//  {

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
    public void draw(Activity callingActivity , GL10 gl, ImageView view) {
        Log.e(tag, "Spring OverlayRender draw called");

        ImageView image = (ImageView) callingActivity.findViewById(R.id.overlay);
        image.setImageBitmap(tcpConnection.Buffer.getDrawBuffer());
/*
        Paint paint = new Paint();

        Canvas canvas = new Canvas();
        canvas.drawBitmap(tcpConnection.Buffer.getDrawBuffer(), tcpConnection.Buffer.getDrawBuffer().getWidth(), tcpConnection.Buffer.getDrawBuffer().getHeight(), paint);
        drawIPAdress(canvas);
        */

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
	



    void drawIPAdress(Canvas canvas) {
        if (!tcpConnection.stillConnected) {

            canvas.drawText("Ip-Address:" + comonUtils.getIPAddress(true), canvas.getWidth() / 2, canvas.getHeight() / 2, ipAdressPaint);
        }
    }

    @Override
    public void callback(Bitmap bitmap) {
        //any postprocessing of the buffer happens in here
    }

}


