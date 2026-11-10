package com.creditsentinel.tools;

import com.creditsentinel.book.BookReader;
import com.creditsentinel.book.BorrowerProfileView;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class BorrowerProfileTool {

    private final BookReader book;

    public BorrowerProfileTool(BookReader book) {
        this.book = book;
    }

    @Tool(name = "getBorrowerProfile", description = "Read the legal name, sector, headquarters, and relationship start date for one borrower. Does not change any data.")
    public BorrowerProfileView getBorrowerProfile(
            @ToolParam(description = "Borrower UUID") String borrowerId) {
        return book.borrowerProfile(UUID.fromString(borrowerId));
    }
}
