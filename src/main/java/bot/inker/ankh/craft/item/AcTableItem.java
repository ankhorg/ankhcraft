package bot.inker.ankh.craft.item;

import bot.inker.ankh.craft.block.AcTableBlock;
import bot.inker.ankh.craft.config.CraftTableConfig;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.kyori.adventure.key.Key;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.inksnow.ankh.core.api.block.AnkhBlock;
import org.inksnow.ankh.core.api.ioc.IocLazy;
import org.inksnow.ankh.core.api.item.AnkhItem;
import org.inksnow.ankh.core.api.item.AnkhItemService;
import org.inksnow.ankh.core.api.script.PreparedScript;
import org.inksnow.ankh.core.api.script.ScriptContext;
import org.inksnow.ankh.core.api.util.DcLazy;
import org.inksnow.ankh.core.api.world.WorldService;

import javax.annotation.Nonnull;

@Slf4j
public final class AcTableItem implements AnkhItem {
  private static final DcLazy<AnkhItemService> itemService = IocLazy.of(AnkhItemService.class);
  private static final DcLazy<WorldService> worldService = IocLazy.of(WorldService.class);
  private final Key tableId;
  private final CraftTableConfig tableConfig;
  private final AcTableBlock.Factory blockFactory;

  public AcTableItem(Key itemKey, CraftTableConfig tableConfig, AcTableBlock.Factory blockFactory) {
    this.tableId = itemKey;
    this.tableConfig = tableConfig;
    this.blockFactory = blockFactory;
  }

  @Override
  public @Nonnull Key key() {
    return tableId;
  }

  @Override
  public void updateItem(ItemStack item) {
    itemService.get().tagItem(item);
  }

  @Override
  public void onBlockPlace(BlockPlaceEvent event) {
    if (!runRequirements(event, tableConfig.requirements())) {
      event.setCancelled(true);
      return;
    }

    val itemCost = event.getItemInHand().clone();
    itemCost.setAmount(1);
    val ankhBlock = blockFactory.create(itemCost);
    worldService.get().setBlock(event.getBlockPlaced().getLocation(), ankhBlock);

    runOnPlaceActions(event, ankhBlock, tableConfig.actions().onPlace());
  }

  private boolean runRequirements(BlockPlaceEvent event, PreparedScript[] requirements) {
    val player = event.getPlayer();
    val context = ScriptContext.builder()
            .player(player)
            .with("event", event)
            .build();
    for (PreparedScript requirementShell : requirements) {
      Object result;
      try {
        result = requirementShell.execute(context);
      } catch (Exception e) {
        logger.error("Failed to execute requirement", e);
        return false;
      }
      if (!objectToBoolean(result)) {
        return false;
      }
    }
    return true;
  }

  private void runOnPlaceActions(BlockPlaceEvent event, AnkhBlock ankhBlock, PreparedScript[] onPlaceActions) {
    val player = event.getPlayer();
    val context = ScriptContext.builder()
            .player(player)
            .with("event", event)
            .build();
    for (PreparedScript onPlaceAction : onPlaceActions) {
      try {
        onPlaceAction.execute(context);
      } catch (Exception e) {
        logger.error("Failed to execute on-place action", e);
      }
    }
  }

  private boolean objectToBoolean(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj instanceof Boolean) {
      return (Boolean) obj;
    }
    if (!(obj instanceof String)) {
      logger.warn("ankh-craft requirement result type {} can't be cast to boolean", obj.getClass());
    }
    return Boolean.parseBoolean(obj.toString());
  }
}
