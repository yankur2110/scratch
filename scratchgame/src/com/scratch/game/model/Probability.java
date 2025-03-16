package com.scratch.game.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Probability {
    @JsonProperty("standard_symbols")
    private List<CellProbability> standardSymbols;

    @JsonProperty("bonus_symbols")
    private BonusProbability bonusSymbols;

    public List<CellProbability> getStandardSymbols() {
        return standardSymbols;
    }

    public void setStandardSymbols(List<CellProbability> standardSymbols) {
        this.standardSymbols = standardSymbols;
    }

    public BonusProbability getBonusSymbols() {
        return bonusSymbols;
    }

    public void setBonusSymbols(BonusProbability bonusSymbols) {
        this.bonusSymbols = bonusSymbols;
    }
}