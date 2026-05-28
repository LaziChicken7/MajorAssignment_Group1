package com.auction.util;


import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class ImageCacheUtils {

    private static final ConcurrentHashMap<String, SoftReference<Image>> memoryCache = new ConcurrentHashMap<>();

    // THÊM: BỘ NHỚ ĐỆM RIÊNG DÀNH CHO ẢNH PLACEHOLDER
    // Tránh việc spam hàng trăm request lên via.placeholder.com khi cuộn trang
    private static final ConcurrentHashMap<String, Image> placeholderCache = new ConcurrentHashMap<>();

    private static final String CACHE_DIR = System.getProperty("user.home") + File.separator + ".AuctionAppCache" + File.separator + "images";
    private static final ConcurrentHashMap<String, Object> fileLocks = new ConcurrentHashMap<>();

    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(10, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    static {
        File dir = new File(CACHE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // HÀM MỚI: Lấy ảnh Placeholder từ RAM (Nếu chưa có thì tải 1 lần duy nhất)
    private static Image getCachedPlaceholder(String url, int width, int height) {
        if (url == null || url.isEmpty()) return null;
        String key = url + "_" + width + "x" + height;
        // Chỉ khởi tạo new Image đúng 1 lần cho mỗi URL + Size
        return placeholderCache.computeIfAbsent(key, k -> new Image(url, width, height, true, true, true));
    }

    public static void loadImage(ImageView imageView, String imageUrl, int width, int height, String placeholderUrl) {
        // 1. Dùng hàm getCachedPlaceholder để KHÔNG tạo kết nối HTTP mới nữa
        Image placeholderImg = getCachedPlaceholder(placeholderUrl, width, height);

        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImage(placeholderImg);
            return;
        }

        String safeFileName = Math.abs(imageUrl.hashCode()) + ".png";

        SoftReference<Image> cachedRef = memoryCache.get(safeFileName);
        if (cachedRef != null && cachedRef.get() != null) {
            imageView.setImage(cachedRef.get());
            log.info("⚡ RAM Cache Hit: " + safeFileName);
            return;
        }

        // 2. Gán ảnh Loading từ RAM ngay lập tức (Không gây lag UI)
        imageView.setImage(placeholderImg);

        CompletableFuture.runAsync(() -> {
            try {
                File localFile = new File(CACHE_DIR, safeFileName);
                Image finalImage;

                if (localFile.exists() && localFile.length() > 0) {
                    log.info("✅ MỞ LẠI TỪ Ổ CỨNG (Không tải mạng): " + safeFileName);
                    finalImage = new Image(localFile.toURI().toString(), width, height, true, true, false);
                } else {
                    Object lock = fileLocks.computeIfAbsent(safeFileName, k -> new Object());
                    synchronized (lock) {
                        if (!localFile.exists() || localFile.length() == 0) {
                            log.info("🌐 ĐANG TẢI MỚI TỪ MẠNG: " + imageUrl);
                            URL url = new URL(imageUrl);
                            try (InputStream in = url.openStream()) {
                                Files.copy(in, localFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                    finalImage = new Image(localFile.toURI().toString(), width, height, true, true, false);
                }

                memoryCache.put(safeFileName, new SoftReference<>(finalImage));
                Platform.runLater(() -> imageView.setImage(finalImage));

            } catch (Exception e) {
                log.error("❌ LỖI TẢI ẢNH: " + imageUrl + " -> " + e.getMessage());
                File localFile = new File(CACHE_DIR, safeFileName);
                if (localFile.exists()) localFile.delete();

                // 3. Tương tự, dùng Cache cho ảnh báo lỗi
                Image errorImg = getCachedPlaceholder("https://via.placeholder.com/120?text=Error", width, height);
                Platform.runLater(() -> imageView.setImage(errorImg));
            } finally {
                fileLocks.remove(safeFileName);
            }
        }, IMAGE_EXECUTOR);
    }

    public static void clearMemoryCache() {
        memoryCache.clear();
        placeholderCache.clear(); // Xóa luôn cache placeholder
        log.info("🧹 Đã dọn sạch ảnh lưu trên RAM.");
    }

    public static void clearDiskCache() {
        CompletableFuture.runAsync(() -> {
            try {
                File dir = new File(CACHE_DIR);
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            file.delete();
                        }
                    }
                }
                log.info("🗑️ Đã xóa toàn bộ file ảnh rác trên Ổ CỨNG (.AuctionAppCache).");
            } catch (Exception e) {
                log.error("❌ Lỗi khi dọn dẹp ổ cứng: " + e.getMessage());
            }
        });
    }

    public static void clearAllCaches() {
        clearMemoryCache();
        clearDiskCache();
    }
}