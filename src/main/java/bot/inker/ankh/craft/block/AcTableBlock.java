package bot.inker.ankh.craft.block;

import bot.inker.ankh.craft.config.CraftTableConfig;
import bot.inker.ankh.craft.util.KeyUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.inksnow.ankh.core.api.block.AnkhBlock;
import org.inksnow.ankh.core.api.block.TickableBlock;
import org.inksnow.ankh.core.api.hologram.HologramContent;
import org.inksnow.ankh.core.api.hologram.HologramService;
import org.inksnow.ankh.core.api.hologram.HologramTask;
import org.inksnow.ankh.core.api.ioc.IocLazy;
import org.inksnow.ankh.core.api.script.PreparedScript;
import org.inksnow.ankh.core.api.script.ScriptContext;
import org.inksnow.ankh.core.api.util.DcLazy;
import org.inksnow.ankh.core.inventory.storage.AbstractChestMenu;
import org.inksnow.ankh.core.inventory.storage.event.StorageDropFromCursorEvent;
import org.inksnow.ankh.core.inventory.storage.event.StoragePickupEvent;
import org.inksnow.ankh.core.inventory.storage.event.StoragePlaceEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.Objects;

@Slf4j
public final class AcTableBlock implements AnkhBlock, TickableBlock {
  private static final DcLazy<HologramService> hologramService = IocLazy.of(HologramService.class);
  private final @Nonnull Factory factory;
  private final @Nonnull Key tableId;
  private final @Nonnull CraftTableConfig tableConfig;
  private final @Nullable ItemStack dropItem;
  private final @Nonnull AcTableMenu tableMenu;

  private @Nullable Location location;
  private @Nullable HologramTask hologramTask;
  private int tickId;

  public AcTableBlock(@Nonnull Factory factory, @Nonnull Key tableId, @Nonnull CraftTableConfig tableConfig, @Nullable ItemStack dropItem) {
    this.factory = factory;
    this.tableId = tableId;
    this.tableConfig = tableConfig;
    this.dropItem = dropItem;
    this.tableMenu = new AcTableMenu();
  }

  @Override
  public @Nonnull Key key() {
    return tableId;
  }

  @Override
  public void load(@Nonnull Location location) {
    this.location = location;
    location.getBlock().setType(Material.CRAFTING_TABLE);
    hologramTask = hologramService.get().builder()
      .location(location.clone().add(0.5, 1.5, 0.5))
      .content()
      .appendContent("ac: " + tableId)
      .build()
      .build();
  }

  @Override
  public void unload() {
    Objects.requireNonNull(hologramTask, "try to unload a block without load it");
    tableMenu.getInventory().close();
    hologramTask.delete();
  }

  @Override
  public void remove(boolean isDestroy) {
    if (location != null) {
      if (dropItem != null) {
        location.getWorld().dropItem(location, dropItem);
      }

      for (val storageSlot : tableConfig.storageSlots()) {
        val item = tableMenu.getInventory().getItem(storageSlot.keyInt());
        tableMenu.getInventory().setItem(storageSlot.keyInt(), null);
        if (item != null) {
          location.getWorld().dropItemNaturally(location, item);
        }
      }
    }
  }

  @Override
  public void onBlockBreak(@Nonnull BlockBreakEvent event) {
    val player = event.getPlayer();
    val context = ScriptContext.builder()
      .player(player)
      .with("event", event)
      .with("ankhBlock", this)
      .build();
    for (val onBreakAction : tableConfig.actions().onBreak()) {
      try {
        onBreakAction.execute(context);
      } catch (Exception e) {
        logger.error("Failed to execute on-break action", e);
      }
    }
  }

  @Override
  public void onPlayerInteract(@Nonnull PlayerInteractEvent event) {
    if (event.getHand() == EquipmentSlot.OFF_HAND
      || event.useInteractedBlock() != Event.Result.ALLOW
      && event.useInteractedBlock() != Event.Result.DEFAULT
      || event.getPlayer().isSneaking()
      || event.getAction() != Action.RIGHT_CLICK_BLOCK
    ) {
      return;
    }
    event.setUseInteractedBlock(Event.Result.DENY);
    tableMenu.openForPlayer(event.getPlayer());
  }

  @Override
  public void runTick() {
    updateHologram();
  }

  private void updateHologram() {
    val hologramTask = this.hologramTask;
    if (hologramTask == null) {
      return;
    }
    val builder = HologramContent.builder();
    builder.appendContent("Hello, tick=" + (tickId++));
    hologramTask.updateContent(builder.build());
  }

  private void fillSlots(@Nonnull ItemStack[] storageSlot) {
    if (storageSlot.length != tableConfig.storageSlots().length) {
      logger.warn("ankh-craft {} slot size is not same in world-storage({}) and config({})",
        tableId,
        storageSlot.length,
        tableConfig.storageSlots().length);
    }
    for (int i = 0; i < tableConfig.storageSlots().length; i++) {
      val slot = tableConfig.storageSlots()[i];
      tableMenu.getInventory().setItem(slot.keyInt(), storageSlot[i]);
    }
  }

