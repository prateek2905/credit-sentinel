package com.creditsentinel.tools;

import com.creditsentinel.book.BookReader;
import com.creditsentinel.book.ExposureView;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class FacilityExposureTool {

    private final BookReader book;

    public FacilityExposureTool(BookReader book) {
        this.book = book;
    }

    @Tool(name = "getFacilityExposure", description = "Read committed limit, outstanding, and utilization for one facility. Utilization above 1 means the borrower has drawn more than the bank committed. Does not change the limit.")
    public ExposureView getFacilityExposure(
            @ToolParam(description = "Facility UUID") String facilityId) {
        return book.exposure(UUID.fromString(facilityId));
    }
}
