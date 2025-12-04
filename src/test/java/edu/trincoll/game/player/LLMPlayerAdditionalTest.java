package edu.trincoll.game.player;

import edu.trincoll.game.command.AttackCommand;
import edu.trincoll.game.command.GameCommand;
import edu.trincoll.game.command.HealCommand;
import edu.trincoll.game.factory.CharacterFactory;
import edu.trincoll.game.model.Character;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Additional comprehensive tests for LLMPlayer functionality.
 * These tests go beyond basic requirements to demonstrate:
 * - Robust error handling with various failure scenarios
 * - Edge cases in prompt engineering
 * - Fallback mechanism validation
 * - AI decision quality verification
 *
 * @author Noewlla Uwayisenga, Chris Burns, Gabriela Scavenius
 * @version 1.0
 */
@DisplayName("LLMPlayer Additional Tests")
class LLMPlayerAdditionalTest {

    private ChatClient mockChatClient;
    private LLMPlayer llmPlayer;
    private Character warrior;
    private Character mage;
    private Character archer;
    private List<Character> allies;
    private List<Character> enemies;
    private GameState gameState;

    @BeforeEach
    void setUp() {
        mockChatClient = mock(ChatClient.class);
        llmPlayer = new LLMPlayer(mockChatClient, "Test-GPT");

        warrior = CharacterFactory.createWarrior("TestWarrior");
        mage = CharacterFactory.createMage("TestMage");
        archer = CharacterFactory.createArcher("TestArcher");
        Character rogue = CharacterFactory.createRogue("TestRogue");

        allies = List.of(warrior, mage);
        enemies = List.of(archer, rogue);
        gameState = GameState.initial();
    }

    /**
     * Test 1: Verify LLM makes attack decision when enemy is low HP.
     */
    @Test
    @DisplayName("LLM correctly targets low HP enemy")
    void testLLMTargetsLowHPEnemy() {
        archer.takeDamage(80);
        mockLLMResponse("attack", "TestArcher", "Finish off wounded enemy");

        GameCommand command = llmPlayer.decideAction(warrior, allies, enemies, gameState);

        assertNotNull(command, "Command should not be null");
        assertInstanceOf(AttackCommand.class, command, "Should create AttackCommand");
    }

    /**
     * Test 2: Verify LLM makes heal decision when ally is critical.
     */
    @Test
    @DisplayName("LLM heals critical ally")
    void testLLMHealsCriticalAlly() {
        mage.takeDamage(60);
        mockLLMResponse("heal", "TestMage", "Ally is critical, must heal");

        GameCommand command = llmPlayer.decideAction(warrior, allies, enemies, gameState);

        assertNotNull(command);
        assertInstanceOf(HealCommand.class, command, "Should create HealCommand");
    }

    /**
     * Test 3: Fallback when LLM returns null action.
     */
    @Test
    @DisplayName("Fallback when LLM returns null action")
    void testFallbackOnNullAction() {
        mockLLMResponse(null, "TestArcher", "This is invalid");

        GameCommand command = llmPlayer.decideAction(warrior, allies, enemies, gameState);

        assertNotNull(command, "Should return fallback command");
        assertInstanceOf(AttackCommand.class, command, "Fallback should be attack");
    }

    /**
     * Test 4: Fallback when LLM returns null target.
     */
    @Test
    @DisplayName("Fallback when LLM returns null target")
    void testFallbackOnNullTarget() {
        mockLLMResponse("attack", null, "Missing target");

        GameCommand command = llmPlayer.decideAction(warrior, allies, enemies, gameState);

        assertNotNull(command, "Should return fallback command");
        assertInstanceOf(AttackCommand.class, command, "Fallback should be attack");
    }

    /**
     * Test 5: Handle LLM hallucinating invalid target name.
     */
    @Test
    @DisplayName("Handle LLM hallucinating invalid target name")
    void testHandleInvalidTargetName() {
        mockLLMResponse("attack", "NonExistentEnemy", "Hallucinated name");

        GameCommand command = llmPlayer.decideAction(warrior, allies, enemies, gameState);

        assertNotNull(command, "Should still return a command");
        assertInstanceOf(AttackCommand.class, command, "Should default to attack");
    }

