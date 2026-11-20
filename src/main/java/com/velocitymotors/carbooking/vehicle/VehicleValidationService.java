package com.velocitymotors.carbooking.vehicle;

import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class VehicleValidationService {
    private static final Set<String> AVAILABLE_IDS = Set.of("VEH001", "VEH002", "VEH003", "VEH004");

    public void validate(String vehicleId) {
        if (!AVAILABLE_IDS.contains(vehicleId)) {
            throw new IllegalArgumentException("Unknown vehicle ID");
        }
    }
}
