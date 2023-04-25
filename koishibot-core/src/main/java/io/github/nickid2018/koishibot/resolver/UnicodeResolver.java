package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;

@ResolverName("unicode")
@Syntax(syntax = "~unicode [Unicode字符编码，16进制]", help = "获取Unicode字符的信息")
public class UnicodeResolver extends MessageResolver {

    public UnicodeResolver() {
        super("~unicode");
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, DelegateEnvironment environment) {
        try {
            int codepoint = Integer.parseInt(key, 16);
            AsyncUtil.execute(() -> {
                String charStr = new String(Character.toChars(codepoint));
                HttpGet unicodeTable = new HttpGet("https://unicode-table.com/cn/%x/".formatted(codepoint));
                HttpGet ziTool = new HttpGet("https://zi.tools/api/zi/" + WebUtil.encode(charStr));

                try {
                    Document doc = Jsoup.parse(WebUtil.fetchDataInText(unicodeTable));
                    Element element = doc.getElementsByClass("symbol-tabs__tabs-content").get(0);
                    Elements elements = element.children();
                    if (elements.size() < 3) {
                        environment.getMessageSender().sendMessage(context, environment.newText("找不到该字符的信息！"));
                    } else {
                        Element techInfo = elements.get(0);
                        Element codeInfo = elements.get(2);
                        StringBuilder builder = new StringBuilder();
                        Elements techInfoElements = techInfo.getElementsByTag("tr");
                        for (Element e : techInfoElements)
                            builder.append(e.child(0).text()).append(": ").append(e.child(1).text()).append("\n");
                        Elements codeInfoElements = codeInfo.getElementsByTag("tr");
                        codeInfoElements.remove(0);
                        for (Element e : codeInfoElements)
                            builder.append(e.child(0).text()).append(": ").append(e.child(1).text()).append("\n");

                        String textSend = builder.toString();
                        File charImg = null;

                        try {
                            JsonObject object = WebUtil.fetchDataInJson(ziTool).getAsJsonObject();
                            String svg = JsonUtil.getStringInPathOrNull(object, "font." + charStr);
                            if (svg != null) {
                                String made = """
                                        <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.0//EN" "http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="200" height="240" viewBox="0 -46 200 240">
                                            <path fill="currentColor" d="%s" />
                                        </svg>
                                        """.formatted(svg);
                                Transcoder transcoder = new PNGTranscoder();
                                transcoder.addTranscodingHint(ImageTranscoder.KEY_BACKGROUND_COLOR, Color.WHITE);
                                TranscoderInput input = new TranscoderInput(new StringReader(made));
                                charImg = TempFileSystem.createTmpFile("unicode", ".png");
                                try (FileOutputStream os = new FileOutputStream(charImg)) {
                                    TranscoderOutput output = new TranscoderOutput(os);
                                    transcoder.transcode(input, output);
                                }
                            }
                        } catch (Exception ignored) {
                        }

                        if (charImg != null)
                            environment.getMessageSender().sendMessage(context, environment.newChain(
                                    environment.newText(textSend),
                                    environment.newImage(charImg.toURI().toURL())
                            ));
                        else
                            environment.getMessageSender().sendMessage(context, environment.newText(textSend));
                    }
                } catch (Exception e) {
                    environment.getMessageSender().onError(e, "unicode", context, false);
                }
            });
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
