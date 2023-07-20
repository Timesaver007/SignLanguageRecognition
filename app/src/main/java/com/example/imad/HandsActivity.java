package com.example.imad;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class HandsActivity extends AppCompatActivity {
    private ImageButton open_camera;
    private ImageButton open_gallery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.seconddemo);

        open_camera = (ImageButton) findViewById(R.id.camera);
        open_gallery = (ImageButton) findViewById(R.id.gallery);
        open_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity1();
            }
        });

        open_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity2();
            }
        });
    }

    public void openActivity1(){
        Intent intent = new Intent(this, Camera_openCV.class);
        startActivity(intent);
    }

    public  void openActivity2(){
        Intent intent = new Intent(this, Final_Result.class);
        intent.putExtra("task","hand_gallery");
        startActivity(intent);
    }
}