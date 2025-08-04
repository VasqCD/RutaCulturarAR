package com.example.rutaculturalar;

import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Frame;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.RotationController;
import com.google.ar.sceneform.ux.TransformableNode;

// Nuevas importaciones para la arquitectura modular
import com.example.rutaculturalar.ar.managers.ARModelManager;
import com.example.rutaculturalar.ar.managers.SurfaceDetector;
import com.example.rutaculturalar.ar.managers.ContextualInfoProvider;
import com.example.rutaculturalar.ar.managers.LightEstimationManager;
import com.example.rutaculturalar.ar.managers.AR3DInfoProvider;
import com.example.rutaculturalar.ar.interfaces.IARModelManager;
import com.example.rutaculturalar.ar.interfaces.ISurfaceDetector;
import com.example.rutaculturalar.ar.interfaces.IContextualInfoProvider;
import com.example.rutaculturalar.ar.interfaces.IAR3DInfoProvider;
import com.example.rutaculturalar.data.MayanBallCourtInfo;

public class ARViewActivity extends AppCompatActivity {

    private ArFragment arFragment;
    private ModelRenderable model;

    // Nuevos managers siguiendo principios SOLID
    private IARModelManager modelManager;
    private ISurfaceDetector surfaceDetector;
    private IContextualInfoProvider contextualInfoProvider;
    private LightEstimationManager lightManager;
    private IAR3DInfoProvider ar3DInfoProvider;

    private boolean isModelPlaced = false;
    private int infoCounter = 0; // Para rotar entre diferentes informaciones
    private TransformableNode currentModelNode; // Referencia al modelo actual

    // Lista para mantener referencia a todos los objetos colocados
    private java.util.List<AnchorNode> placedObjects = new java.util.ArrayList<>();

