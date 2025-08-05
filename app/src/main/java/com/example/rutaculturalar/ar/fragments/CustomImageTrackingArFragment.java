package com.example.rutaculturalar.ar.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;
import java.io.IOException;
import java.io.InputStream;

public class CustomImageTrackingArFragment extends ArFragment {

    private String[] imagesToTrack;
    private AugmentedImageDatabase imageDatabase;

    public void setImagesToTrack(String[] imagesToTrack) {
        this.imagesToTrack = imagesToTrack;
    }

    @Override
    protected void onSessionConfiguration(Session session, Config config) {
        // Llamar al método padre primero
        super.onSessionConfiguration(session, config);

        // Deshabilitar detección de planos para focus en image tracking
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);

        // Configurar para image tracking
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);

        // Crear y configurar la base de datos de imágenes
        if (imagesToTrack != null && imagesToTrack.length > 0) {
            try {
                imageDatabase = setupAugmentedImageDatabase(session);
                config.setAugmentedImageDatabase(imageDatabase);
            } catch (IOException e) {
                e.printStackTrace();
                // Mostrar error al usuario
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.widget.Toast.makeText(getContext(),
                                "Error cargando imágenes de referencia: " + e.getMessage(),
                                android.widget.Toast.LENGTH_LONG).show();
                    });
                }
            }
        }
    }

    private AugmentedImageDatabase setupAugmentedImageDatabase(Session session) throws IOException {
        AugmentedImageDatabase imageDatabase = new AugmentedImageDatabase(session);

        for (String imageName : imagesToTrack) {
            InputStream is = getContext().getAssets().open(imageName);
            Bitmap bitmap = BitmapFactory.decodeStream(is);

            // Usar nombre sin extensión como identificador
            String imageIdentifier = imageName.replace(".jpg", "").replace(".png", "");

            // Agregar imagen a la base de datos
            // Puedes especificar el ancho físico en metros si conoces el tamaño real de la imagen
            imageDatabase.addImage(imageIdentifier, bitmap);

            is.close();
        }

        return imageDatabase;
    }

    @Override
    public String[] getAdditionalPermissions() {
        String[] additionalPermissions = super.getAdditionalPermissions();
        int permissionLength = additionalPermissions != null ? additionalPermissions.length : 0;
        String[] permissions = new String[permissionLength + 1];

        permissions[0] = android.Manifest.permission.CAMERA;
        if (additionalPermissions != null) {
            System.arraycopy(additionalPermissions, 0, permissions, 1, additionalPermissions.length);
        }

        return permissions;
    }

    // Método público para obtener la base de datos de imágenes si es necesario
    public AugmentedImageDatabase getImageDatabase() {
        return imageDatabase;
    }

    // Método para verificar si las imágenes están configuradas
    public boolean isImageTrackingConfigured() {
        return imageDatabase != null && imagesToTrack != null && imagesToTrack.length > 0;
    }
}