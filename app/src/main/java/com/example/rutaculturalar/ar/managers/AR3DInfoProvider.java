package com.example.rutaculturalar.ar.managers;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.rutaculturalar.ar.interfaces.IAR3DInfoProvider;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

public class AR3DInfoProvider implements IAR3DInfoProvider {
    private Context context;
    private Node infoNode;
    private ViewRenderable infoRenderable;
    private boolean isVisible = false;

    public AR3DInfoProvider(Context context) {
        this.context = context;
    }

    @Override
    public void show3DInfo(String title, String description, Node parentNode, Vector3 offset) {
        hide3DInfo(); // Ocultar info anterior si existe

        // Crear layout para la información 3D
        LinearLayout infoLayout = createInfoLayout(title, description);

        // Crear ViewRenderable para mostrar en AR
        ViewRenderable.builder()
                .setView(context, infoLayout)
                .build()
                .thenAccept(renderable -> {
                    infoRenderable = renderable;

                    // Crear nodo para la información
                    infoNode = new Node();
                    infoNode.setParent(parentNode);
                    infoNode.setRenderable(renderable);

                    // Posicionar la información flotante
                    infoNode.setLocalPosition(offset);

                    // Escalar el nodo del popup para hacerlo más pequeño
                    infoNode.setLocalScale(new Vector3(0.6f, 0.6f, 0.6f));


                    isVisible = true;
                })
                .exceptionally(error -> {
                    error.printStackTrace();
                    return null;
                });
    }

    private LinearLayout createInfoLayout(String title, String description) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.argb(220, 255, 255, 255));
        layout.setPadding(20, 16, 20, 16);
        layout.setGravity(Gravity.CENTER);

        // Título
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(12);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(Color.parseColor("#1E88E5"));
        titleView.setGravity(Gravity.CENTER);
        titleView.setMaxLines(2);

        // Descripción
        TextView descView = new TextView(context);
        descView.setText(description);
        descView.setTextSize(9);
        descView.setTextColor(Color.parseColor("#333333"));
        descView.setGravity(Gravity.CENTER);
        descView.setMaxLines(10);
        descView.setLineSpacing(2, 1.0f);
        descView.setPadding(6, 6, 6, 0);

        layout.addView(titleView);
        layout.addView(descView);

        // Tamaño ajustado
        layout.setLayoutParams(new LinearLayout.LayoutParams(
            (int)(context.getResources().getDisplayMetrics().density * 168),
            (int)(context.getResources().getDisplayMetrics().density * 120)
        ));

        return layout;
    }

    @Override
    public void hide3DInfo() {
        if (infoNode != null) {
            infoNode.setParent(null);
            infoNode = null;
        }
        if (infoRenderable != null) {
            infoRenderable = null;
        }
        isVisible = false;
    }

    @Override
    public void update3DInfoPosition(Vector3 newPosition) {
        if (infoNode != null) {
            infoNode.setLocalPosition(newPosition);
        }
    }

    @Override
    public boolean isInfoVisible() {
        return isVisible;
    }
}
