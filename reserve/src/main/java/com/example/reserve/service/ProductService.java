package com.example.reserve.service;

import com.example.reserve.entity.Stock;
import com.example.reserve.repository.ProductRepository;
import com.example.reserve.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockRepository stockRepository;

    // 재고 감소
    @Transactional
    public void decreaseStock(Long productId, int amount) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 제품의 재고가 없습니다."));

        stock.decrease(amount);
        stockRepository.save(stock);
    }
}