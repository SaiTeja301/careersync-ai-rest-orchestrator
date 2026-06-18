package com.jobautomation.naukri.automation;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.jobautomation.common.constants.JobConstants;
import com.jobautomation.common.dto.JobDto;
import com.jobautomation.naukri.utils.RateLimiter;
import com.jobautomation.naukri.utils.ScreenshotUtils;
import com.jobautomation.naukri.utils.WaitUtils;

/**
 * Naukri.com job scraping bot.
 *
 * Responsibilities:
 *   1. Login to Naukri with email/password
 *   2. Navigate to job search results (keyword + filters)
 *   3. Collect all job listing URLs across N pages
 *   4. Scrape each job's detail page and map to {@link JobDto}
 *
 * Design principles:
 *   - Uses Spring-injected {@code @Lazy WebDriver} — never creates its own ChromeDriver
 *   - All waits via {@link WaitUtils} (explicit condition-based) — no raw Thread.sleep()
 *   - All delays via {@link RateLimiter} (randomised human-like pauses)
 *   - Screenshots captured on every failure via {@link ScreenshotUtils}
 *   - Stale element handling via {@link WaitUtils#retryOnStaleElement}
 *   - Selectors use stable {@code contains(@class,...)} XPath — not hashed CSS module names
 *
 * Package migration:
 *   com.jobbot.automation  →  com.jobautomation.naukri.automation
 *   com.jobbot.dto.JobDto  →  com.jobautomation.common.dto.JobDto
 *   com.jobbot.utils.*     →  com.jobautomation.naukri.utils.*
 *   com.jobbot.constants.JobConstants → com.jobautomation.common.constants.JobConstants
 */
@Component
public class NaukriJobBot {

    private static final Logger log = LoggerFactory.getLogger(NaukriJobBot.class);

    // ── Stable XPath selectors for the Naukri job detail page ───────────────
    // Using contains(@class) to survive CSS module hash changes on Naukri deploys.
    private static final String XPATH_JOB_TITLE       = "//*[contains(@class,'jd-header-title')]";
    private static final String XPATH_COMPANY         = "(//*[contains(@class,'comp-name')]//a)[1]";
    private static final String XPATH_LOCATION        = "//*[contains(@class,'styles_jhc__location__W_pVs')]";
    private static final String XPATH_DESCRIPTION     = "//*[contains(@class,'job-desc')]";
    private static final String XPATH_POSTED          = "(//*[contains(@class,'styles_jhc__stat__PgY67')])[1]";
    private static final String XPATH_APPLY_COUNT     = "(//*[contains(@class,'styles_jhc__stat__PgY67')])[3]";

    // ── CSS selectors for the search results listing page ───────────────────
    // data-job-id is a stable data attribute — safe to rely on.
    private static final String CSS_JOB_TUPLE         = ".srp-jobtuple-wrapper[data-job-id]";
    private static final String CSS_JOB_TUPLE_ALT     = "article[data-job-id]";
    private static final String ATTR_JOB_ID           = "data-job-id";

    // ── Naukri job detail URL pattern ────────────────────────────────────────
    private static final String NAUKRI_JOB_DETAIL_URL = "https://www.naukri.com/job-listings-";
    private static final String PLATFORM              = "Naukri";

    // ── WebDriver ───────────────────────────────────────────────────────────
    private final WebDriver driver;

