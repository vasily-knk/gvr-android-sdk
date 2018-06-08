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
import android.opengl.GLES30;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;

import de.javagl.obj.ObjReader;
import ru.vasilyknk.glwrapper.BufferObject;
import ru.vasilyknk.glwrapper.Engine;
import ru.vasilyknk.glwrapper.Program;
import ru.vasilyknk.glwrapper.ResourceHolder;
import ru.vasilyknk.glwrapper.Texture;
import ru.vasilyknk.glwrapper.VertexArrayObject;
import ru.vasilyknk.glwrapper.VertexAttrib;

import org.jetbrains.annotations.NotNull;
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
public class TreasureHuntActivity extends GvrActivity implements GvrView.StereoRenderer, CubemapContext {

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


  private Engine engine = new Engine();
  private ResourceHolder rh;

  private MeshProg floorProg;
  private MeshProg cubemapProg;

  private VertexArrayObject floorVAO;

  private Matrix4f camera;
  private Matrix4f headView;


  private Vector3f tempPosition;
  private Quaternionf headRotation;

  private float objectDistance = MAX_MODEL_DISTANCE / 2.0f;
  private float floorDepth = 20f;

  private Vibrator vibrator;

  private GvrAudioEngine gvrAudioEngine;
  private volatile int sourceId = GvrAudioEngine.INVALID_ID;
  private volatile int successSourceId = GvrAudioEngine.INVALID_ID;

  private FloatBuffer tempFloatBuffer = FloatBuffer.allocate(4);

  private SceneParams sceneParams = new SceneParams();
  private Matrix4f cubeTransform = new Matrix4f();
  private Matrix4f floorTransform = new Matrix4f();
  private Matrix4f cubemapTransform = new Matrix4f().scale(30.f);
  private Matrix4f objTransform = new Matrix4f();

  private MeshParams meshParams = new MeshParams();
  private MeshRenderer meshRenderer = new MeshRenderer();

  private Matrix4f tempViewMatrix = new Matrix4f();

  private Texture cubemapTexture;

  private Cube cube;

  private ObjMesh objMesh;
  


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

