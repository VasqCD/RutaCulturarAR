package com.example.rutaculturalar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button buttonStartAR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonStartAR = findViewById(R.id.buttonStartAR);

        buttonStartAR.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ARViewActivity.class);
            startActivity(intent);
        });
    }
}


