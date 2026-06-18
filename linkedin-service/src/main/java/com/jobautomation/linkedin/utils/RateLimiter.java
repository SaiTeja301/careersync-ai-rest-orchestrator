package com.jobautomation.linkedin.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Human-like rate limiter for Selenium bots.
 * Replaces all raw Thread.sleep() calls with descriptive, randomised delays
 * to mimic human browsing patterns and avoid bot detection.
 */
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    // Delay ranges (milliseconds)
    private static final long MICRO_MIN_MS  =  300L;
    private static final long MICRO_MAX_MS  =  800L;
    private static final long HUMAN_MIN_MS  = 1500L;
    private static final long HUMAN_MAX_MS  = 4000L;
    private static final long LONG_MIN_MS   = 5000L;
    private static final long LONG_MAX_MS   = 12000L;
    private static final long LOGIN_MIN_MS  = 12000L;
    private static final long LOGIN_MAX_MS  = 18000L;

    private RateLimiter() {
        // Utility class — no instantiation
    }

    /**
     * Very short delay (300ms – 800ms).
     * Use between keystrokes or minor DOM interactions.
     */
    public static void microDelay() {
        sleep(MICRO_MIN_MS, MICRO_MAX_MS, "micro");
    }

    /**
     * Standard human-like delay (1.5s – 4s).
     * Use between page interactions and element clicks.
     */
    public static void humanDelay() {
        sleep(HUMAN_MIN_MS, HUMAN_MAX_MS, "human");
    }

    /**
     * Long post-navigation delay (5s – 12s).
     * Use after page navigations to allow full DOM rendering.
     */
    public static void longDelay() {
        sleep(LONG_MIN_MS, LONG_MAX_MS, "long");
    }

    /**
     * Extended post-login delay (12s – 18s).
     * Use after login submission to allow redirects and session setup.
     */
    public static void loginDelay() {
        sleep(LOGIN_MIN_MS, LOGIN_MAX_MS, "login");
    }

    // --- private helper ---

    private static void sleep(long minMs, long maxMs, String label) {
        long delay = ThreadLocalRandom.current().nextLong(minMs, maxMs + 1);
        log.debug("RateLimiter [{}] — sleeping {}ms", label, delay);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("RateLimiter sleep interrupted: {}", e.getMessage());
        }
    }
}
