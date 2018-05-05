
package com.google.ar.core.examples.java.common.rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import com.google.ar.core.Camera;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.thread;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




public class SpringOverlayRenderer implements DelayCallback() {
	
	Collection<Trackable> Cornermarkers;
	Collection<Anchors> Anchors;
	Pose MapCenterPose;
	Pose cameraPose;
	Pose storedAnchorPose;
	
	boolean stillConnected = false;
	int FrameRate = 30;
	int delayInMs = 1000/FrameRate;
	int bufferIndex = 0;
	Handler callBackPostFrame = new Handler();
	int width;
	int heigth;
	
	//Shader Variables
	Bitmap oldBuffer;
	Bitmap newBuffer;
	private int  springShaderOverlay;
	private static final String TAG = SpringOverlayRenderer.class.getSimpleName();
	private static final String VERTEX_SHADER_NAME = "shaders/overlay.vert";
	private static final String FRAGMENT_SHADER_NAME = "shaders/overlay.frag";
	private static final String OVERLAY_BUFFER_NAME = "SpringOverlayRaw.png";
	
	//TextureBuffer[2]
	/*Constructor*/
	void SpringOverlayRenderer(int width, int heigth) {	
		
		//Shader laden
		loadSpringOverlayShader();

		//Load Logo
		
		//Kick off Rendering
		Renderer.trackAndRenderLoop();	
	}

	/*Loads the shader and attached Uniforms*/
	 public void createOnGlThread(Context context, String overlayScreenTextureName) throws IOException {
		loadSpringOverlayShader(overlayScreenTextureName);

		ShaderUtil.checkGLError(TAG, "Program parameters");
  }
	
	
	
	/*Initalization of the Device as a AR Device*/
	void initializeConnection(){
		ScreenSize deviceDisplayResolution;
		
		deviceDisplayResolution = getDeviceDisplayResolution();

		while !stillConnected {
			stillConnected = initializeConnection(deviceDisplayResolution)
			Thread.sleep(60);
		}
	}

	/*Update a phone with change screensize*/
	void onScreenChange(int width, int heigth) {
		width = width;
		heigth = heigth;
	}

	/*RenderLoop*/
	void trackAndRenderLoop() {
		
			initializeConnection();
		
			//Main Loop
			Anchors = identifyAnchorPoses(Cornermarkers);

			//Ídentify at least one of four anchors 

			//Display Loop 
				if (Anchors.size() > 2) 									
					MapCenterPose = getMapCenterFromAnchors(Anchors);//Computate center of Map
				else
					MapCenterPose = getMapCenterFromSingleAnchor(Anchors);//Computate center of Map from a non-determinstic set of anchors


				if Camera.getTrackingState() == TRACKING {
					//Get Camera Position relative to MapCenter
					cameraPose = Canmera.getPose();
				} else	{
					cameraPose = Canmera.getDisplayOrientedPose ();			
				}
				
				//Transfer Data via USB-Cable an X86  dll
				stillConnected = sendDataToSpring(cameraPose, MapCenterPose);
				
				//Yield and wait
				
				//Picture recived
				stillConnected = reciveDataFromSpring();
				
				if (!stillConnected){
					//On timeout draw last completed picture
					redrawLastBuffer();
				} else {
				//Draw Frame over stored picture
					drawNewBuffer();
				}			
				this.delay(delayInMs, trackAndRenderLoop);	
			}
	
	
	/*Checks every frame for the Trackable Objects on the picture
	@ Param gets a list of trackable markers
	*/
	Collection<Anchor> identifyAnchorPoses(Collection<Trackable> Cornermarkers) {
		
	}

	/*Calculates the MapCenter 
	TODO
	*/
	Pose getMapCenterFromAnchors(Collection<Anchor>  Anchors) {
		Pose CurrentPose;
		Pose BlendedPose;
		Pose NeutralPose;
		NeutralPose = new Pose();
		
		float anchorSize = Anchors.size();
		
		 for (Anchor anchor : Anchors) {
			CurrentPose = anchor.getPose();
			CurrentPose = makeInterpolated(NeutralPose, CurrentPose, 1/anchorSize); 
			BlendedPose = BlendedPose + CurrentPose;
			}
		storedAnchorPose = BlendedPose;
		
		return BlendedPose;
	}
	
