package io.github.nickid2018.koishibot.util.web;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.util.JsonUtil;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebPageRenderer {

    public static final Logger WEB_RENDERER_LOGGER = LoggerFactory.getLogger("Web Renderer");

    private static FirefoxDriver driver;
    private static ExecutorService executor;

    public static void loadWebDriver(JsonObject settingsRoot) {
        close();
        JsonUtil.getString(settingsRoot, "webdriver").ifPresent(web -> {
            System.setProperty("webdriver.gecko.driver", web);
            FirefoxOptions firefoxOptions = new FirefoxOptions();
            firefoxOptions.addArguments("--headless");
            firefoxOptions.addArguments("--no-sandbox");
            firefoxOptions.addPreference("javascript.enabled", false);
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
