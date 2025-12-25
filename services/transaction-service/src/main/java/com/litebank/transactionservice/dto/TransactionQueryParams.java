package com.litebank.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionQueryParams {

    private Long accountId;
    private String transactionType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String referenceId;
    private Integer page;
    private Integer size;
}
