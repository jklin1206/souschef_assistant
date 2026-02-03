# AI Model Router & State Management Architecture

## Overview

A **4-state architecture** with dual-model routing:
- **Cheap model** (instruction follower) for FOLLOW and TIMER_WAIT states
- **GPT-4** (intelligent) for CREATIVE and RECOVERY states

---

## State Machine

### States

| State | Model | Token Budget | Purpose |
|-------|-------|--------------|---------|
| **CREATIVE** | Premium (GPT-4) | 1500 | Recipe generation, meal planning |
| **FOLLOW** | Cheap (Groq/Haiku) | 500 | Step navigation, "next", "done" |
| **RECOVERY** | Premium (GPT-4) | 2000 | Substitutions, troubleshooting, food safety |
| **TIMER_WAIT** | Cheap | 200 | Passive monitoring while timers run |

### State Transition Diagram

```
                    ┌────────────┐
                    │  CREATIVE  │ ◄──────────────────┐
                    └─────┬──────┘                    │
                          │ select recipe            │ abandon
                          ▼                           │
                    ┌────────────┐                    │
          ┌────────►│   FOLLOW   │────────────────────┤
          │         └──┬─────┬───┘                    │
          │   timer    │     │ problem               │
          │   done     │     ▼                       │
          │      ┌─────┴───┐ ┌────────────┐          │
          │      │ TIMER   │ │  RECOVERY  │──────────┘
          │      │  WAIT   │ └──────┬─────┘  solved
          │      └────┬────┘        │
          └───────────┘◄────────────┘
```

### Valid Transitions

| From | To | Trigger |
|------|----|---------|
| CREATIVE | FOLLOW | User selects recipe |
| FOLLOW | TIMER_WAIT | Timer started, >2 min remaining |
| FOLLOW | RECOVERY | Problem detected |
| FOLLOW | CREATIVE | Abandon recipe |
| TIMER_WAIT | FOLLOW | Timer complete |
| TIMER_WAIT | RECOVERY | Urgent problem |
| RECOVERY | FOLLOW | Problem resolved |
| RECOVERY | CREATIVE | Abandon recipe |

---

## Cheap Model Cost Analysis

| Model | Input Cost | Output Cost | Hosting | Best For |
|-------|-----------|-------------|---------|----------|
| **GPT-3.5-turbo** | $0.0005/1K | $0.0015/1K | None | Easiest integration |
| **Claude Haiku** | $0.00025/1K | $0.00125/1K | None | Best value API |
| **Groq (Llama 3)** | Free tier | Free tier | None | Actually free (rate limited) |
| **Local Llama/Mistral** | $0 | $0 | Your server | True free, needs GPU |
| **Gemini Flash** | $0.075/1M | $0.30/1M | None | Very cheap |

**Recommendation**: Start with **Groq** (free tier) or **Claude Haiku** (cheapest reliable API).

---

## Implementation Classes

### State Enums

**Location**: `src/main/java/com/souschef/sous_chef/ai/state/`

```java
// CookingSessionState.java
public enum CookingSessionState {
    CREATIVE(ModelTier.PREMIUM, 1500, true),
    FOLLOW(ModelTier.CHEAP, 500, false),
    RECOVERY(ModelTier.PREMIUM, 2000, false),
    TIMER_WAIT(ModelTier.CHEAP, 200, false);

    private final ModelTier modelTier;
    private final int maxContextTokens;
    private final boolean allowsCreativity;
}

// ModelTier.java
public enum ModelTier {
    CHEAP,    // Groq/Haiku/GPT-3.5 - instruction following
    PREMIUM   // GPT-4 - creative + recovery
}
```

### Model Router

**Location**: `src/main/java/com/souschef/sous_chef/ai/router/`

