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

        String safeFileName = Math.abs(imageUrl.hashCode()) + ".png";

        // BƯỚC 1: Kiểm tra trên RAM (Truy xuất tức thì, không gây lag UI)
        SoftReference<Image> cachedRef = memoryCache.get(safeFileName);
        if (cachedRef != null && cachedRef.get() != null) {
            imageView.setImage(cachedRef.get());
            // System.out.println("⚡ RAM Cache Hit: " + safeFileName);
            return;
        }

        // BƯỚC 2: Đặt ảnh Loading... để giữ khung UI không bị giật lag nhảy loạn xạ
        imageView.setImage(new Image(placeholderUrl, width, height, true, true, true));

        // BƯỚC 3: Đẩy HẾT mọi thao tác nặng (Kiểm tra ổ cứng, Tải mạng, Giải mã ảnh) ra luồng ngầm
        CompletableFuture.runAsync(() -> {
            try {
                File localFile = new File(CACHE_DIR, safeFileName);
                Image finalImage;

                if (localFile.exists() && localFile.length() > 0) {
                    System.out.println("✅ MỞ LẠI TỪ Ổ CỨNG (Không tải mạng): " + safeFileName);

                    // MA THUẬT NẰM Ở ĐÂY: Tham số cuối cùng là FALSE.
                    // Vì ta đang ở luồng ngầm (CompletableFuture), ta ép luồng này phải đọc và giải mã
                    // xong xuôi bức ảnh 100% rồi mới đi tiếp. Đỡ gánh nặng hoàn toàn cho JavaFX UI Thread.
                    finalImage = new Image(localFile.toURI().toString(), width, height, true, true, false);
                } else {
                    Object lock = fileLocks.computeIfAbsent(safeFileName, k -> new Object());
                    synchronized (lock) {
                        if (!localFile.exists() || localFile.length() == 0) {
                            System.out.println("🌐 ĐANG TẢI MỚI TỪ MẠNG: " + imageUrl);
                            URL url = new URL(imageUrl);
                            try (InputStream in = url.openStream()) {
                                Files.copy(in, localFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                    // Đọc file vừa tải xong (tham số cuối cũng = false)
                    finalImage = new Image(localFile.toURI().toString(), width, height, true, true, false);
                }

                // Lưu vào RAM
                memoryCache.put(safeFileName, new SoftReference<>(finalImage));

                // BƯỚC 4: Bức ảnh HÌNH ĐÃ ĐƯỢC GIẢI MÃ HOÀN CHỈNH, giờ chỉ việc gắn lên UI
                // Giao diện (Platform.runLater) sẽ xử lý trong 0.001 giây, không hề bị khựng!
                Platform.runLater(() -> imageView.setImage(finalImage));

            } catch (Exception e) {
                System.err.println("❌ LỖI TẢI ẢNH: " + imageUrl + " -> " + e.getMessage());
                File localFile = new File(CACHE_DIR, safeFileName);
                if (localFile.exists()) localFile.delete(); // Dọn file rác nếu tải xịt

                Image errorImg = new Image("https://via.placeholder.com/120?text=Error", width, height, true, true, true);
                Platform.runLater(() -> imageView.setImage(errorImg));
            } finally {
                fileLocks.remove(safeFileName);
            }
        });
    }
}