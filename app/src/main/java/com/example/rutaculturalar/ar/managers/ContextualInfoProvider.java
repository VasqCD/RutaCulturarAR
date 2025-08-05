package com.example.rutaculturalar.ar.managers;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.PopupWindow;
import com.example.rutaculturalar.R;
import com.example.rutaculturalar.ar.interfaces.IContextualInfoProvider;

public class ContextualInfoProvider implements IContextualInfoProvider {
    private Activity activity;
    private PopupWindow infoPopup;
    private PopupWindow tooltipPopup;

    public ContextualInfoProvider(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void showCulturalInfo(String title, String description) {
        if (infoPopup != null && infoPopup.isShowing()) {
            infoPopup.dismiss();
        }

        View popupView = LayoutInflater.from(activity).inflate(R.layout.cultural_info_popup, null);
        TextView titleView = popupView.findViewById(R.id.cultural_title);
        TextView descriptionView = popupView.findViewById(R.id.cultural_description);
        android.widget.Button closeButton = popupView.findViewById(R.id.close_button);

        titleView.setText(title);
        descriptionView.setText(description);

        // Configurar el botón de cerrar
        closeButton.setOnClickListener(v -> {
            if (infoPopup != null && infoPopup.isShowing()) {
                infoPopup.dismiss();
            }
        });

        infoPopup = new PopupWindow(popupView,
            (int)(activity.getResources().getDisplayMetrics().density * 320),
            (int)(activity.getResources().getDisplayMetrics().density * 400),
            true);

        // Configurar el popup para que se pueda cerrar tocando fuera
        infoPopup.setOutsideTouchable(true);
        infoPopup.setFocusable(true);

        infoPopup.showAtLocation(activity.findViewById(android.R.id.content),
            android.view.Gravity.CENTER, 0, 0);
    }

    @Override
    public void hideCulturalInfo() {
        if (infoPopup != null && infoPopup.isShowing()) {
            infoPopup.dismiss();
        }
    }

    @Override
    public void showTooltip(String message, float x, float y) {
        if (tooltipPopup != null && tooltipPopup.isShowing()) {
            tooltipPopup.dismiss();
        }

        View tooltipView = LayoutInflater.from(activity).inflate(R.layout.tooltip_popup, null);
        TextView messageView = tooltipView.findViewById(R.id.tooltip_message);
        messageView.setText(message);

        tooltipPopup = new PopupWindow(tooltipView,
            (int)(activity.getResources().getDisplayMetrics().density * 200),
            (int)(activity.getResources().getDisplayMetrics().density * 50),
            false);

        tooltipPopup.showAtLocation(activity.findViewById(android.R.id.content),
            android.view.Gravity.NO_GRAVITY, (int)x, (int)y);
    }

    @Override
    public void hideTooltip() {
        if (tooltipPopup != null && tooltipPopup.isShowing()) {
            tooltipPopup.dismiss();
        }
    }
}