```java
@Component
public class ModelRouter {
    private final ChatClient premiumClient;  // GPT-4
    private final ChatClient cheapClient;    // Groq/Haiku/GPT-3.5

    public ChatClient route(SessionContext context) {
        // Check for recovery intent BEFORE using state
        if (isRecoveryIntent(context.getLastUserInput())) {
            context.transitionTo(RECOVERY);
            return premiumClient;
        }

        return switch (context.getCurrentState().getModelTier()) {
            case PREMIUM -> premiumClient;
            case CHEAP -> cheapClient;
        };
    }

    // Keywords that ALWAYS trigger premium model
    private boolean isRecoveryIntent(String input) {
        return containsAny(input.toLowerCase(),
            // Food safety (critical)
            "raw", "undercooked", "safe to eat", "food poison", "allergic",
            // Substitutions
            "don't have", "substitute", "replacement", "instead of", "ran out",
            // Problems
            "burnt", "burning", "curdled", "separated", "ruined", "fix this"
        );
    }
}
```

### Intent Classifier (Fast Pre-routing)

```java
@Component
public class IntentClassifier {
    public enum Intent {
        // Cheap model intents (FOLLOW/TIMER_WAIT)
        NEXT_STEP, PREVIOUS_STEP, SET_TIMER, CHECK_TIMER, READ_STEP,

        // Premium model intents (CREATIVE/RECOVERY)
        RECIPE_GENERATION, SUBSTITUTION, TROUBLESHOOT, FOOD_SAFETY,
        TECHNIQUE_QUESTION, START_COOKING, ABANDON_RECIPE
    }

    public Intent classify(String input, CookingSessionState state) {
        String lower = input.toLowerCase();

        // Universal timer intents
        if (lower.matches(".*(set|start).*(timer|alarm).*")) return SET_TIMER;
        if (lower.matches(".*(how (much|long)|time (left|remaining)).*")) return CHECK_TIMER;

        return switch (state) {
            case FOLLOW -> classifyFollowIntent(lower);
            case CREATIVE -> classifyCreativeIntent(lower);
            default -> Intent.GENERAL;
        };
    }
}
```

---

## Token Optimization Strategy

### Context Budget by State

| State | Max Tokens | What's Included |
|-------|-----------|-----------------|
| CREATIVE | 1500 | User preferences, dietary restrictions, available equipment |
| FOLLOW | 500 | Current step text, next step preview, active timers only |
| RECOVERY | 2000 | Full recipe context + problem description + relevant history |
| TIMER_WAIT | 200 | Active timers only, next action |

### Compressed Snapshots

**Instead of sending full User object (~500 tokens):**
```json
{
  "id": 1, "username": "chef1", "email": "...", "passwordHash": "...",
  "firstName": "John", "lastName": "Doe", "createdAt": "...", "updatedAt": "...",
  "dietaryRestrictions": ["vegetarian"], "skillLevel": "intermediate",
  "equipment": ["instant pot", "stand mixer", "air fryer"]
}
```

**Send compressed snapshot (~100 tokens):**
```
Diet: vegetarian. Skill: intermediate. Equipment: instant pot, stand mixer, air fryer.
```

**Token savings: 80%**

### Session Context Builder

```java
@Data
@Builder
public class SessionContext {
    private String sessionId;
    private CookingSessionState currentState;

    // Compressed snapshots (NOT full objects)
    private UserPreferencesSnapshot userPrefs;      // ~200 tokens
    private ActiveRecipeSnapshot activeRecipe;      // ~300 tokens
    private List<ActiveTimer> timers;               // ~50 tokens
    private RecoveryContext recoveryContext;        // ~400 tokens (only in RECOVERY)

    // Minimal history
    private String lastUserInput;
    private String lastAssistantResponse;

    public String buildContextPrompt() {
        return switch (currentState) {
            case CREATIVE -> buildCreativeContext();  // prefs only
            case FOLLOW -> buildFollowContext();      // current step only
            case RECOVERY -> buildRecoveryContext();  // full context
            case TIMER_WAIT -> buildTimerContext();   // timers only
        };
    }
}
```

---

## History Persistence Strategy

### When to PERSIST (to database)

