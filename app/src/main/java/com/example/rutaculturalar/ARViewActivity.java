package com.example.rutaculturalar;

import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
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

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        // Carga el modelo 3D (.glb desde assets)
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
            rotCtrl.setRotationRateDegrees(30f);   // velocidad de rotación

            modelNode.select();
            // Establecer la escala después
            modelNode.setLocalScale(new Vector3(0.02f, 0.02f, 0.02f));


        });
    }
}
