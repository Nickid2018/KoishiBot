package io.github.nickid2018.koishibot.module.wiki;

import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.util.RegexUtil;
import io.github.nickid2018.koishibot.util.web.WebPageRenderer;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class WikiPageShooter {

    public static final Logger WIKI_PAGE_LOGGER = LoggerFactory.getLogger("Wiki Page Shooter");

    public static final Pattern HEADER_PATTERN = Pattern.compile("h[1-6]");

    public static final String[] SUPPORT_INFOBOX = new String[] {
            "notaninfobox", "infoboxtable", "infoboxSpecial", "infotemplatebox", "infobox2",
            "tpl-infobox", "portable-infobox", "toccolours", "infobox"
    };

    public static Document fetchWikiPage(String url, Map<String, String> headers) throws IOException {
        File buffered = TempFileSystem.getTmpFileBuffered("wiki", url);
        if (buffered != null) {
            String data;
            try (FileReader reader = new FileReader(buffered)) {
                data = IOUtils.toString(reader);
            }
            return Jsoup.parse(data);
        }
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(30000);
        connection.addRequestProperty("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        connection.addRequestProperty("User-Agent", WebUtil.chooseRandomUA());
        for (Map.Entry<String, String> entry : headers.entrySet())
            connection.addRequestProperty(entry.getKey(), entry.getValue());
        String htmlData = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        File tmp = TempFileSystem.createTmpFileBuffered("wiki", url, "wikipage", "html", true);
        try (FileWriter writer = new FileWriter(tmp)) {
            IOUtils.write(htmlData, writer);
        }
        Document document = Jsoup.parse(htmlData);
        if (document.getElementById("mw-content-text") == null)
            throw new IOException("将要渲染的页面无效，可能机器人收到了错误的页面。");
        return document;
    }

    public static Future<File> getInfoBoxShot(String url, String baseURI, WikiInfo info) {
        if (info.getRenderSettings().enable() && WebPageRenderer.getExecutor() != null)
            return WebPageRenderer.getExecutor().submit(() -> getInfoBoxShotInternal(url, baseURI, null, info));
        return null;
    }

    public static Future<File> getInfoBoxShot(String url, String baseURI, Document document, WikiInfo info) {
        if (info.getRenderSettings().enable() && WebPageRenderer.getExecutor() != null)
            return WebPageRenderer.getExecutor().submit(() -> getInfoBoxShotInternal(url, baseURI, document, info));
        return null;
    }

    public static Future<File> getFullPageShot(String url, String baseURI, WikiInfo info) {
        if (info.getRenderSettings().enable() && WebPageRenderer.getExecutor() != null)
            return WebPageRenderer.getExecutor().submit(() -> getFullPageShotInternal(url, baseURI, null, info));
        return null;
    }

    public static Future<File> getFullPageShot(String url, String baseURI, Document document, WikiInfo info) {
        if (info.getRenderSettings().enable() && WebPageRenderer.getExecutor() != null)
            return WebPageRenderer.getExecutor().submit(() -> getFullPageShotInternal(url, baseURI, document, info));
        return null;
    }

    public static Future<File> getSectionShot(String url, Document doc, String baseURI, String section, WikiInfo info) {
        if (info.getRenderSettings().enable() && WebPageRenderer.getExecutor() != null)
            return WebPageRenderer.getExecutor().submit(() -> getSectionShotInternal(url, doc, baseURI, section, info));
        return null;
    }

    private static File getInfoBoxShotInternal(String url, String baseURI, Document doc, WikiInfo info) throws IOException {
        File data = TempFileSystem.getTmpFileBuffered("infobox", url);
        if (data != null)
            return data;
        doc = doc == null ? fetchWikiPage(url, info.getAdditionalHeaders()) : doc;
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
            WIKI_PAGE_LOGGER.info("URL {} has no infobox.", url);
            return null;
        } else
            WIKI_PAGE_LOGGER.info("URL {} has an infobox, element class is {}", url, className);
        File png = TempFileSystem.createTmpFileBuffered("infobox", url, "infobox", "png", false);
        cleanAndRender(baseURI, doc, element, By.className(className), png, info.getRenderSettings());
        return png;
    }

    private static File getFullPageShotInternal(String url, String baseURI, Document doc, WikiInfo info) throws IOException {
        File data = TempFileSystem.getTmpFileBuffered("full", url);
        if (data != null)
            return data;
        doc = doc == null ? fetchWikiPage(url, info.getAdditionalHeaders()) : doc;
        Element element = doc.getElementById("mw-content-text");
        if (element == null)
            throw new IOException("无效的页面");
        File png = TempFileSystem.createTmpFileBuffered(
                "full", url, "full", "png", false);

        cleanAndRender(baseURI, doc, element, By.id("mw-content-text"), png, info.getRenderSettings());
        WIKI_PAGE_LOGGER.info("Rendered a full page, url = {}.", url);
        return png;
    }

    private static File getSectionShotInternal(String url, Document doc, String baseURI, String section, WikiInfo info) throws IOException {
        File data = TempFileSystem.getTmpFileBuffered("section", url + "-" + section);
        if (data != null)
            return data;

        Elements elements = doc.getElementsByClass("mw-headline");
        Element found = null;
        for (Element element : elements) {
            if (element.text().equalsIgnoreCase(section) && RegexUtil.match(HEADER_PATTERN, element.parent().tagName())) {
                found = element.parent();
                break;
            }
        }

        if (found == null || found.nextElementSibling() == null) {
            WIKI_PAGE_LOGGER.error("Can't find render element: {} of {}.", url, section);
            return null;
        }

        String tagName = found.tagName();
        char level = tagName.charAt(1);
        Elements renderElements = new Elements(found);
        Element nowElement;
        Element nextElement = found.nextElementSibling();
        while (nextElement != null &&
                !(RegexUtil.match(HEADER_PATTERN, nextElement.tagName()) && nextElement.tagName().charAt(1) - level <= 0)) {
            nowElement = nextElement;
            nextElement = nowElement.nextElementSibling();
            renderElements.add(nowElement);
        }
        Element element = found.parent().clone();
        element.children().forEach(Element::remove);
        found.parent().parent().appendChild(element);
        renderElements.forEach(element::appendChild);
        File png = TempFileSystem.createTmpFileBuffered(
                "section", url + "-" + section, "section", "png", false);

        cleanAndRender(baseURI, doc, element, By.id("mw-content-text"), png, info.getRenderSettings());
        WIKI_PAGE_LOGGER.info("Rendered section: {} of {}.", section, url);
        return png;
    }

    private static void cleanAndRender(String baseURI, Document doc, Element element,
                                       By by, File png, WikiRenderSettings settings) throws IOException {
        while (!element.equals(doc.body())) {
            Element parent = element.parent();
            for (Element child : parent.children())
                if (!child.equals(element))
                    child.remove();
            element = parent;
            if (element.tagName().equals("template")) {
                Element clone = element.child(0).clone();
                element.parent().appendChild(clone);
                element = clone;
            }
        }
        Queue<Element> bfs = new LinkedList<>();
        bfs.offer(element);
        while (!bfs.isEmpty()) {
            Element now = bfs.poll();
            now.removeClass("mw-collapsible");
            now.removeClass("mw-collapsed");
            now.removeClass("collapsible");
            now.removeClass("collapsed");
            now.children().forEach(bfs::offer);
        }
        doc.body().addClass("heimu_toggle_on");
        doc.head().prependChild(new Element("base").attr("href", baseURI));
        Element heimuToggle = new Element("style").text("""
                        body.heimu_toggle_on .heimu, body.heimu_toggle_on .heimu rt {
                          background-color: rgba(37,37,37,0.13) !important;
                        }
                        """);
        doc.head().appendChild(heimuToggle);
        File html = TempFileSystem.createTmpFileAndCreate("htm", "html");
        try (Writer writer = new FileWriter(html)) {
            IOUtils.write(doc.html(), writer);
        }
        WebPageRenderer.getDriver().manage().window().setSize(new Dimension(settings.width(), settings.height()));
        WebPageRenderer.getDriver().get(html.toURI().toURL().toString());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
        TempFileSystem.unlockFileAndDelete(html);
        byte[] imageData = WebPageRenderer.getDriver().findElement(by).getScreenshotAs(OutputType.BYTES);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        ImageIO.write(image, "png", png);
    }
}
