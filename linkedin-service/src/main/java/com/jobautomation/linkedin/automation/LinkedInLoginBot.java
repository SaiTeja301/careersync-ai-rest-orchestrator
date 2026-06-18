package com.jobautomation.linkedin.automation;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.jobautomation.common.constants.JobConstants;
import com.jobautomation.common.dto.JobDto;
import com.jobautomation.linkedin.utils.RateLimiter;
import com.jobautomation.linkedin.utils.ScreenshotUtils;
import com.jobautomation.linkedin.utils.WaitUtils;

/**
 * LinkedIn job scraping bot.
 *
 * Responsibilities:
 *   1. Login to LinkedIn with email/password
 *   2. Navigate to jobs search results or recommended jobs
 *   3. Collect job listing URLs across pagination pages
 *   4. Scrape job details and map them to {@link JobDto}
 *
 * Design principles:
 *   - Reuses Spring-injected {@code @Lazy WebDriver} bean — never creates its own ChromeDriver
 *   - Uses {@link WaitUtils} for condition-based waiting
 *   - Uses {@link RateLimiter} for randomized human-like delays to bypass anti-bot detection
 *   - Captures screenshots on failure using {@link ScreenshotUtils}
 *   - Logs events via SLF4J instead of standard output
 */
@Component
public class LinkedInLoginBot {

    private static final Logger log = LoggerFactory.getLogger(LinkedInLoginBot.class);

    private final WebDriver driver;

