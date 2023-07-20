package com.example.imad;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Camera_openCV extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Trial Values For setting the resolution of open CV Camera
    private int height=1080;
    private  int width=720;

    // For storing the frames returned by openCV Camera
    private Mat mRgba;
    private Mat mGray;

    // Object of OpenCv Class
    CameraBridgeViewBase cameraBridgeViewBase;

    // Object of the class in which model is invoked
    private signLanguageDetector signLanguageDetector;

    // Object of class for text to speech conversion
    TextToSpeech textToSpeech;

    // Enabling the open cv camera parallely to the on create method
    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case BaseLoaderCallback.SUCCESS:
                    Log.i("Camera_openCV","OpenCv Is Loaded");
                    cameraBridgeViewBase.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_open_cv);

        // Creating an instance of java camera view, setting the frame size and setting its visibility
        cameraBridgeViewBase = (JavaCameraView) findViewById(R.id.camera_view);
        cameraBridgeViewBase.setMaxFrameSize(width, height);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        // Initializing the TTS Object
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i!=TextToSpeech.ERROR){
                    textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });

        // Initializing the Java Class Object by passing the models and their respective input sizes
        try{
            signLanguageDetector=new signLanguageDetector(getAssets(),"hand_model.tflite","custom_Label.txt",300, "custom_sign2.tflite", 96);
            Log.d("MainActivity","Model is successfully loaded");
        }
        catch (IOException e){
            Log.d("MainActivity","Getting some error");
            e.printStackTrace();
        }

    }

    @Override
    protected List<?extends CameraBridgeViewBase> getCameraViewList(){
        return Collections.singletonList(cameraBridgeViewBase);
    }

    public void onCameraViewStarted(int width ,int height){
        mRgba=new Mat(height,width, CvType.CV_8UC4);
        mGray =new Mat(height,width,CvType.CV_8UC1);
    }

    public void onCameraViewStopped(){
        mRgba.release();
    }

    // Function For Capturing Each Frame and Displaying it
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        // The input frame contains of two frames(coloured and grayscale)
        mRgba=inputFrame.rgba();
        mGray=inputFrame.gray();

        // Storing the result image after detection and sending it to camera view to display
        Mat out=new Mat();
        out=signLanguageDetector.recognizeImage(mRgba);
        return out;
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()){
            Log.d("Camera_openCV","Opencv initialization is done");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            //if open cv not loaded
            Log.d("Camera_openCV","Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION,this,baseLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Disabling the open cv object
        if (cameraBridgeViewBase !=null){
            cameraBridgeViewBase.disableView();
        }
    }

    public void onDestroy(){
        // disabling the open cv object
        super.onDestroy();
        if(cameraBridgeViewBase !=null){
            cameraBridgeViewBase.disableView();
        }
    }

    public void add_Text(View view) {
        TextView tv = (TextView) findViewById(R.id.textView2);
        signLanguageDetector.add(tv);
    }

    public void clear_Text(View view) {
        TextView tv = (TextView) findViewById(R.id.textView2);
        signLanguageDetector.clear(tv);
    }

    public void read_Word(View view) {
        String final_string=signLanguageDetector.getword();
        textToSpeech.speak(final_string,TextToSpeech.QUEUE_FLUSH,null);
    }

}