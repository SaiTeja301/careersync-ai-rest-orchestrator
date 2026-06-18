package com.jobautomation.linkedin.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures screenshots on test or automation failure.
 * Automatically saves files to ./screenshots/ with a timestamped name.
 */
public class ScreenshotUtils {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotUtils.class);
    private static final String SCREENSHOT_DIR = "./screenshots/";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private ScreenshotUtils() {
        // Utility class — no instantiation
    }

    /**
     * Captures a screenshot and saves it to ./screenshots/<label>_<timestamp>.png.
     * Call this inside catch blocks to capture the DOM state at the moment of failure.
     *
     * @param driver the active WebDriver instance
     * @param label  a short descriptive label for the screenshot filename
     */
    public static void takeScreenshot(WebDriver driver, String label) {
        if (driver == null) {
            log.warn("Cannot take screenshot — WebDriver is null.");
            return;
        }
        try {
            // Ensure the screenshots directory exists
            Path screenshotDir = Paths.get(SCREENSHOT_DIR);
            if (!Files.exists(screenshotDir)) {
                Files.createDirectories(screenshotDir);
            }

            // Build a timestamped filename
            String timestamp = LocalDateTime.now().format(FORMATTER);
            String safeLabel = label.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            String fileName = SCREENSHOT_DIR + safeLabel + "_" + timestamp + ".png";

            // Take screenshot
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(srcFile.toPath(), Paths.get(fileName));

            log.info("Screenshot saved: {}", fileName);
        } catch (IOException e) {
            log.error("Failed to save screenshot for label '{}': {}", label, e.getMessage());
        } catch (ClassCastException e) {
            log.error("WebDriver does not support screenshots: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error taking screenshot for label '{}': {}", label, e.getMessage());
        }
    }
}
