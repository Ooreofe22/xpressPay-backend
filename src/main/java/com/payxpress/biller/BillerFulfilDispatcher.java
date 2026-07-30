package com.payxpress.biller;

import com.payxpress.model.PaymentTransaction;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BillerFulfilDispatcher {

    private final AirtimeDataService airtimeDataService;
    private final ElectricityService electricityService;
    private final CableTvService cableTvService;
    private final EducationService educationService;

    public BillerFulfilDispatcher(AirtimeDataService airtimeDataService, ElectricityService electricityService,
                                   CableTvService cableTvService, EducationService educationService) {
        this.airtimeDataService = airtimeDataService;
        this.electricityService = electricityService;
        this.cableTvService = cableTvService;
        this.educationService = educationService;
    }

    public FulfilOutcome dispatch(PaymentTransaction txn, Map<String, Object> details) {
        return switch (txn.getBillerCategoryPath()) {
            case "airtime", "data" -> FulfilOutcome.from(airtimeDataService.fulfil(
                    txn.getBillerCategoryPath(), txn.getInternalRequestId(), txn.getBillerUniqueCode(),
                    (String) details.get("phoneNumber"), txn.getAmount()));

            case "electricity" -> FulfilOutcome.from(electricityService.fulfil(
                    txn.getInternalRequestId(), txn.getBillerUniqueCode(),
                    (String) details.get("accountNumber"), (String) details.get("accountType"), txn.getAmount(),
                    (String) details.get("name"), (String) details.get("address")));

            case "cable" -> FulfilOutcome.from(cableTvService.fulfil(
                    txn.getInternalRequestId(), txn.getBillerUniqueCode(),
                    (String) details.get("accountNumber"), (String) details.get("accountType"),
                    toInt(details.get("noOfMonth")), txn.getAmount(),
                    (String) details.get("name"), (String) details.get("phoneNumber")));

            case "education/waec/pin" -> FulfilOutcome.from(educationService.buyWaecPin(
                    txn.getInternalRequestId(), txn.getBillerUniqueCode(),
                    (String) details.get("email"), (String) details.get("phoneNumber")));

            default -> throw new IllegalArgumentException("Unsupported category: " + txn.getBillerCategoryPath());
        };
    }

    private int toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(value));
    }
}