    /**
     * Constructor injection — Spring provides the shared {@code @Lazy WebDriver} bean.
     * The bot never instantiates its own ChromeDriver.
     */
    public NaukriJobBot(@Lazy WebDriver driver) {
        this.driver = driver;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 1. LOGIN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Logs in to Naukri using the provided credentials.
     * Navigates to the login page, fills in email and password, clicks Login,
     * then waits for the session to establish.
     *
     * @param email    Naukri registered email
     * @param password Naukri account password
     */
    public void naukriLogin(String email, String password) {
        log.info("Navigating to Naukri login page...");
        try {
            driver.get(JobConstants.JOB_PORTAL_NAUKRI.getConstantValue());
            WaitUtils.waitForPageLoad(driver, 20);
            RateLimiter.humanDelay();

            // Enter email
            By emailLocator = By.xpath("//span[contains(text(),'Email ID / Username')]/following-sibling::input");
            WebElement emailField = WaitUtils.waitForVisibility(driver, emailLocator, 15);
            if (emailField != null) {
                emailField.clear();
                emailField.sendKeys(email);
                log.info("Email entered.");
                RateLimiter.humanDelay();
            } else {
                log.warn("Email field not found — Naukri login page may not have loaded.");
                ScreenshotUtils.takeScreenshot(driver, "naukri_login_email_not_found");
            }

            // Enter password
            By passwordLocator = By.xpath("//span[contains(text(),'Password')]/following-sibling::input");
            WebElement passwordField = WaitUtils.waitForVisibility(driver, passwordLocator, 10);
            if (passwordField != null) {
                passwordField.clear();
                passwordField.sendKeys(password);
                log.info("Password entered.");
                RateLimiter.humanDelay();
            } else {
                log.warn("Password field not found on Naukri login page.");
                ScreenshotUtils.takeScreenshot(driver, "naukri_login_password_not_found");
            }

            // Click Login button
            By loginBtnLocator = By.xpath("//button[contains(text(),'Login')]");
            WebElement loginBtn = WaitUtils.waitForClickable(driver, loginBtnLocator, 10);
            if (loginBtn != null) {
                loginBtn.click();
                log.info("Login button clicked — waiting for session to establish...");
                RateLimiter.loginDelay();
                WaitUtils.waitForPageLoad(driver, 20);
                log.info("Naukri login complete. Current URL: {}", driver.getCurrentUrl());
            } else {
                log.error("Login button not found — cannot proceed.");
                ScreenshotUtils.takeScreenshot(driver, "naukri_login_btn_not_found");
            }

        } catch (Exception e) {
            log.error("Naukri login failed: {}", e.getMessage(), e);
            ScreenshotUtils.takeScreenshot(driver, "naukri_login_error");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. SCRAPE BY KEYWORD (main entry point for keyword-based search)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Scrapes jobs from Naukri search results for a given skill/tech stack.
     * Generates paginated URLs from page 1 to {@code pageCount} and collects
     * all job listing URLs before visiting each one.
     *
     * @param skillTechStack the search keyword (e.g. "java spring boot")
     * @param pageCount      number of result pages to scrape (1-based)
     * @return list of scraped {@link JobDto} objects
     */
    public List<JobDto> scrapeJobsBySkillTechStack(String skillTechStack, int pageCount) {
        log.info("Starting Naukri keyword scrape — keyword='{}', pages={}", skillTechStack, pageCount);
        Set<String> jobUrls = new HashSet<>();
        List<String> pageUrls = buildPaginatedSearchUrls(skillTechStack, pageCount);

        for (String pageUrl : pageUrls) {
            try {
                log.info("Navigating to search page: {}", pageUrl);
                driver.navigate().to(pageUrl);
                WaitUtils.waitForPageLoad(driver, 20);
                RateLimiter.longDelay();

                String pageSource = driver.getPageSource();
                Document doc = Jsoup.parse(pageSource);
                collectJobLinksFromSearchPage(doc, jobUrls);

            } catch (Exception e) {
                log.error("Failed to scrape search page '{}': {}", pageUrl, e.getMessage());
                ScreenshotUtils.takeScreenshot(driver, "naukri_search_page_error");
            }
        }

        log.info("Collected {} unique job URLs from search pages.", jobUrls.size());
        return scrapeNaukriJobDetails(jobUrls);
    }

    /**
     * Scrapes jobs from a pre-built Naukri search URL (called from the service layer).
     * Appends {@code &pageNo=N} to generate paginated versions of the URL.
     *
     * @param searchUrl full Naukri search URL (built by the service)
     * @param pageCount number of pages to scrape
     * @return list of scraped {@link JobDto} objects
     */
    public List<JobDto> scrapeJobsBySearchUrl(String searchUrl, int pageCount) {
        log.info("Starting Naukri URL-based scrape — url='{}', pages={}", searchUrl, pageCount);
        Set<String> jobUrls = new HashSet<>();

        for (int page = 1; page <= pageCount; page++) {
            String pageUrl = searchUrl + "&pageNo=" + page;
            try {
                log.info("Navigating to page {}: {}", page, pageUrl);
                driver.navigate().to(pageUrl);
                WaitUtils.waitForPageLoad(driver, 20);
                RateLimiter.longDelay();

                String pageSource = driver.getPageSource();
                Document doc = Jsoup.parse(pageSource);
                collectJobLinksFromSearchPage(doc, jobUrls);

            } catch (Exception e) {
                log.error("Failed to scrape URL page {} '{}': {}", page, pageUrl, e.getMessage());
                ScreenshotUtils.takeScreenshot(driver, "naukri_url_page_" + page + "_error");
            }
        }

        log.info("Collected {} unique job URLs from URL-based scrape.", jobUrls.size());
        return scrapeNaukriJobDetails(jobUrls);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. SCRAPE RECOMMENDED JOBS (authenticated user — post-login)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Scrapes recommended jobs for the logged-in Naukri user.
     * Tabs on the recommended jobs page represent different recommendation categories.
     *
     * @return list of scraped {@link JobDto} objects
     */
    public List<JobDto> scrapeRecommendedJobs() {
        String recommendedUrl = "https://www.naukri.com/mnjuser/recommendedjobs";
        log.info("Navigating to Naukri recommended jobs: {}", recommendedUrl);
        Set<String> jobUrls = new HashSet<>();

        try {
            driver.navigate().to(recommendedUrl);
            WaitUtils.waitForPageLoad(driver, 20);
            RateLimiter.longDelay();

            // Collect from the initial page
            Document doc = Jsoup.parse(driver.getPageSource());
            collectJobLinksFromRecommendedPage(doc, jobUrls);

            // Click through each recommendation tab
            List<WebElement> tabs = WaitUtils.waitForPresenceOfAll(
                    driver, By.xpath("//div[contains(@class,'tab-list-item')]"), 10);

            log.info("Found {} recommendation tabs.", tabs.size());

            for (int i = 0; i < tabs.size(); i++) {
                // Re-fetch elements each iteration to avoid StaleElementReferenceException
                final int tabIndex = i;
                WaitUtils.retryOnStaleElement(() -> {
                    List<WebElement> freshTabs = driver.findElements(
                            By.xpath("//div[contains(@class,'tab-list-item')]"));
                    if (tabIndex < freshTabs.size()) {
                        freshTabs.get(tabIndex).click();
                        log.info("Clicked recommendation tab {}.", tabIndex + 1);
                        RateLimiter.longDelay();
                        Document tabDoc = Jsoup.parse(driver.getPageSource());
                        collectJobLinksFromRecommendedPage(tabDoc, jobUrls);
                    }
                    return null;
                }, 3);
            }

        } catch (Exception e) {
            log.error("Failed to scrape recommended jobs: {}", e.getMessage(), e);
            ScreenshotUtils.takeScreenshot(driver, "naukri_recommended_error");
        }

        log.info("Collected {} unique job URLs from recommended jobs.", jobUrls.size());
        return scrapeNaukriJobDetails(jobUrls);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 4. JOB LINK COLLECTION HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Extracts job-listing URLs from a Naukri search results page.
     * Uses {@code data-job-id} attribute which is a stable, non-hashed identifier.
     * Falls back to an alternate selector if the primary yields nothing.
     *
     * @param doc     Jsoup Document of the search results page
     * @param jobUrls set to add discovered URLs into (deduplication via Set)
     */
    public void collectJobLinksFromSearchPage(Document doc, Set<String> jobUrls) {
        Elements jobElements = doc.select(CSS_JOB_TUPLE);

        // Fallback selector if primary returns nothing
        if (jobElements.isEmpty()) {
            log.debug("Primary search selector returned 0 jobs — trying fallback selector.");
            jobElements = doc.select(CSS_JOB_TUPLE_ALT);
        }

        if (jobElements.isEmpty()) {
            log.warn("No job elements found on search results page — Naukri DOM may have changed.");
            return;
        }

        for (Element element : jobElements) {
            Optional.ofNullable(element.attr(ATTR_JOB_ID))
                    .filter(id -> !id.isBlank())
                    .ifPresent(id -> {
                        String jobUrl = NAUKRI_JOB_DETAIL_URL + id;
                        if (jobUrls.add(jobUrl)) {
                            log.debug("Found job URL: {}", jobUrl);
                        }
                    });
        }
        log.info("collectJobLinksFromSearchPage: found {} new positions. Total so far: {}",
                jobElements.size(), jobUrls.size());
    }

    /**
     * Extracts job-listing URLs from the Naukri recommended-jobs page.
     * The recommended page uses a different HTML wrapper than search results.
     *
     * @param doc     Jsoup Document of the recommended jobs page
     * @param jobUrls set to add discovered URLs into
     */
    public void collectJobLinksFromRecommendedPage(Document doc, Set<String> jobUrls) {
        // Try the search selector first — Naukri sometimes reuses the same structure
        collectJobLinksFromSearchPage(doc, jobUrls);

        // Also try the older recommended-page selector as a fallback
        Elements legacyElements = doc.select(".list .jobTuple[data-job-id]");
        for (Element element : legacyElements) {
            Optional.ofNullable(element.attr(ATTR_JOB_ID))
                    .filter(id -> !id.isBlank())
                    .ifPresent(id -> {
                        String jobUrl = NAUKRI_JOB_DETAIL_URL + id;
                        if (jobUrls.add(jobUrl)) {
                            log.debug("Found recommended job URL (legacy selector): {}", jobUrl);
                        }
                    });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 5. JOB DETAIL SCRAPING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Visits each job detail URL and extracts structured data into a {@link JobDto}.
     *
     * Selector strategy:
     *   Primary: stable {@code contains(@class,...)} XPath — survives CSS hash changes
     *   Fallback: JSON-LD structured data ({@code <script type="application/ld+json">})
     *
     * @param jobUrls set of Naukri job detail URLs to visit
     * @return list of populated {@link JobDto} objects (best-effort fields)
     */
    public List<JobDto> scrapeNaukriJobDetails(Set<String> jobUrls) {
        List<JobDto> scrapedJobs = new ArrayList<>();

        for (String url : jobUrls) {
            log.info("Scraping job detail: {}", url);
            try {
                driver.navigate().to(url);
                WaitUtils.waitForPageLoad(driver, 20);
                RateLimiter.humanDelay();

                // Wait for a key element before parsing to ensure JS-rendered content is ready
                WaitUtils.waitForVisibility(driver, By.xpath(XPATH_JOB_TITLE), 12);

                String pageSource = driver.getPageSource();
                Document doc = Jsoup.parse(pageSource);

                // ── Primary extraction via stable XPath-class selectors ──────────
                String title       = extractText(doc, XPATH_JOB_TITLE);
                String company     = extractText(doc, XPATH_COMPANY);
                String location    = extractText(doc, XPATH_LOCATION);
                String description = extractText(doc, XPATH_DESCRIPTION);
                String jobPosted   = extractText(doc, XPATH_POSTED);
                String applyCount  = extractText(doc, XPATH_APPLY_COUNT);

                // ── Fallback: JSON-LD structured data ───────────────────────────
                if (isBlank(title) || isBlank(company)) {
                    log.debug("Primary selectors missed title/company — trying JSON-LD fallback.");
                    JobDto fallback = extractFromJsonLd(doc);
                    if (isBlank(title))       title       = fallback.getTitle();
                    if (isBlank(company))     company     = fallback.getCompany();
                    if (isBlank(location))    location    = fallback.getLocation();
                    if (isBlank(description)) description = fallback.getDescription();
                }

                // ── Build DTO ────────────────────────────────────────────────────
                JobDto job = new JobDto();
                job.setJobUrl(url);
                job.setTitle(isBlank(title)       ? "Unknown Title"    : title);
                job.setCompany(isBlank(company)   ? "Unknown Company"  : company);
                job.setLocation(isBlank(location) ? "Unknown Location" : location);
                job.setDescription(description);
                job.setJob_posted(isBlank(jobPosted)  ? "Not Specified" : jobPosted);
                job.setJob_applyed_count_status(isBlank(applyCount) ? "Not Specified" : applyCount);
                job.setPlatform(PLATFORM);

                scrapedJobs.add(job);
                log.info("Scraped: [{}] @ [{}] — {}", job.getTitle(), job.getCompany(), url);

                // Short pause between detail pages to avoid rate-limiting
                RateLimiter.humanDelay();

            } catch (Exception e) {
                log.error("Failed to scrape job detail '{}': {}", url, e.getMessage());
                ScreenshotUtils.takeScreenshot(driver, "naukri_job_detail_error");
            }
        }

        log.info("scrapeNaukriJobDetails complete — {} jobs scraped out of {} URLs.",
                scrapedJobs.size(), jobUrls.size());
        return scrapedJobs;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 6. PAGINATION URL BUILDER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Builds a list of N paginated Naukri search URLs for a given keyword.
     * Page numbers are 1-based and appended as {@code &pageNo=N}.
     *
     * @param keyword   the search keyword (e.g. "java spring boot")
     * @param pageCount the number of pages to generate
     * @return list of paginated URLs
     */
    private List<String> buildPaginatedSearchUrls(String keyword, int pageCount) {
        List<String> urls = new ArrayList<>();
        String encodedKeyword;
        try {
            encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        } catch (Exception e) {
            encodedKeyword = keyword.replace(" ", "%20");
        }

        // Naukri slug format: "java-spring-boot-jobs"
        String slug = keyword.toLowerCase().trim().replace(" ", "-");

        for (int page = 1; page <= pageCount; page++) {
            String url = String.format(
                    "https://www.naukri.com/%s-jobs?k=%s&nignbevent_src=jobsearchDeskGNB&pageNo=%d",
                    slug, encodedKeyword, page);
            urls.add(url);
            log.debug("Generated pagination URL page {}: {}", page, url);
        }
        return urls;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 7. EXTRACTION HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Safely extracts trimmed text from a Jsoup document using an XPath expression.
     * Returns an empty string if the element is not found.
     */
    private String extractText(Document doc, String xpath) {
        try {
            Elements elements = doc.selectXpath(xpath);
            return elements.isEmpty() ? "" : elements.first().text().trim();
        } catch (Exception e) {
            log.debug("XPath extraction failed for '{}': {}", xpath, e.getMessage());
            return "";
        }
    }

    /**
     * Attempts to extract job metadata from JSON-LD structured data embedded in the page.
     * This is a stable fallback since JSON-LD is a Google/schema.org standard
     * and does not change with CSS module hash updates.
     *
     * @param doc the Jsoup document of the job detail page
     * @return a partially-filled JobDto with available fields (blanks for missing ones)
     */
    private JobDto extractFromJsonLd(Document doc) {
        JobDto dto = new JobDto();
        try {
            Elements ldScripts = doc.select("script[type=application/ld+json]");
            for (Element script : ldScripts) {
                String json = script.html();
                if (json.contains("JobPosting")) {
                    // Simple string extraction — avoids adding a Jackson/Gson dependency
                    dto.setTitle(extractJsonField(json, "title"));
                    dto.setCompany(extractJsonField(json, "name"));        // hiringOrganization.name
                    dto.setLocation(extractJsonField(json, "addressLocality"));
                    dto.setDescription(extractJsonField(json, "description"));
                    log.debug("JSON-LD fallback succeeded: title='{}', company='{}'",
                            dto.getTitle(), dto.getCompany());
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("JSON-LD extraction failed: {}", e.getMessage());
        }
        return dto;
    }

    /**
     * Minimal regex-free JSON field extractor.
     * Extracts the value of {@code "fieldName": "value"} from a JSON string.
     */
    private String extractJsonField(String json, String fieldName) {
        String marker = "\"" + fieldName + "\"";
        int idx = json.indexOf(marker);
        if (idx < 0) return "";
        int colonIdx = json.indexOf(":", idx);
        if (colonIdx < 0) return "";
        int quoteStart = json.indexOf("\"", colonIdx + 1);
        if (quoteStart < 0) return "";
        int quoteEnd = json.indexOf("\"", quoteStart + 1);
        if (quoteEnd < 0) return "";
        return json.substring(quoteStart + 1, quoteEnd).trim();
    }

    /**
     * Null-safe blank check.
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 8. DRIVER LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Quits the WebDriver and releases browser resources.
     * Always call this in a finally block after scraping completes.
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
}
