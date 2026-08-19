package com.payxpress.service;

import com.payxpress.biller.BillerApiClient;
import com.payxpress.dto.BillerListRequest;
import com.payxpress.dto.BillerListResponse;
import com.payxpress.model.Biller;
import com.payxpress.repository.BillerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class BillerSyncService {

    private final BillerApiClient billerApiClient;
    private final BillerRepository billerRepository;

    public BillerSyncService(BillerApiClient billerApiClient, BillerRepository billerRepository) {
        this.billerApiClient = billerApiClient;
        this.billerRepository = billerRepository;
    }

    /** Pulls the full paginated /products catalog and upserts into the Biller table. Safe to re-run. */
    @Transactional
    public void syncAllBillers() {
        int page = 0;
        boolean hasNext = true;

        while (hasNext) {
            BillerListResponse response = billerApiClient.get(
                    "/products", new BillerListRequest(50, page), BillerListResponse.class);

            BillerListResponse.Data data = response.data();
            for (BillerListResponse.ProductDTO product : data.productDTOList()) {
                upsertBiller(product);
            }

            hasNext = data.hasNextRecord();
            page++;
        }
    }

    private void upsertBiller(BillerListResponse.ProductDTO product) {
        Biller biller = billerRepository.findByUniqueCode(product.uniqueCode()).orElseGet(Biller::new);

        biller.setUniqueCode(product.uniqueCode());
        biller.setName(product.name());
        biller.setBillerName(product.billerName());
        biller.setCategoryName(product.categoryDTO().name());
        biller.setCategoryPath(resolveCategoryPath(product.categoryDTO().name()));
        biller.setRequiresLookup(product.lookUp());
        biller.setFixedAmount(product.fixedAmount());
        biller.setAmount(product.amount());
        biller.setMinimumAmount(product.minimumAmount());
        biller.setMaximumAmount(product.maximumAmount());
        biller.setImageUrl(product.imageUrl());
        biller.setLastSyncedAt(Instant.now());

        billerRepository.save(biller);
    }

    private String resolveCategoryPath(String categoryName) {
        return switch (categoryName.toUpperCase()) {
            case "AIRTIME" -> "airtime";
            case "DATA" -> "data";
            case "ELECTRICITY" -> "electricity";
            case "CABLE TV", "CABLETV" -> "cable";
            case "EDUCATION" -> "education"; // WAEC PIN specifically handled separately at fulfil time
            default -> categoryName.toLowerCase().replace(" ", "-");
        };
    }
}
