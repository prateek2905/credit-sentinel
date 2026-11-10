package com.creditsentinel.book;

public record CovenantLineView(
        String type,
        String threshold,
        String actual,
        boolean breached,
        String direction) {
}
