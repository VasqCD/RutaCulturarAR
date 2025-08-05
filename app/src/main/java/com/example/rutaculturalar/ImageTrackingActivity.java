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
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import com.example.rutaculturalar.ar.managers.ARModelManager;
import com.example.rutaculturalar.ar.managers.AR3DInfoProvider;
import com.example.rutaculturalar.ar.managers.ImageTracker;
import com.example.rutaculturalar.ar.interfaces.IARModelManager;
import com.example.rutaculturalar.ar.interfaces.IAR3DInfoProvider;
import com.example.rutaculturalar.data.MayanBallCourtInfo;

import java.util.Collection;

public class ImageTrackingActivity extends AppCompatActivity implements ImageTracker.IImageTrackingCallback {

    private ArFragment arFragment;
    private ModelRenderable model;
    private ImageTracker imageTracker;
    private IARModelManager modelManager;
    private IAR3DInfoProvider ar3DInfoProvider;

    private android.widget.Button btnBack;
    private AnchorNode currentImageAnchor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_tracking);

        // Verificar permisos de cámara antes de inicializar AR
        if (!hasCameraPermission()) {
            requestCameraPermission();
            return;
        }

        // Inicializar ArFragment
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        // Inicializar managers
        initializeManagers();

        // Configurar botón volver
        setupBackButton();

        // Cargar modelo
        loadMayanBallCourtModel();

        // Configurar image tracking
        setupImageTracking();

        // Configurar frame listener para detectar imágenes
        setupFrameListener();
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
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadMayanBallCourtModel() {
        modelManager.loadModel("CampoFutbolMaya.glb", new IARModelManager.ModelLoadCallback() {
            @Override
            public void onModelLoaded(ModelRenderable loadedModel) {
                model = loadedModel;
                Toast.makeText(ImageTrackingActivity.this, "Modelo cargado - Enfoca una imagen del campo maya", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onModelLoadError(String error) {
                Toast.makeText(ImageTrackingActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupImageTracking() {
        // Esperar a que la sesión AR esté lista
        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if (imageTracker == null && arFragment.getArSceneView().getSession() != null) {
                imageTracker = new ImageTracker(
                    this,
                    arFragment.getArSceneView().getSession(),
                    this
                );

                // Configurar las imágenes a detectar (deben estar en assets/)
                String[] imagesToTrack = {"campo_maya_reference.jpg"}; // Imagen de referencia del campo maya
                imageTracker.setupImageDatabase(imagesToTrack);
                imageTracker.enableImageTracking(true);
            }
        });
    }

    private void setupFrameListener() {
        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            Frame frame = arFragment.getArSceneView().getArFrame();
            if (frame != null && imageTracker != null) {
                // Verificar imágenes detectadas en cada frame
                Collection<AugmentedImage> augmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);

                for (AugmentedImage augmentedImage : augmentedImages) {
                    if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
                        imageTracker.onImageDetected(augmentedImage);
                    } else if (augmentedImage.getTrackingState() == TrackingState.STOPPED) {
                        imageTracker.onImageLost(augmentedImage);
                    }
                }
            }
        });
    }

    @Override
    public void onImageFound(AugmentedImage augmentedImage) {
        runOnUiThread(() -> {
            if (model != null && currentImageAnchor == null) {
                // Crear anchor en la posición de la imagen detectada
                currentImageAnchor = new AnchorNode(augmentedImage.createAnchor(augmentedImage.getCenterPose()));
                currentImageAnchor.setParent(arFragment.getArSceneView().getScene());

                // Crear nodo del modelo
                TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                modelNode.setParent(currentImageAnchor);
                modelNode.setRenderable(model);

                // Configurar escala apropiada
                modelNode.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));

                // Configurar controles
                setupModelControls(modelNode);

                // Mostrar información cultural flotante
                showCulturalInfo(modelNode);

                Toast.makeText(this, "¡Campo maya detectado! Toca el modelo para más información", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onImageLost(AugmentedImage augmentedImage) {
        runOnUiThread(() -> {
            if (currentImageAnchor != null) {
                currentImageAnchor.setParent(null);
                currentImageAnchor = null;
                ar3DInfoProvider.hide3DInfo();
                Toast.makeText(this, "Imagen perdida - Vuelve a enfocar la imagen", Toast.LENGTH_SHORT).show();
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
                // Reiniciar la actividad si se conceden los permisos
                recreate();
            } else {
                // Mostrar mensaje y cerrar si no se conceden los permisos
                Toast.makeText(this, "Permisos de cámara requeridos para AR", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
