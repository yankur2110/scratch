package com.scratch.game.service;

import com.scratch.game.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class GameService {
    private final GameConfig config;
    private final Random random;

    public GameService(GameConfig config) {
        this.config = config;
        this.random = new Random();
    }

    public GameResult play(double betAmount) {
        String[][] matrix = generateMatrix();

        Map<String, Integer> symbolCounts = countSymbols(matrix);
        Map<String, Map<String, List<List<String>>>> linearMatches = findLinearMatches(matrix);

        Map<String, List<String>> appliedWinCombinations = new HashMap<>();
        double totalReward = 0;

        // Process standard symbols winning combinations
        for (String symbol : symbolCounts.keySet()) {
            if (config.getSymbols().get(symbol).isStandard()) {
                double symbolReward = calculateSymbolReward(symbol, symbolCounts.get(symbol), linearMatches, appliedWinCombinations, betAmount);
                totalReward += symbolReward;
            }
        }

        // If no win combinations, return result with zero reward
        if (totalReward == 0) {
            GameResult result = new GameResult();
            result.setMatrix(matrix);
            result.setReward(0);
            return result;
        }

        // Process bonus symbols
        String bonusSymbol = findBonusSymbolInMatrix(matrix);
        double finalReward = applyBonusSymbol(totalReward, bonusSymbol);

        // Create result
        GameResult result = new GameResult();
        result.setMatrix(matrix);
        result.setReward(finalReward);
        result.setAppliedWinningCombinations(appliedWinCombinations);
        if (bonusSymbol != null && !bonusSymbol.equals("MISS")) {
            result.setAppliedBonusSymbol(bonusSymbol);
        }

        return result;
    }

    private String[][] generateMatrix() {
        int rows = config.getRows();
        int columns = config.getColumns();
        String[][] matrix = new String[rows][columns];

        // Fill matrix with standard symbols based on probabilities
        for (CellProbability cellProb : config.getProbabilities().getStandardSymbols()) {
            int row = cellProb.getRow();
            int column = cellProb.getColumn();

            if (row < rows && column < columns) {
                matrix[row][column] = selectSymbolByProbability(cellProb.getSymbols());
            }
        }

        // Fill any missing cells with standard symbols using default probabilities
        CellProbability defaultCellProb = config.getProbabilities().getStandardSymbols().get(0);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (matrix[i][j] == null) {
                    matrix[i][j] = selectSymbolByProbability(defaultCellProb.getSymbols());
                }
            }
        }

        // Randomly replace one cell with a bonus symbol
        int bonusRow = random.nextInt(rows);
        int bonusColumn = random.nextInt(columns);
        String bonusSymbol = selectSymbolByProbability(config.getProbabilities().getBonusSymbols().getSymbols());
        matrix[bonusRow][bonusColumn] = bonusSymbol;

        return matrix;
    }

    private String selectSymbolByProbability(Map<String, Integer> symbolProbabilities) {
        int totalProbability = symbolProbabilities.values().stream().mapToInt(Integer::intValue).sum();
        int randomValue = random.nextInt(totalProbability) + 1;

        int cumulativeProbability = 0;
        for (Map.Entry<String, Integer> entry : symbolProbabilities.entrySet()) {
            cumulativeProbability += entry.getValue();
            if (randomValue <= cumulativeProbability) {
                return entry.getKey();
            }
        }

        // Default return (should not reach here)
        return symbolProbabilities.keySet().iterator().next();
    }

    private Map<String, Integer> countSymbols(String[][] matrix) {
        Map<String, Integer> counts = new HashMap<>();

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                String symbol = matrix[i][j];
                if (config.getSymbols().containsKey(symbol) && config.getSymbols().get(symbol).isStandard()) {
                    counts.put(symbol, counts.getOrDefault(symbol, 0) + 1);
                }
            }
        }

        return counts;
    }

    private Map<String, Map<String, List<List<String>>>> findLinearMatches(String[][] matrix) {
        Map<String, Map<String, List<List<String>>>> result = new HashMap<>();

        for (Map.Entry<String, WinCombination> entry : config.getWinCombinations().entrySet()) {
            String combinationName = entry.getKey();
            WinCombination combination = entry.getValue();

            if ("linear_symbols".equals(combination.getWhen()) && combination.getCoveredAreas() != null) {
                for (List<String> area : combination.getCoveredAreas()) {
                    String firstSymbol = null;
                    boolean isMatch = true;

                    List<int[]> positions = new ArrayList<>();
                    for (String position : area) {
                        String[] parts = position.split(":");
                        int row = Integer.parseInt(parts[0]);
                        int col = Integer.parseInt(parts[1]);

                        if (row < matrix.length && col < matrix[row].length) {
                            String symbol = matrix[row][col];

                            if (config.getSymbols().containsKey(symbol) && config.getSymbols().get(symbol).isStandard()) {
                                if (firstSymbol == null) {
                                    firstSymbol = symbol;
                                } else if (!symbol.equals(firstSymbol)) {
                                    isMatch = false;
                                    break;
                                }

                                positions.add(new int[]{row, col});
                            } else {
                                isMatch = false;
                                break;
                            }
                        } else {
                            isMatch = false;
                            break;
                        }
                    }

                    if (isMatch && firstSymbol != null) {
                        result.computeIfAbsent(firstSymbol, k -> new HashMap<>())
                                .computeIfAbsent(combination.getGroup(), k -> new ArrayList<>())
                                .add(area);
                    }
                }
            }
        }

        return result;
    }

    private double calculateSymbolReward(String symbol, int count,
                                         Map<String, Map<String, List<List<String>>>> linearMatches,
                                         Map<String, List<String>> appliedWinCombinations,
                                         double betAmount) {
        double reward = 0;
        double symbolMultiplier = config.getSymbols().get(symbol).getRewardMultiplier();
        List<String> appliedCombinations = new ArrayList<>();

        // Check same symbol count combinations
        for (Map.Entry<String, WinCombination> entry : config.getWinCombinations().entrySet()) {
            WinCombination combination = entry.getValue();

            if ("same_symbols".equals(combination.getWhen()) &&
                    combination.getCount() != null &&
                    count >= combination.getCount()) {

                // Find the highest count combination in the same group
                String highestCombination = findHighestCountCombination(symbol, count, combination.getGroup());

                if (highestCombination != null) {
                    WinCombination highestWinComb = config.getWinCombinations().get(highestCombination);
                    reward = betAmount * symbolMultiplier * highestWinComb.getRewardMultiplier();
                    appliedCombinations.add(highestCombination);
                    break;
                }
            }
        }

        // Check linear combinations
        if (linearMatches.containsKey(symbol)) {
            Map<String, List<List<String>>> symbolLinearMatches = linearMatches.get(symbol);

            for (String group : symbolLinearMatches.keySet()) {
                // Find the win combination for this group
                String combinationName = findCombinationByGroup(group);

                if (combinationName != null) {
                    WinCombination combination = config.getWinCombinations().get(combinationName);

                    if (reward == 0) {
                        reward = betAmount * symbolMultiplier * combination.getRewardMultiplier();
                    } else {
                        reward *= combination.getRewardMultiplier();
                    }

                    appliedCombinations.add(combinationName);
                }
            }
        }

        // If there are applied combinations, add them to the result
        if (!appliedCombinations.isEmpty()) {
            appliedWinCombinations.put(symbol, appliedCombinations);
        }

        return reward;
    }

    private String findHighestCountCombination(String symbol, int count, String group) {
        String highestCombination = null;
        int highestCount = 0;

        for (Map.Entry<String, WinCombination> entry : config.getWinCombinations().entrySet()) {
            WinCombination combination = entry.getValue();

            if (group.equals(combination.getGroup()) &&
                    "same_symbols".equals(combination.getWhen()) &&
                    combination.getCount() != null &&
                    count >= combination.getCount() &&
                    combination.getCount() > highestCount) {

                highestCount = combination.getCount();
                highestCombination = entry.getKey();
            }
        }

        return highestCombination;
    }

    private String findCombinationByGroup(String group) {
        for (Map.Entry<String, WinCombination> entry : config.getWinCombinations().entrySet()) {
            if (group.equals(entry.getValue().getGroup())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String findBonusSymbolInMatrix(String[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                String symbol = matrix[i][j];
                if (config.getSymbols().containsKey(symbol) &&
                        config.getSymbols().get(symbol).isBonus()) {
                    return symbol;
                }
            }
        }
        return null;
    }

    private double applyBonusSymbol(double reward, String bonusSymbol) {
        if (bonusSymbol == null || "MISS".equals(bonusSymbol)) {
            return reward;
        }

        Symbol symbol = config.getSymbols().get(bonusSymbol);

        if ("multiply_reward".equals(symbol.getImpact())) {
            return reward * symbol.getRewardMultiplier();
        } else if ("extra_bonus".equals(symbol.getImpact()) && symbol.getExtra() != null) {
            return reward + symbol.getExtra();
        }

        return reward;
    }
}