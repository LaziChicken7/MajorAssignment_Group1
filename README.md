# AuctionBidSystem - Major Assignment Group 1

**Auction System** xây dựng bằng Java, Backend sử dụng **Spring Boot** và Frontend sử dụng **JavaFX**

##  **Mô tả dự án**
Hệ thống cho phép:
- ✅ Quản lý người dùng (Users) với 3 roles: Admin, Seller, Bidder
- ✅ Quản lý sản phẩm đấu giá (Auction Items)
- ✅ Tạo và quản lý phiên đấu giá (Auction Sessions)
- ✅ Đặt giá (Bidding) trong phiên đấu giá
- ✅ Quản lý thanh toán (Payment) và ví tiền (Wallet)
- ✅ Hệ thống thông báo Real-time (Notifications)
- ✅ Upload ảnh sản phẩm (Seller) và avatar người dùng
- ✅ WebSocket Real-time Updates
---

## Members
  * **25022036 _ Vũ Đức Trung**   - Lead Backend
  * **25022006 _ Phan Văn Tuyến** - Developer Backend
  * **25021609 _ Hồ Tú An**       - Developer JavaFX Frontend
  * **25022014 _ Đậu Thu Thảo**   - Developer JavaFX Frontend
---

## **Công nghệ sử dụng**

  ### Backend
  - **Ngôn ngữ:** Java 21/25
  - **Framework:** Spring Boot 4.0.5
  - **ORM:** Spring Data JPA
  - **Database:** MySQL 8.0+
  - **Build Tool:** Maven
  - **WebSocket:** Spring WebSocket + Tyrus
  - **Security:** Spring Security

  ### Frontend
  - **Ngôn ngữ:** Java 17+
  - **UI Framework:** JavaFX 17.0.6
  - **HTTP Client:** HttpClient (Java built-in)
  - **WebSocket Client:** Tyrus WebSocket
  - **JSON Serialization:** Gson, Jackson
  - **Build Tool:** Maven

  ### Cơ sở dữ liệu
  - **Database:** MySQL 8.0+
  - **Port:** 3306
  ---

## **Cấu trúc thư mục**

  ```
  MajorAssignment_Group1/
  ├── AuctionBidSystemSpringBootRework/     ← Backend Spring Boot
  │   ├── src/
  │   ├── pom.xml
  │   └── target/
  │       └── server.jar                    ← Fat JAR chạy được
  ├── auctionsystemdemo/                    ← Frontend JavaFX
  │   ├── src/
  │   ├── pom.xml
  │   └── target/
  │       └── client.jar                    ← Fat JAR chạy được
  ├── docs/                                 ← Tài liệu
  │   └── diagram
  ├── server.jar                            ← Copy tại root (dễ chạy)
  ├── client.jar                            ← Copy tại root (dễ chạy)
  ├── README.md                             ← File này
  └── .git/                                 ← Git repository
  ```

## 📍 **Vị trí các file JAR**

  Root Directory:
  - server.jar    (~62 MB)  - Backend Server executable
  - client.jar    (~24 MB)  - Frontend Client executable
  ---

## **Yêu cầu cài đặt**

  ### Bắt buộc
  - **Java 17+** (đủ để chạy cả `server.jar` lẫn `client.jar`)
  - **Java 21+** (bắt buộc nếu muốn **tự build lại** server từ source)
  - **MySQL 8.0+** (hoặc version tương thích)
  - **XAMPP Control Panel** (để quản lý MySQL) - *Optional*

### Kiểm tra phiên bản Java

  ```bash
  java -version
  # Output: openjdk version "17.x" hoặc cao hơn
  ```

### Kiểm tra MySQL

  ```bash
  mysql --version
  # Output: mysql  Ver 8.x hoặc tương tự
  ```

