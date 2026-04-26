package org.auctionfx.auctionbidsystemspringbootrework.controller;

import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ItemCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items")
public class ItemController {
    @Autowired
    private ItemService itemService;

    @PostMapping("/create")
    public String createItem(@RequestBody ItemCreationRequest request) {
        return itemService.createItem(request);
    }
}