    // Referencias a los botones
    private android.widget.Button btnBack;
    private android.widget.Button btnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_view);

        // Inicializar ArFragment
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        // Inicializar botones
        initializeButtons();

        // Inicializar managers con inyección de dependencias
        initializeManagers();

        // Cargar modelo usando el nuevo manager
        loadMayanBallCourtModel();

        // Configurar interacciones AR mejoradas
        setupARInteractions();

        // Configurar actualización de iluminación
        setupLightEstimation();
    }

    private void initializeButtons() {
        btnBack = findViewById(R.id.btnBack);
        btnClear = findViewById(R.id.btnClear);

        // Configurar botón volver
        btnBack.setOnClickListener(v -> {
            // Limpiar recursos antes de salir
            clearAllObjects();
            if (ar3DInfoProvider != null) {
                ar3DInfoProvider.hide3DInfo();
            }
            // Volver a MainActivity
            finish();
        });

        // Configurar botón limpiar
        btnClear.setOnClickListener(v -> {
            clearAllObjects();
            Toast.makeText(this, "Todos los objetos han sido eliminados", Toast.LENGTH_SHORT).show();
        });
    }

    private void clearAllObjects() {
        // Ocultar información 3D
        if (ar3DInfoProvider != null) {
            ar3DInfoProvider.hide3DInfo();
        }

        // Remover todos los objetos colocados
        for (AnchorNode anchorNode : placedObjects) {
            if (anchorNode != null) {
                anchorNode.setParent(null);
                anchorNode.getAnchor().detach();
            }
        }
        placedObjects.clear();

        // Resetear variables
        isModelPlaced = false;
        currentModelNode = null;
        infoCounter = 0;
    }

    private void initializeManagers() {
        modelManager = new ARModelManager(this);
        contextualInfoProvider = new ContextualInfoProvider(this);
        ar3DInfoProvider = new AR3DInfoProvider(this);

        // Inicializar surface detector después de que la sesión AR esté lista
        initializeSurfaceDetectorWhenReady();

        lightManager = new LightEstimationManager(arFragment.getArSceneView());
    }

    private void initializeSurfaceDetectorWhenReady() {
        // Esperar a que la sesión AR esté lista antes de inicializar el surface detector
        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if (surfaceDetector == null && arFragment.getArSceneView().getSession() != null) {
                surfaceDetector = new SurfaceDetector(arFragment.getArSceneView().getSession());
                setupSurfaceDetection();
            }
        });
    }

    private void setupSurfaceDetection() {
        if (surfaceDetector != null) {
            // Habilitar detección de superficies horizontales y verticales
            surfaceDetector.enableHorizontalPlaneDetection(true);
            surfaceDetector.enableVerticalPlaneDetection(true);
        }
    }

    private void loadMayanBallCourtModel() {
        modelManager.loadModel("CampoFutbolMaya.glb", new IARModelManager.ModelLoadCallback() {
            @Override
            public void onModelLoaded(ModelRenderable loadedModel) {
                model = loadedModel;
                Toast.makeText(ARViewActivity.this, "Modelo del campo maya cargado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onModelLoadError(String error) {
                Toast.makeText(ARViewActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupARInteractions() {
        arFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
            if (model == null) {
                Toast.makeText(this, "Modelo aún cargando...", Toast.LENGTH_SHORT).show();
                return;
            }

            // Ocultar información 3D anterior si existe
            if (ar3DInfoProvider.isInfoVisible()) {
                ar3DInfoProvider.hide3DInfo();
            }

            // Verificar tipo de superficie detectada con fallback
            if (surfaceDetector != null) {
                if (surfaceDetector.isHorizontalPlane(plane)) {
                    placeModelOnSurface(hitResult, "horizontal");
                } else if (surfaceDetector.isVerticalPlane(plane)) {
                    // Mostrar tooltip en 3D en lugar de popup plano
                    showVerticalSurfaceWarning(hitResult);
                    return;
                }
            } else {
                // Fallback: colocar el modelo sin verificación del tipo de superficie
                placeModelOnSurface(hitResult, "detectada");
            }
        });
    }

    private void showVerticalSurfaceWarning(HitResult hitResult) {
        // Crear un nodo temporal para mostrar el warning en 3D
        AnchorNode tempNode = new AnchorNode(hitResult.createAnchor());
        tempNode.setParent(arFragment.getArSceneView().getScene());

        ar3DInfoProvider.show3DInfo(
            "Superficie Vertical",
            "Usa una superficie horizontal para colocar el campo maya",
            tempNode,
            new Vector3(0, 0.1f, 0)
        );

        // Remover el warning después de 3 segundos
        arFragment.getArSceneView().getScene().addOnUpdateListener(new com.google.ar.sceneform.Scene.OnUpdateListener() {
            private long startTime = System.currentTimeMillis();

            @Override
            public void onUpdate(com.google.ar.sceneform.FrameTime frameTime) {
                if (System.currentTimeMillis() - startTime > 3000) {
                    ar3DInfoProvider.hide3DInfo();
                    tempNode.setParent(null);
                    arFragment.getArSceneView().getScene().removeOnUpdateListener(this);
                }
            }
        });
    }

    private void placeModelOnSurface(HitResult hitResult, String surfaceType) {
        AnchorNode anchorNode = new AnchorNode(hitResult.createAnchor());
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Agregar a la lista de objetos colocados para poder eliminarlos después
        placedObjects.add(anchorNode);

        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
        modelNode.setParent(anchorNode);
        modelNode.setRenderable(model);

        // Guardar referencia al modelo actual
        currentModelNode = modelNode;

        // Configurar controles mejorados
        setupModelControls(modelNode);

        // Establecer escala apropiada para el campo maya
        modelNode.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
        modelNode.select();

        isModelPlaced = true;

        // Mostrar información inicial en 3D flotante
        show3DCulturalInfo(MayanBallCourtInfo.getMainCourtInfo(), modelNode);

        Toast.makeText(this, "Campo maya colocado - Toca el modelo para más información", Toast.LENGTH_SHORT).show();
    }

    private void setupModelControls(TransformableNode modelNode) {
        // Limitar el rango de escala para mantener proporciones realistas
        modelNode.getScaleController().setMinScale(3f);
        modelNode.getScaleController().setMaxScale(6f);

        // Configurar rotación suave
        RotationController rotCtrl = modelNode.getRotationController();
        rotCtrl.setEnabled(true);
        rotCtrl.setRotationRateDegrees(45f);

        // Listener para mostrar información al tocar el modelo
        modelNode.setOnTapListener((hitTestResult, motionEvent) -> {
            showNext3DCulturalInfo();
        });
    }

    private void show3DCulturalInfo(MayanBallCourtInfo.CulturalData culturalData, TransformableNode modelNode) {
        if (currentModelNode != null) {
            // Posicionar la información flotante más cerca del modelo
            Vector3 modelPosition = modelNode.getWorldPosition();
            Vector3 infoOffset = new Vector3(0.4f, 0.3f, 0); // Reducido de 0.6f a 0.3f en Y para acercarlo

            // Crear un nodo independiente para la información
            AnchorNode infoAnchor = new AnchorNode();
            infoAnchor.setParent(arFragment.getArSceneView().getScene());
            infoAnchor.setWorldPosition(Vector3.add(modelPosition, infoOffset));

            ar3DInfoProvider.show3DInfo(
                culturalData.title,
                culturalData.description,
                infoAnchor, // Usar nodo independiente
                new Vector3(0, 0, 0) // Sin offset adicional
            );
        }
    }

    private void showNext3DCulturalInfo() {
        MayanBallCourtInfo.CulturalData[] infoOptions = {
            MayanBallCourtInfo.getMainCourtInfo(),
            MayanBallCourtInfo.getHistoricalContext(),
            MayanBallCourtInfo.getArchitecturalDetails(),
            MayanBallCourtInfo.getGameRules()
        };

        MayanBallCourtInfo.CulturalData selectedInfo = infoOptions[infoCounter % infoOptions.length];

        // Mostrar información en 3D
        show3DCulturalInfo(selectedInfo, currentModelNode);

        infoCounter++;
    }

    private void setupLightEstimation() {
        // Configurar actualización continua de la iluminación usando ArSceneView
        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            Frame frame = arFragment.getArSceneView().getArFrame();
            if (lightManager != null && frame != null) {
                lightManager.updateLighting(frame);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (contextualInfoProvider != null) {
            contextualInfoProvider.hideCulturalInfo();
            contextualInfoProvider.hideTooltip();
        }
        if (ar3DInfoProvider != null) {
            ar3DInfoProvider.hide3DInfo();
        }
    }
}
