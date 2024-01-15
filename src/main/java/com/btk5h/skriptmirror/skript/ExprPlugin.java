package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.ObjectWrapper;
import org.skriptlang.reflect.java.elements.structures.StructImport;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jdt.annotation.NonNull;

public class ExprPlugin extends SimplePropertyExpression<Object, ObjectWrapper> {

  static {
    Skript.registerExpression(ExprPlugin.class, ObjectWrapper.class, ExpressionType.PROPERTY, "[(an|the)] instance of [the] plugin %javatype/string%");
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
    super.init(exprs, matchedPattern, isDelayed, parseResult);

    if (exprs[0] instanceof StructImport.ImportHandler) {
      JavaType javaType = ((StructImport.ImportHandler) exprs[0]).getJavaType();
      Class<?> clazz = javaType.getJavaClass();

      if (!JavaPlugin.class.isAssignableFrom(clazz) || JavaPlugin.class.equals(clazz)) {
        Skript.error("The class " + clazz.getSimpleName() + " is not a plugin class");
        return false;
      }
    }

    return true;
  }

  @Override
  public ObjectWrapper convert(Object plugin) {
    if (plugin instanceof String) {
      String pluginName = (String) plugin;
      for (Plugin pluginInstance : Bukkit.getPluginManager().getPlugins()) {
        if (pluginInstance.getName().equalsIgnoreCase(pluginName)) {
          return ObjectWrapper.create(pluginInstance);
        }
      }

      return null;
    } else {
      Class<?> clazz = ((JavaType) plugin).getJavaClass();

      if (!JavaPlugin.class.isAssignableFrom(clazz) || JavaPlugin.class.equals(clazz)) {
        return null;
      }

      return ObjectWrapper.create(JavaPlugin.getPlugin(clazz.asSubclass(JavaPlugin.class)));
    }
  }

  @Override
  @NonNull
  public Class<? extends ObjectWrapper> getReturnType() {
    return ObjectWrapper.class;
  }

  @Override
  @NonNull
  protected String getPropertyName() {
    return "plugin instance";
  }

}
