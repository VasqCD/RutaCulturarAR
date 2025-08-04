package com.example.rutaculturalar.ar.interfaces;

public interface IContextualInfoProvider {
    void showCulturalInfo(String title, String description);
    void hideCulturalInfo();
    void showTooltip(String message, float x, float y);
    void hideTooltip();
}
