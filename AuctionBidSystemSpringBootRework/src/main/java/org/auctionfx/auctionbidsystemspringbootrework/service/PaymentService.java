package org.auctionfx.auctionbidsystemspringbootrework.service;

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
public class PaymentService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    // Ép kiểu an toàn từ User sang Bidder
    private Bidder getBidder(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_INVALID));
        if (!(user instanceof Bidder)) {
            throw new RuntimeException("User don't have Wallet functionality (Not Bidder/Seller)");
        }
        return (Bidder) user;
    }

    // Hàm phụ trợ tìm Bidder bằng UserName (Dán đoạn này vào dưới cùng của class PaymentService)
    private Bidder getBidderByUserName(String userName) {
        User user = userRepository.findByUserName(userName);
        if (!(user instanceof Bidder)) {
            throw new RuntimeException("User not found or not have Wallet functionality!");
        }
        return (Bidder) user;
    }

    // 1. Đóng băng tiền (Khi người dùng bấm Place Bid)
    // Trừ tiền ở Ví chính -> Cộng vào Ví đóng băng
    @Transactional(rollbackFor = Exception.class) // Nếu có lỗi xảy ra, toàn bộ tiền sẽ được rollback lại như cũ
    public String freezeMoney(String userId, BigDecimal amount) {
        Bidder bidder = getBidder(userId);

        if (bidder.getMoneyOnWallet().compareTo(amount) < 0) {
            throw new PaymentException(ErrorCode.NOT_ENOUGH_MONEY_ON_WALLET);
        }

        bidder.setMoneyOnWallet(bidder.getMoneyOnWallet().subtract(amount));
        bidder.setMoneyinFrozen(bidder.getMoneyinFrozen().add(amount));

        userRepository.save(bidder);
        return "Freeze Money successfully!";
    }

    // 2. Hoàn tiền (Khi có người khác đặt giá cao hơn, hoặc hủy phiên)
    // Trừ tiền ở Ví đóng băng -> Trả lại Ví chính
    @Transactional(rollbackFor = Exception.class)
    public String unFreezeMoney(String userId, BigDecimal amount) {
        Bidder bidder = getBidder(userId);

        if (bidder.getMoneyinFrozen().compareTo(amount) < 0) {
            throw new PaymentException(ErrorCode.NOT_ENOUGH_MONEY_IN_FROZEN);
        }

        bidder.setMoneyinFrozen(bidder.getMoneyinFrozen().subtract(amount));
        bidder.setMoneyOnWallet(bidder.getMoneyOnWallet().add(amount));

        userRepository.save(bidder);
        return "Unfreeze Money successfully!";
    }

    // 3. Chuyển tiền khi kết thúc (Người mua thanh toán cho người bán)
    // Trừ tiền ở Ví đóng băng của người mua -> Cộng vào Ví chính của người bán
    @Transactional(rollbackFor = Exception.class)
    public String transferMoney(String fromUserId, String toUserId, BigDecimal amount) {
        Bidder buyer = getBidder(fromUserId);
        Bidder seller = getBidder(toUserId);

        if (buyer.getMoneyinFrozen().compareTo(amount) < 0) {
            throw new PaymentException(ErrorCode.NOT_ENOUGH_MONEY_IN_FROZEN);
        }

        // Trừ của người mua
        buyer.setMoneyinFrozen(buyer.getMoneyinFrozen().subtract(amount));

        // Cộng cho người bán
        seller.setMoneyOnWallet(seller.getMoneyOnWallet().add(amount));

        userRepository.save(buyer);
        userRepository.save(seller);

        return "Transfer Money successfully!";
    }

    // 4. Nạp tiền vào ví
    @Transactional(rollbackFor = Exception.class)
    public String deposit(String userName, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException(ErrorCode.DEPOSIT_MONEY_INVALID);
        }
        Bidder bidder = getBidderByUserName(userName);
        bidder.setMoneyOnWallet(bidder.getMoneyOnWallet().add(amount));
        userRepository.save(bidder);

        return "Deposit successfully! Money on wallet now is: " + bidder.getMoneyOnWallet();
    }

    // 5. Rút tiền vào ví
    @Transactional(rollbackFor = Exception.class)
    public String withdraw(String userName, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException(ErrorCode.WITHDRAW_MONEY_INVALID);
        }
        Bidder bidder = getBidderByUserName(userName);
        if (bidder.getMoneyOnWallet().compareTo(amount) < 0) {
            throw new PaymentException(ErrorCode.NOT_ENOUGH_MONEY_ON_WALLET);
        }

        bidder.setMoneyOnWallet(bidder.getMoneyOnWallet().subtract(amount));
        userRepository.save(bidder);

        return "Withdraw successfully! Money on wallet now is: " + bidder.getMoneyOnWallet();
    }

    // 6. Đưa ra danh sách các sản phẩm đấu giá thành công hay thất bại của một user
    // Hàm này sẽ trả về dữ liệu về cho VÍ TIỀN
    public Map<String, Object> getMyWalletAndHistory(String userName) {
        // 1. Lấy thông tin ví tiền
        Bidder bidder = getBidderByUserName(userName);
        Map<String, Object> responseData = new HashMap<>();

        responseData.put("moneyOnWallet", bidder.getMoneyOnWallet());
        responseData.put("moneyinFrozen", bidder.getMoneyinFrozen());

        // 2. Lọc giao dịch thành công
        List<Auction> wonAuctions = auctionRepository.findWonAuctions(userName);
        List<TransactionHistoryResponse> successList = new ArrayList<>();
        for (Auction auction : wonAuctions) {
            successList.add(new TransactionHistoryResponse(
                    auction.getBidProduct().getName(),
                    auction.getHighestBid(),
                    TransactionStatus.SUCCESS
            ));
        }

        // 3. Lọc giao dịch thất bại
        List<Auction> lostAuctions = auctionRepository.findLostAuctions(userName);
        List<TransactionHistoryResponse> failedList = new ArrayList<>();
        for (Auction auction : lostAuctions) {
            failedList.add(new TransactionHistoryResponse(
                    auction.getBidProduct().getName(),
                    auction.getHighestBid(),
                    TransactionStatus.FAILED
            ));
        }

        // 4. Đóng gói tất cả trả về cho JavaFX
        responseData.put("successTransaction", successList);
        responseData.put("failedTransaction", failedList);

        return responseData;
    }
}
