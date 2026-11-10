package com.creditsentinel.tools;

import com.creditsentinel.book.BookReader;
import com.creditsentinel.book.CovenantStatusView;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class CovenantStatusTool {

    private final BookReader book;

    public CovenantStatusTool(BookReader book) {
        this.book = book;
    }

    @Tool(name = "getCovenantStatus", description = "Read the latest covenant test for one facility. MIN_DSCR fails when actual is below the minimum. MAX_LEVERAGE fails when actual is above the maximum. Does not waive a covenant.")
    public CovenantStatusView getCovenantStatus(
            @ToolParam(description = "Facility UUID") String facilityId) {
        return book.covenantStatus(UUID.fromString(facilityId));
    }
}
