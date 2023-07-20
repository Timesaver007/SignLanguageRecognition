package com.example.imad;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class CaptchaActivity extends AppCompatActivity {

    private ImageButton button1;
    private ImageButton button2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thirddemo);

        button1 = (ImageButton) findViewById(R.id.camera);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openactivity1();
            }
        });

        button2 = (ImageButton) findViewById(R.id.gallery);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity2();
            }
        });
    }

    public void openactivity1(){
        Intent intent = new Intent(this, Final_Result.class);
        intent.putExtra("task","captcha_camera");
        startActivity(intent);
    }

    public  void openActivity2(){
        Intent intent = new Intent(this, Final_Result.class);
        intent.putExtra("task","captcha_gallery");
        startActivity(intent);
    }

}