| Scenario | Reason |
|----------|--------|
| Recipe completed | User history, potential favorite |
| Recovery interaction | Learning data for future suggestions |
| Creative session user engaged with | User showed interest |
| Session timeout with active recipe | Resume capability |

### When to DISCARD (in-memory only)

| Scenario | Reason |
|----------|--------|
| "Next step", "done", "what's next" | No long-term value |
| Timer checks | Transient state |
| Abandoned recipe (no explicit save) | User didn't want it |
| Routine acknowledgments | No information |

### In-Memory History Limits by State

```java
private int getMaxHistoryTurns(CookingSessionState state) {
    return switch (state) {
        case CREATIVE -> 5;     // Need conversation flow
        case FOLLOW -> 2;       // Just immediate context
        case RECOVERY -> 4;     // Need problem context
        case TIMER_WAIT -> 1;   // Almost nothing
    };
}
```

---

## Timer Integration

### Timer State Transitions

```java
@Component
public class CookingTimerManager {

    public ActiveTimer createTimer(String sessionId, String label,
                                   Duration duration, String onComplete) {
        // Create timer
        // Schedule completion notification via WebSocket
        // Evaluate state transition to TIMER_WAIT if all timers > 2 min
    }

    private void evaluateStateForTimers(String sessionId) {
        Duration shortest = getShortestRemaining(sessionId);
        SessionContext ctx = getContext(sessionId);

        // Transition to TIMER_WAIT if waiting > 2 min
        if (shortest.toMinutes() >= 2 && ctx.getCurrentState() == FOLLOW) {
            transitionTo(sessionId, TIMER_WAIT);
        }
        // Wake up when timer almost done
        if (shortest.toSeconds() < 30 && ctx.getCurrentState() == TIMER_WAIT) {
            transitionTo(sessionId, FOLLOW);
        }
    }
}
```

---

## Configuration

### application.properties

```properties
# Premium Model (GPT-4 for creative + recovery)
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4-turbo-preview
spring.ai.openai.chat.options.temperature=0.7

# Cheap Model Option 1: Groq (Llama 3 - has free tier)
groq.api.key=${GROQ_API_KEY}
groq.api.base-url=https://api.groq.com/openai/v1
groq.model=llama3-8b-8192

# Cheap Model Option 2: Claude Haiku (backup)
# anthropic.api.key=${ANTHROPIC_API_KEY}
```

### AiModelConfig.java

```java
@Configuration
public class AiModelConfig {

    @Bean("premiumChatClient")
    public ChatClient premiumChatClient(OpenAiChatModel openAiModel) {
        return ChatClient.builder(openAiModel)
            .defaultOptions(ChatOptionsBuilder.builder()
                .temperature(0.3)
                .build())
            .build();
    }

    @Bean("cheapChatClient")
    public ChatClient cheapChatClient() {
        // Groq/Haiku with standard settings
    }
}
```

---

## File Structure

```
src/main/java/com/souschef/sous_chef/
├── ai/
│   ├── state/
│   │   ├── CookingSessionState.java
│   │   ├── ModelTier.java
│   │   └── StateTransitionRule.java
│   ├── router/
│   │   ├── ModelRouter.java
│   │   └── IntentClassifier.java
│   ├── context/
│   │   ├── SessionContext.java
│   │   ├── UserPreferencesSnapshot.java
│   │   ├── ActiveRecipeSnapshot.java
│   │   └── RecoveryContext.java
│   ├── timer/
│   │   ├── CookingTimerManager.java
│   │   └── ActiveTimer.java
│   └── history/
│       └── ConversationHistoryManager.java
├── config/
│   └── AiModelConfig.java
└── service/
    └── CookingAssistantService.java
```

---

## Verification Checklist

- [ ] State transitions work correctly
- [ ] FOLLOW uses cheap model
- [ ] RECOVERY uses GPT-4
- [ ] "substitute" keyword triggers GPT-4 even in FOLLOW state
- [ ] FOLLOW context < 500 tokens
- [ ] CREATIVE context < 1500 tokens
- [ ] History trimmed per state limits
- [ ] Timers trigger state transitions appropriately