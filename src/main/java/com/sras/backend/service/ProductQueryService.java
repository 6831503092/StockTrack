package com.sras.backend.service;

import com.sras.backend.dto.ProductDto;
import com.sras.backend.dto.ZoneProductSummaryDto;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.Zone;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final ZoneRepository zoneRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ZoneProductSummaryDto getZoneProductSummary() {
        List<Zone> allZones = zoneRepository.findAll();
        long activeZonesCount = allZones.size();
        List<Product> products = productRepository.findByIsActiveTrue();

        long storedUnits = 0;
        long lowStockCount = 0;

        for (Product p : products) {
            int stock = p.getCurrentStock() != null ? p.getCurrentStock() : 0;
            int minStock = p.getMinStockAlert() != null ? p.getMinStockAlert() : 20;
            storedUnits += stock;
            if (stock <= minStock) {
                lowStockCount++;
            }
        }

        long totalCapacity = allZones.stream()
                .mapToLong(z -> z.getCapacity() != null ? z.getCapacity() : 500L)
                .sum();

        return ZoneProductSummaryDto.builder()
                .activeZonesCount(activeZonesCount)
                .storedUnits(storedUnits)
                .totalProducts(products.size())
                .lowStockCount(lowStockCount)
                .totalCapacity(totalCapacity)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getZoneProducts(String zoneCode, String search) {
        String q = search != null ? search.trim().toLowerCase() : "";
        String zFilter = zoneCode != null ? zoneCode.trim() : "";

        return productRepository.findByIsActiveTrue().stream()
                .filter(p -> zFilter.isEmpty() || zFilter.equalsIgnoreCase("ALL") ||
                        (p.getZone() != null && p.getZone().getZoneCode().equalsIgnoreCase(zFilter)))
                .filter(p -> {
                    if (q.isEmpty()) return true;
                    boolean matchId = p.getProductId() != null && String.valueOf(p.getProductId()).contains(q);
                    boolean matchName = p.getProductName() != null && p.getProductName().toLowerCase().contains(q);
                    boolean matchZone = p.getZone() != null && (
                            p.getZone().getZoneCode().toLowerCase().contains(q) ||
                            p.getZone().getZoneName().toLowerCase().contains(q)
                    );
                    return matchId || matchName || matchZone;
                })
                .sorted(Comparator.comparing(Product::getProductId))
                .map(ProductDto::fromEntity)
                .toList();
    }
}
