package org.auctionfx.auctionbidsystemspringbootrework.service;

import lombok.extern.slf4j.Slf4j;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ItemCancellationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ItemCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Art;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Electronic;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Vehicle;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Seller;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.enums.AuctionStatus;
import org.auctionfx.auctionbidsystemspringbootrework.enums.NotificationType;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ItemException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.AuctionRepository;
import org.auctionfx.auctionbidsystemspringbootrework.repository.ItemRepository;
import org.auctionfx.auctionbidsystemspringbootrework.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j // Kích hoạt bộ ghi log của Lombok
public class ItemService {
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private PaymentService paymentService;

    // Thư mục lưu trữ file upload trên server (tương đối từ root project)
    private static final String UPLOAD_DIR = "uploads/images/items/";

    @Transactional
    public String createItem(ItemCreationRequest request) {
        log.info("SERVICE: Bắt đầu xử lý tạo Item mới loại [{}] cho Seller [{}]", request.getItemType(), request.getSellerUserName());

        // 1. Kiểm tra quyền của Seller
        User user = userRepository.findByUserName(request.getSellerUserName());
        if (user == null) {
            log.error("Lỗi: Không tìm thấy Username [{}]", request.getSellerUserName());
            throw new ItemException(ErrorCode.USER_NOT_FOUND);
        }
        if (!(user instanceof Seller)) {
            log.error("Lỗi: User [{}] không phải là SELLER, từ chối tạo Item", request.getSellerUserName());
            throw new ItemException(ErrorCode.SELLER_INVALID);
        }

        // 2. Tạo sản phẩm
        Item item;
        switch (request.getItemType()) {
            case ART -> {
                Art art = new Art();
                art.setNameAuthor(request.getNameAuthor());
                art.setCreationYear(request.getCreationYear());
                item = art;
            }
            case ELECTRONIC -> {
                Electronic elec = new Electronic();
                elec.setBrand(request.getBrand());
                elec.setWarrantyMonths(request.getWarrantyMonths());
                item = elec;
            }
            case VEHICLE -> {
                Vehicle veh = new Vehicle();
                veh.setEngineType(request.getEngineType());
                veh.setMileage(request.getMileage());
                item = veh;
            }
            default -> {
                log.error("Lỗi: Loại sản phẩm [{}] không hợp lệ", request.getItemType());
                throw new ItemException(ErrorCode.ITEM_INVALID);
            }
        }

        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setStartPrice(request.getStartPrice());
        item.setSeller((Seller) user);

        // BẠN CẦN BỔ SUNG DÒNG NÀY ĐỂ DATABASE LƯU ĐƯỢC CHỮ "VEHICLE", "ART"...
        item.setItemType(request.getItemType());

        // Cập nhật danh sách ảnh cho Item nếu có
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            log.debug("Cập nhật danh sách URL ảnh có sẵn cho Item");
            item.setImageUrls(request.getImageUrls());
        }

