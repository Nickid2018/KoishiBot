package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.core.MessageInfo;
import io.github.nickid2018.koishibot.core.MessageManager;
import io.github.nickid2018.koishibot.util.WebUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.Image;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.w3c.dom.DOMImplementation;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LaTeXResolver extends MessageResolver {

    private final Transcoder transcoder;

    public LaTeXResolver() {
        super("~latex");
        transcoder = new PNGTranscoder();
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 0.1f);
    }

    @Override
    public boolean needAt() {
        return false;
    }

    @Override
    public boolean resolveInternal(String key, MessageInfo info) {
        KoishiBotMain.INSTANCE.executor.execute(() -> {
            String latex = key.trim();
            try {
                String data = "https://zh.wikipedia.org/w/api.php?action=parse&format=json&contentmodel=wikitext&text=";
                data += WebUtil.encode("<math chem>" + latex + "</math>");
                data = WebUtil.mirror(data);
                JsonObject object = WebUtil.fetchDataInJson(new HttpGet(data)).getAsJsonObject();
                String parsed = WebUtil.getDataInPathOrNull(object, "parse.text.*");
                if (parsed == null)
                    throw new IOException("API未返回任何内容");
                Document jsoup = Jsoup.parse(parsed);
                Elements error = jsoup.getElementsByClass("error texerror");
                if (error.size() != 0)
                    throw new IOException(error.get(0).ownText());
                Elements img = jsoup.getElementsByTag("img");
                if (img.size() == 0)
                    throw new IOException("无法找到LaTeX渲染结果");
                String str = WebUtil.fetchDataInPlain(new HttpGet(img.get(0).attr("src")));
                TranscoderInput input = new TranscoderInput(new ReaderInputStream(new StringReader(str), StandardCharsets.UTF_8));
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                TranscoderOutput output = new TranscoderOutput(os);
                transcoder.transcode(input, output);
                os.close();
                Image image = Contact.uploadImage(
                        KoishiBotMain.INSTANCE.botKoishi.getAsFriend(),
                        new ByteArrayInputStream(os.toByteArray()));
                info.sendMessageWithQuote(image);
            } catch (Exception e) {
                MessageManager.onError(e, "latex", info, true);
            }
        });
        return true;
    }
}
