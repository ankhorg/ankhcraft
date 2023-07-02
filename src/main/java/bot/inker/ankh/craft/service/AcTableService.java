package bot.inker.ankh.craft.service;

import bot.inker.ankh.craft.block.AcTableBlock;
import bot.inker.ankh.craft.config.AcConfigService;
import bot.inker.ankh.craft.config.CraftTableConfig;
import bot.inker.ankh.craft.item.AcTableItem;
import com.google.inject.Injector;
import lombok.val;
import net.kyori.adventure.key.Key;
import org.bukkit.event.EventPriority;
import org.inksnow.ankh.core.api.block.BlockRegistry;
import org.inksnow.ankh.core.api.item.AnkhItemRegistry;
import org.inksnow.ankh.core.api.plugin.PluginLifeCycle;
import org.inksnow.ankh.core.api.plugin.annotations.SubscriptLifecycle;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public final class AcTableService {
  private final Injector injector;
  private final AcConfigService acConfigService;
  private final AnkhItemRegistry itemRegistry;
  private final BlockRegistry blockRegistry;

  @Inject
  private AcTableService(Injector injector, AcConfigService acConfigService, AnkhItemRegistry itemRegistry, BlockRegistry blockRegistry) {
    this.injector = injector;
    this.acConfigService = acConfigService;
    this.itemRegistry = itemRegistry;
    this.blockRegistry = blockRegistry;
  }

  @SubscriptLifecycle(value = PluginLifeCycle.LOAD, priority = EventPriority.LOWEST)
  private void onLoad(){
    val loadedConfig = acConfigService.getLoaded();
    for (Map.Entry<Key, CraftTableConfig> entry : loadedConfig.entrySet()) {
      val tableBlockFactory = new AcTableBlock.Factory(entry.getKey(), entry.getValue());
      val tableItem = new AcTableItem(entry.getKey(), entry.getValue(), tableBlockFactory);

      itemRegistry.register(tableItem);
      blockRegistry.register(tableBlockFactory);
    }
  }
}
