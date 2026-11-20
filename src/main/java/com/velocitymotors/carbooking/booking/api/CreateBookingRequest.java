package com.velocitymotors.carbooking.booking.api;

import com.velocitymotors.carbooking.booking.domain.PaymentMode;
import com.velocitymotors.carbooking.booking.domain.VehicleCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateBookingRequest(
        @NotBlank @Size(max = 150) String customerName,
        @NotBlank @Size(max = 50) String vehicleId,
        @NotNull LocalDate rentalStartDate,
        @NotNull LocalDate rentalEndDate,
        @NotNull VehicleCategory vehicleCategory,
        @NotNull PaymentMode paymentMode,
        @NotBlank @Size(max = 100) String paymentReference) { }
