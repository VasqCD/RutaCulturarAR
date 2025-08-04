package com.example.rutaculturalar.ar.managers;

import com.example.rutaculturalar.ar.interfaces.ISurfaceDetector;
import com.google.ar.core.Plane;
import com.google.ar.core.Config;
import com.google.ar.core.Session;

public class SurfaceDetector implements ISurfaceDetector {
    private Session arSession;
    private boolean verticalPlanesEnabled = true;
    private boolean horizontalPlanesEnabled = true;

    public SurfaceDetector(Session arSession) {
        this.arSession = arSession;
        configureSession();
    }

    private void configureSession() {
        if (arSession != null) {
            Config config = arSession.getConfig();
            config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
            arSession.configure(config);
        }
    }

    @Override
    public void enableVerticalPlaneDetection(boolean enable) {
        this.verticalPlanesEnabled = enable;
        updatePlaneFindingMode();
    }

    @Override
    public void enableHorizontalPlaneDetection(boolean enable) {
        this.horizontalPlanesEnabled = enable;
        updatePlaneFindingMode();
    }

    private void updatePlaneFindingMode() {
        if (arSession != null) {
            Config config = arSession.getConfig();
            if (verticalPlanesEnabled && horizontalPlanesEnabled) {
                config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
            } else if (horizontalPlanesEnabled) {
                config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
            } else if (verticalPlanesEnabled) {
                config.setPlaneFindingMode(Config.PlaneFindingMode.VERTICAL);
            } else {
                config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
            }
            arSession.configure(config);
        }
    }

    @Override
    public boolean isVerticalPlane(Plane plane) {
        return plane.getType() == Plane.Type.VERTICAL;
    }

    @Override
    public boolean isHorizontalPlane(Plane plane) {
        return plane.getType() == Plane.Type.HORIZONTAL_UPWARD_FACING ||
               plane.getType() == Plane.Type.HORIZONTAL_DOWNWARD_FACING;
    }
}
