package com.example.rutaculturalar.ar.managers;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;
import com.example.rutaculturalar.ar.interfaces.IARModelManager;
import com.google.ar.sceneform.rendering.ModelRenderable;

public class ARModelManager implements IARModelManager {
    private Context context;

    public ARModelManager(Context context) {
        this.context = context;
    }

    @Override
    public void loadModel(String modelPath, ModelLoadCallback callback) {
        ModelRenderable.builder()
                .setSource(context, Uri.parse(modelPath))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(callback::onModelLoaded)
                .exceptionally(error -> {
                    callback.onModelLoadError("Error cargando modelo: " + error.getMessage());
                    return null;
                });
    }
}
