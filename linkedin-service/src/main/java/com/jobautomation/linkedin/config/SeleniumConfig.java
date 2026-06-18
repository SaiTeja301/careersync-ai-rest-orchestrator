package com.jobautomation.linkedin.config;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Selenium WebDriver configuration for LinkedIn scraping.
 * The WebDriver bean is lazily initialized so Chrome only opens when first needed.
 */
@Configuration
public class SeleniumConfig {

    @Value("${selenium.headless:false}")
    private boolean headless;

    @Bean
    @Lazy
    public WebDriver webDriver() {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        } else {
            options.addArguments("--start-maximized");
        }
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        return new ChromeDriver(options);
    }
}
