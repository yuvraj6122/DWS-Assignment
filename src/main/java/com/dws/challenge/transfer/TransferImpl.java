package com.dws.challenge.transfer;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class TransferImpl implements TransferService {

    private final AccountsRepository accountsRepository;
    private final NotificationService notificationService;

    @Autowired
    public TransferImpl(AccountsRepository accountsRepository, NotificationService notificationService) {
        this.accountsRepository = accountsRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void transferMoney(String accountFromId, String accountToId, BigDecimal amount) {
        Account accountFrom = accountsRepository.getAccount(accountFromId);
        Account accountTo = accountsRepository.getAccount(accountToId);

        boolean sourceAccount = Objects.isNull(accountFrom);
        boolean destinationAccount = Objects.isNull(accountTo);

        if (sourceAccount && destinationAccount) {
            throw new AccountNotFoundException("Neither account was not present in our database");
        } else if (sourceAccount){
            throw new AccountNotFoundException("Source account was not present in our database");
        } else if (destinationAccount){
            throw new AccountNotFoundException("Destination account was not present in our database");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        synchronized (this) {
            if (accountFrom.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException("Insufficient balance");
            }
            accountFrom.setBalance(accountFrom.getBalance().subtract(amount));
            accountTo.setBalance(accountTo.getBalance().add(amount));
        }

        String sourceMessage = String.format("Transferred %.2f to account %s", amount, accountToId);
        String destinationMessage =  String.format("Received %.2f from account %s", amount, accountFromId);
        notificationService.notifyAboutTransfer(accountFrom, sourceMessage);
        notificationService.notifyAboutTransfer(accountTo, destinationMessage);
    }
}
