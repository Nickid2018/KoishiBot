package io.github.nickid2018.koishibot.wiki;

import io.github.nickid2018.koishibot.KoishiBotMain;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.HasFullPageScreenshot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class InfoBoxShooter {

    public static final Random NAME_RANDOM = new Random();
    public static final File DATA_FOLDER = KoishiBotMain.INSTANCE.getDataFolder();

    private static WebDriver driver;
    private static ExecutorService executor;

    public static void loadWebDriver() {
        close();
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.addArguments("--headless");
        firefoxOptions.addArguments("--no-sandbox");
        driver = new FirefoxDriver(firefoxOptions);
        driver.manage().window().setSize(new Dimension(0, 0));
        executor = Executors.newSingleThreadExecutor();
    }

    public static final String[] SUPPORT_INFOBOX = new String[] {
            "notaninfobox", "infoboxtable", "infoboxSpecial", "infotemplatebox", "infobox2",
            "tpl-infobox", "portable-infobox", "infobox"
    };

    public static Future<File> getInfoBoxShot(String url, String baseURI) {
        if (executor != null)
            return executor.submit(() -> getInfoBoxShotInternal(url, baseURI));
        return null;
    }

    private static File getInfoBoxShotInternal(String url, String baseURI) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.addRequestProperty("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        String htmlData = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(htmlData);

        Element element = null;
        String className = null;
        for (String name : SUPPORT_INFOBOX) {
            Elements elements = doc.getElementsByClass(name);
            if (elements.size() > 0) {
                element = elements.get(0);
                className = name;
                break;
            }
        }

        if (element == null)
            return null;

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

        File html = new File(DATA_FOLDER, "htm" + NAME_RANDOM.nextLong() + ".html");
        KoishiBotMain.FILES_NOT_DELETE.add(html);
        try (Writer writer = new FileWriter(html)) {
            IOUtils.write(doc.html(), writer);
        }

        driver.get(html.getAbsolutePath());
        File srcFile = ((HasFullPageScreenshot) driver).getFullPageScreenshotAs(OutputType.FILE);
        KoishiBotMain.FILES_NOT_DELETE.remove(html);

        File png = new File(DATA_FOLDER, "infobox" + NAME_RANDOM.nextLong() + ".png");
        KoishiBotMain.FILES_NOT_DELETE.add(png);
        BufferedImage image = ImageIO.read(srcFile);
        WebElement element2 = driver.findElement(By.className(className));
        BufferedImage sub = image.getSubimage(element2.getLocation().x,
                element2.getLocation().y, element2.getSize().width, element2.getSize().height);
        ImageIO.write(sub, "png", png);

        return png;
    }

    public static void close() {
        if (executor != null)
            executor.shutdownNow();
        if (driver != null)
            driver.quit();
    }
}