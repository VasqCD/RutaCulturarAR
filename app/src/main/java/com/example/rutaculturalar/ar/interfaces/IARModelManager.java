package com.example.rutaculturalar.ar.interfaces;

import com.google.ar.sceneform.rendering.ModelRenderable;

public interface IARModelManager {
    void loadModel(String modelPath, ModelLoadCallback callback);

    interface ModelLoadCallback {
        void onModelLoaded(ModelRenderable model);
        void onModelLoadError(String error);
    }
}
