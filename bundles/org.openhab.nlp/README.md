# Natural language processor

This natural language processor (NLP) bundle is based on [Apache OpenNLP](https://opennlp.apache.org) for intent classification and entity extraction (thanks to [nlp-intent-toolkit](https://github.com/mlehman/nlp-intent-toolkit)).

It is composed of a modular intent-based skill system with learning data provisioning (basic skills to retrieve item statuses, historical data and send basic commands are built-in, but more can be injected by other OSGi dependency injection).

This bundle works great in tandem with the openHAB voice services to provide a [Human Language Interpreter](http://docs.openhab.org/configuration/multimedia.html#human-language-interpreter). Coupled with speech-to-text and text-to-speech engines it allows to build privacy-focused specialized voice assistant services.

A perfect match for this bundle is the corresponding REST part that adds
text based chat capabilities.

## Develop for the NLP in openHAB

You can develop your own skills as OSGi services. Have a look at the existing skills in `nlp.internal.skill` in this bundle.

The general approach is to extend `AbstractItemIntentInterpreter`:

```java 
@Component(service = Skill.class, immediate = true)
public class GetStatusSkill extends AbstractItemIntentInterpreter {

    @Override
    public String getIntentId() {
        return "get-status"; // your unique intent name
    }
    
    @Override
    public IntentInterpretation interpret(Intent intent, String language) {
        IntentInterpretation interpretation = new IntentInterpretation();
        return interpretation;
    }
}
```

openHAB is item centric, so are the skills. You generally want to interact with items
usually. To retrieve all recognized items of the query, use the helper methods `findItems`:

```java
@Override
    public IntentInterpretation interpret(Intent intent, String language) {
    Set<Item> matchedItems = findItems(intent);
    
    return interpretation;
}
```

/**
 * This {@link Skill} is used to show the status of objects to the user - builds or retrieves a card with the matching
 * items with the {@link CardBuilder}.
 *
 * @author Yannick Schaus - Initial contribution
 */
@Component(service = Skill.class, immediate = true)
public class GetStatusSkill extends AbstractItemIntentInterpreter {

    @Override
    public String getIntentId() {
        return "get-status";
    }

    @Override
    public IntentInterpretation interpret(Intent intent, String language) {
        IntentInterpretation interpretation = new IntentInterpretation();
        Set<Item> matchedItems = findItems(intent);

        if (intent.getEntities().isEmpty()) {
            interpretation.setAnswer(answerFormatter.getRandomAnswer("general_failure"));
            return interpretation;
        }
        if (matchedItems == null || matchedItems.isEmpty()) {
            interpretation.setAnswer(answerFormatter.getRandomAnswer("answer_nothing_found"));
            interpretation.setHint(answerFormatter.getStandardTagHint(intent.getEntities()));
        } else {
            interpretation.setMatchedItems(matchedItems);
            interpretation.filteredItems = matchedItems;
            interpretation.intent = intent;

            // interpretation.setCard(cardBuilder.buildCard(intent, matchedItems));
            interpretation.setAnswer(answerFormatter.getRandomAnswer("info_found_simple"));
        }

        return interpretation;
    }

    @Reference
    protected void setItemResolver(ItemResolver itemResolver) {
        this.itemResolver = itemResolver;
    }

    protected void unsetItemResolver(ItemResolver itemResolver) {
        this.itemResolver = null;
    }
}