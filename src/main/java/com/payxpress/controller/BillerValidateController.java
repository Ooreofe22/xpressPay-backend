package com.payxpress.controller;

import com.payxpress.service.CableTvService;
import com.payxpress.service.ElectricityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/billers")
public class BillerValidateController {

    private final ElectricityService electricityService;
    private final CableTvService cableTvService;

    public BillerValidateController(ElectricityService electricityService, CableTvService cableTvService) {
        this.electricityService = electricityService;
        this.cableTvService = cableTvService;
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody ValidateRequest request) {
        return switch (request.categoryPath()) {
            case "electricity" -> {
                var result = electricityService.validateMeter(
                        request.uniqueCode(), request.accountNumber(), request.accountType());
                yield result.isSuccessful()
                        ? ResponseEntity.ok(Map.of(
                                "valid", true,
                                "name", result.data().name(),
                                "address", result.data().address(),
                                "minimumVending", result.data().minimumVending()))
                        : ResponseEntity.badRequest().body(Map.of("valid", false, "message", result.responseMessage()));
            }
            case "cable" -> {
                var result = cableTvService.validate(
                        request.uniqueCode(), request.accountNumber(),
                        request.noOfMonth() != null ? request.noOfMonth() : 1);
                yield result.isSuccessful()
                        ? ResponseEntity.ok(Map.of(
                                "valid", true,
                                "name", result.data().name(),
                                "amount", result.data().amount()))
                        : ResponseEntity.badRequest().body(Map.of("valid", false, "message", result.responseMessage()));
            }
            default -> ResponseEntity.badRequest().body(Map.of("message", "No validate step for " + request.categoryPath()));
        };
    }

    public record ValidateRequest(String uniqueCode, String categoryPath, String accountNumber,
                                   String accountType, Integer noOfMonth) {}
}
