package io.github.nickid2018.koishibot.resolver;

import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.core.MessageInfo;
import io.github.nickid2018.koishibot.core.MessageManager;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class QRCodeResolver extends MessageResolver {

    private static final Map<EncodeHintType, Object> HINTS = new HashMap<>();
    private static final MultiFormatWriter WRITER = new MultiFormatWriter();
    private static final MultiFormatReader READER = new MultiFormatReader();

    static {
        HINTS.put(EncodeHintType.CHARACTER_SET, "utf-8");
        HINTS.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        HINTS.put(EncodeHintType.MARGIN, 2);
    }

    public QRCodeResolver() {
        super("~qrcode");
    }

    @Override
    public boolean needAt() {
        return false;
    }

    @Override
    public boolean resolveInternal(String key, MessageInfo info) {
        key = key.trim();
        String[] data = key.split(" ", 2);
        switch (data[0].toLowerCase(Locale.ROOT)) {
            case "encode":
                if (data.length != 2)
                    return false;
                encode(data[1], info);
                return true;
            case "decode":
                return decode(info);
        }
        return false;
    }

    private void encode(String message, MessageInfo info) {
        KoishiBotMain.INSTANCE.executor.execute(() -> {
            try {
                BitMatrix matrix = WRITER.encode(message, BarcodeFormat.QR_CODE, 200, 200, HINTS);
                ByteArrayOutputStream boas = new ByteArrayOutputStream();
                BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_BGR);
                for (int x = 0; x < matrix.getWidth(); x++)
                    for (int y = 0; y < matrix.getHeight(); y++)
                        if (matrix.get(x, y))
                            image.setRGB(x, y, 0);
                        else
                            image.setRGB(x, y, 0xFFFFFFFF);
                ImageIO.write(image, "png", boas);
                Image output = Contact.uploadImage(
                        KoishiBotMain.INSTANCE.botKoishi.getAsFriend(), new ByteArrayInputStream(boas.toByteArray()));
                info.sendMessageRecallable(output);
            } catch (Exception e) {
                MessageManager.onError(e, "qrcode.encode", info, true);
            }
        });
    }

    private boolean decode(MessageInfo info) {
        Image image = null;
        for (Message message : info.data) {
            if (message instanceof Image) {
                image = (Image) message;
                break;
            }
        }
        if (image == null)
            return false;
        Image finalImage = image;
        KoishiBotMain.INSTANCE.executor.execute(() -> {
            try {
                String url = Image.queryUrl(finalImage);
                BufferedImage qrcode = ImageIO.read(new URL(url));
                RGBLuminanceSource source = new RGBLuminanceSource(qrcode.getWidth(), qrcode.getHeight(),
                        qrcode.getRGB(0, 0, qrcode.getWidth(), qrcode.getHeight(),
                                null, 0, qrcode.getWidth()));
                Result result = READER.decode(new BinaryBitmap(new HybridBinarizer(source)));
                info.sendMessageRecallable(MessageUtils.newChain(
                        new QuoteReply(info.data),
                        new PlainText(result.getText())
                ));
            } catch (NotFoundException e) {
                info.sendMessageRecallable(MessageUtils.newChain(
                        new QuoteReply(info.data),
                        new PlainText("图片内找不到有效的二维码")
                ));
            } catch (Exception e) {
                MessageManager.onError(e, "qrcode.decode", info, true);
            }
        });
        return true;
    }
}
