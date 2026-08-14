package ru.truwlf.trueauth;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class PlatformScheduler {
    private final TrueAuthPlugin plugin;
    private final boolean folia;

    PlatformScheduler(TrueAuthPlugin plugin) {
        this.plugin = plugin;
        folia = hasClass("io.papermc.paper.threadedregions.RegionizedServer");
    }

    boolean isFolia() { return folia; }

    void runGlobal(Runnable task) {
        if (!folia) { plugin.getServer().getScheduler().runTask(plugin, task); return; }
        invoke(serverMethod("getGlobalRegionScheduler"), plugin.getServer(), "run", task);
    }

    void runAsync(Runnable task) {
        if (!folia) { plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task); return; }
        invoke(serverMethod("getAsyncScheduler"), plugin.getServer(), "runNow", task);
    }

    TaskHandle runLater(Player player, Runnable task, long delayTicks) {
        if (!folia) return new BukkitHandle(plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks));
        Object scheduler = invokeNoArgs(player, "getScheduler");
        return new ReflectiveHandle(invokeScheduler(scheduler, "runDelayed", task, null, delayTicks));
    }

    void run(Player player, Runnable task) {
        if (!folia) { plugin.getServer().getScheduler().runTask(plugin, task); return; }
        Object scheduler = invokeNoArgs(player, "getScheduler");
        invokeScheduler(scheduler, "run", task, (Object) null);
    }

    void runTimer(Runnable task, long delayTicks, long periodTicks) {
        if (!folia) { plugin.getServer().getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks); return; }
        invoke(serverMethod("getGlobalRegionScheduler"), plugin.getServer(), "runAtFixedRate", task, delayTicks, periodTicks);
    }

    interface TaskHandle { void cancel(); }
    private record BukkitHandle(BukkitTask task) implements TaskHandle { public void cancel() { task.cancel(); } }
    private record ReflectiveHandle(Object task) implements TaskHandle {
        public void cancel() {
            try { task.getClass().getMethod("cancel").invoke(task); }
            catch (ReflectiveOperationException exception) { throw new IllegalStateException("Could not cancel Folia task", exception); }
        }
    }

    private Object invokeScheduler(Object scheduler, String name, Runnable task, Object... extra) {
        Method method = findSchedulerMethod(scheduler.getClass(), name, extra.length);
        Class<?> consumerType = method.getParameterTypes()[1];
        Object consumer = Proxy.newProxyInstance(consumerType.getClassLoader(), new Class<?>[]{consumerType}, handler(task));
        Object[] arguments = new Object[2 + extra.length];
        arguments[0] = plugin;
        arguments[1] = consumer;
        System.arraycopy(extra, 0, arguments, 2, extra.length);
        try { return method.invoke(scheduler, arguments); }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException("Could not invoke Folia scheduler", exception); }
    }

    private Object invoke(Method accessor, Object target, String schedulerMethod, Runnable task, Object... extra) {
        try { return invokeScheduler(accessor.invoke(target), schedulerMethod, task, extra); }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException("Could not access Folia scheduler", exception); }
    }

    private static InvocationHandler handler(Runnable task) {
        return (proxy, method, args) -> {
            if (method.getName().equals("accept") && method.getParameterCount() == 1) task.run();
            return null;
        };
    }

    private Method serverMethod(String name) {
        for (Method method : plugin.getServer().getClass().getMethods()) if (method.getName().equals(name) && method.getParameterCount() == 0) return method;
        throw new IllegalStateException("Folia method is not available: " + name);
    }

    private static Method findSchedulerMethod(Class<?> type, String name, int extraCount) {
        for (Method method : type.getMethods()) if (method.getName().equals(name) && method.getParameterCount() == extraCount + 2) return method;
        throw new IllegalStateException("Folia scheduler method is not available: " + name);
    }

    private static Object invokeNoArgs(Object target, String name) {
        try { return target.getClass().getMethod(name).invoke(target); }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException("Folia entity scheduler is not available", exception); }
    }

    private static boolean hasClass(String name) {
        try { Class.forName(name); return true; }
        catch (ClassNotFoundException ignored) { return false; }
    }
}
