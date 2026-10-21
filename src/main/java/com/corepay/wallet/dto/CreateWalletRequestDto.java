package com.corepay.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWalletRequestDto {
    private String customerId;   // Marked private
    private String currencyCode; // Marked private
}