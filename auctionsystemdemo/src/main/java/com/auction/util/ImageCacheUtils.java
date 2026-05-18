package com.auction.util;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCacheUtils {

    private static final ConcurrentHashMap<String, SoftReference<Image>> memoryCache = new ConcurrentHashMap<>();

    // Thư mục lưu cache vĩnh viễn trên máy tính
    private static final String CACHE_DIR = System.getProperty("user.home") + File.separator + ".AuctionAppCache" + File.separator + "images";
    private static final ConcurrentHashMap<String, Object> fileLocks = new ConcurrentHashMap<>();

    static {
        File dir = new File(CACHE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static void loadImage(ImageView imageView, String imageUrl, int width, int height, String placeholderUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImage(new Image(placeholderUrl, width, height, true, true, true));
            return;
        }

        // Tạo tên file duy nhất dựa trên URL
        String safeFileName = Math.abs(imageUrl.hashCode()) + ".png";

        // BƯỚC 1: Kiểm tra trên RAM
        SoftReference<Image> cachedRef = memoryCache.get(safeFileName);
        if (cachedRef != null && cachedRef.get() != null) {
            imageView.setImage(cachedRef.get());
            // System.out.println("RAM Cache Hit: " + safeFileName);
            return;
        }

        File localFile = new File(CACHE_DIR, safeFileName);

        // BƯỚC 2: Kiểm tra trên Ổ CỨNG (SỬA LỖI CHỚP NHÁY Ở ĐÂY)
        if (localFile.exists() && localFile.length() > 0) {
            // Nếu đã có trong ổ cứng -> Load ĐỒNG BỘ (backgroundLoading = false)
            // Điều này giúp ảnh xuất hiện NGAY LẬP TỨC, không hề có dòng chữ Loading...
            Image diskImage = new Image(localFile.toURI().toString(), width, height, true, false, false);

            memoryCache.put(safeFileName, new SoftReference<>(diskImage));
            imageView.setImage(diskImage);

            System.out.println("✅ MỞ LẠI TỪ Ổ CỨNG (Không tải mạng): " + safeFileName);
            return;
        }

        // BƯỚC 3: Chưa có ở đâu cả -> Set ảnh Loading... và tải ngầm từ Mạng
        imageView.setImage(new Image(placeholderUrl, width, height, true, true, true));

        Object lock = fileLocks.computeIfAbsent(safeFileName, k -> new Object());

        CompletableFuture.runAsync(() -> {
            try {
                synchronized (lock) {
                    // Double check xem luồng khác đã tải xong chưa
                    if (!localFile.exists() || localFile.length() == 0) {
                        System.out.println("🌐 ĐANG TẢI MỚI TỪ MẠNG: " + imageUrl);
                        URL url = new URL(imageUrl);
                        try (InputStream in = url.openStream()) {
                            Files.copy(in, localFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }

                // Tải xong, đọc file từ ổ cứng lên
                Image finalImage = new Image(localFile.toURI().toString(), width, height, true, false, true);
                memoryCache.put(safeFileName, new SoftReference<>(finalImage));

                // Gắn lên UI
                Platform.runLater(() -> imageView.setImage(finalImage));

            } catch (Exception e) {
                System.err.println("❌ LỖI TẢI ẢNH: " + imageUrl + " -> " + e.getMessage());
                // Xóa file rỗng nếu quá trình tải bị lỗi giữa chừng
                if (localFile.exists()) localFile.delete();

                Image errorImg = new Image("https://via.placeholder.com/120?text=Error", width, height, true, false, true);
                Platform.runLater(() -> imageView.setImage(errorImg));
            } finally {
                fileLocks.remove(safeFileName);
            }
        });
    }
}