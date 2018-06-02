/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.vr.sdk.samples.treasurehunt;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;

import ru.vasilyknk.glwrapper.BufferObject;
import ru.vasilyknk.glwrapper.Engine;
import ru.vasilyknk.glwrapper.Program;
import ru.vasilyknk.glwrapper.ResourceHolder;
import ru.vasilyknk.glwrapper.Uniforms;
import ru.vasilyknk.glwrapper.VertexArrayObject;
import ru.vasilyknk.glwrapper.VertexAttrib;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A Google VR sample application.
 *
 * <p>The TreasureHunt scene consists of a planar ground grid and a floating
 * "treasure" cube. When the user looks at the cube, the cube will turn gold.
 * While gold, the user can activate the Cardboard trigger, either directly
 * using the touch trigger on their Cardboard viewer, or using the Daydream
 * controller-based trigger emulation. Activating the trigger will in turn
 * randomly reposition the cube.
 */
public class TreasureHuntActivity extends GvrActivity implements GvrView.StereoRenderer {

  protected Matrix4f modelCube;
  protected Vector3f modelPosition;
  
  private static final String TAG = "TreasureHuntActivity";

  private static final float Z_NEAR = 0.1f;
  private static final float Z_FAR = 100.0f;

  private static final float CAMERA_Z = 0.01f;
  private static final float TIME_DELTA = 0.3f;
  private static final Vector3fc CUBE_ROTATION_AXIS = new Vector3f(0.5f, 0.5f, 1.0f).normalize();

  private static final float YAW_LIMIT = 0.12f;
  private static final float PITCH_LIMIT = 0.12f;

  private static final int COORDS_PER_VERTEX = 3;

  // We keep the light always position just above the user.
  private static final Vector3fc LIGHT_POS_IN_WORLD_SPACE = new Vector3f(0.0f, 2.0f, 0.0f);

  // Convenience vector for extracting the position from a matrix via multiplication.
  private static final Vector3fc POS_MATRIX_MULTIPLY_VEC = new Vector3f(0, 0, 0);

  private static final float MIN_MODEL_DISTANCE = 3.0f;
  private static final float MAX_MODEL_DISTANCE = 7.0f;

  private static final String OBJECT_SOUND_FILE = "cube_sound.wav";
  private static final String SUCCESS_SOUND_FILE = "success.wav";

  private final Vector3f lightPosInEyeSpace = new Vector3f();

  private FloatBuffer floorVertices;
  private FloatBuffer floorColors;
  private FloatBuffer floorNormals;

  private Engine engine = new Engine();

  private ResourceHolder rh = new ResourceHolder();

  private Program cubeProgram;
  private Program floorProgram;

  private VertexArrayObject cubeVAO, cubeFoundVAO;
  private VertexArrayObject floorVAO;

  CubeParams cubeParams;
  FloorParams floorParams;

  private Matrix4f camera;
  private Matrix4f view;
  private Matrix4f headView;
  private Matrix4f modelViewProjection;
  private Matrix4f modelView;
  private Matrix4f modelFloor;

  private Vector3f tempPosition;
  private Quaternionf headRotation;

  private float objectDistance = MAX_MODEL_DISTANCE / 2.0f;
  private float floorDepth = 20f;

  private Vibrator vibrator;

  private GvrAudioEngine gvrAudioEngine;
  private volatile int sourceId = GvrAudioEngine.INVALID_ID;
  private volatile int successSourceId = GvrAudioEngine.INVALID_ID;

  private FloatBuffer tempFloatBuffer = FloatBuffer.allocate(4);

