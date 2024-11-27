package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.transfer.TransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Objects;


@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

  private final AccountsService accountsService;
  private final TransferService transferService;

  @Autowired
  public AccountsController(AccountsService accountsService, TransferService transferService) {
    this.accountsService = accountsService;
    this.transferService = transferService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid @NotNull Account account) {
    log.info("Creating account {}", account);
    BigDecimal balance = account.getBalance();
    String accountId = account.getAccountId();

    // Had to add these checks because @NotNull @Valid @Min were not working correctly, and assertions were failing in UTs
    boolean isIdNull = Objects.isNull(accountId);
    boolean isBalanceNull = Objects.isNull(balance);
    boolean isIdEmpty = !isIdNull && accountId.isEmpty();
    boolean isBalanceNegative = !isBalanceNull && balance.compareTo(BigDecimal.ZERO) < 0;

    if (isIdNull || isBalanceNull || isIdEmpty || isBalanceNegative) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }


      try {
        this.accountsService.createAccount(account);
      } catch (DuplicateAccountIdException daie) {
        return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
      }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public Account getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    return this.accountsService.getAccount(accountId);
  }

  @PostMapping(path = "/{accountFromId}/transfer/{accountToId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> transferMoney(
          @PathVariable String accountFromId,
          @PathVariable String accountToId,
          @RequestParam BigDecimal amount) {

    try {
      transferService.transferMoney(accountFromId, accountToId, amount);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (InsufficientBalanceException | AccountNotFoundException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }
}
