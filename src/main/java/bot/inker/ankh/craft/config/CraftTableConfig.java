package bot.inker.ankh.craft.config;

import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.chars.Char2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import kotlin.text.StringsKt;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.inksnow.ankh.core.api.script.AnkhScriptService;
import org.inksnow.ankh.core.api.script.PreparedScript;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

@AllArgsConstructor(staticName = "of")
@lombok.Builder
public final class CraftTableConfig {
  @Getter
  private final @Nonnull Key id;
  @Getter
  private final @Nonnull String title;
  @Getter
  private final @Nonnull Component titleComponent;
  @Getter
  private final @Nonnull char[][] layout;
  @Getter
  private final @Nonnull Char2ObjectMap<TableSlotConfig> slots;
  @Getter
  private final @Nonnull IntObjectPair<TableSlotConfig>[] storageSlots;
  @Getter
  private final @Nonnull TableSlotConfig[] allSlots;
  @Getter
  private final @Nonnull PreparedScript[] requirements;
  @Getter
  private final @Nonnull ActionsConfig actions;

  public CraftTableConfig(ConfigurationSection configuration) {
    this.id = loadId(configuration);
    this.title = loadTitle(configuration);
    this.titleComponent = LegacyComponentSerializer.legacySection().deserialize(title);
    this.layout = loadLayout(configuration);
    this.slots = loadSlots(configuration);
    this.allSlots = loadAllSlots();
    this.storageSlots = loadStorageSlots();
    this.requirements = loadRequirements(configuration);
    this.actions = new ActionsConfig(configuration.getConfigurationSection("actions"));
  }

  private static <R> R required(R value, String path) {
    if (value == null) {
      throw new IllegalStateException("required config key '" + path + "' not found");
    } else {
      return value;
    }
  }

  private Key loadId(ConfigurationSection configuration){
    try {
      val value = required(configuration.getString("id"), "id");
      if(value.indexOf(':') != -1){
        return Key.key(value);
      }
    }catch (InvalidKeyException e){
    }
    throw new IllegalStateException("config key 'id' must be namespaced key likes 'ankh-craft:test'");
  }

  private String loadTitle(ConfigurationSection configuration) {
    return required(configuration.getString("title"), "title");
  }

  private char[][] loadLayout(ConfigurationSection configuration) {
    val layoutString = required(configuration.getString("layout"), "layout");
    val layoutByLine = StringsKt.split(layoutString, new char[]{'\n'}, false, 0);
    if (layoutByLine.size() < 1 || 6 < layoutByLine.size()) {
      throw new IllegalStateException("config key 'layout' lines should be in [1,6]");
    }
    val result = new char[layoutByLine.size()][];
    for (int i = 0; i < layoutByLine.size(); i++) {
      val layoutLine = layoutByLine.get(i);
      if (layoutLine.length() != 9) {
        throw new IllegalStateException("config key 'layout' per line length should be 9");
      }
      result[i] = layoutLine.toCharArray();
    }
    return result;
  }

  private Char2ObjectMap<TableSlotConfig> loadSlots(ConfigurationSection configuration) {
    val slotSection = required(configuration.getConfigurationSection("slots"), "slots");
    val slotKeyStrings = slotSection.getKeys(false);
    val result = new Char2ObjectArrayMap<TableSlotConfig>(slotKeyStrings.size());
    for (String slotKeyString : slotKeyStrings) {
      if (slotKeyString.length() != 1) {
        throw new IllegalStateException("config key 'slots." + slotKeyString + "' key isn't a char");
      }
      result.put(slotKeyString.charAt(0), new TableSlotConfig(slotKeyString, slotSection.getConfigurationSection(slotKeyString)));
    }
    return new Char2ObjectAVLTreeMap<>(result);
  }

