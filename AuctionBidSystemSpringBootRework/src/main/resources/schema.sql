-- ==============================================================================
-- 1. XÓA BẢNG CŨ (Theo thứ tự ngược: Bảng con xóa trước, bảng cha xóa sau)
-- ==============================================================================
DROP TABLE IF EXISTS chat_messages; -- Bổ sung: Bảng tin nhắn
DROP TABLE IF EXISTS connections;   -- Bổ sung: Bảng kết bạn
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS auto_bid_configs;
DROP TABLE IF EXISTS bid_transactions;
DROP TABLE IF EXISTS auctions;
DROP TABLE IF EXISTS item_image_urls;
DROP TABLE IF EXISTS arts;
DROP TABLE IF EXISTS electronics;
DROP TABLE IF EXISTS vehicles;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS seller_reviews;
DROP TABLE IF EXISTS sellers;
DROP TABLE IF EXISTS bidders;
DROP TABLE IF EXISTS admins;
DROP TABLE IF EXISTS users;

-- ==============================================================================
-- 2. TẠO NHÁNH NGƯỜI DÙNG (USERS -> BIDDERS -> SELLERS & ADMINS)
-- ==============================================================================

-- Bảng gốc User
CREATE TABLE users (
                       id VARCHAR(36) PRIMARY KEY,
                       user_code VARCHAR(50) UNIQUE NOT NULL,
                       user_name VARCHAR(100) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       full_name VARCHAR(100),
                       email VARCHAR(100) UNIQUE,
                       number_phone VARCHAR(20) UNIQUE,
                       citizen_id VARCHAR(50) UNIQUE,
                       role VARCHAR(20) NOT NULL,
                       avatar_url VARCHAR(700) DEFAULT '/images/avatar/default-avatarmacdinh.png',
                       is_banned BOOLEAN DEFAULT FALSE
);

