# Scratch Game

A Java implementation of a scratch game that generates a matrix of symbols and calculates rewards based on winning combinations.

## Requirements

- JDK 8 or higher
- Maven

## Building the Application

1. Clone the repository
2. Build the application using Maven:
```
mvn clean package
```

This will create a JAR file in the `target` directory: `scratch-game-1.0-SNAPSHOT-jar-with-dependencies.jar`

## Running the Application

Run the application with:
```
java -jar target/scratch-game-1.0-SNAPSHOT-jar-with-dependencies.jar --config config.json --betting-amount 100
```

Parameters:
- `--config`: Path to the configuration file (JSON)
- `--betting-amount`: Amount to bet

## Configuration File

The configuration file defines:
- Matrix dimensions
- Symbol types and their reward multipliers
- Symbol probabilities
- Winning combinations

See the provided `config.json` for an example.

## Output

The application generates a JSON output containing:
- The generated matrix
- The calculated reward
- Applied winning combinations
- Applied bonus symbol (if any)

Example output:
```json
{
  "matrix": [
    ["A", "B", "C"],
    ["E", "B", "10x"],
    ["F", "D", "B"]
  ],
  "reward": 3000,
  "applied_winning_combinations": {
    "B": ["same_symbol_3_times"]
  },
  "applied_bonus_symbol": "10x"
}
```
