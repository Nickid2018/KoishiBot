package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.WebUtil;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

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
    public boolean resolveInternal(String key, MessageContext context, Pattern pattern, Environment environment) {
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
                environment.getMessageSender().sendMessageRecallable(context,
                        environment.newImage(new ByteArrayInputStream(os.toByteArray())));
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "latex", context, true);
            }
        });
        return true;
    }
}
