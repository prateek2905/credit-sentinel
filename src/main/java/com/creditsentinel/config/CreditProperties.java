package com.creditsentinel.config;

import com.creditsentinel.domain.LlmMode;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "creditsentinel")
public class CreditProperties {

    private LlmMode llmMode = LlmMode.SCRIPTED;
    private BigDecimal concentrationCap = new BigDecimal("0.35");
    private int maxModelTurns = 6;

    public LlmMode getLlmMode() {
        return llmMode;
    }

    public void setLlmMode(LlmMode llmMode) {
        this.llmMode = llmMode;
    }

    public BigDecimal getConcentrationCap() {
        return concentrationCap;
    }

    public void setConcentrationCap(BigDecimal concentrationCap) {
        this.concentrationCap = concentrationCap;
    }

    public int getMaxModelTurns() {
        return maxModelTurns;
    }

    public void setMaxModelTurns(int maxModelTurns) {
        this.maxModelTurns = maxModelTurns;
    }
}
