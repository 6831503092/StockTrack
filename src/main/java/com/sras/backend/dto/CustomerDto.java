package com.sras.backend.dto;

import com.sras.backend.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
    private Integer customerId;
    private String customerName;
    private Boolean isActive;

    public static CustomerDto fromEntity(Customer customer) {
        if (customer == null) return null;
        return CustomerDto.builder()
                .customerId(customer.getCustomerId())
                .customerName(customer.getCustomerName())
                .isActive(customer.getIsActive())
                .build();
    }
}
