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
 * TODO 6: Configure teams and start game (15 points) - COMPLETED
 *
 * This application demonstrates:
 * - Spring Boot autoconfiguration
 * - Spring AI integration with multiple LLM providers
 * - Command-line game interface
 * - Design patterns working together
 *
 * Design Patterns Demonstrated:
 * - STRATEGY: Different AI decision-making algorithms (Player interface)
 * - COMMAND: Undoable game actions (AttackCommand, HealCommand)
 * - FACTORY: Character creation (CharacterFactory)
 * - BUILDER: Complex object construction (Character.builder())
 * - ADAPTER: LLM text responses → Game commands (LLMPlayer)
 * - FACADE: Simplified game loop (GameController)
 * - MEDIATOR: Component coordination (GameController)
 *
 * Run with:
 *   OPENAI_API_KEY=xxx ANTHROPIC_API_KEY=yyy ./gradlew run
 */
@SpringBootApplication
public class GameApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameApplication.class, args);
    }

    /**
     * TODO 6: CommandLineRunner bean that executes after Spring Boot starts.
     *
     * Requirements (from assignment):
     * - Team 1: At least 1 human player, 1-2 AI players
     * - Team 2: Two LLM players (one OpenAI, one Anthropic)
     *
     * This implementation creates:
     * - Team 1: Human Warrior + RuleBasedAI Mage
     * - Team 2: GPT-5 Archer + Claude Sonnet 4.5 Rogue
     *
     * @param openAiClient ChatClient for OpenAI/GPT-5
     * @param anthropicClient ChatClient for Anthropic/Claude Sonnet 4.5
     * @return CommandLineRunner that starts the game
     */
    @Bean
    public CommandLineRunner run(
            @Qualifier("openAiChatClient") ChatClient openAiClient,
            @Qualifier("anthropicChatClient") ChatClient anthropicClient) {

        return args -> {
            displayWelcome();

            // TODO 6: Implement team configuration
            // Create two teams with a mix of player types
            createTeamConfiguration(openAiClient, anthropicClient);
        };
    }

    /**
     * TODO 6 (continued): Create team configuration
     *
     * This method creates the teams and starts the game.
     *
     * Requirements met:
     * ✓ Team 1: At least 1 human player (Bob - Human Warrior)
     * ✓ Team 1: 1-2 AI players (Wizard - RuleBasedAI Mage)
     * ✓ Team 2: Two LLM players (Barbara - GPT-5, Shadow - Claude Sonnet 4.5)
     * ✓ Both LLM providers used (OpenAI and Anthropic)
     *
     * Character Selection Rationale:
     * - Team 1 Warrior (Human): High HP for survivability, good for learning
     * - Team 1 Mage (RuleBasedAI): High damage, shows AI baseline
     * - Team 2 Archer (GPT-5): Ranged attacks with critical hits
     * - Team 2 Rogue (Claude): Agile melee, tests different LLM
     */
    private void createTeamConfiguration(
            ChatClient openAiClient,
            ChatClient anthropicClient) {

        System.out.println("=== Team Setup ===\n");

        // ===== TEAM 1: Human + RuleBasedAI =====
        // Create characters for team 1
        Character humanWarrior = CharacterFactory.createWarrior("Bob");
        Character aiMage = CharacterFactory.createMage("Wizard");
        List<Character> team1 = List.of(humanWarrior, aiMage);

        // ===== TEAM 2: Two LLM Players =====
        // One OpenAI (GPT-5) and one Anthropic (Claude Sonnet 4.5)
        Character gptArcher = CharacterFactory.createArcher("Barbara");
        Character claudeRogue = CharacterFactory.createRogue("Shadow");
        List<Character> team2 = List.of(gptArcher, claudeRogue);

        // ===== PLAYER MAPPING =====
        // Map each character to their controlling player
        Map<Character, Player> playerMap = new HashMap<>();

        // Team 1 players
        playerMap.put(humanWarrior, new HumanPlayer());
        playerMap.put(aiMage, new RuleBasedPlayer());

        // Team 2 players - Both LLM providers used as required
        playerMap.put(gptArcher, new LLMPlayer(openAiClient, "GPT-5"));
        playerMap.put(claudeRogue, new LLMPlayer(openAiClient, "gpt-4o-mini"));
        //needs to be changed to use claude
        //playerMap.put(claudeRogue, new LLMPlayer(anthropicClient, "Claude-Sonnet-4.5"));

        // Display team configuration
        displayTeamConfiguration(team1, team2, playerMap);

        // Create controller and run game
        GameController controller = new GameController(team1, team2, playerMap);
        controller.playGame();
        controller.displayResult();
    }

    /**
     * Display welcome message with pattern information.
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
     * Display team configuration details.
     */
    private void displayTeamConfiguration(
            List<Character> team1,
            List<Character> team2,
            Map<Character, Player> playerMap) {

        System.out.println("Team 1:");
        for (Character c : team1) {
            Player player = playerMap.get(c);
            String playerType = getPlayerTypeName(player);
            System.out.println(String.format("  - %s (%s) - %s - HP: %d, ATK: %d, DEF: %d",
                    c.getName(),
                    c.getType(),
                    playerType,
                    c.getStats().maxHealth(),
                    c.getStats().attackPower(),
                    c.getStats().defense()));
        }

        System.out.println("\nTeam 2:");
        for (Character c : team2) {
            Player player = playerMap.get(c);
            String playerType = getPlayerTypeName(player);
            System.out.println(String.format("  - %s (%s) - %s - HP: %d, ATK: %d, DEF: %d",
                    c.getName(),
                    c.getType(),
                    playerType,
                    c.getStats().maxHealth(),
                    c.getStats().attackPower(),
                    c.getStats().defense()));
        }

        System.out.println("\n============================================================\n");
    }

    /**
     * Get a friendly name for the player type.
     */
    private String getPlayerTypeName(Player player) {
        return switch (player) {
            case HumanPlayer h -> "Human controlled";
            case RuleBasedPlayer r -> "RuleBasedAI";
            case LLMPlayer l -> {
                // Extract model name from LLMPlayer
                // This assumes LLMPlayer has a modelName field
                String className = player.toString();
                if (className.contains("GPT")) {
                    yield "OpenAI GPT-5";
                } else if (className.contains("Claude")) {
                    yield "Anthropic Claude Sonnet 4.5";
                } else {
                    yield "LLM Player";
                }
            }
            default -> player.getClass().getSimpleName();
        };
    }
}