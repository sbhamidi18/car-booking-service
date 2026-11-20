package com.velocitymotors.carbooking.payment.banktransfer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BankTransferPaymentRepository extends JpaRepository<BankTransferPayment, String> { }
