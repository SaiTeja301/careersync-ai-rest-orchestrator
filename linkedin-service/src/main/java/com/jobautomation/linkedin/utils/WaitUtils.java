package com.jobautomation.linkedin.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Centralized WebDriver wait utility.
 * Replaces all Thread.sleep() calls and provides stale-element retry.
 */
public class WaitUtils {

    private static final Logger log = LoggerFactory.getLogger(WaitUtils.class);

    private WaitUtils() {
        // Utility class — no instantiation
    }

    /**
     * Wait until an element is visible on the page.
     *
     * @param driver  the WebDriver instance
     * @param locator the By locator to look for
     * @param seconds maximum time to wait in seconds
     * @return the visible WebElement, or null if timeout
     */
    public static WebElement waitForVisibility(WebDriver driver, By locator, int seconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (Exception e) {
            log.warn("Element not visible after {}s: {} — {}", seconds, locator, e.getMessage());
            return null;
        }
    }

    /**
     * Wait until an element is clickable.
     *
     * @param driver  the WebDriver instance
     * @param locator the By locator
     * @param seconds maximum time to wait
     * @return the clickable WebElement, or null if timeout
     */
    public static WebElement waitForClickable(WebDriver driver, By locator, int seconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
            return wait.until(ExpectedConditions.elementToBeClickable(locator));
        } catch (Exception e) {
            log.warn("Element not clickable after {}s: {} — {}", seconds, locator, e.getMessage());
            return null;
        }
    }

    /**
     * Wait until at least one element matching the locator is present in the DOM.
     *
     * @param driver  the WebDriver instance
     * @param locator the By locator
     * @param seconds maximum time to wait
     * @return list of matching elements (may be empty on timeout)
     */
    public static List<WebElement> waitForPresenceOfAll(WebDriver driver, By locator, int seconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
            return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
        } catch (Exception e) {
            log.warn("Elements not present after {}s: {} — {}", seconds, locator, e.getMessage());
            return List.of();
        }
    }

    /**
     * Wait until the page's document.readyState becomes "complete".
     *
     * @param driver  the WebDriver instance
     * @param seconds maximum time to wait
     */
    public static void waitForPageLoad(WebDriver driver, int seconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
            wait.until(d -> ((org.openqa.selenium.JavascriptExecutor) d)
                    .executeScript("return document.readyState")
                    .equals("complete"));
        } catch (Exception e) {
            log.warn("Page did not fully load within {}s — {}", seconds, e.getMessage());
        }
    }

    /**
     * Retry a Selenium action that may throw StaleElementReferenceException.
     * Useful when the DOM re-renders after a navigation or AJAX update.
     *
     * @param action     the action to retry (a Supplier returning any type T)
     * @param maxRetries number of retry attempts
     * @param <T>        return type of the action
     * @return the result of the successful action, or null after all retries fail
     */
    public static <T> T retryOnStaleElement(Supplier<T> action, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return action.get();
            } catch (StaleElementReferenceException e) {
                log.warn("StaleElementReferenceException on attempt {}/{} — retrying...", attempt, maxRetries);
                RateLimiter.microDelay();
            } catch (Exception e) {
                log.error("Non-stale exception on attempt {}/{}: {}", attempt, maxRetries, e.getMessage());
                return null;
            }
        }
        log.error("All {} retry attempts failed due to stale element.", maxRetries);
        return null;
    }
}