        itemRepository.save(item);
        log.info("Tạo Item thành công! Sinh ra ID: {}", item.getId());
        return "Create item successfully, Item id is: " + item.getId();
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    //  *** Admin hủy sản phẩm ***
    //   - Cập nhật trạng thái auction thành CANCELLED
    //   - Hoàn tiền cho người thắng cuộc (nếu có)
    //   - Gửi thông báo cho người bán và người mua
    @Transactional
    public String cancelItemByAdmin(String itemId, ItemCancellationRequest request) {
        log.warn("SERVICE: Bắt đầu xử lý luồng Admin HỦY SẢN PHẨM ID [{}]", itemId);

        // 1. Kiểm tra Item có tồn tại không
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.error("Lỗi Hủy: Không tìm thấy Item [{}]", itemId);
                    return new ItemException(ErrorCode.ITEM_NOT_FOUND);
                });

        // 2. Tìm auction liên quan đến item này
        Auction auction = auctionRepository.findByBidProduct(item)
                .orElseThrow(() -> {
                    log.error("Lỗi Hủy: Không tìm thấy Auction liên kết với Item [{}]", itemId);
                    return new ItemException(ErrorCode.AUCTION_NOT_FOUND);
                });

        // 3. Kiểm tra xem auction đã hủy hoặc đã thanh toán rồi không (không được hủy lại)
        if (auction.getStatus() == AuctionStatus.CANCELLED) {
            log.warn("Lỗi Hủy: Auction [{}] đã bị CANCELLED từ trước", auction.getId());
            throw new ItemException(ErrorCode.CONDITION_CANCEL_AUCTION_INVALID);
        }
        if (auction.getStatus() == AuctionStatus.PAID) {
            log.warn("Lỗi Hủy: Auction [{}] đã hoàn tất thanh toán (PAID), không thể hủy", auction.getId());
            throw new ItemException(ErrorCode.CONDITION_CANCEL_AUCTION_INVALID);
        }

        // 4. Kiểm tra xem có người thắng cuộc không
        Bidder winner = auction.getHighestBid() != null ? auction.getWinningUser() : null;

        // 5. Nếu có người thắng cuộc, hoàn tiền từ moneyInFrozen về moneyOnWallet
        if (winner != null && auction.getHighestBid() != null && auction.getHighestBid().compareTo(BigDecimal.ZERO) > 0) {
            try {
                log.info("Tiến hành hoàn lại số tiền đóng băng {} VND cho người thắng [{}]", auction.getHighestBid(), winner.getUserName());
                paymentService.unFreezeMoney(winner.getId(), auction.getHighestBid());
            } catch (Exception e) {
                // Nếu hoàn tiền thất bại, throw exception
                log.error("LỖI NGHIÊM TRỌNG: Quá trình hoàn tiền thất bại cho User [{}]", winner.getUserName(), e);
                throw new ItemException(ErrorCode.NOT_ENOUGH_MONEY_IN_FROZEN);
            }
        }

        // 6. status auction thành CANCELLED
        auction.setStatus(AuctionStatus.CANCELLED);
        auctionRepository.save(auction);
        log.info("Đã chuyển trạng thái Auction [{}] thành CANCELLED", auction.getId());

        // 7. Gửi thông báo cho người bán
        String sellerTitle = "Sản phẩm bị hủy bởi Admin";
        String sellerDescription = String.format("sản phẩm đã bị hủy do admin, vui lòng liên hệ admin để tìm hiểu rõ nguyên nhân", request.getReason());
        notificationService.createNotification(auction.getSeller(), auction, NotificationType.ITEM_CANCELLED_BY_ADMIN,
                sellerTitle, sellerDescription);

        // 8. Gửi thông báo cho người mua (nếu có người thắng cuộc)
        if (winner != null) {
            String buyerTitle = "Sản phẩm đã bị hủy";
            String buyerDescription = String.format("sản phẩm đã bị hủy do admin, tiền được hoàn lại trong ví", request.getReason());
            notificationService.createNotification(winner, auction, NotificationType.ITEM_CANCELLED_BY_ADMIN,
                    buyerTitle, buyerDescription);
        }

        log.info("Xử lý Hủy Sản Phẩm [{}] thành công và đã gửi đầy đủ thông báo.", itemId);
        return "Item cancelled successfully by admin";
    }

    // Logic xử lý Upload Image được dời từ Controller sang
    @Transactional
    public List<String> uploadImagesForItem(String itemId, MultipartFile[] files) throws IOException {
        log.info("SERVICE: Xử lý lưu file vật lý cho Item [{}]", itemId);

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.error("Lỗi Upload: Không tìm thấy Item [{}]", itemId);
                    return new ItemException(ErrorCode.ITEM_NOT_FOUND);
                });

        if (files == null || files.length == 0) {
            log.error("Lỗi Upload: Mảng files truyền vào bị Null hoặc rỗng");
            throw new IOException("No files provided");
        }

        // 2. Thư mục vật lý sẽ tạo ra: uploads/images/items/{itemId}/
        String itemDirPath = UPLOAD_DIR + itemId + "/";
        File uploadDir = new File(itemDirPath);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            log.error("Lỗi Upload: Không thể tạo thư mục [{}]", itemDirPath);
            throw new IOException("Could not create directory: " + itemDirPath);
        }

        List<String> fileUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName != null && originalFileName.contains(".")
                    ? originalFileName.substring(originalFileName.lastIndexOf(".")) : "";

            String newFileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = Paths.get(itemDirPath + newFileName);

            log.debug("Đang lưu file: {}", newFileName);
            Files.write(filePath, file.getBytes());

            // 3. Đường dẫn lưu vào Database sẽ là: /uploads/images/items/{itemId}/{newFileName}
            // (Thêm dấu "/" ở đầu để chuẩn định dạng URL web)
            String fileUrl = "/" + UPLOAD_DIR + itemId + "/" + newFileName;
            fileUrls.add(fileUrl);
        }

        List<String> currentImages = item.getImageUrls();
        if (currentImages == null) currentImages = new ArrayList<>();
        currentImages.addAll(fileUrls);
        item.setImageUrls(currentImages);
        itemRepository.save(item);

        log.info("Lưu thành công {} ảnh vào thư mục của Item [{}]", fileUrls.size(), itemId);
        return fileUrls;
    }
}