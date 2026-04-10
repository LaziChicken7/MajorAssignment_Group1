module com.auction {
    requires javafx.controls;
    requires javafx.fxml;

    // Cho phép JavaFX truy cập vào thư mục view và root để load giao diện
    opens com.auction to javafx.fxml, javafx.graphics;
    opens com.auction.view to javafx.fxml;

    // CỰC KỲ QUAN TRỌNG: Cho phép FXML truy cập vào các Controller của bạn
    opens com.auction.controller to javafx.fxml;

    // Export các package để có thể chạy được
    exports com.auction;
    exports com.auction.controller;
}