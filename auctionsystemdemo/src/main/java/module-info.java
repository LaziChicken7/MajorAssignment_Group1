module com.auction {
    requires javafx.controls;
    requires javafx.fxml;

    // Thư viện gọi API và ép kiểu JSON
    requires java.net.http;
    requires com.google.gson;

    // Cho phép JavaFX truy cập vào thư mục view và root để load giao diện
    opens com.auction to javafx.fxml, javafx.graphics;
    opens com.auction.view to javafx.fxml;

    // CỰC KỲ QUAN TRỌNG: Cho phép FXML truy cập vào các Controller
    opens com.auction.controller to javafx.fxml;

    // CỰC KỲ QUAN TRỌNG 2: Cho phép Gson soi vào các file Model để gán data JSON
    opens com.auction.model to com.google.gson;
    exports com.auction.model; // Export để các class khác có thể dùng

    // Export các package để có thể chạy được
    exports com.auction;
    exports com.auction.controller;
    exports com.auction.util; // Mở thêm gói util để gọi ApiService và SessionManager
}