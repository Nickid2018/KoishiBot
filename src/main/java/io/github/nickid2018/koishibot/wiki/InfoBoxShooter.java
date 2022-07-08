package io.github.nickid2018.koishibot.wiki;

import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.util.WebUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

public class InfoBoxShooter {

    public static final Logger INFOBOX_LOGGER = LoggerFactory.getLogger("Wiki Infobox");

    static {
        try {
            Class<?> remoteDriverClass = RemoteWebDriver.class;
            Field field = remoteDriverClass.getDeclaredField("logger");
            field.setAccessible(true);
            ((java.util.logging.Logger) field.get(null)).setLevel(Level.OFF);
        } catch (Exception e) {
            INFOBOX_LOGGER.error("Cannot turn off logger.", e);
        }

    }

    private static FirefoxDriver driver;
    private static ExecutorService executor;

    public static void loadWebDriver() {
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.addArguments("--headless");
        firefoxOptions.addArguments("--no-sandbox");
        driver = new FirefoxDriver(firefoxOptions);
        driver.manage().window().setSize(new Dimension(0, 0));
        executor = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder().uncaughtExceptionHandler(
                (t, e) -> ErrorRecord.enqueueError("wiki.infoboxshoot", e)
        ).daemon(true).namingPattern("Infobox Shooter %d").build());
        INFOBOX_LOGGER.info("Infobox Shooter initialized.");
    }

    public static final String[] SUPPORT_INFOBOX = new String[] {
            "notaninfobox", "infoboxtable", "infoboxSpecial", "infotemplatebox", "infobox2",
            "tpl-infobox", "portable-infobox", "toccolours", "infobox"
    };

    public static Future<File> getInfoBoxShot(String url, String baseURI) {
        if (executor != null)
            return executor.submit(() -> getInfoBoxShotInternal(url, baseURI));
        return null;
    }

    private static File getInfoBoxShotInternal(String url, String baseURI) throws IOException {
        File data = TempFileSystem.getTmpFileBuffered("infobox", url);
        if (data != null)
            return data;

        URLConnection connection = new URL(url).openConnection();
        connection.addRequestProperty("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        connection.addRequestProperty("User-Agent", WebUtil.chooseRandomUA());
        String htmlData = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(htmlData);

        Element element = null;
        String className = null;
        for (String name : SUPPORT_INFOBOX) {
            Elements elements = doc.getElementsByClass(name);
            Optional<Element> elementFind = elements.stream().filter(elementNow -> elementNow.classNames().contains(name)).findFirst();
            if (elementFind.isPresent()) {
                element = elementFind.get();
                className = name;
                break;
            }
        }

        if (element == null) {
            INFOBOX_LOGGER.info("URL {} has no infobox.", url);
            return null;
        } else
            INFOBOX_LOGGER.info("URL {} has an infobox, element class is {}", url, className);

        while (!element.equals(doc.body())) {
            Element parent = element.parent();
            for (Element child : parent.children())
                if (!child.equals(element))
                    child.remove();
            element = parent;
            element.removeClass("mw-collapsible");
            element.removeClass("mw-collapsed");
            if (element.id().equals("mw-content-text")) {
                element = element.clone();
                break;
            }
        }

        doc.body().children().forEach(Element::remove);
        doc.body().appendChild(element);
        doc.body().addClass("heimu_toggle_on");
        doc.head().prependChild(new Element("base").attr("href", baseURI));
        Element heimuToggle = new Element("style").text(
                        "body.heimu_toggle_on .heimu, body.heimu_toggle_on .heimu rt {\n" +
                        "  background-color: rgba(37,37,37,0.13) !important;\n" +
                        "}");
        doc.head().appendChild(heimuToggle);

        File html = TempFileSystem.createTmpFileAndCreate("htm", "html");
        try (Writer writer = new FileWriter(html)) {
            IOUtils.write(doc.html(), writer);
        }

        driver.get(html.getAbsolutePath());
        File srcFile = driver.getFullPageScreenshotAs(OutputType.FILE);
        TempFileSystem.unlockFileAndDelete(html);

        File png = TempFileSystem.createTmpFileBuffered("infobox", url, "infobox", "png", false);
        BufferedImage image = ImageIO.read(srcFile);
        try {
            WebElement element2 = driver.findElement(By.className(className));
            BufferedImage sub = image.getSubimage(element2.getLocation().x,
                    element2.getLocation().y, element2.getSize().width, element2.getSize().height);
            ImageIO.write(sub, "png", png);
            srcFile.delete();
        } catch (Exception e) {
            throw new IOException("无法渲染信息框", e);
        }

        return png;
    }

    public static void close() {
        if (executor != null)
            executor.shutdownNow();
        if (driver != null)
            driver.quit();
        if (executor != null || driver != null)
            INFOBOX_LOGGER.info("Infobox Shooter closed.");
        executor = null;
        driver = null;
    }
}
