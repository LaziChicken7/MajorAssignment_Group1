package com.auction;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Giao diện mặc định khi bật app lên là Login
        scene = new Scene(loadFXML("view/Login"));
        stage.setTitle("Hệ Thống Đấu Giá Trực Tuyến");
        stage.setScene(scene);
        stage.show();
    }

    // Hàm tiện ích để đổi màn hình (Ví dụ: từ Login -> Home)
    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}