package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.core.MessageInfo;
import io.github.nickid2018.koishibot.core.MessageManager;
import io.github.nickid2018.koishibot.util.WebUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.Image;
import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class LaTeXResolver extends MessageResolver {

    private final Transcoder transcoder;
    private final Transcoder alphaTranscoder;

    public LaTeXResolver() {
        super("~latex");
        transcoder = new PNGTranscoder();
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 0.1f);
        transcoder.addTranscodingHint(ImageTranscoder.KEY_BACKGROUND_COLOR, Color.WHITE);
        alphaTranscoder = new PNGTranscoder();
        alphaTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 0.1f);
    }

    @Override
    public boolean needAt() {
        return false;
    }

    @Override
    public boolean groupOnly() {
        return false;
    }

    @Override
    public boolean resolveInternal(String key, MessageInfo info) {
        KoishiBotMain.INSTANCE.executor.execute(() -> {
            String latex = key;
            Transcoder use = transcoder;
            if (latex.startsWith("-a") || latex.startsWith("-A")) {
                use = alphaTranscoder;
                latex = latex.substring(2);
            }
            latex = latex.trim();
            try {
                String data = WebUtil.fetchDataInText(new HttpGet("https://math.vercel.app/?from=" + WebUtil.encode(latex)));
                Document document = Jsoup.parse(data);
                Elements errors = document.getElementsByAttribute("data-mjx-error");
                if (errors.size() > 0)
                    throw new IOException(errors.get(0).attr("data-mjx-error"));
                TranscoderInput input = new TranscoderInput(new ReaderInputStream(new StringReader(data), StandardCharsets.UTF_8));
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                TranscoderOutput output = new TranscoderOutput(os);
                use.transcode(input, output);
                os.close();
                Image image = Contact.uploadImage(
                        KoishiBotMain.INSTANCE.botKoishi.getAsFriend(),
                        new ByteArrayInputStream(os.toByteArray()));
                info.sendMessageRecallable(image);
            } catch (Exception e) {
                MessageManager.onError(e, "latex", info, true);
            }
        });
        return true;
    }
}
