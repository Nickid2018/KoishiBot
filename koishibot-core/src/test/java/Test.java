import com.google.gson.JsonObject;
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

public class Test {

    public static void main(String[] args) throws Exception {
        int codepoint = Integer.parseInt("25E49", 16);
        String charStr = new String(Character.toChars(codepoint));
        HttpGet get = new HttpGet("https://zi.tools/api/zi/" + WebUtil.encode(charStr));
        HttpGet get2 = new HttpGet("https://unicode-table.com/cn/%x/".formatted(codepoint));

        // --------------------
        Document doc = Jsoup.parse(WebUtil.fetchDataInText(get2));
        Element element = doc.getElementsByClass("symbol-tabs__tabs-content").get(0);
        Elements elements = element.children();
        if (elements.size() != 3) {
            //...
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
            System.out.println(builder);
        }
        //--------------

        JsonObject object = WebUtil.fetchDataInJson(get).getAsJsonObject();
        String svg = JsonUtil.getStringInPathOrNull(object, "font." + charStr);
        if (svg == null) {
            //...
        } else {
            String made = """
                    <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.0//EN" "http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd">
                    <svg xmlns="http://www.w3.org/2000/svg" width="200" height="240" viewBox="0 -46 200 240">
                        <path fill="currentColor" d="%s" />
                    </svg>
                    """.formatted(svg);
            Transcoder transcoder = new PNGTranscoder();
            transcoder.addTranscodingHint(ImageTranscoder.KEY_BACKGROUND_COLOR, Color.WHITE);
            TranscoderInput input = new TranscoderInput(new StringReader(made));
            File temp = new File("D:\\a.png");
            try (FileOutputStream os = new FileOutputStream(temp)) {
                TranscoderOutput output = new TranscoderOutput(os);
                transcoder.transcode(input, output);
            }
        }
    }
}
