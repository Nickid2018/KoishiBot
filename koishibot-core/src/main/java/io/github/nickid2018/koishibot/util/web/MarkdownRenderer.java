package io.github.nickid2018.koishibot.util.web;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.core.TempFileSystem;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class MarkdownRenderer {

    public static final String DOC_HEAD = """
            <!DOCTYPE html>
            <html>
            <head>
            	<link rel="stylesheet" href="https://github.githubassets.com/assets/light-5178aee0ee76.css" />
                <link rel="stylesheet" href="https://github.githubassets.com/assets/primer-494ab2110a2a.css" />
                <link rel="stylesheet" href="https://github.githubassets.com/assets/global-5a9114f3bf45.css" />
                <link rel="stylesheet" href="https://github.githubassets.com/assets/github-82b524748602.css" />
              	<link rel="stylesheet" href="https://github.githubassets.com/assets/code-3d7b701fc6eb.css" />
            </head>
            <body>
            <div class="markdown-body" style="padding:10px;width:min-content">
            """;
    public static final String DOC_END = """
            </div>
            </body>
            </html>
            """;

    public static void render(String markdown, FailableConsumer<File, Exception> additional, Consumer<Exception> exception) throws IOException {
        File tmp = TempFileSystem.createTmpFileAndCreate("md", "png");
        render(markdown, tmp, () -> {
            additional.accept(tmp);
            TempFileSystem.unlockFileAndDelete(tmp);
            return null;
        }, exception);
    }

    public static void render(String markdown, File buffered, Callable<Void> additional, Consumer<Exception> exception) {
        WebPageRenderer.getExecutor().execute(() -> {
            try {
                JsonObject object = new JsonObject();
                object.addProperty("text", markdown);
                HttpPost post = new HttpPost("https://api.github.com/markdown");
                post.setHeader("Accept", "application/vnd.github+json");
                post.setEntity(new StringEntity(object.toString(), StandardCharsets.UTF_8));

                String convert = WebUtil.fetchDataInText(post);
                File tmpHTML = TempFileSystem.createTmpFileAndCreate("help", "html");
                FileWriter fileWriter = new FileWriter(tmpHTML);
                fileWriter.write(DOC_HEAD);
                fileWriter.write(convert);
                fileWriter.write(DOC_END);
                fileWriter.close();

                WebPageRenderer.getDriver().manage().window().setSize(new Dimension(10000, 10000));
                WebPageRenderer.getDriver().get(tmpHTML.toURI().toURL().toString());
                byte[] imageData = WebPageRenderer.getDriver().findElement(By.className("markdown-body"))
                        .getScreenshotAs(OutputType.BYTES);
                TempFileSystem.unlockFileAndDelete(tmpHTML);

                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
                ImageIO.write(image, "png", buffered);

                additional.call();
            } catch (Exception e) {
                exception.accept(e);
            }
        });
    }
}
