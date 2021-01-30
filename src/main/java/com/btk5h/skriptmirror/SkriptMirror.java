package com.btk5h.skriptmirror;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.util.Version;
import com.btk5h.skriptmirror.skript.CondParseLater;
import com.btk5h.skriptmirror.skript.custom.condition.CustomConditionSection;
import com.btk5h.skriptmirror.skript.custom.effect.CustomEffectSection;
import com.btk5h.skriptmirror.skript.custom.event.CustomEventSection;
import com.btk5h.skriptmirror.skript.custom.expression.CustomExpressionSection;
import com.btk5h.skriptmirror.skript.reflect.ExprJavaCall;
import com.btk5h.skriptmirror.skript.reflect.ExprProxy;
import com.btk5h.skriptmirror.skript.reflect.sections.CondSection;
import com.btk5h.skriptmirror.util.SkriptReflection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SkriptMirror extends JavaPlugin {
  private static SkriptMirror instance;
  private static SkriptAddon addonInstance;

  public SkriptMirror() {
    if (instance == null) {
      instance = this;
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();

    try {
      getAddonInstance().loadClasses("com.btk5h.skriptmirror.skript");

      Path dataFolder = SkriptMirror.getInstance().getDataFolder().toPath();
      LibraryLoader.loadLibraries(dataFolder);
    } catch (IOException e) {
      e.printStackTrace();
    }

    ParseOrderWorkarounds.reorderSyntax();

    // Disable *all* and/or warnings
    SkriptReflection.disableAndOrWarnings();

    Metrics metrics = new Metrics(this, 10157);

    metrics.addCustomChart(new Metrics.DrilldownPie("skript_version", () -> {
      Map<String, Map<String, Integer>> map = new HashMap<>();

      Version version = Skript.getVersion();
      Map<String, Integer> entry = new HashMap<>();
      entry.put(version.toString(), 1);

      map.put("" + version.getMajor() + "." + version.getMinor(), entry);

      return map;
    }));

    metrics.addCustomChart(new Metrics.SimplePie("preload_enabled",
      () -> "" + getConfig().getBoolean("enable-preloading")));
    metrics.addCustomChart(new Metrics.SimplePie("deferred_parsing_used",
      () -> "" + CondParseLater.deferredParsingUsed));

    metrics.addCustomChart(new Metrics.SingleLineChart("java_calls_made", () -> {
      int i = ExprJavaCall.javaCallsMade;
      ExprJavaCall.javaCallsMade = 0;
      return i;
    }));

    metrics.addCustomChart(new Metrics.SimplePie("custom_conditions_used",
      () -> "" + CustomConditionSection.customConditionsUsed));
    metrics.addCustomChart(new Metrics.SimplePie("custom_effects_used",
      () -> "" + CustomEffectSection.customEffectsUsed));
    metrics.addCustomChart(new Metrics.SimplePie("custom_events_used",
      () -> "" + CustomEventSection.customEventsUsed));
    metrics.addCustomChart(new Metrics.SimplePie("custom_expressions_used",
      () -> "" + CustomExpressionSection.customExpressionsUsed));

    metrics.addCustomChart(new Metrics.SimplePie("proxies_used",
      () -> "" + ExprProxy.proxiesUsed));
    metrics.addCustomChart(new Metrics.SimplePie("sections_used",
      () -> "" + CondSection.sectionsUsed));

  }

  public static SkriptAddon getAddonInstance() {
    if (addonInstance == null) {
      addonInstance = Skript.registerAddon(getInstance()).setLanguageFileDirectory("lang");
    }
    return addonInstance;
  }

  public static SkriptMirror getInstance() {
    if (instance == null) {
      throw new IllegalStateException();
    }
    return instance;
  }

}
