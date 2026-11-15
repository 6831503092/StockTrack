package com.sras.backend.controller;

import com.sras.backend.dto.ApiResponse;
import com.sras.backend.dto.ProductDto;
import com.sras.backend.dto.ProductUpdateRequest;
import com.sras.backend.entity.User;
import com.sras.backend.enums.Role;
import com.sras.backend.security.CurrentUser;
import com.sras.backend.security.RequireRoles;
import com.sras.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id:[0-9]+}")
    @RequireRoles({Role.STAFF, Role.MANAGER, Role.ADMIN, Role.OWNER})
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(
            @PathVariable("id") Integer id
    ) {
        ProductDto product = productService.getProduct(id);
        return ResponseEntity.ok(ApiResponse.ok("Product retrieved successfully", product));
    }

    @PutMapping("/{id:[0-9]+}")
    @RequireRoles({Role.MANAGER, Role.OWNER, Role.ADMIN})
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable("id") Integer id,
            @RequestBody ProductUpdateRequest req,
            @CurrentUser User managerUser
    ) {
        ProductDto updated = productService.updateProduct(id, req, managerUser);
        return ResponseEntity.ok(ApiResponse.ok("Product updated successfully", updated));
    }
}
