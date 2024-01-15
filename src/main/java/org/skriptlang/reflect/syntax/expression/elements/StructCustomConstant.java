package org.skriptlang.reflect.syntax.expression.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.util.StringUtils;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.skriptlang.reflect.syntax.expression.ConstantGetEvent;
import org.skriptlang.reflect.syntax.expression.ConstantSyntaxInfo;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;

import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

public class StructCustomConstant extends CustomSyntaxStructure<ConstantSyntaxInfo> {

  static {
    Skript.registerStructure(StructCustomConstant.class, EntryValidator.builder()
        .addSection("get", false)
        // We only have one entry, so we know it's 'get'
        .missingRequiredEntryMessage(key -> "Computed options don't work without a get section")
        .build(),
        "option <.+>"
    );
  }

  private static final DataTracker<ConstantSyntaxInfo> dataTracker = new DataTracker<>();

  static {
    // noinspection unchecked
    Skript.registerExpression(CustomExpression.class, Object.class, ExpressionType.SIMPLE);
    Optional<ExpressionInfo<?, ?>> info = StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(Skript.getExpressions(), Spliterator.ORDERED), false)
        .filter(i -> i.getElementClass() == CustomExpression.class)
        .findFirst();
    info.ifPresent(dataTracker::setInfo);
  }

  @Override
  protected DataTracker<ConstantSyntaxInfo> getDataTracker() {
    return dataTracker;
  }

  @Override
  public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, EntryContainer entryContainer) {
    String option = parseResult.regexes.get(0).group();

    SectionNode sectionNode = entryContainer.get("get", SectionNode.class, false);
    getParser().setCurrentEvent("custom constant getter", ConstantGetEvent.class);
    List<TriggerItem> items = SkriptUtil.getItemsFromNode(sectionNode);
    Trigger getter = new Trigger(getParser().getCurrentScript(), "get @{" + option + "}", new SimpleEvent(), items);

    computeOption(option, getter);
    return true;
  }

  private static void computeOption(String option, Trigger getter) {
    ConstantGetEvent constantEvent = new ConstantGetEvent(0, null);
    getter.execute(constantEvent);
    // Get result as a string
    String result = StringUtils.join(constantEvent.getOutput());

    // Get options of current script, and add it to that
    SkriptReflection.getOptions(ParserInstance.get().getCurrentScript())
        .put(option, result);
  }

}