    /**
     * Constructor injection — Spring provides the shared {@code @Lazy WebDriver} bean.
     */
    public LinkedInLoginBot(@org.springframework.context.annotation.Lazy WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Logs in to LinkedIn using the provided credentials.
     * Navigates to the login page, enters email and password, clicks Sign in,
     * and waits for the session to be established.
     *
     * @param email    LinkedIn registered email
     * @param password LinkedIn account password
     */
    public void linkedinLogin(String email, String password) {
        log.info("Navigating to LinkedIn login page...");
        try {
            driver.get(JobConstants.JOB_PORTAL_LINKEDIN.getConstantValue());
            WaitUtils.waitForPageLoad(driver, 20);
            RateLimiter.longDelay();

            // Enter email/username
            By usernameLocator = By.xpath("//input[@id='username']");
            WebElement usernameField = WaitUtils.waitForVisibility(driver, usernameLocator, 15);
            if (usernameField != null) {
                usernameField.clear();
                usernameField.sendKeys(email);
                log.info("Username entered.");
                RateLimiter.humanDelay();
            } else {
                log.warn("Username field not found — LinkedIn login page may not have loaded.");
                ScreenshotUtils.takeScreenshot(driver, "linkedin_login_username_not_found");
            }

            // Enter password
            By passwordLocator = By.xpath("//input[@id='password']");
            WebElement passwordField = WaitUtils.waitForVisibility(driver, passwordLocator, 10);
            if (passwordField != null) {
                passwordField.clear();
                passwordField.sendKeys(password);
                log.info("Password entered.");
                RateLimiter.humanDelay();
            } else {
                log.warn("Password field not found on LinkedIn login page.");
                ScreenshotUtils.takeScreenshot(driver, "linkedin_login_password_not_found");
            }

            // Click Sign in button
            By signInBtnLocator = By.xpath("//button[@aria-label='Sign in']");
            WebElement signInBtn = WaitUtils.waitForClickable(driver, signInBtnLocator, 10);
            if (signInBtn != null) {
                signInBtn.click();
                log.info("Sign in button clicked — waiting for session to establish...");
                RateLimiter.loginDelay();
                WaitUtils.waitForPageLoad(driver, 20);
                log.info("LinkedIn login step complete. Current URL: {}", driver.getCurrentUrl());
            } else {
                log.error("Sign in button not found — cannot proceed.");
                ScreenshotUtils.takeScreenshot(driver, "linkedin_login_btn_not_found");
            }

        } catch (Exception e) {
            log.error("LinkedIn login failed: {}", e.getMessage(), e);
            ScreenshotUtils.takeScreenshot(driver, "linkedin_login_error");
        }
    }

    /**
     * Scrapes recommended and easy-apply jobs for the logged-in user.
     *
     * @return list of scraped {@link JobDto} objects
     */
    public List<JobDto> scrapeJobs() {
        log.info("Starting LinkedIn recommended jobs scraping...");
        List<JobDto> scrapedJobs = new ArrayList<>();
        try {
            // Clicking on jobs in main page
            By jobsSpanLocator = By.xpath("//span[@title='Jobs']");
            WebElement jobsSpan = WaitUtils.waitForClickable(driver, jobsSpanLocator, 10);
            if (jobsSpan != null) {
                jobsSpan.click();
                log.info("Clicked Jobs tab on main page.");
                RateLimiter.longDelay();
            } else {
                log.info("Jobs tab not clickable/found. Navigating directly to jobs URL: {}", JobConstants.JOBS_LINKEDURL.getConstantValue());
                driver.navigate().to(JobConstants.JOBS_LINKEDURL.getConstantValue());
                WaitUtils.waitForPageLoad(driver, 20);
                RateLimiter.longDelay();
            }

            String pageSource = driver.getPageSource();
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(pageSource);
            org.jsoup.select.Elements jobCountElement = doc
                    .selectXpath("//span[contains(., 'Show all')]/parent::span/parent::a");

            java.util.List<String> easyApplyjobLinks = jobCountElement.stream()
                    .map(ele -> ele.attr("href"))
                    .collect(java.util.stream.Collectors.toList());

            log.info("Easy Apply Job Links: {} Count: {}", easyApplyjobLinks, easyApplyjobLinks.size());

            java.util.List<String> jobUrls = new java.util.ArrayList<>();

            // implemented the pagination location and storing all links in this list
            java.util.List<String> derivedJobLinks = easyApplyjobLinks.stream().filter(link -> link.contains(
                    "https://www.linkedin.com/jobs/collections/recommended/?discover=recommended&discoveryOrigin=JOBS_HOME_JYMBII")
                    || link.contains(
                            "https://www.linkedin.com/jobs/collections/easy-apply/?discover=true&discoveryOrigin=JOBS_HOME_EXPANDED_JOB_COLLECTIONS&subscriptionOrigin=JOBS_HOME"))
                    .flatMap(link -> {
                        if (link.contains(
                                "https://www.linkedin.com/jobs/collections/recommended/?discover=recommended&discoveryOrigin=JOBS_HOME_JYMBII")) {
                            return java.util.stream.IntStream.rangeClosed(0, 5).mapToObj(
                                     i -> "https://www.linkedin.com/jobs/collections/recommended/?discover=recommended&discoveryOrigin=JOBS_HOME_JYMBII&start="
                                             + (i * 24));
                        } else {
                            return java.util.stream.IntStream.rangeClosed(0, 5).mapToObj(
                                    i -> "https://www.linkedin.com/jobs/collections/easy-apply/?discover=true&discoveryOrigin=JOBS_HOME_EXPANDED_JOB_COLLECTIONS&start="
                                             + (i * 25) + "&subscriptionOrigin=JOBS_HOME");
                        }
                    }).collect(java.util.stream.Collectors.toList());

            easyApplyjobLinks.addAll(derivedJobLinks);

            // getting or Scraping all job details
            easyApplyjobLinks.stream()
                    .distinct()
                    .forEach(link -> {
                        try {
                            log.info("Navigating to collection link: {}", link);
                            driver.navigate().to(link);
                            WaitUtils.waitForPageLoad(driver, 20);
                            RateLimiter.longDelay();
                            
                            String jobPageSource = driver.getPageSource();
                            org.jsoup.nodes.Document jobDoc = org.jsoup.Jsoup.parse(jobPageSource);
                            RateLimiter.humanDelay();
                            
                            org.jsoup.select.Elements Jobprofiles = jobDoc.select(".ember-view");
                            java.util.List<String> extractedJobLinks = Jobprofiles.stream()
                                    .map(joblink -> joblink.select("a").attr("href"))
                                    .filter(joblink -> joblink.contains("/jobs/view/"))
                                    .map(joblink -> "https://www.linkedin.com" + joblink)
                                    .distinct()
                                    .collect(java.util.stream.Collectors.toList());

                            jobUrls.addAll(extractedJobLinks);
                        } catch (Exception e) {
                            log.error("Failed to process link: {} due to: {}", link, e.getMessage());
                            ScreenshotUtils.takeScreenshot(driver, "linkedin_collection_process_error");
                        }
                    });

            scrapedJobs = scrapeLinkedInJobDetails(jobUrls);

        } catch (Exception e) {
            log.error("Failed during scrapeJobs: {}", e.getMessage(), e);
            ScreenshotUtils.takeScreenshot(driver, "linkedin_scrape_jobs_error");
        }
        return scrapedJobs;
    }

    /**
     * Scrapes LinkedIn jobs by search title and posted time filter (in hours).
     *
     * @param title job search title
     * @param hours only scrape jobs posted within this many hours
     * @return list of scraped {@link JobDto} objects
     */
    public List<JobDto> scrapeLatestJobsByTitleandTime(String title, Integer hours) {
        log.info("Starting LinkedIn scrapeLatestJobsByTitleandTime for title: {} and hours: {}", title, hours);
        List<JobDto> scrapedJobs = new ArrayList<>();
        List<String> jobUrls = new ArrayList<>();
        try {
            long hoursInSeconds = (hours * 60) * 60;
            String url = "https://www.linkedin.com/jobs/search-results/?f_TPR=r" + hoursInSeconds + "&keywords="
                    + title + "%20Developer&origin=JOB_COLLECTION_PAGE_SEARCH_BUTTON";
            log.info("Navigating to URL: {}", url);
            driver.navigate().to(url);
            WaitUtils.waitForPageLoad(driver, 20);
            RateLimiter.longDelay();

            By pageButtonsLocator = By.cssSelector(".jobs-search-pagination__pages li button span");
            List<WebElement> pageButtons = driver.findElements(pageButtonsLocator);

            if (!pageButtons.isEmpty()) {
                List<String> paginationJobLinks = new ArrayList<>();
                OptionalInt maxPage = pageButtons.stream()
                        .map(WebElement::getText)
                        .map(String::trim)
                        .filter(text -> !text.isEmpty())
                        .filter(text -> text.matches("\\d+"))
                        .mapToInt(Integer::parseInt)
                        .max();
                if (!maxPage.isEmpty()) {
                    int maxPageInt = maxPage.getAsInt();
                    log.info("Found {} pagination pages.", maxPageInt);
                    for (int i = 0; i < maxPageInt; i++) {
                        String jobPageUrl = url + "&start=" + (i * 25);
                        paginationJobLinks.add(jobPageUrl);
                    }
                    paginationJobLinks.stream()
                            .distinct()
                            .forEach(jobPageLink -> {
                                try {
                                    log.info("Navigating to pagination link: {}", jobPageLink);
                                    driver.navigate().to(jobPageLink);
                                    WaitUtils.waitForPageLoad(driver, 20);
                                    RateLimiter.longDelay();
                                    
                                    String jobDetailPageSource = driver.getPageSource();
                                    Document jobDetailDoc = Jsoup.parse(jobDetailPageSource);
                                    Elements jobElements = jobDetailDoc
                                            .select(".ember-view .job-card-job-posting-card-wrapper a");
                                    List<String> jobLinks = jobElements.stream()
                                            .map(ele -> ele.attr("href"))
                                            .filter(link -> link.contains(
                                                    "https://www.linkedin.com/jobs/search-results/?currentJobId"))
                                            .map(link -> StringUtils.substringBetween(link,
                                                    "https://www.linkedin.com/jobs/search-results/?currentJobId=",
                                                    "&keywords"))
                                            .map(link -> "https://www.linkedin.com/jobs/view/" + link + "/")
                                            .distinct()
                                            .collect(java.util.stream.Collectors.toList());
                                    jobUrls.addAll(jobLinks);
                                } catch (Exception e) {
                                    log.error("Failed to process link: {} due to: {}", jobPageLink, e.getMessage());
                                    ScreenshotUtils.takeScreenshot(driver, "linkedin_pagination_process_error");
                                }
                            });
                    scrapedJobs = scrapeLinkedInJobDetails(jobUrls);
                }
            } else {
                log.info("No pagination pages found. Processing single page search results.");
                driver.navigate().to(url);
                WaitUtils.waitForPageLoad(driver, 20);
                RateLimiter.longDelay();
                
                String jobDetailPageSource = driver.getPageSource();
                Document jobDetailDoc = Jsoup.parse(jobDetailPageSource);
                Elements jobElements = jobDetailDoc.select(".ember-view .job-card-job-posting-card-wrapper a");
                List<String> jobLinks = jobElements.stream()
                        .map(ele -> ele.attr("href"))
                        .filter(link -> link.contains("https://www.linkedin.com/jobs/search-results/?currentJobId"))
                        .map(link -> StringUtils.substringBetween(link,
                                "https://www.linkedin.com/jobs/search-results/?currentJobId=", "&keywords"))
                        .map(link -> "https://www.linkedin.com/jobs/view/" + link + "/")
                        .distinct()
                        .collect(java.util.stream.Collectors.toList());
                jobUrls.addAll(jobLinks);

                scrapedJobs = scrapeLinkedInJobDetails(jobUrls);
            }
        } catch (Exception e) {
            log.error("Failed during scrapeLatestJobsByTitleandTime: {}", e.getMessage(), e);
            ScreenshotUtils.takeScreenshot(driver, "linkedin_scrape_latest_error");
        }
        return scrapedJobs;
    }

    /**
     * Navigates to individual job detail pages and extracts job metadata into a list of {@link JobDto}.
     *
     * @param jobUrls list of LinkedIn job view URLs
     * @return list of populated {@link JobDto} objects
     */
    public List<JobDto> scrapeLinkedInJobDetails(List<String> jobUrls) {
        log.info("Scraping details for {} unique job URLs...", jobUrls.stream().distinct().count());
        List<JobDto> scrapedJobs = new ArrayList<>();
        jobUrls.stream()
                .distinct()
                .forEach(url -> {
                    try {
                        log.info("Navigating to job detail URL: {}", url);
                        driver.navigate().to(url);
                        WaitUtils.waitForPageLoad(driver, 20);
                        RateLimiter.humanDelay();

                        // Wait for workspace element to ensure JS content has loaded
                        WaitUtils.waitForVisibility(driver, By.xpath("//*[@id='workspace']"), 8);

                        String jobDetailPageSource = driver.getPageSource();
                        org.jsoup.nodes.Document jobDetailDoc = org.jsoup.Jsoup.parse(jobDetailPageSource);

                        // Extract Key Details
                        String description = jobDetailDoc
                                .selectXpath("//section//div[3]/div[3]/div/div/div/div/div/p").text();
                        String title = jobDetailDoc
                                .selectXpath("//*[@id='workspace']//div[div/a]/following-sibling::div[1]/div/p").text();
                        String company = jobDetailDoc.selectXpath(
                                "(//*[@id='workspace']//div[div/a])[1]")
                                .text();
                        String location = jobDetailDoc.selectXpath(
                                "(//*[@id='workspace']//div/div/div/div[1]/div/div/div[2]/div/div[1]/p/span[1])[1]")
                                .text();

                        Elements parent = jobDetailDoc.selectXpath("//*[@id='workspace']//p");
                        String jobPosted = parent.select("span:nth-of-type(4)").text();
                        String applyCountStatus = jobDetailDoc.selectXpath("//*[@id='workspace']//p/span[7]").text();

                        String cleanUrl = StringUtils.substringBefore(url, "?");
                        log.info("Scraped job details: [{}] @ [{}] - {}", title, company, cleanUrl);

                        // Create JobDto
                        JobDto job = new JobDto();
                        job.setJobUrl(cleanUrl);
                        job.setDescription(description);
                        job.setTitle(StringUtils.isBlank(title) ? "Unknown Title" : title);
                        job.setCompany(StringUtils.isBlank(company) ? "Unknown Company" : company);
                        job.setLocation(StringUtils.isBlank(location) ? "Unknown Location" : location);
                        job.setJob_posted(StringUtils.isBlank(jobPosted) ? "Not Specified" : jobPosted);
                        job.setJob_applyed_count_status(
                                StringUtils.isBlank(applyCountStatus) ? "Not Specified" : applyCountStatus);
                        job.setPlatform("LinkedIn");

                        scrapedJobs.add(job);
                        
                        RateLimiter.humanDelay();
                    } catch (Exception e) {
                        log.error("Failed to extract details from URL: {} due to: {}", url, e.getMessage());
                        ScreenshotUtils.takeScreenshot(driver, "linkedin_job_detail_error");
                    }
                });
        return scrapedJobs;
    }

    /**
     * Quits the WebDriver and releases browser resources.
     */
    public void quitWebDriver() {
        if (driver != null) {
            log.info("Quitting WebDriver...");
            try {
                driver.quit();
            } catch (Exception e) {
                log.warn("Error quitting WebDriver: {}", e.getMessage());
            }
        }
    }

    /**
     * Deprecated delegate for backwards compatibility.
     */
    @Deprecated
    public void quitwebdriver() {
        quitWebDriver();
    }
}
