package com.auction.util;

import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import java.util.Objects;

public class AlertUtils {

    // Dùng chung 1 hàm duy nhất cho cả Alert và TextInputDialog
    public static void applyStyle(Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();

        try {
            // ĐƯỜNG DẪN CHUẨN ĐẾN FILE CSS
            String cssPath = Objects.requireNonNull(AlertUtils.class.getResource("/com/auction/view/style.css")).toExternalForm();

            // Tránh add trùng lặp CSS nếu mở nhiều lần
            if (!dialogPane.getStylesheets().contains(cssPath)) {
                dialogPane.getStylesheets().add(cssPath);
            }
        } catch (Exception e) {
            System.err.println("Không tìm thấy file CSS cho Dialog! " + e.getMessage());
        }

        // Gắn class định dạng dùng chung cho mọi hộp thoại
        if (!dialogPane.getStyleClass().contains("custom-alert")) {
            dialogPane.getStyleClass().add("custom-alert");
        }

        // Tự động chuyển đổi Light/Dark mode
        if (SessionManager.isDarkMode) {
            if (!dialogPane.getStyleClass().contains("dark-theme")) {
                dialogPane.getStyleClass().add("dark-theme");
            }
        } else {
            dialogPane.getStyleClass().remove("dark-theme");
        }
    }
}