package com.example.rutaculturalar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    Button buttonStartAR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Establecer fondo desde assets
        LinearLayout layout = findViewById(R.id.mainLayout); // Asegúrate que el LinearLayout tenga este id
        try {
            InputStream is = getAssets().open("fondo.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            layout.setBackground(new BitmapDrawable(getResources(), bitmap));
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        buttonStartAR = findViewById(R.id.buttonStartAR);

        buttonStartAR.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ARViewActivity.class);
            startActivity(intent);
        });
    }
}
