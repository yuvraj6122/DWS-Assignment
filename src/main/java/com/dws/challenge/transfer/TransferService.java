package com.dws.challenge.transfer;

import lombok.SneakyThrows;

import java.math.BigDecimal;

public interface TransferService {

    void transferMoney(String accountFromId, String accountToId, BigDecimal amount);
}