  private @Nonnull ItemStack[] encodeSlots() {
    val storageSlots = new ItemStack[tableConfig.storageSlots().length];
    for (int i = 0; i < tableConfig.storageSlots().length; i++) {
      val slot = tableConfig.storageSlots()[i];
      storageSlots[i] = tableMenu.getInventory().getItem(slot.keyInt());
    }
    return storageSlots;
  }

  @Override
  public byte[] save() {
    val bout = new ByteArrayOutputStream();
    try (val out = new DataOutputStream(bout)) {
      writeItem(out, dropItem);
      val slots = encodeSlots();
      out.writeInt(slots.length);
      for (ItemStack slot : slots) {
        writeItem(out, slot);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return bout.toByteArray();
  }

  private static void writeItem(@Nonnull DataOutputStream out, @Nullable ItemStack itemStack) throws IOException {
    if(itemStack == null){
      out.writeInt(0);
      return;
    }
    val bytes = itemStack.serializeAsBytes();
    out.writeInt(bytes.length);
    out.write(bytes);
  }

  private static @Nullable ItemStack readItem(@Nonnull DataInputStream in) throws IOException {
    val bytesSize = in.readInt();
    if(bytesSize == 0){
      return null;
    }
    val bytes = new byte[bytesSize];
    in.readFully(bytes);
    return ItemStack.deserializeBytes(bytes);
  }

  public static class Factory implements AnkhBlock.Factory {
    private final Key tableId;
    private final CraftTableConfig tableConfig;

    public Factory(Key tableId, CraftTableConfig tableConfig) {
      this.tableId = KeyUtil.warpKey(tableId);
      this.tableConfig = tableConfig;
    }

    @Override
    public @Nonnull AnkhBlock load(@Nonnull Key id, @Nonnull byte[] data) {
      if (!KeyUtil.warpKey(id).equals(tableId)) {
        throw new IllegalArgumentException("Invalid id for " + tableId + " " + id);
      }
      try (val in = new DataInputStream(new ByteArrayInputStream(data))) {
        val dropItem = readItem(in);
        val storageSlotCount = in.readInt();
        val storageSlot = new ItemStack[storageSlotCount];
        for (int i = 0; i < storageSlotCount; i++) {
          storageSlot[i] = readItem(in);
        }
        val acTableBlock = new AcTableBlock(this, tableId, tableConfig, dropItem);
        acTableBlock.fillSlots(storageSlot);
        return acTableBlock;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public @Nonnull Key key() {
      return tableId;
    }

    public @Nonnull AnkhBlock create(@Nullable ItemStack dropItem) {
      return new AcTableBlock(this, tableId, tableConfig, dropItem);
    }
  }

  private class AcTableMenu extends AbstractChestMenu {
    @Override
    protected Inventory createInventory() {
      val inventory = Bukkit.createInventory(this, 9 * tableConfig.layout().length, tableConfig.titleComponent());
      for (int i = 0; i < tableConfig.layout().length; i++) {
        val layoutLine = tableConfig.layout()[i];
        for (int j = 0; j < layoutLine.length; j++) {
          val layoutEntry = layoutLine[j];
          val slot = tableConfig.slots().get(layoutEntry);
          if (slot.type().hasDefault()) {
            inventory.setItem(9 * i + j, slot.material());
          }
        }
      }
      return inventory;
    }

    // @Override
    protected void acceptMenuOpen(Player player, InventoryView view) {
      runActions(player, tableConfig.actions().onOpenInventory());
    }

    @Override
    public void acceptCloseEvent(@Nonnull InventoryCloseEvent event) {
      runActions((Player) event.getPlayer(), tableConfig.actions().onCloseInventory());
    }

    private void runActions(Player player, PreparedScript[] actions) {
      if (actions.length == 0) {
        return;
      }
      val context = ScriptContext.builder()
        .player(player)
        .with("inventory", getInventory())
        .with("ankhBlock", AcTableBlock.this)
        .with("tableMenu", this)
        .build();

      for (val action : actions) {
        try {
          action.execute(context);
        } catch (Exception e) {
          logger.error("Failed to execute action", e);
        }
      }
    }

    @Override
    protected void canPlace(@Nonnull StoragePlaceEvent event, @Nonnull Cancellable cancelToken) {
      val inventory = getInventory();
      val rawSlot = event.slot();
      val isClickChest = rawSlot < inventory.getSize();
      if(!isClickChest){
        return;
      }
      val slot = tableConfig.allSlots()[rawSlot];
      if(slot == null){
        cancelToken.setCancelled(true);
        return;
      }
      if (!slot.type().canPlace()) {
        cancelToken.setCancelled(true);
      }
    }

    @Override
    protected void canPickup(@Nonnull StoragePickupEvent event, @Nonnull Cancellable cancelToken) {
      val inventory = getInventory();
      val rawSlot = event.slot();
      val isClickChest = rawSlot < inventory.getSize();
      if(!isClickChest){
        return;
      }
      val slot = tableConfig.allSlots()[rawSlot];
      if(slot == null){
        cancelToken.setCancelled(true);
        return;
      }
      if (!slot.type().canPickup()) {
        cancelToken.setCancelled(true);
      }
    }

    @Override
    protected void canDropFromCursor(@Nonnull StorageDropFromCursorEvent event, @Nonnull Cancellable cancelToken) {
      //
    }
  }
}
