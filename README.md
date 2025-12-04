# Assignment 6: AI-Powered Game Players

**Team:** Noewlla Uwayisenga, Chris Burns, Gabriela Scavenius  
**Date:** December 2024

---

## Executive Summary

Successfully integrated LLMs into a turn-based RPG using Spring AI. All 6 TODOs complete, 162 tests passing at 100%, with robust AI-powered gameplay using GPT-5 and Claude Sonnet 4.5.


---

## Game Configuration

### Team 1: Human + Rule-Based AI
- **Bob (Warrior)** - Human | HP: 150, ATK: 20, DEF: 15
- **Wizard (Mage)** - RuleBasedAI | HP: 80, ATK: 35, DEF: 8

### Team 2: LLM Players
- **Barbara (Archer)** - OpenAI GPT-5 | HP: 100, ATK: 25, DEF: 10
- **Shadow (Rogue)** - Claude Sonnet 4.5 | HP: 90, ATK: 30, DEF: 12

---

## Design Patterns

### From Assignment 5
- **Strategy**: `Player` interface with multiple implementations
- **Command**: `AttackCommand`, `HealCommand` for encapsulated actions
- **Factory**: `CharacterFactory` for pre-configured characters
- **Builder**: `Character.builder()` for object construction

### New in Assignment 6
- **Adapter**: `LLMPlayer` converts JSON → `GameCommand`
- **Facade**: `GameController.playGame()` simplifies game loop
- **Mediator**: `GameController` coordinates all components

**Key Insight:** Strategy pattern from Assignment 5 enabled adding LLM players with **zero changes** to existing game loop code.

---

## AI Implementation

### Prompt Engineering (TODO 1)

7-part structure:
1. **Role Definition**: "You are Bob, a WARRIOR..."
2. **Current Status**: HP, attack, defense with percentages
3. **Ally Status**: Team health with visual indicators (⚠️ CRITICAL)
4. **Enemy Status**: Opponent health with markers (⚡ WOUNDED)
5. **Available Actions**: Concrete outcomes for each action
6. **Tactical Guidance**: Role-specific strategic advice
7. **JSON Format**: Explicit structure specification

### Spring AI Integration (TODO 2)
```java
Decision decision = chatClient.prompt()
    .user(prompt)
    .call()
    .entity(Decision.class);  // Auto-deserialize JSON
```

Benefits: Unified API, automatic parsing, built-in error handling, production-ready.

### Error Handling (TODO 3)

Robust fallbacks ensure game never crashes:
- Network failures → Attack weakest enemy
- Malformed JSON → Validate and fallback
- Invalid target → Search with fallback
- Null values → Default action

---

## Additional Testing

**10 comprehensive tests** beyond requirements (100% pass rate):

**AI Decision Making:**
1. LLM targets low HP enemy correctly
2. LLM heals critical ally appropriately
3. LLM adapts strategy across turns

**Error Handling:**
4. Fallback on null action
5. Fallback on null target
6. Handle invalid target names (hallucinations)
7. Graceful handling of API exceptions

**Edge Cases:**
8. Case-insensitive target matching
9. Unknown action type fallback
10. Multiple consecutive LLM decisions

**Total Tests:** 162 | **Failures:** 0 | **Duration:** 1.215s

---

## Running the Game

### Setup
```bash
# Set API keys
export OPENAI_API_KEY=sk-...
export ANTHROPIC_API_KEY=sk-ant-...

# Build and test
./gradlew build
./gradlew test

# Run game
./gradlew run
```

### Expected Output
```
============================================================
AI-POWERED RPG GAME
============================================================

Team 1:
  - Bob (WARRIOR) - HumanPlayer - HP: 150
  - Wizard (MAGE) - RuleBasedPlayer - HP: 80

Team 2:
  - Barbara (ARCHER) - LLM Player - HP: 100
  - Shadow (ROGUE) - LLM Player - HP: 90
============================================================
```

---

## Learning Reflections

### 1. Strategy Pattern Impact
The Strategy pattern from Assignment 5 made extension trivial. Adding `LLMPlayer` required zero changes to existing code - just implement the `Player` interface. This demonstrates the Open-Closed Principle perfectly.

### 2. Prompt Engineering
Most critical elements:
- **Health percentages** (LLMs reason better about relative values)
- **Visual indicators** (⚠️ ⚡ focus attention)
- **Explicit JSON format** (reduces parsing errors)
- **Role-specific guidance** (improves tactical decisions)

### 3. AI vs Rule-Based
LLMs showed superior decision-making:
- Contextual adaptation to game state
- Strategic reasoning with explanations
- Tactical variety vs predictable patterns

### 4. Error Handling
Production AI must assume failures. Our fallback strategy ensures continuity:
```java
try {
    Decision decision = chatClient.prompt()...
    if (decision.action() == null) return defaultAction();
    // Validate and execute
} catch (Exception e) {
    return defaultAction();  // Always have backup
}
```

### 5. Spring AI Benefits
- **Unified API** across providers
- **Auto-deserialization** via `.entity()`
- **Configuration-based** provider switching
- **Production-ready** error handling

Compared to raw HTTP: eliminates manual parsing, retries, auth, and provider-specific code.

---

## Requirements Checklist

- [x] TODO 1: LLM Prompt (10 pts)
- [x] TODO 2: Spring AI Integration (15 pts)
- [x] TODO 3: Decision → Command (10 pts)
- [x] TODO 4: Game Loop (15 pts)
- [x] TODO 5: Turn Processing (10 pts)
- [x] TODO 6: Team Configuration (15 pts)
- [x] Code Quality (10 pts)
- [x] Testing: 162 tests + 10 additional (10 pts)
- [x] Documentation (10 pts)
- [x] Demo (5 pts)


---

## Team Contributions

- **Noewlla Uwayisenga:** LLM integration, prompt engineering, Spring AI implementation
- **Chris Burns:** Game controller, team configuration, game loop, turn processing
- **Gabriela Scavenius:** Error handling, testing (10 additional tests), documentation, final submission

---

## Resources

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [OpenAI API Docs](https://platform.openai.com/docs)
- [Anthropic Claude Docs](https://docs.anthropic.com/)
- Assignment 5 codebase

---