  private TableSlotConfig[] loadAllSlots(){
    val result = new TableSlotConfig[9 * layout.length];
    for (int i = 0; i < this.layout.length; i++) {
      val layoutLine = this.layout[i];
      for (int j = 0; j < layoutLine.length; j++) {
        val layoutEntry = layoutLine[j];
        val slot = slots.get(layoutEntry);
        if (slot == null) {
          throw new IllegalArgumentException("undefined slot '" + layoutEntry + "' used in layout");
        }
        result[9*i+j] = slot;
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private IntObjectPair<TableSlotConfig>[] loadStorageSlots() {
    val result = new LinkedList<IntObjectPair<TableSlotConfig>>();
    for (int i = 0; i < allSlots.length; i++) {
      val slot = allSlots[i];
      if (slot.type.canPlace() || slot.type.canPickup()) {
        result.add(IntObjectPair.of(i, slot));
      }
    }
    return result.toArray(IntObjectPair[]::new);
  }

  private PreparedScript[] loadRequirements(ConfigurationSection configuration) {
    return toScript(configuration.getStringList("requirements"), "requirements");
  }

  @AllArgsConstructor(staticName = "of")
  @lombok.Builder
  public static class TableSlotConfig {
    @Getter
    private final @Nonnull TableSlotType type;
    @Getter
    private final @Nullable ItemStack material;

    public TableSlotConfig(String slotKeyString, ConfigurationSection configuration) {
      this.type = loadType(slotKeyString, configuration);
      this.material = type.hasDefault() ? loadMaterial(slotKeyString, configuration) : null;
    }

    private ItemStack loadMaterial(String slotKeyString, ConfigurationSection configuration) {
      val materialName = required(configuration.getString("material"), "slots." + slotKeyString + ".material");
      final Material material;
      try {
        material = Material.valueOf(materialName);
      } catch (IllegalArgumentException e) {
        throw new IllegalStateException("config key 'slots." + slotKeyString + ".material' value is not support.");
      }
      return new ItemStack(material);
    }

    private TableSlotType loadType(String slotKeyString, ConfigurationSection configuration) {
      val typeString = required(configuration.getString("type"), "slots." + slotKeyString + ".type");
      try {
        return TableSlotType.valueOf(typeString);
      } catch (IllegalArgumentException e) {
        throw new IllegalStateException("config key 'slots." + slotKeyString + ".type' value is not support. " +
          "support values: ICON, SPACE, PRODUCT");
      }
    }
  }

  public static class ActionsConfig {
    private final Map<String, List<String>> actionsMap;
    private final Map<String, PreparedScript[]> cacheMap;
    private final Function<String, PreparedScript[]> bootMethod;
    @Getter
    private final PreparedScript[] onPlace;
    @Getter
    private final PreparedScript[] onBreak;
    @Getter
    private final PreparedScript[] onOpenInventory;
    @Getter
    private final PreparedScript[] onCloseInventory;

    public ActionsConfig(ConfigurationSection configuration) {
      if (configuration == null) {
        this.actionsMap = Collections.emptyMap();
      } else {
        val actionKeys = configuration.getKeys(false);
        val resultMap = new HashMap<String, List<String>>(actionKeys.size());
        for (val actionKey : actionKeys) {
          resultMap.put(actionKey, configuration.getStringList(actionKey));
        }
        this.actionsMap = resultMap;
      }
      this.cacheMap = new ConcurrentSkipListMap<>();
      this.bootMethod = this::getImpl;

      this.onBreak = get("on-break");
      this.onPlace = get("on-place");
      this.onOpenInventory = get("on-open-inventory");
      this.onCloseInventory = get("on-close-inventory");
    }

    public PreparedScript[] get(String name){
      return cacheMap.computeIfAbsent(name, bootMethod);
    }

    private PreparedScript[] getImpl(Object name) {
      return toScript(actionsMap.getOrDefault(name, Collections.emptyList()), "actions."+name);
    }
  }

  private static PreparedScript[] toScript(List<String> scriptTextList, String path){
    val ankhScriptService = AnkhScriptService.instance();
    return scriptTextList.stream()
      .map(it->{
        try {
          return ankhScriptService.prepareShell(it);
        } catch (Exception e) {
          throw new IllegalArgumentException("Failed to parse script in " + path ,e);
        }
      })
      .toArray(PreparedScript[]::new);
  }
}
