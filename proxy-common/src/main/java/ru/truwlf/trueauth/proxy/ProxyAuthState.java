package ru.truwlf.trueauth.proxy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProxyAuthState {
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

    public UUID open(UUID player) {
        UUID session = UUID.randomUUID();
        entries.put(player, new Entry(session));
        return session;
    }

    public UUID session(UUID player) {
        Entry entry = entries.get(player);
        return entry == null ? null : entry.session;
    }

    public boolean current(UUID player, UUID session) {
        Entry entry = entries.get(player);
        return entry != null && entry.session.equals(session);
    }

    public boolean authenticated(UUID player) {
        Entry entry = entries.get(player);
        return entry != null && entry.authenticated;
    }

    public boolean authenticated(UUID player, UUID session) {
        Entry entry = entries.get(player);
        return entry != null && entry.session.equals(session) && entry.authenticated;
    }

    public boolean beginAttempt(UUID player, UUID session, long now, long cooldownMillis, int maxAttempts) {
        return beginAttempt(player, session, now, cooldownMillis, maxAttempts, false);
    }

    public boolean beginAttempt(UUID player, UUID session, long now, long cooldownMillis, int maxAttempts, boolean allowAuthenticated) {
        Entry entry = entries.get(player);
        if (entry == null || !entry.session.equals(session) || (!allowAuthenticated && entry.authenticated) || entry.inFlight) return false;
        synchronized (entry) {
            if ((!allowAuthenticated && entry.authenticated) || entry.inFlight || entry.attempts >= maxAttempts || now - entry.lastAttempt < cooldownMillis) return false;
            entry.lastAttempt = now;
            entry.inFlight = true;
            entry.attempts++;
            return true;
        }
    }

    public void finishAttempt(UUID player, UUID session) {
        Entry entry = entries.get(player);
        if (entry != null && entry.session.equals(session)) {
            synchronized (entry) { entry.inFlight = false; }
        }
    }

    public boolean authenticate(UUID player, UUID session) {
        Entry entry = entries.get(player);
        if (entry == null || !entry.session.equals(session)) return false;
        synchronized (entry) {
            if (!entry.session.equals(session)) return false;
            entry.authenticated = true;
            entry.inFlight = false;
            return true;
        }
    }

    public void remove(UUID player) { entries.remove(player); }

    private static final class Entry {
        private final UUID session;
        private long lastAttempt;
        private int attempts;
        private boolean inFlight;
        private boolean authenticated;
        private Entry(UUID session) { this.session = session; }
    }
}
