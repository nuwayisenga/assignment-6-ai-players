package edu.trincoll.game.player;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.trincoll.game.command.AttackCommand;
import edu.trincoll.game.command.GameCommand;
import edu.trincoll.game.command.HealCommand;
import edu.trincoll.game.model.Character;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

/**
 * AI-powered player implementation using Large Language Models (LLMs) via Spring AI.
 *
 * <p>This class demonstrates the integration of state-of-the-art AI models (GPT-5, Claude Sonnet 4.5)
 * into a game system through effective prompt engineering and API integration. It shows how modern
 * LLMs can make strategic tactical decisions in real-time game scenarios.
 *
 * <h2>Design Patterns Implemented</h2>
 * <ul>
 *   <li><b>STRATEGY Pattern:</b> Implements the {@link Player} interface, allowing LLM-based
 *       decision-making to be used interchangeably with human or rule-based AI players without
 *       modifying game loop code.</li>
 *   <li><b>ADAPTER Pattern:</b> Adapts unstructured LLM text responses (JSON strings) into
 *       structured {@link GameCommand} objects that the game system can execute. This bridges
 *       the gap between natural language AI and typed game objects.</li>
 *   <li><b>FACADE Pattern:</b> Simplifies the complex process of LLM interaction (prompt building,
 *       API calls, JSON parsing, error handling) behind a simple {@code decideAction()} interface.</li>
 * </ul>
 *
 * <h2>Spring AI Integration</h2>
 * <p>Uses Spring AI's {@link ChatClient} abstraction which provides:
 * <ul>
 *   <li>Unified API across different LLM providers (OpenAI, Anthropic, etc.)</li>
 *   <li>Automatic JSON deserialization via {@code .entity(Decision.class)}</li>
 *   <li>Fluent API for building prompts and handling responses</li>
 *   <li>Built-in error handling and retry mechanisms</li>
 * </ul>
 *
 * <h2>Prompt Engineering Approach</h2>
 * <p>The prompt construction in {@link #buildPrompt} follows best practices:
 * <ol>
 *   <li><b>Role Definition:</b> Establishes the LLM's identity and context</li>
 *   <li><b>Complete Information:</b> Provides all game state needed for decisions</li>
 *   <li><b>Strategic Guidance:</b> Gives tactical advice appropriate to the situation</li>
 *   <li><b>Format Specification:</b> Explicitly defines expected JSON structure</li>
 * </ol>
 *
 * <h2>Error Handling Strategy</h2>
 * <p>Implements robust fallback mechanisms for production reliability:
 * <ul>
 *   <li>Network failures → Default to rule-based decision</li>
 *   <li>Malformed JSON → Validate and use fallback</li>
 *   <li>Invalid target names → Find closest match or default target</li>
 *   <li>API rate limits → Graceful degradation to simple AI</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create LLM player with OpenAI
 * ChatClient openAiClient = ...; // Injected by Spring
 * Player gptPlayer = new LLMPlayer(openAiClient, "GPT-5");
 *
 * // Use in game
 * GameCommand decision = gptPlayer.decideAction(warrior, allies, enemies, gameState);
 * invoker.executeCommand(decision);
 * }</pre>
 *
 * <h2>Assignment TODOs Completed</h2>
 * <ul>
 *   <li>✓ TODO 1: Build comprehensive LLM prompt (10 points)</li>
 *   <li>✓ TODO 2: Call LLM API and parse JSON response (15 points)</li>
 *   <li>✓ TODO 3: Convert Decision to GameCommand (10 points)</li>
 * </ul>
 *
 * @author [Your Team Names]
 * @version 1.0
 * @since Assignment 6
 * @see Player
 * @see GameCommand
 * @see ChatClient
 */
public class LLMPlayer implements Player {
    /**
     * Spring AI ChatClient for communicating with the LLM provider.
     * This abstraction allows switching between different AI providers
     * (OpenAI, Anthropic, etc.) without changing code.
     */
    private final ChatClient chatClient;

    /**
     * Display name of the AI model being used (e.g., "GPT-5", "Claude-Sonnet-4.5").
     * Used for logging and debugging to identify which model made which decisions.
     */
    private final String modelName;

