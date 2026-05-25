# AuctionBidSystem – Major Assignment Group 1

**Hệ thống đấu giá trực tuyến theo mô hình Client-Server**, xây dựng bằng Java.  
Backend dùng **Spring Boot** (REST API + WebSocket), Frontend dùng **JavaFX**.

---

## Thành viên nhóm
| MSSV     | Họ tên         | Vai trò                   |
|----------|----------------|---------------------------|
| 25022036 | Vũ Đức Trung   | Lead Backend              |
| 25022006 | Phan Văn Tuyến | Developer Backend         |
| 25021609 | Hồ Tú An       | Developer JavaFX Frontend |
| 25022014 | Đậu Thu Thảo   | Developer JavaFX Frontend |
---

## Mô tả bài toán

Hệ thống cho phép nhiều người dùng tham gia **đấu giá trực tuyến** theo thời gian thực:
- **Admin** quản lý tài khoản, phân quyền người dùng.
- **Seller** đăng sản phẩm, tạo phiên đấu giá, xác nhận thanh toán.
- **Bidder** tham gia đặt giá, quản lý ví tiền, nhận thông báo real-time.

Hệ thống hỗ trợ nhiều client kết nối đồng thời đến một server duy nhất qua mạng LAN hoặc localhost.
---

## Công nghệ sử dụng

### Backend
| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot 4.x |
| ORM | Spring Data JPA (Hibernate) |
| Database | MySQL 8.0+ |
| Real-time | Spring WebSocket + STOMP |
| Security | Spring Security |
| Build | Maven (`maven-shade-plugin` → fat JAR) |

### Frontend
| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 17+ |
| UI Framework | JavaFX 17 |
| HTTP Client | Java HttpClient (built-in) |
| WebSocket | Tyrus WebSocket Client |
| JSON | Gson, Jackson |
| Build | Maven (`maven-shade-plugin` → fat JAR) |

### Môi trường chạy / Yêu cầu cài đặt

| Yêu cầu | Phiên bản |
|---|---|
| **Java JRE/JDK** | **17+** (đủ để chạy cả 2 file JAR) |
| **MySQL** | 8.0+ (chạy trên máy server) |

```bash
# Kiểm tra Java
java -version
# Output mong đợi: openjdk version "17.x.x" hoặc cao hơn
```

---

##  Cấu trúc thư mục

```
MajorAssignment_Group1/
├── AuctionBidSystemSpringBootRework/   ← Module Backend (Spring Boot)
│   ├── src/main/java/                  ← Source code Java
│   ├── src/main/resources/
│   │   └── application.yaml           ← Cấu hình DB, port, ...
│   └── pom.xml
│
├── auctionsystemdemo/                  ← Module Frontend (JavaFX)
│   ├── src/main/java/                  ← Source code Java + FXML
│   └── pom.xml
│
├── docs/                               ← Tài liệu, diagram
├── .github/workflows/backend-ci.yml   ← CI/CD tự động build & release JAR
└── README.md
```
---

##  Vị trí các file JAR

> **CI/CD tự động build và đẩy JAR lên GitHub Releases sau mỗi lần push lên `main`.**  
> Không cần tự build — tải thẳng từ Releases là chạy được.