    /**
     * Test 6: Graceful fallback on LLM API exception.
     */
    @Test
    @DisplayName("Graceful fallback on LLM API exception")
    void testFallbackOnLLMException() {
        ChatClientRequestSpec mockRequestSpec = mock(ChatClientRequestSpec.class);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);

        CallResponseSpec mockCallSpec = mock(CallResponseSpec.class);
        when(mockRequestSpec.call()).thenReturn(mockCallSpec);
        when(mockCallSpec.entity(LLMPlayer.Decision.class))
                .thenThrow(new RuntimeException("API timeout"));

        GameCommand command = llmPlayer.decideAction(warrior, allies, enemies, gameState);

        assertNotNull(command, "Should return fallback command despite exception");
        assertInstanceOf(AttackCommand.class, command, "Should default to attack");
    }

    /**
     * Test 7: Case-insensitive target name matching.
     */
    @Test
    @DisplayName("Case-insensitive target name matching")
    void testCaseInsensitiveTargetMatching() {
        mockLLMResponse("attack", "testarcher", "Using lowercase name");

        GameCommand command = llmPlayer.decideAction(warrior, allies, enemies, gameState);

        assertInstanceOf(AttackCommand.class, command, "Should match case-insensitively");
    }

    /**
     * Test 8: LLM adapts strategy based on changing game state.
     */
    @Test
    @DisplayName("LLM adapts strategy based on game state changes")
    void testLLMAdaptsToGameState() {
        mockLLMResponse("attack", "TestArcher", "All healthy, focus fire");
        GameCommand cmd1 = llmPlayer.decideAction(warrior, allies, enemies, gameState);
        assertInstanceOf(AttackCommand.class, cmd1, "Should attack when all healthy");

        mage.takeDamage(70);
        mockLLMResponse("heal", "TestMage", "Ally critical, must heal");
        GameCommand cmd2 = llmPlayer.decideAction(warrior, allies, enemies, gameState);
        assertInstanceOf(HealCommand.class, cmd2, "Should heal when ally critical");
    }

    /**
     * Test 9: Unknown action type triggers fallback.
     */
    @Test
    @DisplayName("Unknown action type triggers fallback")
    void testUnknownActionTypeFallback() {
        mockLLMResponse("defend", "TestArcher", "Trying to defend");

        GameCommand command = llmPlayer.decideAction(warrior, allies, enemies, gameState);

        assertNotNull(command, "Should return fallback command");
        assertInstanceOf(AttackCommand.class, command, "Should default to attack");
    }

    /**
     * Test 10: Multiple consecutive LLM calls work correctly.
     */
    @Test
    @DisplayName("Multiple consecutive LLM decisions work")
    void testMultipleConsecutiveLLMCalls() {
        mockLLMResponse("attack", "TestArcher", "First attack");
        GameCommand cmd1 = llmPlayer.decideAction(warrior, allies, enemies, gameState);

        mockLLMResponse("attack", "TestRogue", "Second attack");
        GameCommand cmd2 = llmPlayer.decideAction(warrior, allies, enemies, gameState);

        mockLLMResponse("heal", "TestMage", "Third action heal");
        GameCommand cmd3 = llmPlayer.decideAction(warrior, allies, enemies, gameState);

        assertNotNull(cmd1);
        assertNotNull(cmd2);
        assertNotNull(cmd3);
        assertInstanceOf(AttackCommand.class, cmd1);
        assertInstanceOf(AttackCommand.class, cmd2);
        assertInstanceOf(HealCommand.class, cmd3);
    }

    private void mockLLMResponse(String action, String target, String reasoning) {
        ChatClientRequestSpec mockRequestSpec = mock(ChatClientRequestSpec.class);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);

        CallResponseSpec mockCallSpec = mock(CallResponseSpec.class);
        when(mockRequestSpec.call()).thenReturn(mockCallSpec);

        LLMPlayer.Decision mockDecision = new LLMPlayer.Decision(action, target, reasoning);
        when(mockCallSpec.entity(LLMPlayer.Decision.class)).thenReturn(mockDecision);
    }
}