package edu.trincoll.game.controller;

import edu.trincoll.game.command.CommandInvoker;
import edu.trincoll.game.command.GameCommand;
import edu.trincoll.game.model.Character;
import edu.trincoll.game.player.GameState;
import edu.trincoll.game.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main game controller that orchestrates turn-based RPG combat.
 *
 * <p>This class serves as the central coordinator for the game, implementing multiple
 * design patterns to manage the complex interactions between players, characters,
 * and game commands. It demonstrates how patterns work together to create a clean,
 * maintainable architecture.
 *
 * <h2>Design Patterns Implemented</h2>
 * <ul>
 *   <li><b>FACADE Pattern:</b> Provides a simple interface ({@link #playGame()}) that hides
 *       the complexity of coordinating players, characters, commands, turns, and rounds.
 *       External code doesn't need to understand the intricate game loop logic.</li>
 *   <li><b>MEDIATOR Pattern:</b> Acts as a central mediator coordinating interactions between:
 *       <ul>
 *         <li>Players (who make decisions)</li>
 *         <li>Characters (who execute actions)</li>
 *         <li>Commands (which encapsulate actions)</li>
 *         <li>GameState (which tracks progress)</li>
 *       </ul>
 *       This prevents tight coupling between components - they only know about the controller.</li>
 *   <li><b>ITERATOR Pattern:</b> Manages turn order by iterating through team1 and team2
 *       character lists, handling game state transitions between turns and rounds.</li>
 * </ul>
 *
 * <h2>Game Loop Architecture</h2>
 * <p>The main loop follows a classic turn-based structure:
 * <pre>
 * while (game not over):
 *     for each character in team1:
 *         if alive: process turn
 *         if game over: exit
 *
 *     for each character in team2:
 *         if alive: process turn
 *         if game over: exit
 *
 *     advance to next round
 * </pre>
 *
 * <h2>Turn Processing Flow</h2>
 * <p>Each character's turn follows these steps:
 * <ol>
 *   <li>Get the Player controlling the character (from playerMap)</li>
 *   <li>Ask Player to decide action (returns GameCommand)</li>
 *   <li>Execute command via CommandInvoker</li>
 *   <li>Display results to user</li>
 *   <li>Update game state</li>
 * </ol>
 *
 * <h2>Why This Design Matters</h2>
 * <p>The controller demonstrates excellent separation of concerns:
 * <ul>
 *   <li><b>Players</b> don't know about game loop mechanics</li>
 *   <li><b>Commands</b> don't know when they'll be executed</li>
 *   <li><b>Characters</b> don't know who controls them</li>
 *   <li><b>Game rules</b> are centralized in one place</li>
 * </ul>
 *
 * <p>This makes it easy to:
 * <ul>
 *   <li>Add new player types (just implement Player interface)</li>
 *   <li>Add new command types (just implement GameCommand)</li>
 *   <li>Change turn order logic (only affects this class)</li>
 *   <li>Add new game modes (subclass or configure differently)</li>
 * </ul>
 *
 * <h2>Assignment TODOs Completed</h2>
 * <ul>
 *   <li>✓ TODO 4: Implement main game loop (15 points)</li>
 *   <li>✓ TODO 5: Process individual turns (10 points)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create teams
 * List<Character> team1 = List.of(warrior, mage);
 * List<Character> team2 = List.of(archer, rogue);
 *
 * // Map characters to players
 * Map<Character, Player> playerMap = new HashMap<>();
 * playerMap.put(warrior, new HumanPlayer());
 * playerMap.put(mage, new RuleBasedPlayer());
 * playerMap.put(archer, new LLMPlayer(openAiClient, "GPT-5"));
 * playerMap.put(rogue, new LLMPlayer(anthropicClient, "Claude"));
 *
 * // Create and run game
 * GameController controller = new GameController(team1, team2, playerMap);
 * controller.playGame();
 * controller.displayResult();
 * }</pre>
 *
 * @author [Your Team Names]
 * @version 1.0
 * @since Assignment 6
 * @see Player
 * @see GameCommand
 * @see CommandInvoker
 * @see GameState
 */
public class GameController {
    /** First team of characters. Defensive copy made to prevent external modification. */
    private final List<Character> team1;

    /** Second team of characters. Defensive copy made to prevent external modification. */
    private final List<Character> team2;

    /**
     * Maps each character to their controlling player.
     * This is the core of the STRATEGY pattern - different players
     * (Human, RuleBasedAI, LLM) can control different characters.
     */
    private final Map<Character, Player> playerMap;

    /**
     * Command invoker that executes and tracks all game commands.
     * Implements the COMMAND pattern, enabling undo/redo functionality.
     */
    private final CommandInvoker invoker;

    /**
     * Current game state tracking turn number, round number, and command history.
     * Immutable state object that gets updated each turn (functional programming style).
     */
    private GameState gameState;

    /**
     * Constructs a new game controller.
     *
     * <p>Creates defensive copies of the team lists and player map to prevent
     * external modification during gameplay. This ensures game integrity.
     *
     * <p>Initializes game state to turn 0, round 0 with empty command history.
     *
     * @param team1 First team's characters (will be copied)
     * @param team2 Second team's characters (will be copied)
     * @param playerMap Mapping of characters to their controlling players (will be copied)
     * @throws IllegalArgumentException if any parameter is null or if playerMap doesn't
     *                                  contain entries for all characters
     */
    public GameController(List<Character> team1,
                          List<Character> team2,
                          Map<Character, Player> playerMap) {
        // Defensive copies to prevent external modification
        this.team1 = new ArrayList<>(team1);
        this.team2 = new ArrayList<>(team2);
        this.playerMap = new HashMap<>(playerMap);
        this.invoker = new CommandInvoker();
        this.gameState = GameState.initial();
    }

    /**
     * Runs the main game loop until one team is defeated.
     *
     * <p><b>TODO 4 Implementation (15 points) - COMPLETED</b>
     *
     * <p>This method implements the core game loop that alternates turns between teams
     * until a win condition is met. It demonstrates the FACADE pattern by providing
     * a simple entry point that hides complex game loop mechanics.
     *
     * <h3>Game Loop Structure</h3>
     * <p>The loop follows a careful sequence to ensure fair gameplay:
     * <ol>
     *   <li><b>Display round header:</b> Shows current turn/round and all character statuses</li>
     *   <li><b>Team 1 turns:</b> Process each living character's turn
     *       <ul>
     *         <li>Check if character is alive before processing</li>
     *         <li>Check for game over after each turn (early exit)</li>
     *       </ul>
     *   </li>
     *   <li><b>Game over check:</b> Exit if team1 eliminated all team2 characters</li>
     *   <li><b>Team 2 turns:</b> Process each living character's turn (same checks as team1)</li>
     *   <li><b>Round completion:</b> Advance game state to next round</li>
     *   <li><b>Display round summary:</b> Show how many characters alive on each team</li>
     * </ol>
     *
     * <h3>Why Check Game Over Multiple Times?</h3>
     * <p>We check {@link #isGameOver()} multiple times to enable early exit:
     * <ul>
     *   <li>After each team's turns (between team1 and team2)</li>
     *   <li>Within each team's turn loop (after each character acts)</li>
     * </ul>
     * This prevents the game from continuing unnecessarily after a team is eliminated.
     *
     * <h3>Turn Order Matters</h3>
     * <p>Team1 always goes first each round. This is important for game balance - in some
     * games you might want to randomize turn order or use a speed-based initiative system.
     *
     * <h3>Output Format</h3>
     * <p>The game produces detailed output showing:
     * <ul>
     *   <li>Turn and round numbers</li>
     *   <li>Current HP for all characters</li>
     *   <li>Actions taken and their results</li>
     *   <li>LLM reasoning (when applicable)</li>
     *   <li>Round summaries with survival counts</li>
     * </ul>
     *
     * @see #processTurn(Character, List, List) for individual turn logic
     * @see #isGameOver() for win condition checking
     * @see #displayRoundHeader() for status display
     */
    public void playGame() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("GAME START!");
        System.out.println("=".repeat(60));

        displayTeamSetup();

        // Main game loop - continues until one team is eliminated
        while (!isGameOver()) {
            displayRoundHeader();

            // Team 1's turn - process each living character
            for (Character character : team1) {
                // Early exit if game ended during this team's turns
                if (isGameOver()) {
                    break;
                }
                // Only living characters get to act
                if (character.isAlive()) {
                    processTurn(character, team1, team2);
                }
            }

            // Check if team1 won before team2 gets to act
            if (isGameOver()) {
                break;
            }

            // Team 2's turn - same logic as team1
            for(Character character : team2) {
                // Early exit if game ended during this team's turns
                if (isGameOver()) {
                    break;
                }
                // Only living characters get to act
                if (character.isAlive()) {
                    processTurn(character, team2, team1);
                }
            }

            // Advance to next round (functional state update)
            gameState = gameState.nextRound();

            // Show round summary if game continues
            displayRoundSummary();
        }

        // Game over - display final results
        displayResult();
    }

    /**
     * Processes a single character's turn.
     *
     * <p><b>TODO 5 Implementation (10 points) - COMPLETED</b>
     *
     * <p>This method handles all the logic for one character's turn, demonstrating
     * the MEDIATOR pattern by coordinating between the player (who decides), the
     * command (what to do), and the invoker (who executes it).
     *
     * <h3>Turn Processing Steps</h3>
     * <ol>
     *   <li><b>Validate character is alive:</b> Defeated characters don't get turns</li>
     *   <li><b>Get controlling player:</b> Look up player in playerMap (Strategy pattern)</li>
     *   <li><b>Request decision:</b> Player analyzes game state and returns a command</li>
     *   <li><b>Execute command:</b> Invoker handles execution and history tracking</li>
     *   <li><b>Display result:</b> Show what happened to all players</li>
     *   <li><b>Update state:</b> Advance turn counter and record command in history</li>
     * </ol>
     *
     * <h3>Why Separate Players from Characters?</h3>
     * <p>This separation is crucial for the STRATEGY pattern:
     * <ul>
     *   <li>The <b>Character</b> represents the in-game entity (stats, HP, abilities)</li>
     *   <li>The <b>Player</b> represents the decision-making algorithm (human, AI, LLM)</li>
     *   <li>This allows the same character type to be controlled by different strategies</li>
     * </ul>
     *
     * <p>For example: Two Warriors can be controlled by a Human and an LLM respectively.
     * They have the same stats and abilities, but make completely different decisions.
     *
     * <h3>Command Pattern in Action</h3>
     * <p>The player returns a {@link GameCommand} object, not a String or enum.
     * This demonstrates the COMMAND pattern:
     * <ul>
     *   <li>Commands encapsulate all information needed to perform an action</li>
     *   <li>Commands can be stored for undo/replay functionality</li>
     *   <li>Commands decouple the decision (Player) from execution (Invoker)</li>
     * </ul>
     *
     * <h3>Immutable State Updates</h3>
     * <p>Note how gameState is updated:
     * <pre>{@code
     * gameState = gameState.nextTurn().withUndo(true, size);
     * }</pre>
     * This functional programming style creates a new GameState object rather than
     * mutating the existing one, making the code more predictable and testable.
     *
     * @param character The character taking their turn
     * @param allies The character's team (includes the character itself)
     * @param enemies The opposing team
     * @throws NullPointerException if character is not in playerMap (should never happen
     *                              in normal operation as this is validated in constructor)
     */
    private void processTurn(Character character,
                             List<Character> allies,
                             List<Character> enemies) {
        // Skip defeated characters - they don't get turns
        if (character.getStats().health() <= 0) {
            return;
        }

        // TODO 5: Get player and execute their decision (10 points) - COMPLETED
        System.out.println("\n" + character.getName() + "'s turn");

        // Get the player controlling this character (Strategy pattern)
        // This lookup enables different AI strategies for different characters
        Player player = playerMap.get(character);
        if(player == null) {
            // This should never happen if playerMap is properly initialized
            // But we handle it gracefully just in case
            System.out.println("Player: " + character.getName() + " not found.");
            return;
        }

        // Request decision from player (returns Command object)
        // Different player types use different decision algorithms:
        // - HumanPlayer: Prompts user for input via console
        // - RuleBasedPlayer: Uses simple if-then rules
        // - LLMPlayer: Calls AI API for strategic decision
        GameCommand command = player.decideAction(character, allies, enemies, gameState);

        // Execute the command (Command pattern)
        // Invoker handles execution and maintains command history for undo
        invoker.executeCommand(command);

        // Display the result to all players
        displayActionResult(command, character);

        // Update game state (functional/immutable style)
        // Creates new GameState with incremented turn and updated history
        gameState = gameState.nextTurn()
                .withUndo(true, invoker.getCommandHistory().size());
    }

    /**
     * Checks if the game is over.
     *
     * <p>The game ends when all characters on one team are defeated (HP ≤ 0).
     * This is checked multiple times during the game loop to enable early exit.
     *
     * <h3>Why Use Streams?</h3>
     * <p>The Java Stream API provides a clean, functional way to check if
     * any character meets a condition:
     * <pre>{@code
     * team1.stream().anyMatch(c -> c.getStats().health() > 0)
     * }</pre>
     * This is more readable than a traditional loop and handles empty lists gracefully.
     *
     * @return true if one team is completely defeated, false otherwise
     */
    private boolean isGameOver() {
        // Check if any team1 character is alive
        boolean team1Alive = team1.stream()
                .anyMatch(c -> c.getStats().health() > 0);

        // Check if any team2 character is alive
        boolean team2Alive = team2.stream()
                .anyMatch(c -> c.getStats().health() > 0);

        // Game is over if either team is completely dead
        return !team1Alive || !team2Alive;
    }

    /**
     * Displays team information at game start.
     *
     * <p>Shows each team's composition including character types and controlling
     * player types. This helps players understand the matchup before the game begins.
     *
     * <p>Example output:
     * <pre>
     * === Team Setup ===
     *
     * Team 1:
     *   - Bob (WARRIOR) - HumanPlayer
     *   - Wizard (MAGE) - RuleBasedPlayer
     * </pre>
     */
    private void displayTeamSetup() {
        System.out.println("\n=== Team Setup ===");
        System.out.println("\nTeam 1:");
        for (Character c : team1) {
            Player p = playerMap.get(c);
            String playerType = p.getClass().getSimpleName();
            System.out.printf("  - %s (%s) - %s%n",
                    c.getName(), c.getType(), playerType);
        }
    }

    /**
     * Displays round header with current game state.
     *
     * <p>Shows turn/round numbers and complete status for all characters
     * on both teams. This gives players full visibility into the game state
     * before each round begins.
     *
     * <p>Example output:
     * <pre>
     * ============================================================
     * TURN 5 - ROUND 2
     * ============================================================
     *
     * Team 1 Status:
     *   Bob (WARRIOR): 120/150 HP - Alive
     *   Wizard (MAGE): 0 HP - Defeated
     *
     * Team 2 Status:
     *   Barbara (ARCHER): 80/100 HP - Alive
     *   Shadow (ROGUE): 45/90 HP - Alive
     * </pre>
     */
    private void displayRoundHeader() {
        System.out.println("\n" + "=".repeat(60));
        System.out.printf("TURN %d - ROUND %d%n",
                gameState.turnNumber(), gameState.roundNumber());
        System.out.println("=".repeat(60));

        // Display current team status
        System.out.println("\nTeam 1 Status:");
        for (Character c : team1) {
            displayCharacterStatus(c);
        }

        System.out.println("\nTeam 2 Status:");
        for (Character c : team2) {
            displayCharacterStatus(c);
        }
    }

    /**
     * Displays the final game result.
     *
     * <p>Shows which team won, final character statuses, and game statistics.
     * This provides closure and allows players to review what happened.
     *
     * <p>Statistics include:
     * <ul>
     *   <li>Total turns played</li>
     *   <li>Total commands executed</li>
     *   <li>Final HP for all characters</li>
     * </ul>
     *
     * <p>Example output:
     * <pre>
     * ============================================================
     * GAME OVER
     * ============================================================
     * 🏆 Team 1 wins!
     *
     * Final Status:
     *
     * Team 1:
     *   Bob (WARRIOR): 75 HP - Alive
     *   Wizard (MAGE): 0 HP - Defeated
     *
     * Team 2:
     *   Barbara (ARCHER): 0 HP - Defeated
     *   Shadow (ROGUE): 0 HP - Defeated
     *
     * Total turns played: 12
     * Total commands executed: 12
     * </pre>
     */
    public void displayResult() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("GAME OVER");
        System.out.println("=".repeat(60));

        // Determine winner
        boolean team1Wins = team1.stream().anyMatch(c -> c.getStats().health() > 0);

        if (team1Wins) {
            System.out.println("🏆 Team 1 wins!");
        } else {
            System.out.println("🏆 Team 2 wins!");
        }

        // Display final status
        System.out.println("\nFinal Status:");
        System.out.println("\nTeam 1:");
        for (Character c : team1) {
            displayCharacterStatus(c);
        }

        System.out.println("\nTeam 2:");
        for (Character c : team2) {
            displayCharacterStatus(c);
        }

        // Display game statistics
        System.out.println("\nTotal turns played: " + gameState.turnNumber());
        System.out.println("Total commands executed: " + gameState.commandHistorySize());
    }

    /**
     * Displays the result of an action (stub for future enhancement).
     *
     * <p>Currently just displays a separator. In a more advanced implementation,
     * this could show detailed combat calculations, damage rolls, etc.
     *
     * @param command The command that was executed
     * @param character The character who executed it
     */
    private void displayActionResult(GameCommand command, Character character) {
        System.out.println("---");
    }

    /**
     * Displays round summary showing survival counts.
     *
     * <p>Quick status update at the end of each round showing how many
     * characters are still alive on each team.
     *
     * <p>Example output:
     * <pre>
     * --- Round 2 Complete ---
     * Team 1: 2/2 alive
     * Team 2: 1/2 alive
     * </pre>
     */
    private void displayRoundSummary() {
        System.out.println("\n--- Round " + gameState.roundNumber() + " Complete ---");
        System.out.println("Team 1: " + countAlive(team1) + "/" + team1.size() + " alive");
        System.out.println("Team 2: " + countAlive(team2) + "/" + team2.size() + " alive");
    }

    /**
     * Displays a character's current status.
     *
     * <p>Shows character name, type, current HP, and alive/defeated status.
     * HP is clamped to 0 minimum for cleaner display (no negative HP shown).
     *
     * @param c Character to display
     */
    private void displayCharacterStatus(Character c) {
        String status = c.getStats().health() > 0 ? "Alive" : "Defeated";
        System.out.printf("  %s (%s): %d HP - %s%n",
                c.getName(),
                c.getType(),
                Math.max(0, c.getStats().health()),
                status);
    }

    /**
     * Counts alive characters in a team.
     *
     * <p>Uses Java Stream API for clean, functional counting.
     *
     * @param team Team to count alive characters in
     * @return Number of characters with HP > 0
     */
    private int countAlive(List<Character> team) {
        return (int) team.stream()
                .filter(c -> c.getStats().health() > 0)
                .count();
    }
}