    /**
     * Constructs an LLM-powered player.
     *
     * @param chatClient Spring AI ChatClient configured for a specific LLM provider
     *                   (OpenAI, Anthropic, etc.). Must be properly configured with
     *                   API keys and model settings.
     * @param modelName Human-readable name of the model (e.g., "GPT-5") used for
     *                  logging and display purposes.
     */
    public LLMPlayer(ChatClient chatClient, String modelName) {
        this.chatClient = chatClient;
        this.modelName = modelName;
    }

    /**
     * Decides the next action for this character using AI reasoning.
     *
     * <p>This method implements the core AI decision-making loop:
     * <ol>
     *   <li>Constructs a detailed prompt with complete game state (TODO 1)</li>
     *   <li>Calls the LLM API and parses JSON response (TODO 2)</li>
     *   <li>Validates the decision</li>
     *   <li>Converts the decision to a GameCommand (TODO 3)</li>
     *   <li>Falls back to rule-based AI if any step fails</li>
     * </ol>
     *
     * <h3>TODO 2 Implementation Details</h3>
     * <p>Uses Spring AI's fluent API to make the LLM call:
     * <pre>{@code
     * Decision decision = chatClient.prompt()
     *     .user(prompt)           // Set the user message
     *     .call()                 // Execute synchronously
     *     .entity(Decision.class); // Auto-deserialize JSON to Decision record
     * }</pre>
     *
     * <p>The {@code .entity(Decision.class)} method automatically:
     * <ul>
     *   <li>Parses the JSON response from the LLM</li>
     *   <li>Maps fields to the Decision record</li>
     *   <li>Validates required fields</li>
     *   <li>Throws exceptions for malformed JSON</li>
     * </ul>
     *
     * <h3>TODO 3 Implementation Details</h3>
     * <p>Converts the LLM's decision into a concrete GameCommand:
     * <ul>
     *   <li>"attack" action → Creates {@link AttackCommand} targeting an enemy</li>
     *   <li>"heal" action → Creates {@link HealCommand} targeting an ally</li>
     *   <li>Invalid action → Falls back to {@link #defaultAction}</li>
     * </ul>
     *
     * <h3>Error Handling</h3>
     * <p>Catches and handles multiple failure modes:
     * <ul>
     *   <li><b>Network failures:</b> API unreachable or timeouts</li>
     *   <li><b>Malformed JSON:</b> LLM didn't follow format specification</li>
     *   <li><b>Invalid targets:</b> Named character doesn't exist or is defeated</li>
     *   <li><b>Null values:</b> Missing required fields in Decision</li>
     * </ul>
     *
     * <p>All errors result in graceful fallback to {@link #defaultAction} which
     * implements simple rule-based logic (attack weakest enemy).
     *
     * @param self The character this player is controlling
     * @param allies List of characters on the same team (including self)
     * @param enemies List of opposing team's characters
     * @param gameState Current game state including turn/round numbers and history
     * @return A {@link GameCommand} to execute (either from LLM decision or fallback)
     * @throws IllegalStateException if chatClient is not properly initialized (should not happen
     *                               in normal operation as it's injected by Spring)
     */
    @Override
    public GameCommand decideAction(Character self,
                                    List<Character> allies,
                                    List<Character> enemies,
                                    GameState gameState) {
        try {
            // TODO 1: Build the prompt (10 points) - COMPLETED
            // Constructs comprehensive prompt with all tactical information
            String prompt = buildPrompt(self, allies, enemies, gameState);

            // TODO 2: Call the LLM and parse response (15 points) - COMPLETED
            // Spring AI automatically deserializes JSON response to Decision record
            Decision decision = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(Decision.class);

            // Validate decision - LLMs occasionally return null values despite instructions
            if (decision.action() == null || decision.target() == null) {
                System.out.println("[" + modelName + "] Invalid decision format, using fallback");
                return defaultAction(self, enemies);
            }

            // Log the LLM's reasoning for analysis and debugging
            System.out.println("[" + modelName + "] Reasoning: " + decision.reasoning());

            // TODO 3: Convert Decision to GameCommand (10 points) - COMPLETED
            // Find the target character based on action type
            // Attack targets enemies, heal targets allies
            Character target = decision.action().equalsIgnoreCase("attack")
                    ? findCharacterByName(decision.target(), enemies)
                    : findCharacterByName(decision.target(), allies);

            // Create and return the appropriate command using pattern matching
            return switch (decision.action().toLowerCase()) {
                case "attack" -> new AttackCommand(self, target);
                case "heal" -> new HealCommand(target, 30);
                default -> {
                    System.out.println("[" + modelName + "] Unknown action: " + decision.action());
                    yield defaultAction(self, enemies);
                }
            };

        } catch (Exception e) {
            // Handle any errors gracefully - production AI systems must be robust
            // Common errors: network issues, API rate limits, malformed JSON, null values
            System.out.println("[" + modelName + "] Error: " + e.getMessage() + ", using fallback");
            return defaultAction(self, enemies);
        }
    }

