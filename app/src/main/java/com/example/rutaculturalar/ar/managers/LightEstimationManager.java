package com.example.rutaculturalar.ar.managers;

import android.util.Log;

import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;

/**
 * Configuración según guía oficial de ARCore sin usar APIs no disponibles.
 */
public class LightEstimationManager {
    private static final String TAG = "LightEstimationManager";

    private ArSceneView arSceneView;
    private Scene scene;
    private Session session;

    // Estado de configuración
    private Config.LightEstimationMode currentMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR;
    private boolean isConfigured = false;

    // Optimización: Evitar asignación en cada frame (según guía ARCore)
    private final float[] colorCorrection = new float[4];

    // Variables para almacenar valores de iluminación actuales
    private Color currentAmbientColor = new Color(1.0f, 1.0f, 1.0f);
    private Vector3 currentLightDirection = new Vector3(0.0f, -1.0f, 0.0f);
    private float currentLightIntensity = 1.0f;

    public LightEstimationManager(ArSceneView arSceneView) {
        this.arSceneView = arSceneView;
        this.scene = arSceneView.getScene();

        // Configurar la sesión cuando esté disponible
        configureLightEstimationWhenReady();
    }

    /**
     * Configura la estimación de luz cuando la sesión AR está lista
     */
    private void configureLightEstimationWhenReady() {
        // Esperar a que la sesión esté disponible
        arSceneView.getScene().addOnUpdateListener(frameTime -> {
            if (!isConfigured && arSceneView.getSession() != null) {
                this.session = arSceneView.getSession();
                configureLightEstimation();
                isConfigured = true;
            }
        });
    }

    /**
     * Configura la estimación de luz según la guía oficial de ARCore
     */
    private void configureLightEstimation() {
        if (session == null) return;

        try {
            Config config = session.getConfig();

            // Configurar modo HDR ambiental (recomendado por ARCore)
            config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
            session.configure(config);

            currentMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR;
            Log.d(TAG, "Configurado modo ENVIRONMENTAL_HDR exitosamente");

        } catch (Exception e) {
            Log.w(TAG, "Error configurando modo HDR, fallback a AMBIENT_INTENSITY: " + e.getMessage());
            // Fallback a modo básico si HDR no está disponible
            configureFallbackMode();
        }
    }

    /**
     * Configura modo fallback si HDR no está disponible
     */
    private void configureFallbackMode() {
        try {
            Config config = session.getConfig();
            config.setLightEstimationMode(Config.LightEstimationMode.AMBIENT_INTENSITY);
            session.configure(config);

            currentMode = Config.LightEstimationMode.AMBIENT_INTENSITY;
            Log.d(TAG, "Configurado modo AMBIENT_INTENSITY como fallback");

        } catch (Exception e) {
            Log.e(TAG, "Error configurando estimación de luz: " + e.getMessage());
            currentMode = Config.LightEstimationMode.DISABLED;
        }
    }

