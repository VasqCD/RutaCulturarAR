package com.example.rutaculturalar;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.RotationController;
import com.google.ar.sceneform.ux.TransformableNode;

public class ARViewActivity extends AppCompatActivity {

    private ArFragment arFragment;
    private ModelRenderable model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_view);

        // Botón de volver
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Botón de ayuda
        ImageButton btnHelp = findViewById(R.id.btnHelp);
        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.info_dialog, null);
                ImageView infoImage = dialogView.findViewById(R.id.infoImage);
                TextView infoTitle = dialogView.findViewById(R.id.infoTitle);
                TextView infoText = dialogView.findViewById(R.id.infoText);
                TextView infoSubtitle1 = dialogView.findViewById(R.id.infoSubtitle1);
                TextView infoText1 = dialogView.findViewById(R.id.infoText1);
                TextView infoSubtitle2 = dialogView.findViewById(R.id.infoSubtitle2);
                TextView infoText2 = dialogView.findViewById(R.id.infoText2);
                TextView infoSubtitle3 = dialogView.findViewById(R.id.infoSubtitle3);
                TextView infoText3 = dialogView.findViewById(R.id.infoText3);
                infoImage.setImageResource(R.drawable.modelo_info); // Asegúrate de tener modelo_info.png en res/drawable
                infoTitle.setText(getString(R.string.info_title));
                infoText.setText(getString(R.string.info_text));
                infoSubtitle1.setText(getString(R.string.info_subtitle1));
                infoText1.setText(getString(R.string.info_text1));
                infoSubtitle2.setText(getString(R.string.info_subtitle2));
                infoText2.setText(getString(R.string.info_text2));
                infoSubtitle3.setText(getString(R.string.info_subtitle3));
                infoText3.setText(getString(R.string.info_text3));
                new AlertDialog.Builder(ARViewActivity.this)
                        .setTitle("Información")
                        .setView(dialogView)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        // Carga el modelo 3D
        ModelRenderable.builder()
                .setSource(this, Uri.parse("modelo2.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(renderable -> model = renderable)
                .exceptionally(error -> {
                    Toast.makeText(this, "Error cargando modelo", Toast.LENGTH_SHORT).show();
                    return null;
                });

        arFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
            if (model == null) return;

            AnchorNode anchorNode = new AnchorNode(hitResult.createAnchor());
            anchorNode.setParent(arFragment.getArSceneView().getScene());

            TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
            modelNode.setParent(anchorNode);
            modelNode.setRenderable(model);

            // Limitar el rango de escala permitido por gestos
            modelNode.getScaleController().setMinScale(0.25f);
            modelNode.getScaleController().setMaxScale(0.50f);

            // Configurar el controlador de rotación
            RotationController rotCtrl = modelNode.getRotationController();
            rotCtrl.setEnabled(true);
            rotCtrl.setRotationRateDegrees(30f);   // velocidad de rotacion

            modelNode.select();
            // Establecer la escala después
            modelNode.setLocalScale(new Vector3(0.02f, 0.02f, 0.02f));


        });
    }
}
