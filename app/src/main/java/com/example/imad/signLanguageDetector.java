package com.example.imad;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.widget.TextView;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class signLanguageDetector {
    private int INPUT_SIZE;
    private int SignModelSize=0;
    private String final_text="", curr_text="";

    // Object of interpreter class which allows us to load the model and execute it
    private Interpreter interpreter;        // For Hand Detection
    private Interpreter interpreter2;       // For Sign Detection
    private List<String> labelList;

    private int height=0;
    private  int width=0;


    // Constructor of this class to initialize variables and load the models
    signLanguageDetector(AssetManager assetManager, String modelPath, String labelPath, int inputSize, String signModel, int signModelSize) throws IOException {
        INPUT_SIZE=inputSize;
        SignModelSize=signModelSize;

        // Creating an Object of options class to add features to the model
        Interpreter.Options options=new Interpreter.Options();
        options.setNumThreads(4);       // For Multiple threads
        // Loading the model and initializing the interpreter
        interpreter=new Interpreter(loadModelFile(assetManager,modelPath),options);

        // Load the Labels of the model
        labelList=loadLabelList(assetManager,labelPath);

        Interpreter.Options options2=new Interpreter.Options();
        options2.setNumThreads(4);
        interpreter2=new Interpreter(loadModelFile(assetManager,signModel),options2);
    }

    // Pre defined method to get the the model file and its description
    private ByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor=assetManager.openFd(modelPath);
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startOffset =fileDescriptor.getStartOffset();
        long declaredLength=fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);
    }

    // To Load the label map in the list
    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        List<String> labelList=new ArrayList<>();
        // Buffered Reader for reading from the inputstreamreader of label map
        BufferedReader reader=new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));

        // Loop through each line and store it
        String line;
        while ((line=reader.readLine())!=null){
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    // Function to run the interpreter on image input
    public Mat recognizeImage(Mat mat_image){

        // Rotating the image by 90 degree to get portrait frame as default frame orientation is landscape
        Mat rotated_mat_image=new Mat();
        // Taking the transpose of the mat_image and flipping it to get the rotated image
        Mat a=mat_image.t();
        Core.flip(a,rotated_mat_image,1);
        a.release();

        // Converting the Mat to bitmap for further processing
        Bitmap bitmap=null;
        bitmap=Bitmap.createBitmap(rotated_mat_image.cols(),rotated_mat_image.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rotated_mat_image,bitmap);
        height=bitmap.getHeight();
        width=bitmap.getWidth();

        // Resize the bitmap to the same size as of the model
        Bitmap scaledBitmap=Bitmap.createScaledBitmap(bitmap,INPUT_SIZE,INPUT_SIZE,false);

        // Convert bitmap to bytebuffer in order to input to the model
        ByteBuffer byteBuffer=convertBitmapToByteBuffer(scaledBitmap);
        Object[] input=new Object[1];
        input[0]=byteBuffer;

        Map<Integer,Object> output_map=new TreeMap<>();
        float[][][]boxes =new float[1][10][4];  // top 10 objects detected and their coordinates
        float[][] scores=new float[1][10];      // scores of these objects
        float[][] classes=new float[1][10];     // classes of these object
        // Add to object_map
        output_map.put(0,boxes);
        output_map.put(1,classes);
        output_map.put(2,scores);

        // Run the interpreter
        interpreter.runForMultipleInputsOutputs(input,output_map);

        Object value=output_map.get(0);
        Object Object_class=output_map.get(1);
        Object score=output_map.get(2);

        // Loop through each object
        for (int i=0;i<10;i++){
            float class_value=(float) Array.get(Array.get(Object_class,0),i);
            float score_value=(float) Array.get(Array.get(score,0),i);

            // Threshold value for hand detection
            if(score_value>0.7){
                Object box1=Array.get(Array.get(value,0),i);

                // Multiply it with Original height and width of frame
                float y1=(float) Array.get(box1,0)*height;
                float x1=(float) Array.get(box1,1)*width;
                float y2=(float) Array.get(box1,2)*height;
                float x2=(float) Array.get(box1,3)*width;
                if(y1<0){
                    y1=0;
                }
                if(x1<0){
                    x1=0;
                }
                if(x2>width){
                    x2=width;
                }
                if(y2>height){
                    y2=height;
                }
                float h1=y2-y1;
                float w1=x2-x1;

                // Crop the Hand Image
                Rect cropped_roi = new Rect((int)x1,(int)y1,(int)w1,(int)h1);
                Mat cropped = new Mat (rotated_mat_image, cropped_roi).clone();

                // Running the sign detection model
                Bitmap bitmap1 = null;
                bitmap1 = Bitmap.createBitmap(cropped.cols(),cropped.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(cropped,bitmap1);

                Bitmap scaledBitmap1=Bitmap.createScaledBitmap(bitmap1,SignModelSize,SignModelSize,false);
                ByteBuffer byteBuffer1=convertBitmapToByteBuffer1(scaledBitmap1);

                float[][] output_class_value = new float[1][1];
                interpreter2.run(byteBuffer1,output_class_value);

                String sign_val = get_alphabets(output_class_value[0][0]);
                curr_text=sign_val;

                // Log.d("ReturnValue: ", "Object_Class_Value " + sign_val + output_class_value[0][0]);
                // Drawing the box around the hand and putting the identified letter
                Imgproc.putText(rotated_mat_image,"" + sign_val, new Point(x1+10,y1+40),2,1.5,new Scalar(255, 255, 255, 255),2);
                Imgproc.rectangle(rotated_mat_image,new Point(x1,y1),new Point(x2,y2),new Scalar(0, 255, 0, 255),2);
            }
        }

        // Rotating back again to the default orientation of frame
        Mat b=rotated_mat_image.t();
        Core.flip(b,mat_image,0);
        b.release();
        return mat_image;
    }

    public String recognizeImageGallery(Mat mat_image){
        Bitmap bitmap=null;
        bitmap=Bitmap.createBitmap(mat_image.cols(),mat_image.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat_image,bitmap);
        height=bitmap.getHeight();
        width=bitmap.getWidth();

        // scale the bitmap to input size of model
        Bitmap scaledBitmap=Bitmap.createScaledBitmap(bitmap,INPUT_SIZE,INPUT_SIZE,false);
        ByteBuffer byteBuffer=convertBitmapToByteBuffer(scaledBitmap);
        Object[] input=new Object[1];
        input[0]=byteBuffer;

        Map<Integer,Object> output_map=new TreeMap<>();

        float[][][]boxes =new float[1][10][4];
        float[][] scores=new float[1][10];
        float[][] classes=new float[1][10];
        output_map.put(0,boxes);
        output_map.put(1,classes);
        output_map.put(2,scores);

        // Detect Hand
        interpreter.runForMultipleInputsOutputs(input,output_map);

        Object value=output_map.get(0);
        Object Object_class=output_map.get(1);
        Object score=output_map.get(2);

        for (int i=0;i<10;i++){
            float class_value=(float) Array.get(Array.get(Object_class,0),i);
            float score_value=(float) Array.get(Array.get(score,0),i);

            if(score_value>0.7){
                Object box1=Array.get(Array.get(value,0),i);

                // we are multiplying it with Original height and width of frame
                float y1=(float) Array.get(box1,0)*height;
                float x1=(float) Array.get(box1,1)*width;
                float y2=(float) Array.get(box1,2)*height;
                float x2=(float) Array.get(box1,3)*width;

                if(y1<0){
                    y1=0;
                }
                if(x1<0){
                    x1=0;
                }
                if(x2>width){
                    x2=width;
                }
                if(y2>height){
                    y2=height;
                }

                float h1=y2-y1;
                float w1=x2-x1;

                Rect cropped_roi = new Rect((int)x1,(int)y1,(int)w1,(int)h1);
                Mat cropped = new Mat (mat_image, cropped_roi).clone();

                Bitmap bitmap1 = null;
                bitmap1 = Bitmap.createBitmap(cropped.cols(),cropped.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(cropped,bitmap1);

                Bitmap scaledBitmap1=Bitmap.createScaledBitmap(bitmap1,SignModelSize,SignModelSize,false);
                ByteBuffer byteBuffer1=convertBitmapToByteBuffer1(scaledBitmap1);

                float[][] output_class_value = new float[1][1];
                interpreter2.run(byteBuffer1,output_class_value);
                String sign_val = get_alphabets(output_class_value[0][0]);
                curr_text=sign_val;

//                Log.d("ReturnValue: ", sign_val + output_class_value[0][0]);
//                Imgproc.putText(mat_image,"" + sign_val, new Point(x1+10,y1+40),2,1.5,new Scalar(255, 255, 255, 255),2);
//                Imgproc.rectangle(mat_image,new Point(x1,y1),new Point(x2,y2),new Scalar(0, 255, 0, 255),2);
            }

        }
        return curr_text;
    }

    // Function to get alphabet from respective float value
    private String get_alphabets(float v) {
        String val="";
        if(v>=-0.5 && v<0.5){
            val="A";
        }
        else if(v>=0.5 && v<1.5){
            val="B";
        }
        else if(v>=1.5 && v<2.5){
            val="C";
        }
        else if(v>=2.5 && v<3.5){
            val="D";
        }
        else if(v>=3.5 && v<4.5){
            val="E";
        }
        else if(v>=4.5 && v<5.5){
            val="F";
        }
        else if(v>=5.5 && v<6.5){
            val="G";
        }
        else if(v>=6.5 && v<7.5){
            val="H";
        }
        else if(v>=7.5 && v<8.5){
            val="I";
        }
        else if(v>=8.5 && v<9.5){
            val="J";
        }
        else if(v>=9.5 && v<10.5){
            val="K";
        }
        else if(v>=10.5 && v<11.5){
            val="L";
        }
        else if(v>=11.5 && v<12.5){
            val="M";
        }
        else if(v>=12.5 && v<13.5){
            val="N";
        }
        else if(v>=13.5 && v<14.5){
            val="O";
        }
        else if(v>=14.5 && v<15.5){
            val="P";
        }
        else if(v>=15.5 && v<16.5){
            val="Q";
        }
        else if(v>=16.5 && v<17.5){
            val="R";
        }
        else if(v>=17.5 && v<18.5){
            val="S";
        }
        else if(v>=18.5 && v<19.5){
            val="T";
        }
        else if(v>=19.5 && v<20.5){
            val="U";
        }
        else if(v>=20.5 && v<21.5){
            val="V";
        }
        else if(v>=21.5 && v<22.5){
            val="W";
        }
        else if(v>=22.5 && v<23.5){
            val="X";
        }
        else if(v>=23.5 && v<24.5){
            val="Y";
        }
        return val;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer;
        // For scaling the image
        int quant=1;
        int size_images=INPUT_SIZE;
        if(quant==0){
            byteBuffer=ByteBuffer.allocateDirect(1*size_images*size_images*3);
        }
        else {
            byteBuffer=ByteBuffer.allocateDirect(4*1*size_images*size_images*3);
        }
        byteBuffer.order(ByteOrder.nativeOrder());
        // Getting the bitmap pixels in an int array
        int[] intValues=new int[size_images*size_images];
        bitmap.getPixels(intValues,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
        int pixel=0;

        // Code for putting each int value to byte buffer after proper conversion
        for (int i=0;i<size_images;++i){
            for (int j=0;j<size_images;++j){
                final  int val=intValues[pixel++];
                // Retriving R, G, B  value from int values of each pixel
                if(quant==0){
                    byteBuffer.put((byte) ((val>>16)&0xFF));
                    byteBuffer.put((byte) ((val>>8)&0xFF));
                    byteBuffer.put((byte) (val&0xFF));
                }
                else {
                    byteBuffer.putFloat((((val >> 16) & 0xFF))/255.0f);
                    byteBuffer.putFloat((((val >> 8) & 0xFF))/255.0f);
                    byteBuffer.putFloat((((val) & 0xFF))/255.0f);
                }
            }
        }
        return byteBuffer;
    }

    private ByteBuffer convertBitmapToByteBuffer1(Bitmap bitmap) {
        ByteBuffer byteBuffer;
        int quant=1;
        int size_images=SignModelSize;
        if(quant==0){
            byteBuffer=ByteBuffer.allocateDirect(1*size_images*size_images*3);
        }
        else {
            byteBuffer=ByteBuffer.allocateDirect(4*1*size_images*size_images*3);
        }
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues=new int[size_images*size_images];
        bitmap.getPixels(intValues,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
        int pixel=0;
        for (int i=0;i<size_images;++i){
            for (int j=0;j<size_images;++j){
                final  int val=intValues[pixel++];
                if(quant==0){
                    byteBuffer.put((byte) ((val>>16)&0xFF));
                    byteBuffer.put((byte) ((val>>8)&0xFF));
                    byteBuffer.put((byte) (val&0xFF));
                }
                else {
                    byteBuffer.putFloat(((val >> 16) & 0xFF));
                    byteBuffer.putFloat(((val >> 8) & 0xFF));
                    byteBuffer.putFloat(((val) & 0xFF));
                }
            }
        }
        return byteBuffer;
    }

    public void clear(TextView tv){
        final_text="";
        tv.setText(final_text);
    }

    public void add(TextView tv){
        final_text=final_text+curr_text;
        tv.setText(final_text);
    }

    public String getword(){
        return final_text;
    }

}