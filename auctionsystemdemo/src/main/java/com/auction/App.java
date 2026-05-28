package com.auction;


import lombok.extern.slf4j.Slf4j;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;

@Slf4j
public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // SỬA DẤU CHẤM THÀNH DẤU GẠCH CHÉO Ở ĐÂY
        URL fxmlLocation = getClass().getResource("/com/auction/view/dashboard/Login.fxml");

        if (fxmlLocation == null) {
            log.info("Lỗi: Không tìm thấy file Main.fxml. Hãy kiểm tra lại thư mục resources!");
            return;
        }
        Image logo = new Image(getClass().getResourceAsStream("/com/auction/view/logo.png"));
        primaryStage.getIcons().add(logo);

        Parent root = FXMLLoader.load(fxmlLocation);
        Scene scene = new Scene(root);

        primaryStage.setTitle("Hệ thống đấu giá trực tuyến");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}