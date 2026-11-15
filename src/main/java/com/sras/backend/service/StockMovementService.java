package com.sras.backend.service;

import com.sras.backend.entity.Product;
import com.sras.backend.entity.Zone;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final ProductRepository productRepository;

    @Transactional
    public Product adjustStock(Product product, int deltaQty) {
        if (product == null) {
            throw new ValidationException("Product cannot be null for stock adjustment");
        }

        int current = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        int updated = current + deltaQty;

        if (updated < 0) {
            throw new ValidationException("Insufficient stock for " + product.getProductName() +
                    " (Current: " + current + ", Requested delta: " + deltaQty + ")");
        }

        product.setCurrentStock(updated);
        Product saved = productRepository.save(product);
        log.debug("Adjusted stock for product #{} ({}): {} -> {}",
                product.getProductId(), product.getProductName(), current, updated);
        return saved;
    }

    @Transactional
    public void swapZones(Zone fromZone, Zone toZone) {
        if (fromZone == null || toZone == null) {
            throw new ValidationException("Both source and destination zones are required for zone swap");
        }

        List<Product> fromProducts = productRepository.findByZoneZoneIdAndIsActiveTrue(fromZone.getZoneId());
        List<Product> toProducts = productRepository.findByZoneZoneIdAndIsActiveTrue(toZone.getZoneId());

        if (fromProducts != null && !fromProducts.isEmpty()) {
            for (Product p : fromProducts) {
                p.setZone(toZone);
            }
            productRepository.saveAll(fromProducts);
        }

        if (toProducts != null && !toProducts.isEmpty()) {
            for (Product p : toProducts) {
                p.setZone(fromZone);
            }
            productRepository.saveAll(toProducts);
        }

        log.info("Swapped product locations between zone {} and zone {}",
                fromZone.getZoneCode(), toZone.getZoneCode());
    }
}
