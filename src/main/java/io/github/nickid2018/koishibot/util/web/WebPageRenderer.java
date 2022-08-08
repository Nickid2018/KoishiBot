package io.github.nickid2018.koishibot.util.web;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.util.JsonUtil;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.openqa.selenium.Keys;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebPageRenderer {

    public static final Logger WEB_RENDERER_LOGGER = LoggerFactory.getLogger("Web Renderer");
    private static FirefoxDriver driver;
    private static ExecutorService executor;

    private static final AtomicBoolean jsEnabled = new AtomicBoolean(true);

    public static void loadWebDriver(JsonObject settingsRoot) {
        close();
        JsonUtil.getString(settingsRoot, "webdriver").ifPresent(web -> {
            System.setProperty("webdriver.gecko.driver", web);
            FirefoxOptions firefoxOptions = new FirefoxOptions();
            firefoxOptions.addArguments("--headless");
            firefoxOptions.addArguments("--no-sandbox");
            firefoxOptions.addPreference("javascript.enabled", true);
            driver = new FirefoxDriver(firefoxOptions);
            executor = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder().uncaughtExceptionHandler(
                    (t, e) -> ErrorRecord.enqueueError("web.renderer", e)
            ).daemon(true).namingPattern("Web Page Renderer").build());
            WEB_RENDERER_LOGGER.info("Web page renderer initialized.");
        });
    }

    public static FirefoxDriver getDriver() {
        return driver;
    }

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static void expectJS(boolean toggle) {
        if (jsEnabled.get() != toggle)
            switchJSEnabled();
    }

    private static void switchJSEnabled() {
        driver.get("about:config");
        Actions act = new Actions(driver);
        act.sendKeys(Keys.RETURN).sendKeys("javascript.enabled").perform();
        try {
            Thread.sleep(5);
        } catch (InterruptedException ignored) {
        }
        act.sendKeys(Keys.TAB).sendKeys(Keys.TAB).sendKeys(Keys.RETURN).sendKeys(Keys.F5).perform();

        boolean get = jsEnabled.get();
        jsEnabled.compareAndSet(get, !get);
        WEB_RENDERER_LOGGER.info("JavaScript now switches {}", !get);
    }

    public static void close() {
        if (executor != null)
            executor.shutdownNow();
        if (driver != null)
            driver.quit();
        if (executor != null || driver != null)
            WEB_RENDERER_LOGGER.info("Web Page Renderer closed.");
        executor = null;
        driver = null;
    }
}
