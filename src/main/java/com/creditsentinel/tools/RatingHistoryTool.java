package com.creditsentinel.tools;

import com.creditsentinel.book.BookReader;
import com.creditsentinel.book.RatingHistoryView;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class RatingHistoryTool {

    private final BookReader book;

    public RatingHistoryTool(BookReader book) {
        this.book = book;
    }

    @Tool(name = "getRatingHistory", description = "Read the internal rating history for one borrower. crossedBelowInvestmentGrade is true only when a downgrade moves from BBB or stronger to BB or weaker. Does not assign a new rating.")
    public RatingHistoryView getRatingHistory(
            @ToolParam(description = "Borrower UUID") String borrowerId) {
        return book.ratingHistory(UUID.fromString(borrowerId));
    }
}
