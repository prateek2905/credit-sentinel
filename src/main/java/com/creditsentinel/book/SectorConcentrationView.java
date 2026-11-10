package com.creditsentinel.book;

public record SectorConcentrationView(
        String sector,
        String sectorOutstanding,
        String totalOutstanding,
        String share,
        String policyCap,
        boolean abovePolicyCap) {
}
