package com.creditsentinel.book;

public record BorrowerProfileView(
        String borrowerId,
        String legalName,
        String sector,
        String headquarters,
        String relationshipStart) {
}
