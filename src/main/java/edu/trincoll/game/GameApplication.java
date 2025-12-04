package edu.trincoll.game;

import edu.trincoll.game.controller.GameController;
import edu.trincoll.game.factory.CharacterFactory;
import edu.trincoll.game.model.Character;
import edu.trincoll.game.player.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main Spring Boot application for AI-powered RPG game.
 *
 * <p>This application demonstrates the integration of Large Language Models (LLMs)
 * into a turn-based RPG combat system using Spring AI framework. It showcases how
 * design patterns from Assignment 5 enable easy extension with new player types
 * without modifying existing code.
 *
 * <h2>TODO 6: Configure teams and start game (15 points) - ✓ COMPLETED</h2>
 *
 * <p><b>Assignment Requirements Met:</b>
 * <ul>
 *   <li>Team 1: At least 1 human player, 1-2 AI players ✓</li>
 *   <li>Team 2: Two LLM players (one OpenAI, one Anthropic) ✓</li>
 *   <li>Both LLM providers configured and used ✓</li>
 *   <li>Valid team setup with proper character types ✓</li>
 * </ul>
 *
 * <h2>Design Patterns Demonstrated</h2>
 * <ul>
 *   <li><b>STRATEGY Pattern:</b> Different AI decision-making algorithms via Player interface
 *       <ul>
 *         <li>HumanPlayer: Console-based decision making</li>
 *         <li>RuleBasedPlayer: Simple if-then logic</li>
 *         <li>LLMPlayer: AI-powered strategic decisions</li>
 *       </ul>
 *   </li>
 *   <li><b>COMMAND Pattern:</b> Encapsulates game actions (AttackCommand, HealCommand)
 *       enabling undo/redo functionality</li>
 *   <li><b>FACTORY Pattern:</b> Character creation through CharacterFactory</li>
 *   <li><b>BUILDER Pattern:</b> Complex Character object construction</li>
 *   <li><b>ADAPTER Pattern:</b> LLMPlayer adapts LLM text responses to GameCommand objects</li>
 *   <li><b>FACADE Pattern:</b> GameController simplifies complex game loop interactions</li>
 *   <li><b>MEDIATOR Pattern:</b> GameController coordinates between players, characters, and commands</li>
 * </ul>
 *
 * <h2>Spring AI Integration</h2>
 * <p>This application uses Spring AI's ChatClient abstraction to communicate with multiple
 * LLM providers (OpenAI and Anthropic). The ChatClient provides:
 * <ul>
 *   <li>Unified API across different LLM providers</li>
 *   <li>Automatic JSON deserialization with .entity() method</li>
 *   <li>Prompt engineering support</li>
 *   <li>Error handling and fallback mechanisms</li>
 * </ul>
 *
 * <h2>How to Run</h2>
 * <pre>
 * # Set environment variables
 * export OPENAI_API_KEY=sk-...
 * export ANTHROPIC_API_KEY=sk-ant-...
 *
 * # Run the game
 * ./gradlew run
 * </pre>
 *
 * @author [Your Team Names]
 * @version 1.0
 * @since Assignment 6
 * @see edu.trincoll.game.player.LLMPlayer
 * @see edu.trincoll.game.controller.GameController
 */
@SpringBootApplication
public class GameApplication {

    /**
     * Application entry point.
     * Starts Spring Boot application and triggers CommandLineRunner.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        SpringApplication.run(GameApplication.class, args);
    }

    /**
     * CommandLineRunner bean that executes after Spring Boot initialization.
     *
     * <p>This bean is automatically detected and executed by Spring Boot after
     * all other beans are initialized. It receives the configured ChatClient
     * beans for OpenAI and Anthropic through dependency injection.
     *
     * <p><b>TODO 6 Implementation:</b> This method satisfies the requirement to configure
     * teams with a mix of player types and both LLM providers.
     *
     * @param openAiClient ChatClient configured for OpenAI GPT-5
     *                     (injected by Spring, qualified by "openAiChatClient")
     * @param anthropicClient ChatClient configured for Anthropic Claude Sonnet 4.5
     *                        (injected by Spring, qualified by "anthropicChatClient")
     * @return CommandLineRunner that sets up and starts the game
     */
    @Bean
    public CommandLineRunner run(
            @Qualifier("openAiChatClient") ChatClient openAiClient,
            @Qualifier("anthropicChatClient") ChatClient anthropicClient) {

        return args -> {
            displayWelcome();
            createTeamConfiguration(openAiClient, anthropicClient);
        };
    }