##  **Hướng dẫn chạy**

  ### **Bước 1 : Khởi động MySQL**

    **Cách 1: Dùng XAMPP Control Panel** (Nếu có)
    ```
    1. Mở XAMPP Control Panel v3.3.0
    2. Click "Start" nút MySQL
    3. Chờ trạng thái "Running" (màu xanh)
    ```

    **Cách 2: Dùng MySQL Command Line**
    ```bash
    mysql -u root -p
    # Nhập password MySQL (mặc định: root)
    ```

  ### **Bước 2 : Khởi động Backend Server** (Terminal 1)

    ```bash
    java -jar server.jar
    ```

    **Output mong đợi:**
    ```
    2026-05-25 16:00:00.000  INFO 1234 --- [ main] o.s.b.w.embedded.tomcat.TomcatWebServer  : 
    Tomcat started on port(s): 8080 (http) with context path: '/auction'
    ```

    **Kiểm tra Server:**
    ```bash
    curl http://localhost:8080/auction
    ```

  ### **Bước 3 : Chạy Client #1** (Terminal 2)

    ```bash
    java -jar client.jar
    ```

    Giao diện JavaFX sẽ mở ra, bạn có thể:
    - Đăng ký tài khoản < mặc định là bidder _ muốn upseller phải qua admin >
    - Đăng nhập < đã có tài khoản >

  ### **Bước 4 : Chạy thêm Client #2** (Terminal 3)

    Mở terminal mới và chạy lệnh tương tự:
    ```bash
    java -jar client.jar
    ```

## **Cấu hình cơ sở dữ liệu**

### Thông tin kết nối mặc định

  ```yaml
  Database: AuctionBidSystemTest
  Host: localhost
  Port: 3306
  User: root
  Password: root
  ```

### File cấu hình

  Đường dẫn: `AuctionBidSystemSpringBootRework/src/main/resources/application.yaml`

  ```yaml
  spring:
    datasource:
      url: jdbc:mysql://localhost:3306/AuctionBidSystemTest?createDatabaseIfNotExist=true
      username: root
      password: root
  ```

### Tạo Database tự động

  Server sẽ tự động tạo database nếu chưa có (thanks to `?createDatabaseIfNotExist=true`).

---

## **API Chính của Backend**

  Backend chạy tại: `http://localhost:8080/auction`

### Quản lý User
  - `POST   /auction/users/register` - Đăng ký tài khoản mới (mặc định role: BIDDER)
  - `POST   /auction/users/login` - Đăng nhập
  - `GET    /auction/users/profile/{userName}` - Xem thông tin cá nhân
  - `PUT    /auction/users/profile/{userName}` - Cập nhật thông tin cá nhân
  - `PUT    /auction/users/upgrade-to-seller/{userName}` - Nâng cấp lên Seller (Admin)
  - `PUT    /auction/users/reset-password` - Đổi mật khẩu mới
  - `POST   /auction/users/verify-reset-info` - Xác thực thông tin quên mật khẩu
  - `POST   /auction/users/{userName}/avatar` - Upload avatar
  - `DELETE /auction/users/{userName}/avatar` - Xóa avatar về mặc định
  - `GET    /auction/users/{userName}/status` - Kiểm tra trạng thái online
  - `POST   /auction/users/reviews` - Viết đánh giá seller
  - `GET    /auction/users/{sellerUsername}/reviews` - Xem đánh giá seller
  - `GET    /auction/users/search?keyword=` - Tìm kiếm người dùng
  - `GET    /auction/users/admin` - Lấy toàn bộ danh sách user (Admin)
  - `PUT    /auction/users/admin/{userName}/ban` - Khóa/Mở khóa tài khoản (Admin)
  - `DELETE /auction/users/admin/{userName}` - Xóa user (Admin)

### Quản lý Item
  - `POST   /auction/items/create` - Tạo auction item (Seller)
  - `POST   /auction/items/{itemId}/upload-images` - Upload ảnh sản phẩm
  - `PUT    /auction/items/cancel/{itemId}` - Hủy item

