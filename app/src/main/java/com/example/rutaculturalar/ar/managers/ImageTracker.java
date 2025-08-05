package com.example.rutaculturalar.ar.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.rutaculturalar.ImageTrackingActivity;
import com.example.rutaculturalar.ar.interfaces.IImageTracker;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
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
    private AugmentedImageDatabase imageDatabase; // ¡FALTABA ESTO!

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
            // CREAR LA BASE DE DATOS DE IMÁGENES AUMENTADAS
            imageDatabase = new AugmentedImageDatabase(arSession);

            for (String imageName : imageNames) {
                InputStream is = context.getAssets().open(imageName);
                Bitmap bitmap = BitmapFactory.decodeStream(is);

                // AÑADIR IMAGEN A LA BASE DE DATOS CON NOMBRE IDENTIFICADOR
                String imageIdentifier = imageName.replace(".jpg", "").replace(".png", "");
                int imageIndex = imageDatabase.addImage(imageIdentifier, bitmap);

                trackedImages.put(imageIdentifier, false);
                is.close();
            }

            // CONFIGURAR LA SESIÓN CON LA BASE DE DATOS
            Config config = arSession.getConfig();
            config.setAugmentedImageDatabase(imageDatabase); // ¡ESTO ES CRUCIAL!
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            arSession.configure(config);

        } catch (IOException e) {
            e.printStackTrace();
            // Callback de error si es necesario
            if (callback instanceof ImageTrackingActivity) {
                ((ImageTrackingActivity) callback).runOnUiThread(() ->
                        android.widget.Toast.makeText(context,
                                "Error cargando imágenes de referencia: " + e.getMessage(),
                                android.widget.Toast.LENGTH_LONG).show()
                );
            }
        }
    }

    @Override
    public void enableImageTracking(boolean enable) {
        Config config = arSession.getConfig();
        if (enable) {
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            // Mantener la base de datos configurada
            if (imageDatabase != null) {
                config.setAugmentedImageDatabase(imageDatabase);
            }
        } else {
            config.setUpdateMode(Config.UpdateMode.BLOCKING);
            config.setAugmentedImageDatabase(null);
        }
        arSession.configure(config);
    }

    @Override
    public void onImageDetected(AugmentedImage augmentedImage) {
        String imageName = augmentedImage.getName();
        if (imageName != null && !trackedImages.getOrDefault(imageName, false)) {
            trackedImages.put(imageName, true);
            if (callback != null) {
                callback.onImageFound(augmentedImage);
            }
        }
    }

    @Override
    public void onImageLost(AugmentedImage augmentedImage) {
        String imageName = augmentedImage.getName();
        if (imageName != null) {
            trackedImages.put(imageName, false);
            if (callback != null) {
                callback.onImageLost(augmentedImage);
            }
        }
    }

    @Override
    public boolean isImageBeingTracked(String imageName) {
        return trackedImages.getOrDefault(imageName, false);
    }
}