*** Tải tại:** [https://github.com/LaziChicken7/MajorAssignment_Group1/releases](https://github.com/LaziChicken7/MajorAssignment_Group1/releases)

| File         | Mô tả                                                    |
|--------------|----------------------------------------------------------|
| `server.jar` | Backend Spring Boot – fat JAR                            |
| `client.jar` | Frontend JavaFX – fat JAR                                |

Sau khi tải, để 2 file ở bất kỳ thư mục nào tùy thích — **không cần nằm trong project**.

---

## Hướng dẫn chạy

### Bước 1 – Khởi động MySQL

Đảm bảo MySQL đang chạy trên máy chạy server (port `3306`).  
Dùng XAMPP, MySQL Workbench, hoặc command line đều được.

> Server sẽ **tự động tạo database** `AuctionBidSystemTest` nếu chưa có.  
> Thông tin kết nối mặc định: `username = root`, `password = root`.

### Bước 2 – Chạy Server (Terminal 1)

Mở terminal tại thư mục chứa `server.jar`:

```bash
java -jar server.jar
```

Server khởi động thành công khi thấy log:
```
Tomcat started on port(s): 8080 (http)
```

> 💡 **Máy nào chạy `server.jar` thì máy đó là server.**  
> Ghi nhớ **IP của máy này** (dùng `ipconfig` trên Windows hoặc `ifconfig` trên Linux/macOS).

### Bước 3 – Chạy Client (Terminal 2, 3, ...)

Mở terminal tại thư mục chứa `client.jar`:

```bash
java -jar client.jar
```

- Giao diện đăng nhập sẽ hiện ra.
- Nhập **IP của máy đang chạy server** vào ô Server IP.
- Nếu chạy cùng 1 máy thì nhập `localhost`.

> Muốn mở **nhiều client**: chạy lại lệnh trên ở các terminal khác nhau.

---

## 👤 Hướng dẫn tạo tài khoản

| Role       | Cách tạo                                                |
|------------|---------------------------------------------------------|
| **Bidder** | Tự đăng ký trực tiếp trên giao diện client              |
| **Seller** | Đăng ký Bidder → nhờ Admin nâng cấp qua giao diện Admin |
| **Admin**  | Tạo qua API (xem bên dưới)                              |

### Tạo tài khoản Admin (lần đầu)

Sau khi server đã chạy, gọi API đăng ký:

```bash
curl -X POST http://<SERVER_IP>:8080/auction/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "admin",
    "password": "admin123",
    "fullName": "Administrator",
    "email": "admin@example.com",
    "role": "ADMIN"
  }'
```

*Thay `<SERVER_IP>` bằng IP thực hoặc `localhost` nếu chạy cùng máy.*

---

## ✅ Danh sách chức năng đã hoàn thành

### Authentication & Phân quyền
- ✅ Đăng ký / Đăng nhập với 3 roles: Admin, Seller, Bidder
- ✅ Quản lý phân quyền (Role-based access control)
- ✅ Nâng cấp Bidder → Seller (Admin thực hiện)
- ✅ Khóa / Mở khóa tài khoản (Admin)

### Quản lý Sản phẩm & Phiên Đấu giá
- ✅ Tạo Auction Item (Seller) + Upload ảnh sản phẩm
- ✅ Tạo / Bắt đầu / Đóng / Hủy phiên đấu giá
- ✅ Đặt giá (Bidding) với kiểm tra hợp lệ
- ✅ Tự động đặt giá (Auto-bidding)
- ✅ Tự động gia hạn thời gian đấu giá (Anti-sniping)
- ✅ Biểu đồ lịch sử giá của phiên đấu giá


### Thanh toán & Ví tiền
- ✅ Nạp / Rút tiền từ ví
- ✅ Thanh toán sau đấu giá (Seller chấp nhận / từ chối)
- ✅ Lịch sử giao dịch ví

### Real-time & Thông báo
- ✅ WebSocket – cập nhật giá real-time khi có bid mới
- ✅ Hệ thống thông báo real-time (Notifications)
- ✅ Chat real-time giữa người dùng (STOMP/WebSocket)

### Quản trị (Admin)
- ✅ Xem & quản lý toàn bộ danh sách người dùng
- ✅ Hủy phiên đấu giá vi phạm

### Khác
- ✅ Tìm kiếm phiên đấu giá, người dùng
- ✅ Đánh giá Seller
- ✅ Upload / Xóa avatar người dùng

---

## Tài liệu & Demo

| Nội dung              | Link                                                                        |
|-----------------------|-----------------------------------------------------------------------------|
| Báo cáo PDF           | *(cập nhật trước deadline)*                                                 |
| Video Demo            | *(cập nhật trước deadline)*                                                 |
| GitHub Releases (JAR) | [Releases](https://github.com/LaziChicken7/MajorAssignment_Group1/releases) |

---

## Troubleshooting

**Lỗi "Connection refused" / Client không kết nối được server**
- Kiểm tra `java -jar server.jar` đã chạy chưa
- Nhập đúng IP máy chạy server (dùng `ipconfig` để kiểm tra)
- Kiểm tra firewall cho phép port `8080`

**Lỗi "Port 8080 already in use"**
```powershell
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Lỗi kết nối MySQL**
- Kiểm tra MySQL đang chạy (port `3306`)
- Mặc định `username = root`, `password = root` — đổi trong `application.yaml` nếu khác

---

## Thông tin môn học

**Môn học:** Lập trình Nâng cao (LTNC)  
**Trường:** Đại học Công nghệ – ĐHQGHN  
**Deadline nộp bài:** 23:59, ngày 31/05/2026
