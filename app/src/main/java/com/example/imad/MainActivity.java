package com.example.imad;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity {
    private ImageButton button1;
    private ImageButton button2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymaindemo);

        button1 = (ImageButton) findViewById(R.id.hand);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openactivity1();
            }
        });

        button2 = (ImageButton) findViewById(R.id.captcha);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity2();
            }
        });
    }

    public void openactivity1(){
        Intent intent = new Intent(this,HandsActivity.class);
        startActivity(intent);
    }

    public  void openActivity2(){
        Intent intent = new Intent(this, CaptchaActivity.class);
        startActivity(intent);
    }
}