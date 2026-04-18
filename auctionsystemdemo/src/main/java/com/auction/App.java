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
        // ĐỔI DÒNG NÀY: Thay "view/Login" thành "view/Home"
        // Thêm kích thước 1280x720 để màn hình hiển thị rộng rãi giống bản thiết kế
        scene = new Scene(loadFXML("view/Login"), 1280, 720);

        stage.setTitle("Hệ Thống Đấu Giá Trực Tuyến");
        stage.setScene(scene);
        stage.centerOnScreen(); // Hiển thị cửa sổ ở giữa màn hình desktop
        stage.show();
    }

    // Hàm tiện ích để đổi màn hình
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