	/* Calculates the Position of the MapCenter for a single or none anchors
	TODO
	*/
	Pose getMapCenterFromSingleAnchor(Collection<Anchor>  Anchors) {
		 for (Anchor anchor : Anchors) {
			 if (anchor) {
				 storedAnchorPose = anchor.getPose();				 
			 }
		 }
		return storedAnchorPose
	}
	/*Send the MapCenter and  Data via USB from Spring on the X86
	TODO
	*/
	boolean sendDataToSpring(Pose cameraPose, Pose MapCenterPose) {
		
	}
	
	/*Lade den Spring Overlay shader
	TODO
	*/
	void loadSpringOverlayShader( string overlayScreenTextureName) {
		int vertexShader =
			ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
		int fragmentShader =
			ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);
		
		
	    springShaderOverlay = GLES20.glCreateProgram();
		GLES20.glAttachShader(springShaderOverlay, vertexShader);
		GLES20.glAttachShader(springShaderOverlay, fragmentShader);
		GLES20.glLinkProgram(springShaderOverlay);
		GLES20.glUseProgram(springShaderOverlay);

		ShaderUtil.checkGLError(TAG, "Program creation");
	}

	/*Recive Data via USB from Spring on the X86
	TODO
	*/
	boolean  reciveDataFromSpring(){
		//store the formerly newBuffer in the oldBuffer
		oldBuffer= newBuffer;		
		
		newBuffer= BitmapFactory.decodeStream(context.getAssets().open(OVERLAY_BUFFER_NAME));
		
			
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glGenTextures(textures.length, textures, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

		GLES20.glTexParameteri(
		GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

	
	}

	/*Draw the new Recived Data
	TODO
	*/
	void redrawLastBuffer(){
	    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, oldBuffer, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		drawCameraBackground();
		drawSpringOverlay();
	}

	/*Draw the new Recived Data
	TODO
	*/
	void drawNewBuffer(){
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, newBuffer, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);	
		 ShaderUtil.checkGLError(TAG, "Texture loading");
		 drawCameraBackground();
		 drawSpringOverlay();
	}
	
	
	void drawSpringOverlay() {
		 // Planes must be sorted by distance from camera so that we draw closer planes first, and
		// they occlude the farther planes.

		
/*		TODO Blend in the transfered picture

		float[] cameraView = new float[16];
		cameraPose.inverse().toMatrix(cameraView, 0);

		// Planes are drawn with additive blending, masked by the alpha channel for occlusion.

	

		
		// Set up the shader.
		GLES20.glUseProgram(springShaderOverlay);

		// Attach the texture.
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
		GLES20.glUniform1i(textureUniform, 0);

		// Shared fragment uniforms.
		GLES20.glUniform4fv(gridControlUniform, 1, GRID_CONTROL, 0);

		// Enable vertex arrays
		GLES20.glEnableVertexAttribArray(planeXZPositionAlphaAttribute);

		ShaderUtil.checkGLError(TAG, "Setting up to draw planes");

	

		// Clean up the state we set
		GLES20.glDisableVertexAttribArray(planeXZPositionAlphaAttribute);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDepthMask(true);

	ShaderUtil.checkGLError(TAG, "Cleaning up after drawing planes");
		*/
	}
}


/*Interface Delayed Callback*/
 public interface DelayCallback{
        void afterDelay();
}

public static void delay(int delayInMs, final DelayCallback delayCallback){
    Handler handler = new Handler();
    handler.postDelayed(new Runnable() {
        @Override
        public void run() {
            delayCallback.afterDelay();
        }
    }, delayInMs); // afterDelay will be executed after (secs*1000) milliseconds.
}