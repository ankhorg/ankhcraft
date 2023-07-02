package bot.inker.ankh.craft.config;

import bot.inker.ankh.craft.api.AnkhCraft;
import bot.inker.ankh.craft.util.KeyUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventPriority;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.inksnow.ankh.core.api.plugin.PluginLifeCycle;
import org.inksnow.ankh.core.api.plugin.annotations.SubscriptLifecycle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Singleton
@Slf4j
public final class AcConfigService {
  private final Map<Key, CraftTableConfig> ankhCraftConfig = new HashMap<>();
  private final Map<Key, CraftTableConfig> unmodifiableAnkhCraftConfig = Collections.unmodifiableMap(ankhCraftConfig);

  @Inject
  private AcConfigService() {
    //
  }

  public @Nullable CraftTableConfig getById(@Nonnull Key tableId) {
    return ankhCraftConfig.get(KeyUtil.warpKey(tableId));
  }

  public @Nonnull Map<Key, CraftTableConfig> getLoaded(){
    return unmodifiableAnkhCraftConfig;
  }

  @SubscriptLifecycle(value = PluginLifeCycle.LOAD, priority = EventPriority.LOWEST)
  private void onLoad(){
    val tableConfigDirectory = new File("plugins/" + AnkhCraft.ID + "/craft-tables");
    val tableConfigFiles = tableConfigDirectory.listFiles(it->!it.isDirectory() && it.getName().endsWith(".yml"));
    for (val tableConfigFile : Objects.requireNonNullElse(tableConfigFiles, new File[0])) {
      try {
        val acConfig = new CraftTableConfig(YamlConfiguration.loadConfiguration(tableConfigFile));
        ankhCraftConfig.put(acConfig.id(), acConfig);
      }catch (Exception e){
        logger.error("Failed to load table config {}", tableConfigFile.getName(), e);
      }
    }
    if(ankhCraftConfig.isEmpty()){
      logger.warn("No any craft config loaded");
    }
  }
}
