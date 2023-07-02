package bot.inker.ankh.craft.command;

import bot.inker.ankh.craft.api.AnkhCraft;
import bot.inker.ankh.craft.config.AcConfigService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.inksnow.ankh.core.api.item.AnkhItemService;
import org.inksnow.ankh.core.api.plugin.PluginLifeCycle;
import org.inksnow.ankh.core.api.plugin.annotations.SubscriptLifecycle;
import org.inksnow.ankh.core.common.util.ExecuteReportUtil;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Singleton
@Slf4j
public final class AcCommand extends Command {
  private static final String NAMESPACED_NAME = AnkhCraft.ID + ":" + AnkhCraft.ID;
  private final AnkhItemService itemService;
  private final AcConfigService configService;

  @Inject
  private AcCommand(AnkhItemService itemService, AcConfigService configService) {
    super(NAMESPACED_NAME, AnkhCraft.NAME, "Ankh Craft Core Command", List.of(
      AnkhCraft.ID,
      AnkhCraft.ID + ":" + "ac",
      "ac"
    ));
    this.itemService = itemService;
    this.configService = configService;
  }

  @SubscriptLifecycle(PluginLifeCycle.ENABLE)
  private void onEnable() {
    Bukkit.getServer().getCommandMap().register(NAMESPACED_NAME, this);
  }

  @Override
  public boolean execute(@Nonnull CommandSender sender, @Nonnull String commandLabel, @Nonnull String[] args) {
    return Objects.requireNonNullElse(
      ExecuteReportUtil.catchReport(sender, () -> execute0(sender, commandLabel, args)),
      Optional.of(false)
    ).orElse(false);
  }

  private boolean execute0(@Nonnull CommandSender sender, @Nonnull String commandLabel, @Nonnull String[] args) throws Exception {
    if (args[0].equals("give")) {
      val player = (Player) sender;
      val item = new ItemStack(Material.BEDROCK);
      val tableId = Key.key(args[1]);
      val tableConfig = configService.getById(tableId);
      itemService.tagItem(item, tableConfig.id());
      item.editMeta(itemMeta -> {
        itemMeta.displayName(Component.text("ac: " + args[1]));
        itemMeta.lore(List.of(Component.text("ac: " + args[1])));
      });
      player.getInventory().addItem(item);
    }
    return true;
  }
}
