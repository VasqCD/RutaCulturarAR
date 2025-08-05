package com.example.rutaculturalar.ar.interfaces;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;

public interface IAR3DInfoProvider {
    void show3DInfo(String title, String description, Node parentNode, Vector3 offset);
    void hide3DInfo();
    void update3DInfoPosition(Vector3 newPosition);
    boolean isInfoVisible();
}
