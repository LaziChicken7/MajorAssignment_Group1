package org.auctionfx.auctionbidsystemspringbootrework.service;

import lombok.extern.slf4j.Slf4j;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.TransactionHistoryResponse;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.enums.TransactionStatus;
import org.auctionfx.auctionbidsystemspringbootrework.repository.AuctionRepository;
import org.springframework.transaction.annotation.Transactional;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.PaymentException;
import org.auctionfx.auctionbidsystemspringbootrework.exception.UserException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
@Slf4j // Kích hoạt bộ ghi log của Lombok
public class PaymentService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    // Ép kiểu an toàn từ User sang Bidder
    private Bidder getBidder(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Lỗi: Không tìm thấy User ID [{}]", userId);
                    return new UserException(ErrorCode.USER_INVALID);
                });
        if (!(user instanceof Bidder)) {
            log.error("Lỗi: User ID [{}] không có chức năng Ví (Không phải Bidder/Seller)", userId);
            throw new RuntimeException("User don't have Wallet functionality (Not Bidder/Seller)");
        }
        return (Bidder) user;
    }

    // Hàm phụ trợ tìm Bidder bằng UserName (Dán đoạn này vào dưới cùng của class PaymentService)
    private Bidder getBidderByUserName(String userName) {
        User user = userRepository.findByUserName(userName);
        if (!(user instanceof Bidder)) {
            log.error("Lỗi: User [{}] không tồn tại hoặc không có chức năng Ví", userName);
            throw new RuntimeException("User not found or not have Wallet functionality!");
        }
        return (Bidder) user;
    }

    // 1. Đóng băng tiền (Khi người dùng bấm Place Bid)
    // Trừ tiền ở Ví chính -> Cộng vào Ví đóng băng
    @Transactional(rollbackFor = Exception.class) // Nếu có lỗi xảy ra, toàn bộ tiền sẽ được rollback lại như cũ
    public String freezeMoney(String userId, BigDecimal amount) {
        log.info("SERVICE: Yêu cầu ĐÓNG BĂNG {} VND của User ID [{}]", amount, userId);
        Bidder bidder = getBidder(userId);

        if (bidder.getMoneyOnWallet().compareTo(amount) < 0) {
            log.warn("Thất bại: User ID [{}] không đủ tiền trong ví (Cần: {}, Có: {})", userId, amount, bidder.getMoneyOnWallet());
            throw new PaymentException(ErrorCode.NOT_ENOUGH_MONEY_ON_WALLET);
        }

        bidder.setMoneyOnWallet(bidder.getMoneyOnWallet().subtract(amount));
        bidder.setMoneyinFrozen(bidder.getMoneyinFrozen().add(amount));

        userRepository.save(bidder);
        log.info("Đóng băng thành công {} VND cho User ID [{}]", amount, userId);
        return "Freeze Money successfully!";
    }

    // 2. Hoàn tiền (Khi có người khác đặt giá cao hơn, hoặc hủy phiên)
    // Trừ tiền ở Ví đóng băng -> Trả lại Ví chính
    @Transactional(rollbackFor = Exception.class)
    public String unFreezeMoney(String userId, BigDecimal amount) {
        log.info("SERVICE: Yêu cầu HOÀN TRẢ (Unfreeze) {} VND cho User ID [{}]", amount, userId);
        Bidder bidder = getBidder(userId);

        if (bidder.getMoneyinFrozen().compareTo(amount) < 0) {
            log.error("Lỗi nghiêm trọng: Tiền đóng băng của User ID [{}] ({}) ít hơn số tiền cần hoàn ({})", userId, bidder.getMoneyinFrozen(), amount);
            throw new PaymentException(ErrorCode.NOT_ENOUGH_MONEY_IN_FROZEN);
        }

        bidder.setMoneyinFrozen(bidder.getMoneyinFrozen().subtract(amount));
        bidder.setMoneyOnWallet(bidder.getMoneyOnWallet().add(amount));

        userRepository.save(bidder);
        log.info("Hoàn trả thành công {} VND vào ví chính cho User ID [{}]", amount, userId);
        return "Unfreeze Money successfully!";
    }

    // 3. Chuyển tiền khi kết thúc (Người mua thanh toán cho người bán)
    // Trừ tiền ở Ví đóng băng của người mua -> Cộng vào Ví chính của người bán
    @Transactional(rollbackFor = Exception.class)
    public String transferMoney(String fromUserId, String toUserId, BigDecimal amount) {
        log.info("SERVICE: Yêu cầu CHUYỂN {} VND từ Ví đóng băng (Buyer ID: {}) sang Ví chính (Seller ID: {})", amount, fromUserId, toUserId);
        Bidder buyer = getBidder(fromUserId);
        Bidder seller = getBidder(toUserId);

        if (buyer.getMoneyinFrozen().compareTo(amount) < 0) {
            log.error("Lỗi nghiêm trọng: Người mua ID [{}] không đủ tiền đóng băng để thanh toán", fromUserId);
            throw new PaymentException(ErrorCode.NOT_ENOUGH_MONEY_IN_FROZEN);
        }

        // Trừ của người mua
        buyer.setMoneyinFrozen(buyer.getMoneyinFrozen().subtract(amount));

        // Cộng cho người bán
        seller.setMoneyOnWallet(seller.getMoneyOnWallet().add(amount));

        userRepository.save(buyer);
        userRepository.save(seller);

        log.info("Giao dịch chuyển tiền thành công: {} VND đã được chuyển cho Seller ID [{}]", amount, toUserId);
        return "Transfer Money successfully!";
    }

    // 4. Nạp tiền vào ví
    @Transactional(rollbackFor = Exception.class)
    public String deposit(String userName, BigDecimal amount) {
        log.info("SERVICE: Bắt đầu giao dịch NẠP {} VND cho User [{}]", amount, userName);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Thất bại: Số tiền nạp phải lớn hơn 0 (Nhận được: {})", amount);
            throw new PaymentException(ErrorCode.DEPOSIT_MONEY_INVALID);
        }
        Bidder bidder = getBidderByUserName(userName);
        bidder.setMoneyOnWallet(bidder.getMoneyOnWallet().add(amount));
        userRepository.save(bidder);

        log.info("Nạp tiền thành công. Số dư hiện tại của User [{}]: {}", userName, bidder.getMoneyOnWallet());
        return "Deposit successfully! Money on wallet now is: " + bidder.getMoneyOnWallet();
    }

    // 5. Rút tiền vào ví
    @Transactional(rollbackFor = Exception.class)
    public String withdraw(String userName, BigDecimal amount) {
        log.info("SERVICE: Bắt đầu giao dịch RÚT {} VND cho User [{}]", amount, userName);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Thất bại: Số tiền rút phải lớn hơn 0 (Nhận được: {})", amount);
            throw new PaymentException(ErrorCode.WITHDRAW_MONEY_INVALID);
        }
        Bidder bidder = getBidderByUserName(userName);
        if (bidder.getMoneyOnWallet().compareTo(amount) < 0) {
            log.warn("Thất bại: User [{}] yêu cầu rút {} nhưng số dư chỉ có {}", userName, amount, bidder.getMoneyOnWallet());
            throw new PaymentException(ErrorCode.NOT_ENOUGH_MONEY_ON_WALLET);
        }

        bidder.setMoneyOnWallet(bidder.getMoneyOnWallet().subtract(amount));
        userRepository.save(bidder);

        log.info("Rút tiền thành công. Số dư hiện tại của User [{}]: {}", userName, bidder.getMoneyOnWallet());
        return "Withdraw successfully! Money on wallet now is: " + bidder.getMoneyOnWallet();
    }

    // 6. Đưa ra danh sách các sản phẩm đấu giá thành công hay thất bại của một user
    // Hàm này sẽ trả về dữ liệu về cho VÍ TIỀN
    public Map<String, Object> getMyWalletAndHistory(String userName) {
        log.debug("SERVICE: Truy xuất dữ liệu Ví và Lịch sử giao dịch cho User [{}]", userName);
        Bidder bidder = getBidderByUserName(userName);
        Map<String, Object> responseData = new HashMap<>();

        responseData.put("bankAccountNumber", bidder.getBankAccountNumber());
        responseData.put("moneyOnWallet", bidder.getMoneyOnWallet());
        responseData.put("moneyinFrozen", bidder.getMoneyinFrozen());

        // 2. Lọc giao dịch thành công
        List<Auction> wonAuctions = auctionRepository.findWonAuctions(userName, TransactionStatus.SUCCESS);
        List<TransactionHistoryResponse> successList = new ArrayList<>();

        for (Auction auction : wonAuctions) {
            // Lấy ảnh đầu tiên của sản phẩm (nếu có)
            String firstImageUrl = null;
            if (auction.getBidProduct().getImageUrls() != null && !auction.getBidProduct().getImageUrls().isEmpty()) {
                firstImageUrl = auction.getBidProduct().getImageUrls().get(0);
            }

            successList.add(new TransactionHistoryResponse(
                    auction.getBidProduct().getId(),
                    auction.getBidProduct().getName(),
                    auction.getHighestBid(),
                    auction.getTransactionStatus(),
                    firstImageUrl // TRUYỀN THÊM LINK ẢNH VÀO ĐÂY
            ));
        }

        // 3. Lọc giao dịch thất bại
        List<Auction> lostAuctions = auctionRepository.findLostAuctions(userName, TransactionStatus.FAILED);
        List<TransactionHistoryResponse> failedList = new ArrayList<>();
        for (Auction auction : lostAuctions) {
            // Lấy ảnh đầu tiên của sản phẩm (nếu có)
            String firstImageUrl = null;
            if (auction.getBidProduct().getImageUrls() != null && !auction.getBidProduct().getImageUrls().isEmpty()) {
                firstImageUrl = auction.getBidProduct().getImageUrls().get(0);
            }

            failedList.add(new TransactionHistoryResponse(
                    auction.getBidProduct().getId(), // THÊM DÒNG NÀY
                    auction.getBidProduct().getName(),
                    auction.getHighestBid(),
                    auction.getTransactionStatus() != null ? auction.getTransactionStatus() : TransactionStatus.FAILED,
                    firstImageUrl // TRUYỀN THÊM LINK ẢNH VÀO ĐÂY
            ));
        }

        responseData.put("successTransaction", successList);
        responseData.put("failedTransaction", failedList);

        log.debug("Đã đóng gói xong dữ liệu Ví. Tìm thấy {} giao dịch thành công và {} giao dịch thất bại", successList.size(), failedList.size());
        return responseData;
    }
}