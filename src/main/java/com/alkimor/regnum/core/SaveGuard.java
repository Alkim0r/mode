package com.alkimor.regnum.core;

import com.alkimor.regnum.Regnum;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Страховка сохранений: при каждом запуске мира копирует данные Regnum (королевства, контракты, науку, династии и прочее)
 * в папку regnum_backups, оставляя три последние копии. Если новая версия мода что-то сломает, мир можно вернуть.
 */
public final class SaveGuard {
    private SaveGuard() {}

    public static final int KEEP = 3, DAYS = 7;
    /** Версия формата данных Regnum: растёт, когда структура сохранений меняется несовместимо. */
    public static final int FORMAT = 1;

    /** Читает записанную версию формата (0 — мир новый или без метки) и записывает текущую. Возвращает прежнюю. */
    public static int stamp(MinecraftServer server) throws IOException {
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("regnum_backups");
        Files.createDirectories(dir);
        Path f = dir.resolve("data_format.txt");
        int prev = 0;
        if (Files.isRegularFile(f)) {
            try { prev = Integer.parseInt(Files.readString(f).trim()); } catch (NumberFormatException ignored) {}
        }
        if (prev != FORMAT) Files.writeString(f, Integer.toString(FORMAT));
        return prev;
    }

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent event) {
        try {
            backup(event.getServer());
            int prev = stamp(event.getServer());
            if (prev > FORMAT) Regnum.LOGGER.error("[Regnum] Мир сохранён более новой версией мода (формат {} > {}). Данные скопированы в regnum_backups; возможны потери при откате версии.", prev, FORMAT);
        } catch (Exception e) {
            Regnum.LOGGER.warn("[Regnum] Резервная копия сохранений не удалась: {}", e.toString());
        }
    }

    /** Возвращает число скопированных файлов (0 — нечего копировать). */
    public static int backup(MinecraftServer server) throws IOException {
        Path root = server.getWorldPath(LevelResource.ROOT);
        Path data = root.resolve("data");
        if (!Files.isDirectory(data)) return 0;
        List<Path> files = new ArrayList<>();
        try (Stream<Path> s = Files.list(data)) {
            s.filter(f -> f.getFileName().toString().startsWith("regnum") && f.getFileName().toString().endsWith(".dat")).forEach(files::add);
        }
        if (files.isEmpty()) return 0;
        Path dir = root.resolve("regnum_backups").resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        Files.createDirectories(dir);
        for (Path f : files) Files.copy(f, dir.resolve(f.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        prune(dir.getParent());
        Regnum.LOGGER.info("[Regnum] Резервная копия данных: {} файлов в {}", files.size(), dir.getFileName());
        return files.size();
    }

    private static void prune(Path parent) throws IOException {
        List<Path> dirs = new ArrayList<>();
        try (Stream<Path> s = Files.list(parent)) {
            s.filter(Files::isDirectory).forEach(dirs::add);
        }
        dirs.sort(Comparator.comparing(p -> p.getFileName().toString()));
        // оставляем KEEP последних и по одной (самой свежей) копии за каждые из DAYS прошлых суток
        java.util.Set<Path> keep = new java.util.HashSet<>();
        for (int i = Math.max(0, dirs.size() - KEEP); i < dirs.size(); i++) keep.add(dirs.get(i));
        java.util.Map<String, Path> perDay = new java.util.LinkedHashMap<>();
        for (Path d : dirs) {
            String n = d.getFileName().toString();
            if (n.length() >= 8) perDay.put(n.substring(0, 8), d); // позднее перезаписывает раннее: остаётся самая свежая за день
        }
        List<String> days = new ArrayList<>(perDay.keySet());
        for (int i = Math.max(0, days.size() - DAYS); i < days.size(); i++) keep.add(perDay.get(days.get(i)));
        for (Path d : dirs) {
            if (keep.contains(d)) continue;
            try (Stream<Path> w = Files.walk(d)) {
                w.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }
}
