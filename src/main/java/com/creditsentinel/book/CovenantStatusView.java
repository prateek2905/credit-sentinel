package com.creditsentinel.book;

import java.util.List;

public record CovenantStatusView(String facilityId, List<CovenantLineView> covenants) {
}