    camera = new Matrix4f();
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
    String shading_version = GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION);

    rh = new ResourceHolder();

    Log.i(TAG, "onSurfaceCreated");
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well.

    cube = new Cube(this);
    initFloor();
    initCubemap();

    objMesh = new ObjMesh(this);

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


    VertexArrayObject.Companion.unbindAll();
  }

  @NotNull
  @Override
  public BufferObject createFloatBuffer(@NotNull float[] data) {
    ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
    bb.order(ByteOrder.nativeOrder());

    FloatBuffer fb = bb.asFloatBuffer();
    fb.put(data);
    fb.position(0);

    BufferObject bo = rh.createBufferObject(GLES20.GL_ARRAY_BUFFER);
    bo.setData(fb, data.length * 4, GLES20.GL_STATIC_DRAW);

    return bo;
  }

  private void initFloor() {
    BufferObject floorVertices = createFloatBuffer(WorldLayoutData.FLOOR_COORDS );
    BufferObject floorNormals  = createFloatBuffer(WorldLayoutData.FLOOR_NORMALS);
    BufferObject floorColors   = createFloatBuffer(WorldLayoutData.FLOOR_COLORS );


    Program floorProgram = rh.createProgram(
            readRawTextFileUnsafe(R.raw.light_vertex),
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

    floorVAO = rh.createVertexArrayObject(attribs, new BufferObject[] {
            floorVertices,
            floorNormals,
            floorColors,
    });

    floorProg = new MeshProg(floorProgram);

    floorTransform
            .identity()
            .translate(0, -floorDepth, 0)
    ;

  }

  void initCubemap() {
    Program prog = rh.createProgram(
            readRawTextFileUnsafe(R.raw.cubemap_vertex),
            readRawTextFileUnsafe(R.raw.cubemap_fragment));

    prog.use();

    //int loc1 = prog.getUniformLocation("tex");
    int loc2 = prog.getUniformLocation("u_Model");

    checkGLError("cubemap program");

    final int width = 256, height = 256;

    Random random = new Random();

    byte[] bytes = new byte[width * height * 4];
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            int offset = (y * width + x) * 4;

            bytes[offset + 0] = (byte)(random.nextInt(128));
            bytes[offset + 1] = (byte)(random.nextInt(128));
            bytes[offset + 2] = (byte)(random.nextInt(128));
            bytes[offset + 3] = 1;
        }
    }

    ByteBuffer buf = ByteBuffer.allocate(bytes.length);
    buf.put(bytes);
    buf.rewind();

    cubemapTexture = rh.createTexture(GLES20.GL_TEXTURE_2D);
    cubemapTexture.setStorage2D(3, GLES30.GL_RGBA8, width, height);
    cubemapTexture.setSubImage2D(0, 0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
    cubemapTexture.generateMipmap();

    checkGLError("cubemap tex");


    cubemapProg = new MeshProg(prog);
  }


  /**
   * Updates the cube model position.
   */
  protected void updateModelPosition() {
    cubeTransform
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

  @NotNull
  @Override
  public ResourceHolder getRh() {
    return rh;
  }

  @NotNull
  @Override
  public String readRawTextFileUnsafe(int resId) {
    String str = readRawTextFile(resId);
    if (str == null)
      throw new RuntimeException("Can't read raw text file " + resId);

    return str;
  }

  @NotNull
  @Override
  public SceneParams getSceneParams() {
    return sceneParams;
  }

  @NotNull
  @Override
  public InputStream openRawResource(int id) {
    return getResources().openRawResource(id);
  }

  /**
   * Prepares OpenGL ES before we draw a frame.
   *
   * @param headTransform The head transformation in the new frame.
   */
  @Override
  public void onNewFrame(HeadTransform headTransform) {
    setCubeRotation();

    objTransform.set(cubeTransform).scale(0.005f);

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

    cubeTransform.rotate((float)Math.toRadians(TIME_DELTA), CUBE_ROTATION_AXIS);
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

    eyeView.mul(camera, sceneParams.getView());
    sceneParams.getView().transformPosition(LIGHT_POS_IN_WORLD_SPACE, sceneParams.getViewLightPos());
    sceneParams.getProj().set(eye.getPerspective(Z_NEAR, Z_FAR));

    if (isLookingAtObject(cubeTransform))
      objMesh.render(objTransform, meshRenderer);
    else
      cube.render(cubeTransform, meshRenderer);

    drawFloor();
    drawCubemap();


    VertexArrayObject.Companion.unbindAll();

  }

  @Override
  public void onFinishFrame(Viewport viewport) {}


  /**
   * Draw the floor.
   *
   * <p>This feeds in data for the floor into the shader. Note that this doesn't feed in data about
   * position of the light, so if we rewrite our code to draw the floor first, the lighting might
   * look strange.
   */
  private void drawFloor() {
    applyMeshProg(floorProg, floorTransform);

    VertexArrayObject vao = floorVAO;

    int[] array = new int[2];

    GLES30.glGetIntegerv(GLES30.GL_VERTEX_ARRAY_BINDING, array, 0);

    vao.bind();
    GLES30.glGetIntegerv(GLES30.GL_VERTEX_ARRAY_BINDING, array, 1);

    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 24);
    checkGLError("drawing floor");
  }

  private void drawCubemap() {
    applyMeshProg(cubemapProg, cubemapTransform);

    VertexArrayObject vao = cube.getCubeVAO();

    cubemapTexture.bind();

    vao.bind();
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
    checkGLError("drawing floor");
  }

  /**
   * Called when the Cardboard trigger is pulled.
   */
  @Override
  public void onCardboardTrigger() {
    Log.i(TAG, "onCardboardTrigger");

    if (isLookingAtObject(cubeTransform)) {
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

    cubeTransform.getTranslation(posVec);

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
  @Override
  public boolean isLookingAtObject(@NotNull Matrix4fc m) {
    // Convert object space to camera space. Use the headView from onNewFrame.

    headView.mul(m, tempViewMatrix);
    tempViewMatrix.transformPosition(POS_MATRIX_MULTIPLY_VEC, tempPosition);

//    Matrix.multiplyMM(modelView, 0, headView, 0, modelCube, 0);
//    Matrix.multiplyMV(tempPosition, 0, modelView, 0, POS_MATRIX_MULTIPLY_VEC, 0);

    float pitch = (float) Math.atan2(tempPosition.y, -tempPosition.z);
    float yaw = (float) Math.atan2(tempPosition.x, -tempPosition.z);

    return Math.abs(pitch) < PITCH_LIMIT && Math.abs(yaw) < YAW_LIMIT;
  }

  private void applyMeshProg(MeshProg meshProg, Matrix4fc m) {
    meshParams.update(sceneParams, m);

    Program prog = meshProg.getProg();

    prog.setUniform(meshProg.getLightPos(), sceneParams.getViewLightPos());
    prog.setUniform(meshProg.getModel(), m, false);
    prog.setUniform(meshProg.getModelView(), meshParams.getMv(), false);
    prog.setUniform(meshProg.getModelViewProjection(), meshParams.getMvp(), false);
  }
}
