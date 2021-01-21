package com.btk5h.skriptmirror.skript;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.ObjectWrapper;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.skript.reflect.ExprJavaCall;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CondExpressionStatement extends Condition {

  static {
    Skript.registerCondition(CondExpressionStatement.class, "[(1¦await)] %~javaobject%");
  }

  private static final ExecutorService threadPool =
      Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

  private Expression<Object> arg;
  private boolean isAsynchronous;
  private boolean isCondition;

  @Override
  public boolean check(Event e) {
    Object result = ObjectWrapper.unwrapIfNecessary(arg.getSingle(e));
    return !isCondition || isTruthy(result);
  }

  private boolean isTruthy(Object o) {
    return o != Boolean.FALSE
        && o != null
        && (!(o instanceof Number) || !(((Number) o).doubleValue() == 0 || Double.isNaN(((Number) o).doubleValue())));
  }

  @Override
  protected TriggerItem walk(Event e) {
    if (isAsynchronous) {
      Object localVariables = SkriptReflection.getLocals(e);
      CompletableFuture.runAsync(() -> {
        SkriptReflection.putLocals(localVariables, e);
        check(e);
      }, threadPool)
        .thenAccept(res -> Bukkit.getScheduler().runTask(SkriptMirror.getInstance(), () -> {
          if (getNext() != null)
            TriggerItem.walk(getNext(), e);

          SkriptReflection.removeLocals(e);
        }));
      return null;
    }
    return super.walk(e);
  }

  @Override
  public String toString(Event e, boolean debug) {
    return arg.toString(e, debug);
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    arg = SkriptUtil.defendExpression(exprs[0]);

    if (!(arg instanceof ExprJavaCall))
      return false;

    isAsynchronous = (parseResult.mark & 1) == 1;
    isCondition = SkriptLogger.getNode() instanceof SectionNode;

    if (isCondition && isAsynchronous) {
      Skript.error("Asynchronous java calls may not be used as conditions.");
      return false;
    }

    if (isAsynchronous)
      ScriptLoader.hasDelayBefore = Kleenean.TRUE;

    return SkriptUtil.canInitSafely(arg);
  }
}
