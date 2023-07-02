package bot.inker.ankh.craft.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.key.Key;

@UtilityClass
public class KeyUtil {
  private static final Class<?> keyClass = loadKeyClass();

  @SneakyThrows
  private static Class<?> loadKeyClass(){
    return Class.forName("net.kyori.adventure.key.KeyImpl");
  }
  public static Key warpKey(Key key){
    if(key.getClass() == keyClass){
      return key;
    }else{
      return Key.key(key.namespace(), key.value());
    }
  }

}
