package com.creditsentinel.tools;

import com.creditsentinel.book.BookReader;
import com.creditsentinel.book.PriorCasesView;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class PriorCasesTool {

    private final BookReader book;

    public PriorCasesTool(BookReader book) {
        this.book = book;
    }

    @Tool(name = "searchPriorCases", description = "Read approved or rejected credit cases already on file for this borrower. Does not include the case currently being investigated.")
    public PriorCasesView searchPriorCases(
            @ToolParam(description = "Borrower UUID") String borrowerId) {
        return book.priorCases(UUID.fromString(borrowerId));
    }
}
