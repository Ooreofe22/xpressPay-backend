package com.payxpress.biller;

import java.util.Map;

public record FulfilRequest(String requestId, String uniqueCode, Map<String, Object> details) {}
