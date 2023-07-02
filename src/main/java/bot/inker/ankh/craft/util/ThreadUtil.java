package bot.inker.ankh.craft.util;

import bot.inker.ankh.craft.api.AnkhCraft;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.bukkit.Bukkit;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

@UtilityClass
public class ThreadUtil {
  public static <R> CompletableFuture<R> runInMainThread(Callable<R> callable) {
    if (Bukkit.isPrimaryThread()) {
      try {
        return CompletableFuture.completedFuture(callable.call());
      } catch (Exception e) {
        return CompletableFuture.failedFuture(e);
      }
    } else {
      val future = new CompletableFuture<R>();
      Bukkit.getScheduler().runTask(AnkhCraft.plugin(), () -> {
        try {
          future.complete(callable.call());
        } catch (Exception e) {
          future.completeExceptionally(e);
        }
      });
      return future;
    }
  }

  public static CompletableFuture<Void> runInMainThread(Runnable callable) {
    return runInMainThread(() -> {
      callable.run();
      return null;
    });
  }
}