-- Kế thừa User
CREATE TABLE admins (
                        id VARCHAR(36) PRIMARY KEY,
                        department VARCHAR(100) DEFAULT 'General',
                        CONSTRAINT fk_admin_user FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

-- Kế thừa User
CREATE TABLE bidders (
                         id VARCHAR(36) PRIMARY KEY,
                         address VARCHAR(255),
                         bank_account_number VARCHAR(50),
                         money_on_wallet DECIMAL(19, 2) DEFAULT 0.00,
                         moneyin_frozen DECIMAL(19, 2) DEFAULT 0.00,
                         CONSTRAINT fk_bidder_user FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

-- Kế thừa Bidder
CREATE TABLE sellers (
                         id VARCHAR(36) PRIMARY KEY,
                         rating DOUBLE DEFAULT 0.0,
                         CONSTRAINT fk_seller_bidder FOREIGN KEY (id) REFERENCES bidders(id) ON DELETE CASCADE
);

-- ==============================================================================
-- 3. TẠO NHÁNH SẢN PHẨM (ITEMS -> ART, ELECTRONICS, VEHICLE)
-- ==============================================================================

-- Bảng gốc Item
CREATE TABLE items (
                       id VARCHAR(36) PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       description TEXT,
                       start_price DECIMAL(19, 2) NOT NULL,
                       end_price DECIMAL(19, 2),
                       start_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                       item_type VARCHAR(50) NOT NULL,
                       seller_id VARCHAR(36) NOT NULL,
                       CONSTRAINT fk_item_seller FOREIGN KEY (seller_id) REFERENCES sellers(id)
);

-- Bảng phụ lưu List<String> imageUrls của Item (Do @ElementCollection tạo ra)
CREATE TABLE item_image_urls (
                                 item_id VARCHAR(36) NOT NULL,
                                 image_urls VARCHAR(255),
                                 CONSTRAINT fk_item_images FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- Các bảng con kế thừa Item
CREATE TABLE arts (
                      id VARCHAR(36) PRIMARY KEY,
                      name_author VARCHAR(100),
                      creation_year INT,
                      CONSTRAINT fk_art_item FOREIGN KEY (id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE electronics (
                             id VARCHAR(36) PRIMARY KEY,
                             brand VARCHAR(100),
                             warranty_months INT,
                             CONSTRAINT fk_electronic_item FOREIGN KEY (id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE vehicles (
                          id VARCHAR(36) PRIMARY KEY,
                          engine_type VARCHAR(100),
                          mileage INT,
                          CONSTRAINT fk_vehicle_item FOREIGN KEY (id) REFERENCES items(id) ON DELETE CASCADE
);

-- ==============================================================================
-- 4. TẠO NHÁNH ĐẤU GIÁ VÀ GIAO DỊCH
-- ==============================================================================

-- Bảng Phiên đấu giá
CREATE TABLE auctions (
                          id VARCHAR(36) PRIMARY KEY,
                          start_time DATETIME,
                          end_time DATETIME,
                          highest_bid DECIMAL(19, 2),
                          step_price DECIMAL(19, 2) DEFAULT 1000.00,
                          status VARCHAR(50) DEFAULT 'OPEN',
                          transaction_status VARCHAR(50) DEFAULT NULL,
                          item_id VARCHAR(36) UNIQUE NOT NULL,
                          seller_id VARCHAR(36) NOT NULL,
                          winning_user_id VARCHAR(36),
                          CONSTRAINT fk_auction_item FOREIGN KEY (item_id) REFERENCES items(id),
                          CONSTRAINT fk_auction_seller FOREIGN KEY (seller_id) REFERENCES sellers(id),
                          CONSTRAINT fk_auction_winner FOREIGN KEY (winning_user_id) REFERENCES bidders(id)
);

-- Bảng Lịch sử đặt giá
CREATE TABLE bid_transactions (
                                  id VARCHAR(36) PRIMARY KEY,
                                  bid_amount DECIMAL(19, 2) NOT NULL,
                                  bid_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                                  auction_id VARCHAR(36) NOT NULL,
                                  bidder_id VARCHAR(36) NOT NULL,
                                  CONSTRAINT fk_trans_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                                  CONSTRAINT fk_trans_bidder FOREIGN KEY (bidder_id) REFERENCES bidders(id)
);

-- Bảng Cấu hình Đấu giá tự động (Auto-Bid)
CREATE TABLE auto_bid_configs (
                                  id VARCHAR(36) PRIMARY KEY,
                                  auction_id VARCHAR(36) NOT NULL,
                                  bidder_id VARCHAR(36) NOT NULL,
                                  max_bid_amount DECIMAL(19, 2) NOT NULL,
                                  is_active BOOLEAN DEFAULT TRUE,
                                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT fk_autobid_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                                  CONSTRAINT fk_autobid_bidder FOREIGN KEY (bidder_id) REFERENCES bidders(id) ON DELETE CASCADE
);

-- ==============================================================================
-- 5. TẠO NHÁNH THÔNG BÁO VÀ ĐÁNH GIÁ (NOTIFICATIONS & REVIEWS)
-- ==============================================================================

-- Bảng Thông báo
CREATE TABLE notifications (
                               id VARCHAR(36) PRIMARY KEY,
                               title VARCHAR(255) NOT NULL,
                               description TEXT,
                               type VARCHAR(50) NOT NULL,
                               is_read BOOLEAN DEFAULT FALSE,
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               user_id VARCHAR(36) NOT NULL,
                               auction_id VARCHAR(36),
                               CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                               CONSTRAINT fk_notif_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE SET NULL
);

-- Bảng Đánh giá người bán (Seller Reviews)
CREATE TABLE seller_reviews (
                                id VARCHAR(36) PRIMARY KEY,
                                seller_id VARCHAR(36) NOT NULL,
                                reviewer_id VARCHAR(36) NOT NULL,
                                star INT NOT NULL,
                                comment TEXT,
                                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT fk_review_seller FOREIGN KEY (seller_id) REFERENCES sellers(id) ON DELETE CASCADE,
                                CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ==============================================================================
-- 6. TẠO NHÁNH KẾT BẠN VÀ TIN NHẮN (CHAT & CONNECTIONS)
-- ==============================================================================

-- Bảng Kết bạn / Liên hệ
CREATE TABLE connections (
                             id VARCHAR(36) PRIMARY KEY,
                             sender_id VARCHAR(36) NOT NULL,
                             receiver_id VARCHAR(36) NOT NULL,
                             status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, ACCEPTED, BLOCKED
                             created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_conn_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
                             CONSTRAINT fk_conn_receiver FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Bảng Lịch sử Tin nhắn
CREATE TABLE chat_messages (
                               id VARCHAR(36) PRIMARY KEY,
                               sender_id VARCHAR(36) NOT NULL,
                               receiver_id VARCHAR(36) NOT NULL,
                               content TEXT NOT NULL,
                               is_read BOOLEAN DEFAULT FALSE,
                               send_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_chat_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
                               CONSTRAINT fk_chat_receiver FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ==============================================================================
-- 7. KHỞI TẠO DỮ LIỆU MẪU (SEED DATA)
-- ==============================================================================

-- Tạo một ID cố định (đúng chuẩn 36 ký tự) để dễ dàng đồng bộ giữa các bảng
SET @admin_id = 'admin-id-0000-0000-0000-000000000000';

-- 1. Insert thông tin chung vào bảng gốc 'users'
INSERT INTO users (
    id, user_code, user_name, password, full_name,
    email, number_phone, citizen_id, role, is_banned
) VALUES (
             @admin_id,
             'ADMIN_001',             -- user_code (bắt buộc vì NOT NULL và UNIQUE)
             'admin',                 -- userName
             '$2a$12$e8sMHIM2juO3p1s3nwwZsuAvz2FdfFS9D1nYwtkpZSSoAngyYuQTa',              -- password
             'Admin',                 -- fullName
             'admin@example.com',     -- email
             '0941234567',            -- numberPhone
             '005093123456',          -- citizenId
             'ADMIN',                 -- role
             FALSE
         );

-- 2. Insert ID đó vào bảng con 'admins' để xác nhận đây là một Admin
INSERT INTO admins (id, department)
VALUES (@admin_id, 'System Administration');