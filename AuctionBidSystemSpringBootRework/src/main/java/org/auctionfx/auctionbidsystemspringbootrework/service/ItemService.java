package org.auctionfx.auctionbidsystemspringbootrework.service;

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

import java.math.BigDecimal;
import java.util.List;

@Service
public class ItemService {
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private PaymentService paymentService;

    @Transactional
    public String createItem(ItemCreationRequest request) {
        // 1. Kiểm tra quyền của Seller
        User user = userRepository.findByUserName(request.getSellerUserName());
        if (user == null) {
            throw new ItemException(ErrorCode.USER_NOT_FOUND);
        }
        if (!(user instanceof Seller)) {
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
            default -> throw new ItemException(ErrorCode.ITEM_INVALID);
        }

        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setStartPrice(request.getStartPrice());
        item.setSeller((Seller) user);

        // BẠN CẦN BỔ SUNG DÒNG NÀY ĐỂ DATABASE LƯU ĐƯỢC CHỮ "VEHICLE", "ART"...
        item.setItemType(request.getItemType());

        // Cập nhật danh sách ảnh cho Item nếu có 
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            item.setImageUrls(request.getImageUrls());
        }

        itemRepository.save(item);
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
        // 1. Kiểm tra Item có tồn tại không
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemException(ErrorCode.ITEM_NOT_FOUND));

        // 2. Tìm auction liên quan đến item này
        Auction auction = auctionRepository.findByBidProduct(item)
                .orElseThrow(() -> new ItemException(ErrorCode.AUCTION_NOT_FOUND));

        // 3. Kiểm tra xem auction đã hủy hoặc đã thanh toán rồi không (không được hủy lại)
        if (auction.getStatus() == AuctionStatus.CANCELLED) {
            throw new ItemException(ErrorCode.CONDITION_CANCEL_AUCTION_INVALID);
        }
        if (auction.getStatus() == AuctionStatus.PAID) {
            throw new ItemException(ErrorCode.CONDITION_CANCEL_AUCTION_INVALID);
        }

        // 4. Kiểm tra xem có người thắng cuộc không
        Bidder winner = auction.getHighestBid() != null ? auction.getWinningUser() : null;

        // 5. Nếu có người thắng cuộc, hoàn tiền từ moneyInFrozen về moneyOnWallet
        if (winner != null && auction.getHighestBid() != null && auction.getHighestBid().compareTo(BigDecimal.ZERO) > 0) {
            try {
                paymentService.unFreezeMoney(winner.getId(), auction.getHighestBid());
            } catch (Exception e) {
                // Nếu hoàn tiền thất bại, throw exception
                throw new ItemException(ErrorCode.NOT_ENOUGH_MONEY_IN_FROZEN);
            }
        }

        // 6. status auction thành CANCELLED
        auction.setStatus(AuctionStatus.CANCELLED);
        auctionRepository.save(auction);

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

        return "Item cancelled successfully by admin";
    }
}
