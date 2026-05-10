package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class GrowthItemController {
    @FXML private Label lblName, lblAbsoluteGain, lblFromValue, lblPercentage;
    @FXML private ProgressBar pbGrowth;

    public void setData(String name, double startVal, double currentVal) {
        double increase = currentVal - startVal;
        double percent = (startVal == 0) ? 0 : (increase / startVal) * 100;

        lblName.setText(name);
        lblFromValue.setText("Từ: " + String.format("%,.0f", startVal));
        lblAbsoluteGain.setText("+" + String.format("%,.0f", increase) + " VND");

        // Logic xử lý màu sắc và biểu tượng mũi tên
        if (percent > 100) {
            // VƯỢT MỨC -> MÀU ĐỎ + MŨI TÊN
            lblPercentage.setText(String.format("+%.1f%% ↑", percent));
            lblPercentage.getStyleClass().add("text-growth-alert");
            lblAbsoluteGain.getStyleClass().add("text-growth-alert");
            pbGrowth.getStyleClass().add("progress-alert");
            pbGrowth.setProgress(1.0); // Full thanh
        } else {
            // BÌNH THƯỜNG -> MÀU XANH
            lblPercentage.setText(String.format("+%.1f%%", percent));
            lblPercentage.getStyleClass().add("text-growth-normal");
            lblAbsoluteGain.getStyleClass().add("text-growth-normal");
            pbGrowth.getStyleClass().add("progress-normal");
            pbGrowth.setProgress(percent / 100);
        }
    }
}