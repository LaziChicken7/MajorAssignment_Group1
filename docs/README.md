# AuctionBidSystem - Major Assignment Group 1

## Mô tả dự án
Dự án là hệ thống đấu giá trực tuyến (E-Auction System) xây dựng bằng Java, trong đó backend sử dụng Spring Boot và frontend sử dụng JavaFX.
Hệ thống cho phép quản lý người dùng, sản phẩm đấu giá, phiên đấu giá, đặt giá, thanh toán và thông báo với phân quyền rõ ràng.

## Thành viên nhóm
* **Vũ Đức Trung** - Lead Backend
* **Phan Văn Tuyến** - Developer Backend
* **Hồ Tú An** - Developer JavaFX Frontend
* **Đậu Thu Thảo** - Developer JavaFX Frontend

## Kiến trúc và công nghệ
* **Ngôn ngữ:** Java 21,25
* **Backend:** Spring Boot 4.0.5
* **ORM:** Spring Data JPA
* **Cơ sở dữ liệu:** MySQL
* **Build:** Maven (`mvnw`, `mvnw.cmd`)
* **Frontend:** JavaFX
* **Kiến trúc:** MVC, Layered Architecture, Factory Pattern

## Thư mục chính
* `AuctionBidSystemSpringBootRework/` - backend Spring Boot
* `auctionsystemdemo/` - frontend JavaFX 

## Chức năng đã hoàn thiện
* Đăng ký, đăng nhập và quản lý user
* Phân quyền Admin / Seller / Bidder
* Nâng cấp Bidder thành Seller
* Tạo sản phẩm đấu giá (Item)
* Upload ảnh sản phẩm và avatar user
* Tạo phiên đấu giá, bắt đầu, đóng, hủy phiên đấu giá
* Đặt giá (bid) cho phiên đấu giá
* Thanh toán, chấp nhận hoặc từ chối trả tiền
* Nạp tiền, rút tiền và xem lịch sử ví
* Quản lý thông báo cho user

## Backend API chính
Backend chạy với context path: `/auction`

Một số API tiêu biểu:
* `POST /auction/users/register` - đăng ký tài khoản
* `POST /auction/users/login` - đăng nhập
* `PUT /auction/users/profile/{userName}` - cập nhật profile
* `PUT /auction/users/upgrade-to-seller/{userName}` - nâng cấp Seller
* `POST /auction/items/create` - tạo item mới
* `POST /auction/items/{itemId}/upload-images` - upload ảnh item
* `PUT /auction/items/cancel/{itemId}` - Admin hủy item
* `POST /auction/auctions/create` - tạo phiên đấu giá
* `PUT /auction/auctions/{auctionId}/start` - bắt đầu đấu giá
* `PUT /auction/auctions/{auctionId}/close` - đóng đấu giá
* `POST /auction/auctions/{auctionId}/place-bid` - đặt giá
* `PUT /auction/auctions/{auctionId}/accept-payment` - chấp nhận thanh toán
* `PUT /auction/auctions/{auctionId}/decline-payment` - từ chối thanh toán
* `POST /auction/payments/deposit` - nạp tiền
* `POST /auction/payments/withdraw` - rút tiền
* `GET /auction/payments/{userName}/history` - xem ví và lịch sử giao dịch
* `GET /auction/notifications/{userName}` - lấy thông báo
* `POST /auction/files/upload` - upload file

## Cài đặt và chạy dự án
### Yêu cầu
* Java 21,25
* MySQL
* Maven hoặc dùng `mvnw`, `mvnw.cmd`

### Cấu hình MySQL
Mặc định kết nối đến database:
`jdbc:mysql://localhost:3306/AuctionBidSystemTest?createDatabaseIfNotExist=true`

File cấu hình:
* `AuctionBidSystemSpringBootRework/src/main/resources/application.yaml`

### Khởi động backend
Mở terminal tại `AuctionBidSystemSpringBootRework/` và chạy:
```powershell
mvnw.cmd spring-boot:run
```
Hoặc:
```powershell
mvn spring-boot:run
```

Sau khi chạy, truy cập API tại:
`http://localhost:8080/auction`

## Ghi chú
* Dự án dùng `spring.jpa.hibernate.ddl-auto: none` nên không tự động tạo bảng.<muốn tạo bảng mới thì sửa application.yaml >
* `schema.sql` chứa cấu trúc dữ liệu và bảng khởi tạo.
* Ảnh upload được lưu trong thư mục `uploads/`.
* Frontend JavaFX hiện có trong `auctionsystemdemo/` và `Frontend/auctionsystemdemo/`.

## Hướng phát triển tiếp
* Hoàn thiện kết nối JavaFX với backend
* Thêm xác thực bảo mật JWT/Session
* Cân nhắc realtime cho đấu giá
* Hoàn thiện trải nghiệm thông báo và giao dịch
* Hoàn thiện thêm các tính năng nâng cao