    /**
     * Actualiza la iluminación según el frame actual
     */
    public void updateLighting(Frame frame) {
        if (frame == null || !isConfigured) return;

        try {
            // Obtener estimación de luz del frame actual (según guía ARCore)
            LightEstimate lightEstimate = frame.getLightEstimate();

            if (lightEstimate.getState() != LightEstimate.State.VALID) {
                return;
            }

            // Aplicar según el modo configurado
            switch (currentMode) {
                case ENVIRONMENTAL_HDR:
                    updateEnvironmentalHDR(lightEstimate);
                    break;
                case AMBIENT_INTENSITY:
                    updateAmbientIntensity(lightEstimate);
                    break;
                default:
                    // Modo deshabilitado, no hacer nada
                    break;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error actualizando iluminación: " + e.getMessage());
        }
    }

    /**
     * Actualiza iluminación en modo ENVIRONMENTAL_HDR
     */
    private void updateEnvironmentalHDR(LightEstimate lightEstimate) {
        try {
            // 1. Intentar obtener intensidad y dirección de la luz direccional principal
            try {
                float[] lightIntensity = lightEstimate.getEnvironmentalHdrMainLightIntensity();
                float[] lightDirection = lightEstimate.getEnvironmentalHdrMainLightDirection();

                if (lightIntensity != null && lightDirection != null) {
                    setDirectionalLightValues(lightIntensity, lightDirection);
                }
            } catch (Exception e) {
                Log.d(TAG, "Datos de luz direccional HDR no disponibles: " + e.getMessage());
            }

            // 2. Intentar obtener armónicos esféricos ambientales
            try {
                float[] sphericalHarmonics = lightEstimate.getEnvironmentalHdrAmbientSphericalHarmonics();
                if (sphericalHarmonics != null) {
                    setAmbientSphericalHarmonicsLightValues(sphericalHarmonics);
                }
            } catch (Exception e) {
                Log.d(TAG, "Armónicos esféricos HDR no disponibles: " + e.getMessage());
            }

            // 3. Intentar procesar cubemap HDR
            try {
                processHDRCubemap(lightEstimate);
            } catch (Exception e) {
                Log.d(TAG, "Cubemap HDR no disponible: " + e.getMessage());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error en modo HDR: " + e.getMessage());
            // Fallback a modo básico si hay problemas con HDR
            currentMode = Config.LightEstimationMode.AMBIENT_INTENSITY;
        }
    }

    /**
     * Actualiza iluminación en modo AMBIENT_INTENSITY (según guía oficial)
     */
    private void updateAmbientIntensity(LightEstimate lightEstimate) {
        try {
            // Obtener intensidad de píxeles del modo AMBIENT_INTENSITY
            float pixelIntensity = lightEstimate.getPixelIntensity();

            // Leer corrección de color en el array reutilizable (optimización de ARCore)
            lightEstimate.getColorCorrection(colorCorrection, 0);

            // Aplicar valores básicos
            applyAmbientIntensityLighting(pixelIntensity, colorCorrection);

        } catch (Exception e) {
            Log.e(TAG, "Error en modo AMBIENT_INTENSITY: " + e.getMessage());
        }
    }

    /**
     * Aplica valores de luz direccional principal
     */
    private void setDirectionalLightValues(float[] intensity, float[] direction) {
        try {
            // Verificar que los arrays tengan el tamaño esperado
            if (intensity == null || intensity.length < 3 || direction == null || direction.length < 3) {
                Log.w(TAG, "Arrays de luz direccional con tamaño incorrecto");
                return;
            }

            // Guardar dirección de luz
            currentLightDirection = new Vector3(direction[0], direction[1], direction[2]);

            // Calcular intensidad promedio
            currentLightIntensity = (intensity[0] + intensity[1] + intensity[2]) / 3.0f;
            currentLightIntensity = Math.max(0.1f, Math.min(currentLightIntensity, 2.0f)); // Normalizar

            Log.d(TAG, "Luz direccional actualizada - Dirección: " + currentLightDirection +
                    ", Intensidad: " + currentLightIntensity);

        } catch (Exception e) {
            Log.e(TAG, "Error aplicando luz direccional: " + e.getMessage());
        }
    }

    /**
     * Aplica valores de armónicos esféricos ambientales
     */
    private void setAmbientSphericalHarmonicsLightValues(float[] harmonics) {
        try {
            // Verificar que el array tenga el tamaño esperado (27 elementos: 9 coeficientes * 3 RGB)
            if (harmonics == null || harmonics.length < 3) {
                Log.w(TAG, "Array de armónicos esféricos con tamaño incorrecto");
                return;
            }

            // Extraer el primer coeficiente (DC) que representa la iluminación base
            float ambientR = Math.max(0.1f, Math.min(1.0f, Math.abs(harmonics[0])));
            float ambientG = Math.max(0.1f, Math.min(1.0f, Math.abs(harmonics[1])));
            float ambientB = Math.max(0.1f, Math.min(1.0f, Math.abs(harmonics[2])));

            // Guardar color ambiental actual
            currentAmbientColor = new Color(ambientR, ambientG, ambientB);

            Log.d(TAG, "Color ambiental actualizado: R=" + ambientR + ", G=" + ambientG + ", B=" + ambientB);

        } catch (Exception e) {
            Log.e(TAG, "Error aplicando armónicos esféricos: " + e.getMessage());
        }
    }

    /**
     * Procesa cubemap HDR para reflejos especulares
     */
    private void processHDRCubemap(LightEstimate lightEstimate) {
        try {
            // Obtener cubemap HDR en espacio de color lineal
            android.media.Image[] lightmaps = lightEstimate.acquireEnvironmentalHdrCubeMap();

            if (lightmaps != null && lightmaps.length == 6) {
                Log.d(TAG, "Cubemap HDR obtenido con " + lightmaps.length + " caras");

                // Procesar las 6 caras del cubemap
                for (int i = 0; i < lightmaps.length; i++) {
                    if (lightmaps[i] != null) {
                        // IMPORTANTE: Cerrar imagen después de usar (gestión de memoria)
                        lightmaps[i].close();
                    }
                }
            }

        } catch (Exception e) {
            // Cubemap HDR no disponible o error procesando
            Log.d(TAG, "Cubemap HDR no disponible: " + e.getMessage());
        }
    }

    /**
     * Aplica iluminación básica en modo AMBIENT_INTENSITY
     */
    private void applyAmbientIntensityLighting(float pixelIntensity, float[] colorCorrection) {
        try {
            // Verificar que el array de corrección de color tenga el tamaño correcto
            if (colorCorrection == null || colorCorrection.length < 3) {
                Log.w(TAG, "Array de corrección de color incorrecto");
                return;
            }


            float normalizedIntensity = Math.max(0.3f, Math.min(2.0f, pixelIntensity));

            // Calcular color ambiental con corrección
            float ambientR = Math.max(0.1f, Math.min(1.0f, colorCorrection[0] * normalizedIntensity));
            float ambientG = Math.max(0.1f, Math.min(1.0f, colorCorrection[1] * normalizedIntensity));
            float ambientB = Math.max(0.1f, Math.min(1.0f, colorCorrection[2] * normalizedIntensity));

            // Guardar valores actuales
            currentAmbientColor = new Color(ambientR, ambientG, ambientB);
            currentLightIntensity = normalizedIntensity;

            Log.d(TAG, "Iluminación ambiental aplicada - Intensidad: " + normalizedIntensity +
                    ", Color: R=" + ambientR + ", G=" + ambientG + ", B=" + ambientB);

        } catch (Exception e) {
            Log.e(TAG, "Error aplicando iluminación ambiental: " + e.getMessage());
        }
    }

    /**
     * Obtiene el color ambiental actual calculado
     */
    public Color getCurrentAmbientColor() {
        return currentAmbientColor;
    }

    /**
     * Obtiene la dirección de luz actual calculada
     */
    public Vector3 getCurrentLightDirection() {
        return currentLightDirection;
    }

    /**
     * Obtiene la intensidad de luz actual calculada
     */
    public float getCurrentLightIntensity() {
        return currentLightIntensity;
    }

    /**
     * Cambia el modo de estimación de luz dinámicamente
     */
    public void setLightEstimationMode(Config.LightEstimationMode mode) {
        if (session == null) return;

        try {
            Config config = session.getConfig();
            config.setLightEstimationMode(mode);
            session.configure(config);

            currentMode = mode;
            Log.d(TAG, "Modo de estimación cambiado a: " + mode);

        } catch (Exception e) {
            Log.e(TAG, "Error cambiando modo de estimación: " + e.getMessage());
        }
    }

    /**
     * Obtiene el modo actual de estimación de luz
     */
    public Config.LightEstimationMode getCurrentMode() {
        return currentMode;
    }

    /**
     * Verifica si la estimación de luz está configurada y funcionando
     */
    public boolean isLightEstimationActive() {
        return isConfigured && currentMode != Config.LightEstimationMode.DISABLED;
    }

    /**
     * Limpia recursos para evitar memory leaks
     */
    public void cleanup() {
        try {
            // Resetear valores a defaults
            currentAmbientColor = new Color(1.0f, 1.0f, 1.0f);
            currentLightDirection = new Vector3(0.0f, -1.0f, 0.0f);
            currentLightIntensity = 1.0f;

            Log.d(TAG, "Recursos de iluminación limpiados");

        } catch (Exception e) {
            Log.e(TAG, "Error limpiando recursos: " + e.getMessage());
        }
    }
}