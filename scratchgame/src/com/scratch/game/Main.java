package com.scratch.game;

import com.scratch.game.model.GameConfig;
import com.scratch.game.model.GameResult;
import com.scratch.game.service.GameService;
import com.scratch.game.util.JsonUtil;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String configFilePath = null;
        double betAmount = 0;

        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if ("--config".equals(args[i]) && i + 1 < args.length) {
                configFilePath = args[i + 1];
                i++;
            } else if ("--betting-amount".equals(args[i]) && i + 1 < args.length) {
                try {
                    betAmount = Double.parseDouble(args[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid betting amount: " + args[i + 1]);
                    printUsage();
                    System.exit(1);
                }
            }
        }

        // Validate arguments
        if (configFilePath == null || betAmount <= 0) {
            printUsage();
            System.exit(1);
        }

        try {
            // Read game configuration
            GameConfig config = JsonUtil.readValue(new File(configFilePath), GameConfig.class);

            // Initialize game service
            GameService gameService = new GameService(config);

            // Play the game
            GameResult result = gameService.play(betAmount);

            // Print the result
            System.out.println(JsonUtil.writeValueAsString(result));

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar scratch-game.jar --config <config-file> --betting-amount <amount>");
    }
}