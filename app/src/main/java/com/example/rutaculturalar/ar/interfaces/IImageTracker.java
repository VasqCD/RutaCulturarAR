package com.example.rutaculturalar.ar.interfaces;

import com.google.ar.core.AugmentedImage;

public interface IImageTracker {
    void setupImageDatabase(String[] imageNames);
    void enableImageTracking(boolean enable);
    void onImageDetected(AugmentedImage augmentedImage);
    void onImageLost(AugmentedImage augmentedImage);
    boolean isImageBeingTracked(String imageName);
}
