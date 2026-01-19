package com.litebank.accountservice.dto;

import com.litebank.accountservice.entity.Recipient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientResponse {

    private Long recipientId;
    private Long userId;
    private String accountNumber;
    private String nickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Account public info (from account lookup)
    private Long accountId;     // Account ID for transfer operations
    private String currency;
    private String status;
    private String fullName;

    public static RecipientResponse fromEntity(Recipient recipient) {
        return RecipientResponse.builder()
                .recipientId(recipient.getRecipientId())
                .userId(recipient.getUserId())
                .accountNumber(recipient.getAccountNumber())
                .nickname(recipient.getNickname())
                .createdAt(recipient.getCreatedAt())
                .updatedAt(recipient.getUpdatedAt())
                .build();
    }

    public static RecipientResponse fromEntityWithAccountInfo(
            Recipient recipient,
            AccountPublicInfoResponse accountInfo) {
        return RecipientResponse.builder()
                .recipientId(recipient.getRecipientId())
                .userId(recipient.getUserId())
                .accountNumber(recipient.getAccountNumber())
                .nickname(recipient.getNickname())
                .createdAt(recipient.getCreatedAt())
                .updatedAt(recipient.getUpdatedAt())
                .accountId(accountInfo.getAccountId())
                .currency(accountInfo.getCurrency())
                .status(accountInfo.getStatus())
                .fullName(accountInfo.getFullName())
                .build();
    }
}
