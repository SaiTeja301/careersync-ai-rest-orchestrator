package com.jobautomation.linkedin.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
// RateLimiter replaces raw Thread.sleep in scrollIntoView

public class Generic_Keywords {

    private WebDriver driver;

    public Generic_Keywords(WebDriver driver) {
        this.driver = driver;
    }

    public void singleClick(WebElement element) {
        element.click();
    }

    public void sendText(WebElement element, String data) {
        element.sendKeys(data);
    }

    public void javaScriptClick(WebDriver driver, WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", element);
    }

    public static void typeValue(WebDriver driver, WebElement element, String value) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].value='" + value + "';", element);
    }

    public static void clearTextValue(WebDriver driver, WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].value='';", element);
    }

    public String getTitle(WebDriver driver) {
        return driver.getTitle();
    }

    public void waitForElement(String locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(locator)));
        if (element.isDisplayed()) {
            System.out.println("===== WebDriver is visible======");
        } else {
            System.out.println("===== WebDriver is not visible======");
        }
    }

    public void scrollIntoView(WebElement element, WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView(true);", element);
        RateLimiter.microDelay(); // short, randomised pause — no raw Thread.sleep
    }

    public static String decryptValues(String input) {
        String decrypted = null;
        try {
            String key = "publicKeymerchinsight123";
            java.security.Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            decrypted = new String(cipher.doFinal(Base64.getDecoder().decode(input.getBytes())));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return decrypted;
    }

    // Additional methods based on text file can be added here
}
