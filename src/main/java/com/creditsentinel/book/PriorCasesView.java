package com.creditsentinel.book;

import java.util.List;

public record PriorCasesView(int priorCaseCount, List<PriorCaseView> cases) {
}
