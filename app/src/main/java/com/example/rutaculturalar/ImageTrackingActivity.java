package com.example.rutaculturalar;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.TransformableNode;

import com.example.rutaculturalar.ar.managers.ARModelManager;
import com.example.rutaculturalar.ar.managers.AR3DInfoProvider;
import com.example.rutaculturalar.ar.fragments.CustomImageTrackingArFragment; // NUEVO IMPORT
import com.example.rutaculturalar.ar.interfaces.IARModelManager;
import com.example.rutaculturalar.ar.interfaces.IAR3DInfoProvider;
import com.example.rutaculturalar.data.MayanBallCourtInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ImageTrackingActivity extends AppCompatActivity {

    private CustomImageTrackingArFragment arFragment; // CAMBIO AQUÍ
    private ModelRenderable model;
    private IARModelManager modelManager;
    private IAR3DInfoProvider ar3DInfoProvider;

    private android.widget.Button btnBack;
    private Map<String, AnchorNode> imageAnchors = new HashMap<>(); // Para manejar múltiples imágenes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_tracking);

        // Verificar permisos de cámara antes de inicializar AR
        if (!hasCameraPermission()) {
            requestCameraPermission();
            return;
        }

        // Inicializar CustomArFragment
        setupCustomArFragment();

        // Inicializar managers
        initializeManagers();

        // Configurar botón volver
        setupBackButton();

        // Cargar modelo
        loadMayanBallCourtModel();

        // Configurar frame listener para detectar imágenes
        setupFrameListener();
    }

    private void setupCustomArFragment() {
        arFragment = (CustomImageTrackingArFragment) getSupportFragmentManager()
                .findFragmentById(R.id.arFragment);

        if (arFragment == null) {
            arFragment = new CustomImageTrackingArFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.arFragment, arFragment)
                    .commit();
        }

        // Configurar las imágenes a detectar
        String[] imagesToTrack = {"campo_maya_reference.jpg"};
        arFragment.setImagesToTrack(imagesToTrack);
    }

    private boolean hasCameraPermission() {
        return androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        androidx.core.app.ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.CAMERA}, 1000);
    }

    private void initializeManagers() {
        modelManager = new ARModelManager(this);
        ar3DInfoProvider = new AR3DInfoProvider(this);
    }

    private void setupBackButton() {
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            clearAllAnchors();
            finish();
        });
    }

    private void loadMayanBallCourtModel() {
        modelManager.loadModel("CampoFutbolMaya.glb", new IARModelManager.ModelLoadCallback() {
            @Override
            public void onModelLoaded(ModelRenderable loadedModel) {
                model = loadedModel;
                Toast.makeText(ImageTrackingActivity.this,
                        "Modelo cargado - Enfoca una imagen del campo maya",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onModelLoadError(String error) {
                Toast.makeText(ImageTrackingActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupFrameListener() {
        if (arFragment != null) {
            arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
                Frame frame = arFragment.getArSceneView().getArFrame();
                if (frame != null) {
                    updateAugmentedImages(frame);
                }
            });
        }
    }

    private void updateAugmentedImages(Frame frame) {
        Collection<AugmentedImage> augmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage augmentedImage : augmentedImages) {
            String imageName = augmentedImage.getName();

            if (imageName == null) continue;

            switch (augmentedImage.getTrackingState()) {
                case TRACKING:
                    if (!imageAnchors.containsKey(imageName)) {
                        onImageFound(augmentedImage);
                    }
                    break;

                case STOPPED:
                    if (imageAnchors.containsKey(imageName)) {
                        onImageLost(augmentedImage);
                    }
                    break;

                default:
                    break;
            }
        }
    }

    private void onImageFound(AugmentedImage augmentedImage) {
        runOnUiThread(() -> {
            String imageName = augmentedImage.getName();

            if (model != null && !imageAnchors.containsKey(imageName)) {
                // Crear anchor en la posición de la imagen detectada
                AnchorNode imageAnchor = new AnchorNode(
                        augmentedImage.createAnchor(augmentedImage.getCenterPose())
                );
                imageAnchor.setParent(arFragment.getArSceneView().getScene());
                imageAnchors.put(imageName, imageAnchor);

                // Crear nodo del modelo
                TransformableNode modelNode = new TransformableNode(
                        arFragment.getTransformationSystem()
                );
                modelNode.setParent(imageAnchor);
                modelNode.setRenderable(model);

                // Configurar escala apropiada basada en el tamaño de la imagen
                float imageExtentX = augmentedImage.getExtentX();
                float scaleFactor = imageExtentX / 0.5f; // Ajustar según necesidades
                modelNode.setLocalScale(new Vector3(scaleFactor, scaleFactor, scaleFactor));

                // Configurar controles
                setupModelControls(modelNode);

                // Mostrar información cultural flotante
                showCulturalInfo(modelNode);

                Toast.makeText(this,
                        "¡Campo maya detectado! Toca el modelo para más información",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onImageLost(AugmentedImage augmentedImage) {
        runOnUiThread(() -> {
            String imageName = augmentedImage.getName();
            AnchorNode anchor = imageAnchors.get(imageName);

            if (anchor != null) {
                anchor.setParent(null);
                anchor.getAnchor().detach();
                imageAnchors.remove(imageName);
                ar3DInfoProvider.hide3DInfo();

                Toast.makeText(this,
                        "Imagen perdida - Vuelve a enfocar la imagen",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupModelControls(TransformableNode modelNode) {
        // Configurar controles del modelo
        modelNode.getScaleController().setMinScale(0.05f);
        modelNode.getScaleController().setMaxScale(0.3f);
        modelNode.getRotationController().setEnabled(true);

        // Listener para mostrar información al tocar
        modelNode.setOnTapListener((hitTestResult, motionEvent) -> {
            showCulturalInfo(modelNode);
        });
    }

    private void showCulturalInfo(TransformableNode modelNode) {
        Vector3 modelPosition = modelNode.getWorldPosition();
        Vector3 infoOffset = new Vector3(0.3f, 0.2f, 0);

        AnchorNode infoAnchor = new AnchorNode();
        infoAnchor.setParent(arFragment.getArSceneView().getScene());
        infoAnchor.setWorldPosition(Vector3.add(modelPosition, infoOffset));

        ar3DInfoProvider.show3DInfo(
                MayanBallCourtInfo.getMainCourtInfo().title,
                MayanBallCourtInfo.getMainCourtInfo().description,
                infoAnchor,
                new Vector3(0, 0, 0)
        );
    }

    private void clearAllAnchors() {
        for (AnchorNode anchor : imageAnchors.values()) {
            anchor.setParent(null);
            anchor.getAnchor().detach();
        }
        imageAnchors.clear();

        if (ar3DInfoProvider != null) {
            ar3DInfoProvider.hide3DInfo();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ar3DInfoProvider != null) {
            ar3DInfoProvider.hide3DInfo();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                recreate();
            } else {
                Toast.makeText(this, "Permisos de cámara requeridos para AR", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}