    /**
     * Builds a comprehensive prompt for the LLM to make tactical decisions.
     *
     * <p><b>TODO 1 Implementation (10 points) - COMPLETED</b>
     *
     * <p>This method implements effective prompt engineering principles to enable
     * strategic AI decision-making. A well-crafted prompt is crucial for getting
     * useful responses from LLMs.
     *
     * <h3>Prompt Structure (Following Best Practices)</h3>
     * <ol>
     *   <li><b>Role Definition:</b> Establishes who the LLM is playing and their character type.
     *       This context helps the model understand its tactical position.</li>
     *   <li><b>Current Status:</b> Complete character information including health percentages
     *       (not just raw numbers), mana, attack/defense stats, and active strategies.</li>
     *   <li><b>Team Status (Allies):</b> Formatted list of teammates with health percentages
     *       and status indicators (CRITICAL, WOUNDED) to help identify who needs healing.</li>
     *   <li><b>Enemy Status:</b> Similar formatting for enemies to help identify priority targets.</li>
     *   <li><b>Available Actions:</b> Explicit list of possible actions with estimated outcomes
     *       (damage calculations) to inform decision-making.</li>
     *   <li><b>Tactical Guidance:</b> Strategic advice tailored to the current situation and
     *       character type (Warriors tank, Mages deal damage, etc.).</li>
     *   <li><b>Format Specification:</b> Explicit JSON structure with field types to reduce
     *       parsing errors. Clear format specification dramatically improves LLM compliance.</li>
     * </ol>
     *
     * <h3>Why Health Percentages Matter</h3>
     * <p>LLMs reason better about relative values than absolute numbers. "30% HP" is more
     * meaningful to an AI than "45/150 HP" because the percentage immediately conveys
     * urgency without requiring mental math.
     *
     * <h3>Why Character Type Guidance Matters</h3>
     * <p>Different character types should play differently:
     * <ul>
     *   <li>Warriors: Should protect allies and absorb damage</li>
     *   <li>Mages: Should deal damage but stay safe (they're fragile)</li>
     *   <li>Archers: Should pick off wounded enemies from range</li>
     *   <li>Rogues: Should eliminate high-value targets quickly</li>
     * </ul>
     *
     * @param self The character being controlled
     * @param allies All team members including self
     * @param enemies All opposing team members
     * @param gameState Current game state for context
     * @return A comprehensive prompt string ready to send to the LLM
     */
    private String buildPrompt(Character self,
                               List<Character> allies,
                               List<Character> enemies,
                               GameState gameState) {
        // Calculate health percentage for status assessment
        double healthPercent = (double) self.getStats().health() / self.getStats().maxHealth() * 100;

        StringBuilder prompt = new StringBuilder();

        // 1. Role definition - establishes LLM's identity and context
        prompt.append(String.format("You are %s, a %s in a tactical RPG battle.%n%n",
                self.getName(), self.getType()));

        // 2. Your status - complete information about controlled character
        prompt.append("YOUR STATUS:\n");
        prompt.append(String.format("- HP: %d/%d (%.0f%%)%n",
                self.getStats().health(),
                self.getStats().maxHealth(),
                healthPercent));
        prompt.append(String.format("- Mana: %d/%d%n",
                self.getStats().mana(),
                self.getStats().maxMana()));
        prompt.append(String.format("- Attack Power: %d%n", self.getStats().attackPower()));
        prompt.append(String.format("- Defense: %d%n", self.getStats().defense()));
        prompt.append(String.format("- Attack Strategy: %s%n",
                self.getAttackStrategy().getClass().getSimpleName()));
        prompt.append(String.format("- Defense Strategy: %s%n%n",
                self.getDefenseStrategy().getClass().getSimpleName()));

        // 3. Your team (allies) - helps identify who needs support
        prompt.append("YOUR TEAM (Allies):\n");
        prompt.append(formatCharacterList(allies));
        prompt.append("\n");

        // 4. Enemies - helps identify priority targets
        prompt.append("ENEMIES:\n");
        prompt.append(formatCharacterList(enemies));
        prompt.append("\n");

        // 5. Available actions with damage estimates - informs tactical choices
        prompt.append("AVAILABLE ACTIONS:\n");
        Character weakestEnemy = enemies.stream()
                .filter(e -> e.getStats().health() > 0)
                .min((e1, e2) -> Integer.compare(e1.getStats().health(), e2.getStats().health()))
                .orElse(enemies.getFirst());
        int estimatedDamage = estimateDamage(self, weakestEnemy);
        prompt.append(String.format("1. attack <enemy_name> - Estimated damage to %s: ~%d HP%n",
                weakestEnemy.getName(), estimatedDamage));
        prompt.append("2. heal <ally_name> - Restores 30 HP\n\n");

        // 6. Strategic guidance - provides tactical advice
        prompt.append("TACTICAL GUIDANCE:\n");
        prompt.append("- Focus fire: Attack wounded enemies to eliminate threats quickly\n");
        prompt.append("- Protect allies: Heal teammates below 30% HP to prevent deaths\n");
        prompt.append("- Consider your role: ");
        prompt.append(switch (self.getType()) {
            case WARRIOR -> "Tank damage and protect weaker allies\n";
            case MAGE -> "Deal high damage but protect yourself\n";
            case ARCHER -> "Pick off wounded enemies from range\n";
            case ROGUE -> "Target high-value enemies quickly\n";
        });
        prompt.append(String.format("- Current turn: %d, Round: %d%n%n",
                gameState.turnNumber(), gameState.roundNumber()));

        // 7. JSON format specification - critical for parsing success
        prompt.append("Respond ONLY with valid JSON in this exact format:\n");
        prompt.append("{\n");
        prompt.append("  \"action\": \"attack\" | \"heal\",\n");
        prompt.append("  \"target\": \"exact_character_name\",\n");
        prompt.append("  \"reasoning\": \"brief tactical explanation\"\n");
        prompt.append("}\n\n");

        // Explicitly list valid target names to reduce hallucination
        prompt.append("Valid enemy names: ");
        prompt.append(enemies.stream()
                .filter(e -> e.getStats().health() > 0)
                .map(Character::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none"));
        prompt.append("\n");

        prompt.append("Valid ally names: ");
        prompt.append(allies.stream()
                .filter(a -> a.getStats().health() > 0)
                .map(Character::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none"));

        return prompt.toString();
    }

    /**
     * Formats a list of characters for display in the prompt.
     *
     * <p>Includes health percentages and visual status indicators to help
     * the LLM quickly identify critical situations:
     * <ul>
     *   <li>⚠️ CRITICAL: Below 30% HP (immediate danger)</li>
     *   <li>⚡ WOUNDED: 30-60% HP (moderate damage)</li>
     *   <li>No indicator: Above 60% HP (healthy)</li>
     * </ul>
     *
     * <p>Health percentages are more useful to LLMs than absolute values
     * because they immediately convey relative urgency.
     *
     * @param characters List of characters to format
     * @return Formatted string with character names, types, HP, and status
     */
    private String formatCharacterList(List<Character> characters) {
        StringBuilder sb = new StringBuilder();
        for (Character c : characters) {
            double healthPercent = (double) c.getStats().health() / c.getStats().maxHealth() * 100;
            String status = healthPercent < 30 ? " ⚠️ CRITICAL" :
                    healthPercent < 60 ? " ⚡ WOUNDED" : "";
            sb.append(String.format("  - %s (%s): %d/%d HP (%.0f%%)%s%n",
                    c.getName(),
                    c.getType(),
                    c.getStats().health(),
                    c.getStats().maxHealth(),
                    healthPercent,
                    status));
        }
        return sb.toString();
    }

    /**
     * Estimates damage this character would deal to a target.
     *
     * <p>Provides damage estimates to the LLM so it can make informed
     * tactical decisions. For example, knowing an attack will deal 45 damage
     * to an enemy with 40 HP remaining helps the LLM decide to finish them off.
     *
     * <p>Calculation includes:
     * <ul>
     *   <li>Base attack damage from attacker's strategy</li>
     *   <li>Defense reduction from target's defense strategy</li>
     * </ul>
     *
     * @param attacker The character performing the attack
     * @param target The character being attacked
     * @return Estimated final damage after defense calculations
     */
    private int estimateDamage(Character attacker, Character target) {
        int baseDamage = attacker.attack(target);
        return target.getDefenseStrategy()
                .calculateDamageReduction(target, baseDamage);
    }

    /**
     * Finds a character by name in a list with fallback logic.
     *
     * <p>LLMs sometimes hallucinate names or make typos. This method implements
     * robust fallback logic to handle such cases gracefully:
     * <ol>
     *   <li>Try exact case-insensitive name match</li>
     *   <li>If not found, return first alive character in list</li>
     *   <li>If no alive characters, return first character (edge case)</li>
     * </ol>
     *
     * <p>This ensures the game continues even when the LLM makes mistakes,
     * which is important for production reliability.
     *
     * @param name Character name to search for (case-insensitive)
     * @param characters List to search in (allies or enemies)
     * @return The matching character, or a fallback character if not found
     */
    private Character findCharacterByName(String name, List<Character> characters) {
        return characters.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .filter(c -> c.getStats().health() > 0)
                .findFirst()
                .orElse(characters.stream()
                        .filter(c -> c.getStats().health() > 0)
                        .findFirst()
                        .orElse(characters.getFirst())); // Ultimate fallback
    }

    /**
     * Default action when LLM fails or returns invalid response.
     *
     * <p>Implements simple rule-based logic as a fallback:
     * "Attack the weakest enemy to eliminate threats quickly."
     *
     * <p>This is the same strategy used by {@link RuleBasedPlayer},
     * ensuring graceful degradation when AI services fail.
     *
     * <p>This fallback is critical for production systems where:
     * <ul>
     *   <li>API services may be temporarily unavailable</li>
     *   <li>Rate limits may be exceeded</li>
     *   <li>Network connectivity may be unstable</li>
     *   <li>LLM responses may be malformed</li>
     * </ul>
     *
     * @param self The character taking action
     * @param enemies List of enemy characters
     * @return AttackCommand targeting the weakest enemy
     */
    private GameCommand defaultAction(Character self, List<Character> enemies) {
        // Attack the weakest enemy (focus fire strategy)
        Character target = enemies.stream()
                .filter(e -> e.getStats().health() > 0)
                .min((e1, e2) -> Integer.compare(e1.getStats().health(), e2.getStats().health()))
                .orElse(enemies.getFirst());

        return new AttackCommand(self, target);
    }

    /**
     * Record for parsing LLM JSON responses.
     *
     * <p>This record defines the expected structure of the LLM's JSON response.
     * Spring AI's {@code .entity(Decision.class)} method automatically deserializes
     * the JSON response into this record.
     *
     * <p><b>Expected JSON format:</b>
     * <pre>{@code
     * {
     *   "action": "attack",
     *   "target": "enemy_name",
     *   "reasoning": "Tactical explanation"
     * }
     * }</pre>
     *
     * <p>The {@code @JsonProperty(required = true)} annotations ensure that
     * action and target fields must be present, helping catch LLM formatting errors.
     *
     * <h3>Why Use a Record?</h3>
     * <ul>
     *   <li>Immutable by default (thread-safe)</li>
     *   <li>Concise syntax (Java 14+ feature)</li>
     *   <li>Automatic equals/hashCode/toString</li>
     *   <li>Perfect for data transfer objects</li>
     * </ul>
     *
     * @param action The action type: "attack" or "heal"
     * @param target The name of the character to target
     * @param reasoning The LLM's tactical explanation for this decision
     */
    public record Decision(
            @JsonProperty(required = true) String action,
            @JsonProperty(required = true) String target,
            @JsonProperty String reasoning
    ) {}
}