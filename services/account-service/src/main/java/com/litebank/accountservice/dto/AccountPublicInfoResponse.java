package com.litebank.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public account information response (without sensitive data like balance)
 * Used for account lookup by account number in scenarios like transfer recipient selection
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountPublicInfoResponse {

    private Long accountId;     // Account ID for transfer operations
    private String accountNumber;
    private String currency;
    private String status;
    private String fullName;  // Account owner's full name (from users.full_name, fallback to username)

}
