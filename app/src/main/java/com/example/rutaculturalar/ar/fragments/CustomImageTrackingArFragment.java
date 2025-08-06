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
    private boolean isImageDatabaseConfigured = false;

    public void setImagesToTrack(String[] imagesToTrack) {
        this.imagesToTrack = imagesToTrack;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Deshabilitar la animación de plane discovery (mano que indica mover el dispositivo)
        disablePlaneDiscoveryAnimation();

        // Configurar image tracking cuando el fragmento esté activo
        configureImageTrackingWhenReady();
    }

    private void disablePlaneDiscoveryAnimation() {
        // Método más efectivo: usar un handler para ocultar la animación después de que se cargue
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            hideHandAnimation();
        }, 500); // Esperar medio segundo para que la vista se cargue completamente

        // También configurar un listener para ocultarla continuamente
        if (getArSceneView() != null) {
            getArSceneView().getScene().addOnUpdateListener(frameTime -> {
                hideHandAnimation();
            });
        }
    }

    private void hideHandAnimation() {
        try {
            if (getView() != null) {
                hideHandImageViews(getView());
            }
        } catch (Exception e) {
            android.util.Log.d("ImageTracking", "Error ocultando animación de mano: " + e.getMessage());
        }
    }

    private void hideHandImageViews(android.view.View view) {
        if (view instanceof android.widget.ImageView) {
            android.widget.ImageView imageView = (android.widget.ImageView) view;

            // Verificar si la ImageView contiene la animación de la mano
            if (imageView.getDrawable() != null) {
                String drawableString = imageView.getDrawable().toString();
                if (drawableString.contains("sceneform_hand") || drawableString.contains("hand_phone")) {
                    imageView.setVisibility(android.view.View.GONE);
                    android.util.Log.d("ImageTracking", "Animación de mano ocultada");
                }
            }

            // También verificar por tag o ID
            try {
                String resourceName = "";
                if (imageView.getId() != android.view.View.NO_ID) {
                    resourceName = getResources().getResourceEntryName(imageView.getId()).toLowerCase();
                    if (resourceName.contains("hand") || resourceName.contains("plane") || resourceName.contains("discovery")) {
                        imageView.setVisibility(android.view.View.GONE);
                        android.util.Log.d("ImageTracking", "ImageView ocultada por ID: " + resourceName);
                    }
                }
            } catch (Exception e) {
                // Ignorar errores de ID
            }

        } else if (view instanceof android.view.ViewGroup) {
            // Buscar recursivamente en el ViewGroup
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                hideHandImageViews(viewGroup.getChildAt(i));
            }
        }
    }

    private void configureImageTrackingWhenReady() {
        // Esperar a que la sesión AR esté lista
        getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if (!isImageDatabaseConfigured && getArSceneView().getSession() != null) {
                configureImageTracking();
                isImageDatabaseConfigured = true;
            }
        });
    }

    private void configureImageTracking() {
        Session session = getArSceneView().getSession();
        if (session == null || imagesToTrack == null || imagesToTrack.length == 0) {
            return;
        }

        try {
            // Crear base de datos de imágenes
            imageDatabase = setupAugmentedImageDatabase(session);

            // Configurar la sesión
            Config config = session.getConfig();
            config.setAugmentedImageDatabase(imageDatabase);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);

            // Aplicar configuración
            session.configure(config);

            android.util.Log.d("ImageTracking", "Configuración de image tracking completada");

            // Mostrar mensaje de éxito en UI thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    android.widget.Toast.makeText(getContext(),
                            "Enfoca la imagen del campo maya",
                            android.widget.Toast.LENGTH_SHORT).show();
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("ImageTracking", "Error configurando image tracking: " + e.getMessage());

            // Mostrar error al usuario
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    android.widget.Toast.makeText(getContext(),
                            "Error configurando image tracking: " + e.getMessage(),
                            android.widget.Toast.LENGTH_LONG).show();
                });
            }
        }
    }

    private AugmentedImageDatabase setupAugmentedImageDatabase(Session session) throws IOException {
        AugmentedImageDatabase imageDatabase = new AugmentedImageDatabase(session);

        for (String imageName : imagesToTrack) {
            try {
                InputStream is = getContext().getAssets().open(imageName);
                Bitmap bitmap = BitmapFactory.decodeStream(is);

                if (bitmap == null) {
                    throw new IOException("No se pudo cargar la imagen: " + imageName);
                }

                // Usar nombre sin extensión como identificador
                String imageIdentifier = imageName.replace(".jpg", "").replace(".png", "");

                // Agregar imagen a la base de datos con información de debugging
                int imageIndex = imageDatabase.addImage(imageIdentifier, bitmap);
                android.util.Log.d("ImageTracking", "Imagen agregada: " + imageIdentifier +
                        " (índice: " + imageIndex + ", tamaño: " + bitmap.getWidth() + "x" + bitmap.getHeight() + ")");

                is.close();

            } catch (IOException e) {
                android.util.Log.e("ImageTracking", "Error cargando imagen " + imageName + ": " + e.getMessage());
                throw e;
            }
        }

        android.util.Log.d("ImageTracking", "Base de datos de imágenes creada con " + imagesToTrack.length + " imágenes");
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
        return isImageDatabaseConfigured && imageDatabase != null && imagesToTrack != null && imagesToTrack.length > 0;
    }

    // Método para reconfigurar si es necesario
    public void reconfigureImageTracking() {
        isImageDatabaseConfigured = false;
        configureImageTrackingWhenReady();
    }
}