module com.auction {
    requires javafx.controls;
    requires javafx.fxml;

    // Thư viện gọi API và ép kiểu JSON
    requires java.net.http;
    requires com.google.gson;
    requires javafx.swing;

    // Cho phép JavaFX truy cập vào thư mục view và root để load giao diện
    opens com.auction to javafx.fxml, javafx.graphics;

    // (Lưu ý: Nếu thư mục view của bạn cũng không có file .java nào trực tiếp bên trong, bạn cũng phải xóa dòng dưới này đi)
    opens com.auction.view to javafx.fxml;

    // CỰC KỲ QUAN TRỌNG 2: Cho phép Gson soi vào các file Model để gán data JSON
    opens com.auction.model to com.google.gson;
    exports com.auction.model; // Export để các class khác có thể dùng

    // Export các package để có thể chạy được
    exports com.auction;
    exports com.auction.util;

    // ĐÃ XÓA 2 DÒNG com.auction.controller Ở ĐÂY VÌ NÓ RỖNG

    // Khai báo chính xác từng package con có chứa Controller
    exports com.auction.controller.addauctionitem;
    opens com.auction.controller.addauctionitem to javafx.fxml;

    exports com.auction.controller.notification;
    opens com.auction.controller.notification to javafx.fxml;

    exports com.auction.controller.wallet;
    opens com.auction.controller.wallet to javafx.fxml;

    exports com.auction.controller.profile;
    opens com.auction.controller.profile to javafx.fxml;

    exports com.auction.controller.home;
    opens com.auction.controller.home to javafx.fxml;

    exports com.auction.controller.auction;
    opens com.auction.controller.auction to javafx.fxml;

    exports com.auction.controller.dashboard;
    opens com.auction.controller.dashboard to javafx.fxml;
}