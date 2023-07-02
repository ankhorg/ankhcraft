package bot.inker.ankh.craft.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum TableSlotType {
  ICON(true, false, false),
  SPACE(false, true, true),
  PRODUCT(false, false, true);

  @Getter
  private final boolean hasDefault;
  @Getter
  private final boolean canPlace;
  @Getter
  private final boolean canPickup;
}