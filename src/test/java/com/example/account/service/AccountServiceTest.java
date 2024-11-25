package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.repository.AccountRepository;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountSuccess() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(1L)
                .name("Pobi")
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.existsByAccountNumber(any()))
                .willReturn(false);
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(accountUser)
                        .accountStatus(AccountStatus.IN_USE)
                        .accountNumber("1234567890")
                        .balance(1000L)
                        .build());

        // When
        accountService.createAccount(1L, 1000L);

        // Then
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(1)).save(captor.capture());
        Account savedAccount = captor.getValue();

        assertEquals(accountUser.getId(), savedAccount.getAccountUser().getId());
        assertEquals(1000L, savedAccount.getBalance());
        assertEquals(AccountStatus.IN_USE, savedAccount.getAccountStatus());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    void createAccount_userNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // When
        AccountException exception = assertThrows(AccountException.class, () -> accountService.createAccount(1L, 1000L));

        // Then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("유저 당 최대 계좌 10개")
    void createAccount_maxAccountIs10() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(1L)
                .name("Pobi")
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);

        // When
        AccountException exception = assertThrows(AccountException.class, () -> accountService.createAccount(1L, 1000L));

        // Then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }

    @Test
    @DisplayName("Success - Delete Account")
    void successDeleteAccount() {
        // Given
        AccountUser accountUser = AccountUser.builder().id(1L).build();
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountNumber("1234567890")
                .accountStatus(AccountStatus.IN_USE)
                .balance(0L)
                .build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.of(account));

        // When
        accountService.deleteAccount(1L, "1234567890");

        // Then
        verify(accountRepository, times(1)).save(account);
        assertEquals(AccountStatus.UNREGISTERED, account.getAccountStatus());
    }

    @Test
    @DisplayName("Fail - User Not Found")
    void deleteAccount_userNotFound() {
        // Given
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());

        // When
        AccountException exception = assertThrows(AccountException.class, () ->
                accountService.deleteAccount(1L, "1234567890"));

        // Then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Fail - Account Not Found")
    void deleteAccount_AccountNotFound() {
        // Given
        AccountUser accountUser = AccountUser.builder().id(1L).build();
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.empty());

        // When
        AccountException exception = assertThrows(AccountException.class, () ->
                accountService.deleteAccount(1L, "1234567890"));

        // Then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Fail - User and Account Mismatch")
    void deleteAccountFailed_userUnMatch() {
        // Given
        AccountUser accountUser = AccountUser.builder().id(1L).build();
        Account anotherAccount = Account.builder()
                .accountUser(AccountUser.builder().id(2L).build())
                .accountNumber("1234567890")
                .build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.of(anotherAccount));

        // When
        AccountException exception = assertThrows(AccountException.class, () ->
                accountService.deleteAccount(1L, "1234567890"));

        // Then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("Fail - Balance Not Empty")
    void deleteAccountFailed_balanceNotEmpty() {
        // Given
        AccountUser accountUser = AccountUser.builder().id(1L).build();
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountNumber("1234567890")
                .balance(100L)
                .build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.of(account));

        // When
        AccountException exception = assertThrows(AccountException.class, () ->
                accountService.deleteAccount(1L, "1234567890"));

        // Then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

//
//    @Test
//    @DisplayName("계좌 조회 성공")
//    void testXXX() {
//        //given
//        given(accountRepository.findById(anyLong()))
//                .willReturn(Optional.of(Account.builder()
//                        .accountStatus(AccountStatus.UNREGISTERED)
//                        .accountNumber("65789").build()));
//        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
//
//        //when
//        Account account = accountService.getAccount(4555L);
//
//        //then
//        verify(accountRepository, times(1)).findById(captor.capture());
//        verify(accountRepository, times(0)).save(any());
//        assertEquals(4555L, captor.getValue());
//        assertNotEquals(45515L, captor.getValue());
//        assertEquals("65789", account.getAccountNumber());
//        assertEquals(AccountStatus.UNREGISTERED, account.getAccountStatus());
//    }
//
//    @Test
//    @DisplayName("계좌 조회 실패 - 음수로 조회")
//    void testFailedToSearchAccount() {
//        //given
//        //when
//        RuntimeException exception = assertThrows(RuntimeException.class,
//                () -> accountService.getAccount(-10L));
//
//        //then
//        assertEquals("Minus", exception.getMessage());
//    }
//
//    @Test
//    @DisplayName("Test 이름 변경")
//    void testGetAccount() {
//        //given
//        given(accountRepository.findById(anyLong()))
//                .willReturn(Optional.of(Account.builder()
//                        .accountStatus(AccountStatus.UNREGISTERED)
//                        .accountNumber("65789").build()));
//
//        //when
//        Account account = accountService.getAccount(4555L);
//
//        //then
//        assertEquals("65789", account.getAccountNumber());
//        assertEquals(AccountStatus.UNREGISTERED, account.getAccountStatus());
//    }
//
//    @Test
//    void testGetAccount2() {
//        //given
//        given(accountRepository.findById(anyLong()))
//                .willReturn(Optional.of(Account.builder()
//                        .accountStatus(AccountStatus.UNREGISTERED)
//                        .accountNumber("65789").build()));
//
//        //when
//        Account account = accountService.getAccount(4555L);
//
//        //then
//        assertEquals("65789", account.getAccountNumber());
//        assertEquals(AccountStatus.UNREGISTERED, account.getAccountStatus());
//    }
}