### Quản lý Auction
  - `POST   /auction/auctions/create` - Tạo phiên đấu giá
  - `GET    /auction/auctions` - Lấy danh sách tất cả phiên đấu giá
  - `GET    /auction/auctions/{auctionId}` - Lấy chi tiết 1 phiên đấu giá
  - `GET    /auction/auctions/my-auctions?username=` - Danh sách phiên của Seller
  - `PUT    /auction/auctions/{auctionId}/start` - Bắt đầu phiên đấu giá
  - `PUT    /auction/auctions/{auctionId}/close` - Đóng phiên đấu giá
  - `PUT    /auction/auctions/{auctionId}/cancel` - Hủy phiên đấu giá (Admin)
  - `POST   /auction/auctions/{auctionId}/place-bid` - Đặt giá
  - `PUT    /auction/auctions/{auctionId}/accept-payment` - Chấp nhận thanh toán (Seller)
  - `PUT    /auction/auctions/{auctionId}/decline-payment` - Từ chối thanh toán (Seller)
  - `POST   /auction/auctions/{auctionId}/setup-autobid` - Cài đặt tự động đặt giá
  - `GET    /auction/auctions/{auctionId}/my-autobid?username=` - Xem cấu hình autobid
  - `GET    /auction/auctions/{auctionId}/price-chart` - Lấy dữ liệu biểu đồ giá
  - `GET    /auction/auctions/search?keyword=` - Tìm kiếm phiên đấu giá

### Quản lý Payment / Ví tiền
  - `POST   /auction/payments/deposit` - Nạp tiền vào ví
  - `POST   /auction/payments/withdraw` - Rút tiền từ ví
  - `GET    /auction/payments/{userName}/history` - Xem ví tiền và lịch sử giao dịch

### Thông báo
  - `GET    /auction/notifications/{userName}` - Lấy danh sách thông báo
  - `DELETE /auction/notifications/{notificationId}` - Xóa 1 thông báo
  - `DELETE /auction/notifications/all/{userName}` - Xóa tất cả thông báo
  - `PUT    /auction/notifications/{notificationId}/accept` - Chấp nhận yêu cầu thanh toán
  - `PUT    /auction/notifications/{notificationId}/decline` - Từ chối yêu cầu thanh toán

### Chat
  - `GET    /auction/chat/history/{userA}/{userB}` - Lấy lịch sử hội thoại
  - `WebSocket ws://localhost:8080/auction/ws-chat` - Kết nối STOMP real-time
  - `STOMP  /app/chat.send` - Gửi tin nhắn
  - `STOMP  /topic/messages/{username}` - Nhận tin nhắn real-time

---

## ✅ **Danh sách chức năng hoàn thiện**

  ### Authentication & Authorization
    - ✅ Đăng ký (Register) - 3 roles: Admin, Seller, Bidder
    - ✅ Đăng nhập (Login) - Lưu session
    - ✅ Nâng cấp Bidder thành Seller
    - ✅ Quản lý phân quyền (Role-based)

  ### Quản lý Sản phẩm & Đấu giá
    - ✅ Tạo Auction Item (Seller)
    - ✅ Upload hình ảnh sản phẩm
    - ✅ Tạo phiên đấu giá (Auction Session)
    - ✅ Bắt đầu/Đóng/Hủy phiên đấu giá
    - ✅ Đặt giá (Bidding) - Xác thực bid hợp lệ
    - ✅ Xem lịch sử đấu giá

  ### Quản lý Thanh toán
    - ✅ Nạp tiền vào ví (Deposit)
    - ✅ Rút tiền từ ví (Withdraw)
    - ✅ Chấp nhận/Từ chối thanh toán (Payment)
    - ✅ Xem lịch sử giao dịch ví

  ### Tính năng Real-time
    - ✅ WebSocket - Thông báo real-time
    - ✅ Cập nhật trạng thái phiên đấu giá
    - ✅ Thông báo khi có bid mới
    - ✅ Chat/Thông báo giữa người dùng

  ### Frontend JavaFX
    - ✅ Giao diện đăng nhập/đăng ký
    - ✅ Dashboard chính
    - ✅ Danh sách các phiên đấu giá
    - ✅ Chi tiết phiên đấu giá
    - ✅ Quản lý item của Seller
    - ✅ Xem ví tiền
    - ✅ Hệ thống thông báo sidebar
---

## 📝 **Ghi chú quan trọng**

  1. **Database tự động:** Server sẽ tự động tạo database `AuctionBidSystemTest` lần đầu chạy
  2. **Schema:** File `schema.sql` chứa cấu trúc bảng (nếu cần tạo manual)
  3. **Upload folder:** Ảnh upload được lưu trong `uploads/`
  4. **Port mặc định:**
    - Backend: `8080`
    - MySQL: `3306`
  5. **Timezone:** Hệ thống dùng timezone mặc định của server
