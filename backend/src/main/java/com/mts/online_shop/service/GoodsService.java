package com.mts.online_shop.service;

import com.mts.online_shop.model.Product;
import com.mts.online_shop.repository.GoodsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoodsService {
    private final GoodsRepository goodsRepository;

    public GoodsService(GoodsRepository goodsRepository) {
        this.goodsRepository = goodsRepository;
    }

    public List<Product> findAllGoods() {
        return goodsRepository.findAll();
    }

}
