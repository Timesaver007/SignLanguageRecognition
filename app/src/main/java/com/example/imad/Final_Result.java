package com.example.imad;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

public class Final_Result extends AppCompatActivity {
    private signLanguageDetector signLanguageDetector;
    Bitmap bitmap;
    Mat image;
    ImageButton speaker;
    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    TextToSpeech textToSpeech;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    image=new Mat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_result);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i!=TextToSpeech.ERROR){
                    textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });

        try{
            signLanguageDetector=new signLanguageDetector(getAssets(),"hand_model.tflite","custom_Label.txt",300, "custom_sign2.tflite", 96);
            Log.d("MainActivity","Model is successfully loaded");
        }
        catch (IOException e){
            Log.d("MainActivity","Getting some error");
            e.printStackTrace();
        }

        Intent i = getIntent();
        String task = i.getStringExtra("task");

        if(Objects.equals(task, "hand_gallery")) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, 100);
        }
        else if(Objects.equals(task, "captcha_gallery")) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, 101);
        }
        else if(Objects.equals(task, "captcha_camera")) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, 200);
        }

        speaker = (ImageButton) findViewById(R.id.speak);
        speaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv = (TextView) findViewById(R.id.finalOutput);
                String str = tv.getText().toString();

                char[] captcha = str.toCharArray();
                textToSpeech.speak("",TextToSpeech.QUEUE_FLUSH,null);
                for(char x:captcha){
                    if(x!=' '){
                        textToSpeech.speak(String.valueOf(x),TextToSpeech.QUEUE_ADD,null);
                    }
                }
            }
        });
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==100){
            if(data!=null){
                Uri uri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);
                    Utils.bitmapToMat(bitmap, image);
                    String ans = signLanguageDetector.recognizeImageGallery(image);
                    ImageView outputImage = (ImageView) findViewById(R.id.outputImage);
                    outputImage.setImageBitmap(bitmap);
                    TextView tv = (TextView) findViewById(R.id.finalOutput);
                    tv.setText(ans);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        else if(requestCode==101){
            if(data!=null){
                Uri uri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);
                    ImageView outputImage = (ImageView) findViewById(R.id.outputImage);
                    outputImage.setImageBitmap(bitmap);

                    bitmap = Bitmap.createScaledBitmap(bitmap,480,360,true);

                    InputImage image = InputImage.fromBitmap(bitmap,0);
                    Task<Text> result = recognizer.process(image)
                            .addOnSuccessListener(new OnSuccessListener<Text>() {
                                @Override
                                public void onSuccess(Text visionText) {
                                    String resultText =visionText.getText();
                                    resultText = resultText.replace("\n", "").replace(" ", "");
                                    TextView tv = (TextView) findViewById(R.id.finalOutput);
                                    tv.setText(resultText);
                                }
                            })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Task failed with an exception
                                            // ...
                                        }
                            });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        else if(requestCode==200){
            if(data!=null){
                bitmap = (Bitmap)data.getExtras().get("data");
                ImageView outputImage = (ImageView) findViewById(R.id.outputImage);
                outputImage.setImageBitmap(bitmap);

                bitmap = Bitmap.createScaledBitmap(bitmap,720,1280,true);
                InputImage image = InputImage.fromBitmap(bitmap,0);
                Task<Text> result = recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {
                                String resultText = visionText.getText();
                                resultText = resultText.replace("\n", "").replace("  ", "");
                                TextView tv = (TextView) findViewById(R.id.finalOutput);
                                tv.setText(resultText);
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                        });
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        textToSpeech.speak("",TextToSpeech.QUEUE_FLUSH,null);
    }
}