---

## 🔧 **Troubleshooting**

### ❌ Lỗi: "Connection refused MySQL"
  **Giải pháp:**
  - Kiểm tra MySQL đã start chưa (XAMPP hoặc command line)
  - Kiểm tra username/password trong `application.yaml`
  - Mặc định: user=`root`, password=`root`

### ❌ Lỗi: "Port 8080 already in use"
  **Giải pháp:**
  ```powershell
  # Windows: Tìm process dùng port 8080
  netstat -ano | findstr :8080
  # Kill process theo PID tìm được
  taskkill /PID <PID> /F
  ```
  ```bash
  # Linux/macOS:
  lsof -i :8080
  kill -9 <PID>
  ```

### ❌ Lỗi: JavaFX không chạy
  **Giải pháp:**
  - Kiểm tra Java version >= 17
  - Kiểm tra backend server đã start chưa
  - Kiểm tra firewall cho phép port 8080

### ❌ JAR file "not found"
  **Giải pháp:**
  - Chắc chắn JAR files ở trong thư mục hiện tại
  - Dùng đường dẫn tuyệt đối: `java -jar /full/path/to/server.jar`

---

## **Testing Scenarios**

### Scenario 1: Basic Auction Flow
  1. **Terminal 1:** `java -jar server.jar`
  2. **Terminal 2:** `java -jar client.jar` → Register Admin
  3. **Terminal 3:** `java -jar client.jar` → Register Seller + Upgrade
  4. **Terminal 4:** `java -jar client.jar` → Register Bidder
  5. Seller tạo item & phiên đấu giá
  6. Bidder tham gia & đặt giá
  7. Seller đóng phiên → Bidder thanh toán

### Scenario 2: Concurrent Bidding
  1. Chạy 2-3 client cùng lúc
  2. Cùng đặt giá trên 1 phiên
  3. Kiểm tra xem bid nào được chấp nhận

### Scenario 3: Real-time Notification
  1. Mở 2 client
  2. Client A: Đặt giá
  3. Client B: Nhận thông báo real-time
  4. Chat message hoặc system notification
---

## 📄 **Liên kết tài liệu**

-  **Báo cáo chi tiết:** [PDF Report](chua co)
-  **Video Demo:** [Demo Video](chua co)
-  **Kiến trúc hệ thống:** [Architecture](chua co)
---

## 🎓 **Lập trình nâng cao (LTNC) - Group 1**

**Đại học:** [Trường đại học công nghệ]  
**Môn học:** Lập trình Nâng cao  
**Deadline:** 31/05/2026 23:59
---

##  **FAQ**

**Q: Tôi không có MySQL, có cách nào khác không?**  
A: Bạn có thể dùng Docker:
```bash
docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root mysql:8.0
```

**Q: Làm sao debug nếu có lỗi?**  
A: Kiểm tra logs ở folder `logs/` hoặc console output khi chạy JAR

**Q: Có thể build lại JAR không?**  
A: Có. Cần **Java 21+** và chạy theo thứ tự:

**Build Server** (trong thư mục `AuctionBidSystemSpringBootRework/`):
```powershell
# Windows
cd AuctionBidSystemSpringBootRework
mvnw.cmd clean package -DskipTests
```
```bash
# Linux/macOS
cd AuctionBidSystemSpringBootRework
./mvnw clean package -DskipTests
```
Output: `AuctionBidSystemSpringBootRework/target/server.jar`

**Build Client** (trong thư mục `auctionsystemdemo/`):
```powershell
# Windows
cd auctionsystemdemo
mvnw.cmd clean package -DskipTests
```
```bash
# Linux/macOS
cd auctionsystemdemo
./mvnw clean package -DskipTests
```
Output: `auctionsystemdemo/target/client.jar`

---

## **Liên hệ & Support**
Nếu có vấn đề, liên hệ các thành viên nhóm hoặc tạo issue trên GitHub.
* **25022036@vnu.edu.vn**
* **25022006@vnu.edu.vn**
* **25021609@vnu.edu.vn** 
* **25022014@vnu.edu.vn**
---
 
**Version:** 1.0-SNAPSHOT
