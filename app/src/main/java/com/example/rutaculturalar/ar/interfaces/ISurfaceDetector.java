package com.example.rutaculturalar.ar.interfaces;

import com.google.ar.core.Plane;

public interface ISurfaceDetector {
    void enableVerticalPlaneDetection(boolean enable);
    void enableHorizontalPlaneDetection(boolean enable);
    boolean isVerticalPlane(Plane plane);
    boolean isHorizontalPlane(Plane plane);
}