  /**
   * Checks if we've had an error inside of OpenGL ES, and if so what that error is.
   *
   * @param label Label to report in case of error.
   */
  private static void checkGLError(String label) {
    int error;
    while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
      Log.e(TAG, label + ": glError " + error);
      throw new RuntimeException(label + ": glError " + error);
    }
  }

  /**
   * Sets the view to our GvrView and initializes the transformation matrices we will use
   * to render our scene.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);



    initializeGvrView();

    modelCube = new Matrix4f();
    camera = new Matrix4f();
    view = new Matrix4f();
    modelViewProjection = new Matrix4f();
    modelView = new Matrix4f();
    modelFloor = new Matrix4f();
    tempPosition = new Vector3f();
    // Model first appears directly in front of user.
    modelPosition = new Vector3f(0.0f, 0.0f, -MAX_MODEL_DISTANCE / 2.0f);
    headRotation = new Quaternionf();
    headView = new Matrix4f();
    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    // Initialize 3D audio engine.
    gvrAudioEngine = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
  }

  public void initializeGvrView() {
    setContentView(R.layout.common_ui);

    GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
    gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

    gvrView.setRenderer(this);
    gvrView.setTransitionViewEnabled(true);

    // Enable Cardboard-trigger feedback with Daydream headsets. This is a simple way of supporting
    // Daydream controller input for basic interactions using the existing Cardboard trigger API.
    gvrView.enableCardboardTriggerEmulation();

    if (gvrView.setAsyncReprojectionEnabled(true)) {
      // Async reprojection decouples the app framerate from the display framerate,
      // allowing immersive interaction even at the throttled clockrates set by
      // sustained performance mode.
      AndroidCompat.setSustainedPerformanceMode(this, true);
    }

    setGvrView(gvrView);
  }

  @Override
  public void onPause() {
    gvrAudioEngine.pause();
    super.onPause();
  }

  @Override
  public void onResume() {
    super.onResume();
    gvrAudioEngine.resume();
  }

  @Override
  public void onRendererShutdown() {
    Log.i(TAG, "onRendererShutdown");
    rh.close();
  }

  @Override
  public void onSurfaceChanged(int width, int height) {
    Log.i(TAG, "onSurfaceChanged");
  }

  /**
   * Creates the buffers we use to store information about the 3D world.
   *
   * <p>OpenGL doesn't use Java arrays, but rather needs data in a format it can understand.
   * Hence we use ByteBuffers.
   *
   * @param config The EGL configuration used when creating the surface.
   */
  @Override
  public void onSurfaceCreated(EGLConfig config) {
    Log.i(TAG, "onSurfaceCreated");
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well.

    initCube();
    initFloor();

    modelFloor
            .identity()
            .translate(0, -floorDepth, 0)
          ;

    // Avoid any delays during start-up due to decoding of sound files.
    if (false)
    {
      new Thread(
              new Runnable() {
                @Override
                public void run() {
                  // Start spatial audio playback of OBJECT_SOUND_FILE at the model position. The
                  // returned sourceId handle is stored and allows for repositioning the sound object
                  // whenever the cube position changes.
                  gvrAudioEngine.preloadSoundFile(OBJECT_SOUND_FILE);
                  sourceId = gvrAudioEngine.createSoundObject(OBJECT_SOUND_FILE);
                  gvrAudioEngine.setSoundObjectPosition(
                          sourceId, modelPosition.x, modelPosition.y, modelPosition.z);
                  gvrAudioEngine.playSound(sourceId, true /* looped playback */);
                  // Preload an unspatialized sound to be played on a successful trigger on the cube.
                  gvrAudioEngine.preloadSoundFile(SUCCESS_SOUND_FILE);
                }
              })
              .start();
    }

    updateModelPosition();

    checkGLError("onSurfaceCreated");
  }

  private BufferObject createFloatBuffer(float[] data) {
    ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
    bb.order(ByteOrder.nativeOrder());

    FloatBuffer fb = bb.asFloatBuffer();
    fb.put(data);
    fb.position(0);

    BufferObject bo = rh.createBufferObject(GLES20.GL_ARRAY_BUFFER);
    bo.setData(fb, data.length * 4, GLES20.GL_STATIC_DRAW);

    return bo;
  }

  private void initCube() {
    BufferObject cubeVertices    = createFloatBuffer(WorldLayoutData.CUBE_COORDS      );
    BufferObject cubeColors      = createFloatBuffer(WorldLayoutData.CUBE_COLORS      );
    BufferObject cubeFoundColors = createFloatBuffer(WorldLayoutData.CUBE_FOUND_COLORS);
    BufferObject cubeNormals     = createFloatBuffer(WorldLayoutData.CUBE_NORMALS     );

    cubeProgram = rh.createProgram();

    cubeProgram.init(readRawTextFileUnsafe(R.raw.light_vertex),
            readRawTextFileUnsafe(R.raw.passthrough_fragment));

    cubeProgram.use();
    checkGLError("Cube program");

    int positionIndex = cubeProgram.getAttribLocation("a_Position");
    int normalIndex   = cubeProgram.getAttribLocation("a_Normal"  );
    int colorIndex    = cubeProgram.getAttribLocation("a_Color"   );

    VertexAttrib[] attribs = {
        new VertexAttrib(positionIndex, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, 0),
        new VertexAttrib(normalIndex  , 3           , GLES20.GL_FLOAT, false, 0, 1),
        new VertexAttrib(colorIndex   , 4           , GLES20.GL_FLOAT, false, 0, 2),
    };

    cubeVAO = new VertexArrayObject(attribs, new BufferObject[] {
            cubeVertices   ,
            cubeNormals    ,
            cubeColors     ,
    });

    cubeFoundVAO = new VertexArrayObject(attribs, new BufferObject[] {
            cubeVertices   ,
            cubeNormals    ,
            cubeFoundColors,
    });

    cubeParams = new CubeParams(cubeProgram);
  }

  private void initFloor() {
    BufferObject floorVertices = createFloatBuffer(WorldLayoutData.FLOOR_COORDS );
    BufferObject floorNormals  = createFloatBuffer(WorldLayoutData.FLOOR_NORMALS);
    BufferObject floorColors   = createFloatBuffer(WorldLayoutData.FLOOR_COLORS );


    floorProgram = rh.createProgram();

    floorProgram.init(readRawTextFileUnsafe(R.raw.light_vertex),
            readRawTextFileUnsafe(R.raw.grid_fragment));

    floorProgram.use();
    checkGLError("Floor program");


    int positionIndex = floorProgram.getAttribLocation("a_Position");
    int normalIndex   = floorProgram.getAttribLocation("a_Normal");
    int colorIndex    = floorProgram.getAttribLocation("a_Color");

    VertexAttrib[] attribs = {
            new VertexAttrib(positionIndex, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, 0),
            new VertexAttrib(normalIndex  , 3           , GLES20.GL_FLOAT, false, 0, 1),
            new VertexAttrib(colorIndex   , 4           , GLES20.GL_FLOAT, false, 0, 2),
    };

    floorVAO = new VertexArrayObject(attribs, new BufferObject[] {
            floorVertices,
            floorNormals,
            floorColors,
    });

    floorParams = new FloorParams(floorProgram);
  }


  /**
   * Updates the cube model position.
   */
  protected void updateModelPosition() {
    modelCube
            .identity()
            .translate(modelPosition)
            ;

    // Update the sound location to match it with the new cube position.
    if (sourceId != GvrAudioEngine.INVALID_ID) {
      gvrAudioEngine.setSoundObjectPosition(
          sourceId, modelPosition.x, modelPosition.y, modelPosition.z);
    }
    checkGLError("updateCubePosition");
  }

  /**
   * Converts a raw text file into a string.
   *
   * @param resId The resource ID of the raw text file about to be turned into a shader.
   * @return The context of the text file, or null in case of error.
   */
  private String readRawTextFile(int resId) {
    InputStream inputStream = getResources().openRawResource(resId);
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
      reader.close();
      return sb.toString();
    } catch (IOException e) {
      Log.e(TAG, "Failed to load text file: " + e);
    }
    return null;
  }

  private String readRawTextFileUnsafe(int resId) {
    String str = readRawTextFile(resId);
    if (str == null)
      throw new RuntimeException("Can't read raw text file " + resId);

    return str;
  }

  /**
   * Prepares OpenGL ES before we draw a frame.
   *
   * @param headTransform The head transformation in the new frame.
   */
  @Override
  public void onNewFrame(HeadTransform headTransform) {
    setCubeRotation();

    camera.setLookAt(0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

    float[] tempArr = new float[16];

    headTransform.getHeadView(tempArr, 0);
    headView.set(tempArr);

    // Update the 3d audio engine with the most recent head rotation.
    headTransform.getQuaternion(tempArr, 0);

    headRotation.set(tempArr[0], tempArr[1], tempArr[2], tempArr[3]);

    gvrAudioEngine.setHeadRotation(
        headRotation.x, headRotation.y, headRotation.z, headRotation.w);
    // Regular update call to GVR audio engine.
    gvrAudioEngine.update();

    checkGLError("onReadyToDraw");
  }

  protected void setCubeRotation() {

    modelCube.rotate((float)Math.toRadians(TIME_DELTA), CUBE_ROTATION_AXIS);
    //modelCube.identity();
  }

  /**
   * Draws a frame for an eye.
   *
   * @param eye The eye to render. Includes all required transformations.
   */
  @Override
  public void onDrawEye(Eye eye) {
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    checkGLError("colorParam");

    // Apply the eye transformation to the camera.
    //Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

    // Set the position of the light
    //Matrix.multiplyMV(lightPosInEyeSpace, 0, view, 0, LIGHT_POS_IN_WORLD_SPACE, 0);

    Matrix4f eyeView = new Matrix4f();
    eyeView.set(eye.getEyeView());

    eyeView.mul(camera, view);
    view.transformPosition(LIGHT_POS_IN_WORLD_SPACE, lightPosInEyeSpace);

    // Build the ModelView and ModelViewProjection matrices
    // for calculating cube position and light.

    Matrix4f perspective = new Matrix4f();
    perspective.set(eye.getPerspective(Z_NEAR, Z_FAR));

    view.mul(modelCube, modelView);
    perspective.mul(modelView, modelViewProjection);

    //float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
    //Matrix.multiplyMM(modelView, 0, view, 0, modelCube, 0);
    //Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
    drawCube();


    view.mul(modelFloor, modelView);
    perspective.mul(modelView, modelViewProjection);

    // Set modelView for the floor, so we draw floor in the correct location
    //Matrix.multiplyMM(modelView, 0, view, 0, modelFloor, 0);
    //Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
    drawFloor();
  }

  @Override
  public void onFinishFrame(Viewport viewport) {}

  /**
   * Draw the cube.
   *
   * <p>We've set all of our transformation matrices. Now we simply pass them into the shader.
   */
  public void drawCube() {
    cubeProgram.use();

    Uniforms uf = engine.getUniforms();

    uf.set(cubeParams.getLightPos(), lightPosInEyeSpace);
    uf.set(cubeParams.getModel(), modelCube, false);
    uf.set(cubeParams.getModelView(), modelView, false);
    uf.set(cubeParams.getModelViewProjection(), modelViewProjection, false);


//    GLES20.glUniform3fv(cubeParams.getLightPos(), 1, lightPosInEyeSpace, 0);
//
//    // Set the Model in the shader, used to calculate lighting
//    GLES20.glUniformMatrix4fv(cubeParams.getModel(), 1, false, modelCube, 0);
//
//    // Set the ModelView in the shader, used to calculate lighting
//    GLES20.glUniformMatrix4fv(cubeParams.getModelView(), 1, false, modelView, 0);
//
//    // Set the ModelViewProjection matrix in the shader.
//    GLES20.glUniformMatrix4fv(cubeParams.getModelViewProjection(), 1, false, modelViewProjection, 0);

    VertexArrayObject vao = isLookingAtObject() ? cubeFoundVAO : cubeVAO;
    
    vao.use();
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
    //vao.unUse();

    checkGLError("Drawing cube");

  }

  /**
   * Draw the floor.
   *
   * <p>This feeds in data for the floor into the shader. Note that this doesn't feed in data about
   * position of the light, so if we rewrite our code to draw the floor first, the lighting might
   * look strange.
   */
  public void drawFloor() {
    floorProgram.use();

    Uniforms uf = engine.getUniforms();

    uf.set(floorParams.getLightPos(), lightPosInEyeSpace);
    uf.set(floorParams.getModel(), modelFloor, false);
    uf.set(floorParams.getModelView(), modelView, false);
    uf.set(floorParams.getModelViewProjection(), modelViewProjection, false);


//    // Set ModelView, MVP, position, normals, and color.
//    GLES20.glUniform3fv(floorParams.getLightPos(), 1, lightPosInEyeSpace, 0);
//    GLES20.glUniformMatrix4fv(floorParams.getModel(), 1, false, modelFloor, 0);
//    GLES20.glUniformMatrix4fv(floorParams.getModelView(), 1, false, modelView, 0);
//    GLES20.glUniformMatrix4fv(floorParams.getModelViewProjection(), 1, false, modelViewProjection, 0);

    VertexArrayObject vao = floorVAO;

    vao.use();
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 24);
    //vao.unUse();

    checkGLError("drawing floor");
  }

  /**
   * Called when the Cardboard trigger is pulled.
   */
  @Override
  public void onCardboardTrigger() {
    Log.i(TAG, "onCardboardTrigger");

    if (isLookingAtObject()) {
      successSourceId = gvrAudioEngine.createStereoSound(SUCCESS_SOUND_FILE);
      gvrAudioEngine.playSound(successSourceId, false /* looping disabled */);
      hideObject();
    }

    // Always give user feedback.
    vibrator.vibrate(50);
  }

  /**
   * Find a new random position for the object.
   *
   * <p>We'll rotate it around the Y-axis so it's out of sight, and then up or down by a little bit.
   */
  protected void hideObject() {
    //float[] rotationMatrix = new float[16];
    //float[] posVec = new float[4];

    Matrix4f rotationMatrix = new Matrix4f();
    Vector3f posVec = new Vector3f();

    // First rotate in XZ plane, between 90 and 270 deg away, and scale so that we vary
    // the object's distance from the user.
    float angleXZ = (float) Math.toRadians(Math.random() * 180 + 90);

    rotationMatrix.rotate(angleXZ, 0f, 1f, 0f);

    //Matrix.setRotateM(rotationMatrix, 0, angleXZ, 0f, 1f, 0f);
    float oldObjectDistance = objectDistance;
    objectDistance =
        (float) Math.random() * (MAX_MODEL_DISTANCE - MIN_MODEL_DISTANCE) + MIN_MODEL_DISTANCE;
    float objectScalingFactor = objectDistance / oldObjectDistance;

    modelCube.getTranslation(posVec);

    rotationMatrix.scale(objectScalingFactor);
    rotationMatrix.transformPosition(posVec);

    //Matrix.scaleM(rotationMatrix, 0, objectScalingFactor, objectScalingFactor, objectScalingFactor);
    //Matrix.multiplyMV(posVec, 0, rotationMatrix, 0, modelCube, 12);

    float angleY = (float) Math.random() * 80 - 40; // Angle in Y plane, between -40 and 40.
    angleY = (float) Math.toRadians(angleY);
    float newY = (float) Math.tan(angleY) * objectDistance;

    modelPosition.set(posVec.x, newY, posVec.z);

//    modelPosition[0] = posVec[0];
//    modelPosition[1] = newY;
//    modelPosition[2] = posVec[2];

    updateModelPosition();
  }

  /**
   * Check if user is looking at object by calculating where the object is in eye-space.
   *
   * @return true if the user is looking at the object.
   */
  private boolean isLookingAtObject() {
    // Convert object space to camera space. Use the headView from onNewFrame.

    headView.mul(modelCube, modelView);
    modelView.transformPosition(POS_MATRIX_MULTIPLY_VEC, tempPosition);

//    Matrix.multiplyMM(modelView, 0, headView, 0, modelCube, 0);
//    Matrix.multiplyMV(tempPosition, 0, modelView, 0, POS_MATRIX_MULTIPLY_VEC, 0);

    float pitch = (float) Math.atan2(tempPosition.y, -tempPosition.z);
    float yaw = (float) Math.atan2(tempPosition.x, -tempPosition.z);

    return Math.abs(pitch) < PITCH_LIMIT && Math.abs(yaw) < YAW_LIMIT;
  }
}
