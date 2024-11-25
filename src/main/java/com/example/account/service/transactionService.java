package com.example.account.service;

import com.example.account.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class transactionService {
    private final TransactionRepository transactionRepository;
}