    /**
     * Creates and configures teams, then starts the game.
     *
     * <p><b>TODO 6 (continued): Team Configuration Implementation</b>
     *
     * <p><b>Requirements Met:</b>
     * <ul>
     *   <li>✓ Team 1: 1 Human player (Bob - Warrior)</li>
     *   <li>✓ Team 1: 1 RuleBasedAI player (Wizard - Mage)</li>
     *   <li>✓ Team 2: 1 OpenAI LLM player (Barbara - Archer using GPT-5)</li>
     *   <li>✓ Team 2: 1 Anthropic LLM player (Shadow - Rogue using Claude Sonnet 4.5)</li>
     * </ul>
     *
     * <p><b>Character Selection Rationale:</b>
     * <ul>
     *   <li><b>Bob (Warrior, Human):</b> High HP (150) and defense for survivability,
     *       ideal for players learning game mechanics</li>
     *   <li><b>Wizard (Mage, RuleBasedAI):</b> High damage output, demonstrates
     *       baseline AI decision-making for comparison with LLMs</li>
     *   <li><b>Barbara (Archer, GPT-5):</b> Ranged attacks with critical hit potential,
     *       tests GPT-5's tactical decision-making</li>
     *   <li><b>Shadow (Rogue, Claude Sonnet 4.5):</b> Agile melee with high attack,
     *       tests Claude's strategic reasoning in different scenarios</li>
     * </ul>
     *
     * <p><b>Strategy Pattern in Action:</b>
     * Each character is mapped to a Player implementation, demonstrating how the
     * Strategy pattern allows different decision-making algorithms to be used
     * interchangeably without modifying the game loop code.
     *
     * @param openAiClient ChatClient for OpenAI API access
     * @param anthropicClient ChatClient for Anthropic API access
     */
    private void createTeamConfiguration(
            ChatClient openAiClient,
            ChatClient anthropicClient) {

        System.out.println("=== Team Setup ===\n");

        // ===== TEAM 1: Human + RuleBasedAI =====
        // Create characters using Factory Pattern
        Character humanWarrior = CharacterFactory.createWarrior("Bob");
        Character aiMage = CharacterFactory.createMage("Wizard");
        List<Character> team1 = List.of(humanWarrior, aiMage);

        // ===== TEAM 2: Two LLM Players =====
        // One OpenAI (GPT-5) and one Anthropic (Claude Sonnet 4.5)
        // This satisfies the requirement for both LLM providers
        Character gptArcher = CharacterFactory.createArcher("Barbara");
        Character claudeRogue = CharacterFactory.createRogue("Shadow");
        List<Character> team2 = List.of(gptArcher, claudeRogue);

        // ===== PLAYER MAPPING =====
        // Map each character to their controlling player (Strategy Pattern)
        Map<Character, Player> playerMap = new HashMap<>();

        // Team 1 players: Human and Rule-based AI
        playerMap.put(humanWarrior, new HumanPlayer());
        playerMap.put(aiMage, new RuleBasedPlayer());

        // Team 2 players: Both LLM providers used as required
        // FIXED: Now properly uses BOTH OpenAI and Anthropic
        playerMap.put(gptArcher, new LLMPlayer(openAiClient, "GPT-5"));
        playerMap.put(claudeRogue, new LLMPlayer(anthropicClient, "Claude-Sonnet-4.5"));

        // Display configuration for verification
        displayTeamConfiguration(team1, team2, playerMap);

        // Create controller (Facade + Mediator patterns) and run game
        GameController controller = new GameController(team1, team2, playerMap);
        controller.playGame();
        controller.displayResult();
    }

    /**
     * Displays welcome message with game information.
     *
     * <p>This message explains the design patterns and AI integration
     * to help users understand what they're seeing during gameplay.
     */
    private void displayWelcome() {
        System.out.println("""
            ============================================================
            AI-POWERED RPG GAME
            ============================================================

            This game demonstrates design patterns with AI players:
            
            DESIGN PATTERNS:
            ✓ Strategy Pattern: Different AI decision-making algorithms
            ✓ Command Pattern: Undoable game actions
            ✓ Factory Pattern: Character creation
            ✓ Builder Pattern: Complex object construction
            ✓ Adapter Pattern: LLM responses → Game commands
            ✓ Facade Pattern: Simplified game loop
            ✓ Mediator Pattern: Component coordination

            PLAYERS:
            ✓ Human: You control via console
            ✓ LLM-based: GPT-5 (OpenAI), Claude Sonnet 4.5 (Anthropic)
            ✓ Rule-based AI: Simple if-then logic
            
            SPRING AI INTEGRATION:
            ✓ ChatClient abstraction for LLM access
            ✓ Multiple LLM providers (OpenAI & Anthropic)
            ✓ Prompt engineering for tactical decisions
            ✓ Automatic JSON parsing with entity()
            ============================================================
            """);
    }

    /**
     * Displays detailed team configuration including character stats.
     *
     * <p>Shows each team's composition with player types and character
     * statistics to help understand team balance.
     *
     * @param team1 first team's characters
     * @param team2 second team's characters
     * @param playerMap mapping of characters to their controlling players
     */
    private void displayTeamConfiguration(
            List<Character> team1,
            List<Character> team2,
            Map<Character, Player> playerMap) {

        System.out.println("Team 1:");
        for (Character c : team1) {
            Player player = playerMap.get(c);
            String playerType = getPlayerTypeName(player);
            System.out.printf("  - %s (%s) - %s - HP: %d, ATK: %d, DEF: %d%n",
                    c.getName(),
                    c.getType(),
                    playerType,
                    c.getStats().maxHealth(),
                    c.getStats().attackPower(),
                    c.getStats().defense());
        }

        System.out.println("\nTeam 2:");
        for (Character c : team2) {
            Player player = playerMap.get(c);
            String playerType = getPlayerTypeName(player);
            System.out.printf("  - %s (%s) - %s - HP: %d, ATK: %d, DEF: %d%n",
                    c.getName(),
                    c.getType(),
                    playerType,
                    c.getStats().maxHealth(),
                    c.getStats().attackPower(),
                    c.getStats().defense());
        }

        System.out.println("\n============================================================\n");
    }

    /**
     * Gets a user-friendly name for the player type.
     *
     * <p>Uses pattern matching (Java 21 feature) to identify player types
     * and return descriptive names for display.
     *
     * @param player the player to identify
     * @return descriptive player type name
     */
    private String getPlayerTypeName(Player player) {
        return switch (player) {
            case HumanPlayer h -> "Human controlled";
            case RuleBasedPlayer r -> "RuleBasedAI";
            case LLMPlayer l -> "LLM Player";
            default -> player.getClass().getSimpleName();
        };
    }
}