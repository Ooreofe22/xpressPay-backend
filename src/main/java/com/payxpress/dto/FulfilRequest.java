package com.payxpress.dto;

import java.util.Map;

public record FulfilRequest(String requestId, String uniqueCode, Map<String, Object> details) {}
