package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.web.WebUtil;
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

@ResolverName("latex")
@Syntax(syntax = "~latex [LaTeX表达式]", help = "渲染LaTeX公式")
@Syntax(syntax = "~latex-a [LaTeX表达式]", help = "渲染LaTeX公式，使用透明背景")
public class LaTeXResolver extends MessageResolver {

    private final Transcoder transcoder;
    private final Transcoder alphaTranscoder;

    public LaTeXResolver() {
        super("~latex");
        transcoder = new PNGTranscoder();
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 0.02f);
        transcoder.addTranscodingHint(ImageTranscoder.KEY_BACKGROUND_COLOR, Color.WHITE);
        alphaTranscoder = new PNGTranscoder();
        alphaTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 0.02f);
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        AsyncUtil.execute(() -> {
            String latex = key;
            String[] split = latex.split(" ", 2);
            Transcoder use = transcoder;
            if (split[0].equalsIgnoreCase("-a") && split.length == 2) {
                use = alphaTranscoder;
                latex = split[1];
            }
            latex = latex.trim();
            try {
                String data = WebUtil.fetchDataInText(new HttpGet("https://latex2img.i207m.top/?from=" + WebUtil.encode(latex)));
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
