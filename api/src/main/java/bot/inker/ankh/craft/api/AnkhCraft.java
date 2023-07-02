package bot.inker.ankh.craft.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.inksnow.ankh.core.api.AnkhCoreLoader;
import org.inksnow.ankh.core.api.ioc.IocLazy;
import org.inksnow.ankh.core.api.util.DcLazy;

public class AnkhCraft {
  private AnkhCraft(){
    throw new UnsupportedOperationException();
  }

  public static final String ID = "ankh-craft";
  public static final String NAME = "Ankh Craft";
  public static final Component NAME_COMPONENT = Component.empty()
    .append(Component.text("Ankh", NamedTextColor.YELLOW))
    .append(Component.text("Craft", NamedTextColor.GREEN));

  private static final DcLazy<AnkhCoreLoader> plugin = IocLazy.of(AnkhCoreLoader.class);;

  public static Plugin plugin() {
    return plugin.get();
  }
}
