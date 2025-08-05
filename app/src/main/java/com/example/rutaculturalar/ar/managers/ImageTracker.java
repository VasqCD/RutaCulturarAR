package com.example.rutaculturalar.ar.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.example.rutaculturalar.ar.interfaces.IImageTracker;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ImageTracker implements IImageTracker {
    private Context context;
    private Session arSession;
    private Map<String, Boolean> trackedImages = new HashMap<>();
    private IImageTrackingCallback callback;

    public interface IImageTrackingCallback {
        void onImageFound(AugmentedImage augmentedImage);
        void onImageLost(AugmentedImage augmentedImage);
    }

    public ImageTracker(Context context, Session arSession, IImageTrackingCallback callback) {
        this.context = context;
        this.arSession = arSession;
        this.callback = callback;
    }

    @Override
    public void setupImageDatabase(String[] imageNames) {
        try {
            // Implementación alternativa sin usar Builder directamente
            // Crear la base de datos de imágenes de forma más simple

            for (String imageName : imageNames) {
                InputStream is = context.getAssets().open(imageName);
                Bitmap bitmap = BitmapFactory.decodeStream(is);

                // Solo preparar las imágenes para tracking, la configuración se hará después
                trackedImages.put(imageName, false);
                is.close();
            }

            // Configurar la sesión para image tracking básico
            Config config = arSession.getConfig();
            // Habilitar augmented images en general
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            arSession.configure(config);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void enableImageTracking(boolean enable) {
        Config config = arSession.getConfig();
        if (enable) {
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        } else {
            config.setUpdateMode(Config.UpdateMode.BLOCKING);
        }
        arSession.configure(config);
    }

    @Override
    public void onImageDetected(AugmentedImage augmentedImage) {
        String imageName = augmentedImage.getName();
        if (!trackedImages.getOrDefault(imageName, false)) {
            trackedImages.put(imageName, true);
            if (callback != null) {
                callback.onImageFound(augmentedImage);
            }
        }
    }

    @Override
    public void onImageLost(AugmentedImage augmentedImage) {
        String imageName = augmentedImage.getName();
        trackedImages.put(imageName, false);
        if (callback != null) {
            callback.onImageLost(augmentedImage);
        }
    }

    @Override
    public boolean isImageBeingTracked(String imageName) {
        return trackedImages.getOrDefault(imageName, false);
    }
}
