package me.uc_hussein.ultraschat.logging;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.config.Cfg;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** Daily log files in plugins/ULTRAS_Chat/logs/. Never records message contents. */
public final class ChatLog {
    public enum Category { RELOAD, SETTINGS, LANGUAGE, DEATH, JOIN_QUIT, PM, MENTION, ERROR, CONFIG }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ULTRASChatPlugin plugin;
    private final File dir;
    private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ULTRAS-Chat-Log");
        t.setDaemon(true);
        return t;
    });

    public ChatLog(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "logs");
        this.dir.mkdirs();
    }

    private boolean enabled(Category c) {
        Cfg cfg = plugin.cfg();
        if (cfg == null) {
            return true;
        }
        return cfg.bool("logging.enabled", true)
                && cfg.bool("logging." + c.name().toLowerCase(Locale.ROOT).replace('_', '-'), true);
    }

    public void log(Category c, String msg) {
        if (!enabled(c)) {
            return;
        }
        LocalDate d = LocalDate.now();
        String line = "[" + LocalTime.now().format(TIME) + "] [" + c + "] " + msg + System.lineSeparator();
        try {
            exec.execute(() -> append(d, line));
        } catch (RejectedExecutionException ignored) {
            // shutting down
        }
    }

    public void error(String msg, Throwable t) {
        plugin.getLogger().log(Level.SEVERE, msg, t);
        log(Category.ERROR, msg + (t == null ? "" : ": " + t));
    }

    private void append(LocalDate d, String line) {
        try {
            File f = new File(dir, "ultras-chat-" + d.format(DAY) + ".log");
            Files.writeString(f.toPath(), line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not write log file: " + ex.getMessage());
        }
    }

    /** Deletes log files older than logging.keep-days. */
    public void prune() {
        Cfg cfg = plugin.cfg();
        int keep = cfg == null ? 14 : Math.max(1, cfg.integer("logging.keep-days"));
        long cutoff = System.currentTimeMillis() - keep * 86_400_000L;
        File[] files = dir.listFiles((d, n) -> n.startsWith("ultras-chat-") && n.endsWith(".log"));
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.lastModified() < cutoff && !f.delete()) {
                plugin.getLogger().warning("Could not delete old log " + f.getName());
            }
        }
    }

    public void close() {
        exec.shutdown();
        try {
            exec.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
