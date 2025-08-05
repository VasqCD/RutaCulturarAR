package com.example.rutaculturalar.ar.managers;

import com.google.ar.core.Frame;
import com.google.ar.core.LightEstimate;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Scene;

public class LightEstimationManager {
    private ArSceneView arSceneView;
    private Scene scene;

    public LightEstimationManager(ArSceneView arSceneView) {
        this.arSceneView = arSceneView;
        this.scene = arSceneView.getScene();
    }

    public void updateLighting(Frame frame) {
        if (frame == null) return;

        LightEstimate lightEstimate = frame.getLightEstimate();
        if (lightEstimate.getState() == LightEstimate.State.VALID) {
            // Obtener intensidad del entorno
            float pixelIntensity = lightEstimate.getPixelIntensity();

            // Obtener corrección de color
            float[] colorCorrection = new float[4];
            lightEstimate.getColorCorrection(colorCorrection, 0);

            // Aplicar estimación de luz a la escena
            updateSceneLighting(pixelIntensity, colorCorrection);
        }
    }

    private void updateSceneLighting(float intensity, float[] colorCorrection) {
        if (scene != null && arSceneView != null) {
            float normalizedIntensity = Math.max(0.3f, Math.min(1.5f, intensity));
        }
    }
}
