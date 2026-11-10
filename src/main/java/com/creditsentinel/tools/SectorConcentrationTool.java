package com.creditsentinel.tools;

import com.creditsentinel.book.BookReader;
import com.creditsentinel.book.SectorConcentrationView;
import com.creditsentinel.domain.Sector;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class SectorConcentrationTool {

    private final BookReader book;

    public SectorConcentrationTool(BookReader book) {
        this.book = book;
    }

    @Tool(name = "getSectorConcentration", description = "Read one sector's share of total outstanding versus the policy cap. This is a portfolio fact, not a single-borrower fact.")
    public SectorConcentrationView getSectorConcentration(
            @ToolParam(description = "Sector enum name, for example INDUSTRIALS") String sector) {
        return book.concentration(Sector.valueOf(sector));
    }
}
