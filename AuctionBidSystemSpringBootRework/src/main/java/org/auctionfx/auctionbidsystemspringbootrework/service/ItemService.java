package org.auctionfx.auctionbidsystemspringbootrework.service;

import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ItemCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Art;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Electronic;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Vehicle;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Seller;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ItemException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.ItemRepository;
import org.auctionfx.auctionbidsystemspringbootrework.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ItemService {
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserRepository userRepository;

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
}
