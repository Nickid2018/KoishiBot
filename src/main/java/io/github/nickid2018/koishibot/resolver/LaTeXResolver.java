package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.core.MessageInfo;
import io.github.nickid2018.koishibot.core.MessageManager;
import net.mamoe.mirai.contact.Contact;
import org.scilab.forge.jlatexmath.DefaultTeXFont;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;
import org.scilab.forge.jlatexmath.cyrillic.CyrillicRegistration;
import org.scilab.forge.jlatexmath.greek.GreekRegistration;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class LaTeXResolver extends MessageResolver {

    public LaTeXResolver() {
        super("~latex");
        DefaultTeXFont.registerAlphabet(new CyrillicRegistration());
        DefaultTeXFont.registerAlphabet(new GreekRegistration());
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
                List<BufferedImage> images = new ArrayList<>();
                for (String line : latex.split("\n")) {
                    if (!line.isEmpty()) {
                        TeXFormula formula = new TeXFormula(latex);
                        TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, 40);
                        icon.setInsets(new Insets(5, 5, 5, 5));

                        BufferedImage image = new BufferedImage(
                                icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_BGR);
                        Graphics2D g2 = image.createGraphics();
                        g2.setColor(Color.WHITE);
                        g2.fillRect(0, 0, icon.getIconWidth(), icon.getIconHeight());
                        g2.setColor(Color.BLACK);
                        icon.paintIcon(null, g2, 0, 0);
                        images.add(image);
                    }
                }

                int height = images.stream().mapToInt(BufferedImage::getHeight).sum();
                int width = images.stream().mapToInt(BufferedImage::getWidth).max().getAsInt();

                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
                Graphics2D g2 = image.createGraphics();

                int now = 0;
                for (BufferedImage imageNow : images) {
                    g2.drawImage(imageNow, 0, now, null);
                    now += imageNow.getHeight();
                }

                ByteArrayOutputStream boas = new ByteArrayOutputStream();
                ImageIO.write(image, "png", boas);

                info.sendMessageWithQuote(Contact.uploadImage(
                        KoishiBotMain.INSTANCE.botKoishi.getAsFriend(), new ByteArrayInputStream(boas.toByteArray())));
            } catch (Exception e) {
                MessageManager.onError(e, "无法转换到LaTeX图像", info, true);
            }
        });
        return true;
    }
}
