package io.github.nickid2018.koishibot.util;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.util.value.MutableInt;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ImageRenderer {

    public static Font IMAGE_FONT;
    public static Font IMAGE_FONT_BOLD;

    public static void loadImageSettings(JsonObject settingsRoot) {
        IMAGE_FONT = new Font(null, Font.PLAIN, 20);
        IMAGE_FONT_BOLD = new Font(null, Font.BOLD, 20);
        JsonUtil.getData(settingsRoot, "image", JsonObject.class).ifPresent(image -> {
            IMAGE_FONT = new Font(JsonUtil.getStringOrNull(image, "family"), Font.PLAIN,
                    JsonUtil.getIntOrZero(image, "size"));
            IMAGE_FONT_BOLD = new Font(JsonUtil.getStringOrNull(image, "family"), Font.BOLD,
                    JsonUtil.getIntOrZero(image, "size"));
        });
    }

    public static BufferedImage renderMap(Map<String, String> map, String key, String value) {
        return renderMap(map, key, value, Alignment.CENTER, Alignment.CENTER);
    }

    public static BufferedImage renderMap(Map<String, String> map, String key, String value,
                                          Alignment keyAlign, Alignment valueAlign) {
        Font font = IMAGE_FONT;
        Font fontBold = IMAGE_FONT_BOLD;

        int margin = font.getSize() / 2;
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, false);

        Map<String, String> sorted = new TreeMap<>(map);

        List<String> names = new ArrayList<>(sorted.keySet());
        List<String> values = new ArrayList<>(sorted.values());

        List<Rectangle2D> nameBound = names.stream().map(str -> font.getStringBounds(str, frc)).toList();
        List<Rectangle2D> valueBound = values.stream().map(str -> font.getStringBounds(str, frc)).toList();

        int keyLength = (int) fontBold.getStringBounds(key, frc).getWidth();
        int valLength = (int) fontBold.getStringBounds(value, frc).getWidth();
        int nameLength = (int) (Math.max(nameBound.stream().mapToDouble(Rectangle2D::getWidth)
                .max().orElse(0), keyLength) + margin * 2);
        int valueLength = (int) (Math.max(valueBound.stream().mapToDouble(Rectangle2D::getWidth)
                .max().orElse(0), valLength) + margin * 2);

        int charHeight = font.getSize();
        int cellHeight = charHeight + margin * 2;

        BufferedImage image = new BufferedImage(
                nameLength + valueLength, cellHeight * (map.size() + 1), BufferedImage.TYPE_INT_BGR);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2.setColor(new Color(183, 201, 231));
        g2.fillRect(0, 0, image.getWidth(), cellHeight);
        g2.setColor(new Color(226, 224, 227));
        for (int i = 0; i < map.size() / 2; i++)
            g2.fillRect(0, cellHeight * (i * 2 + 2), image.getWidth(), cellHeight);
        g2.setColor(Color.WHITE);
        g2.drawLine(nameLength, 0, nameLength, image.getHeight());

        g2.setColor(Color.BLACK);
        g2.setFont(fontBold);
        g2.drawString(key, (nameLength - keyLength) / 2, charHeight + margin);
        g2.drawString(value, nameLength + (valueLength - valLength) / 2, charHeight + margin);

        g2.setFont(font);
        for (int i = 0; i < map.size(); i++) {
            int y = charHeight + margin + cellHeight * (i + 1);
            g2.drawString(names.get(i),
                    keyAlign.compute(nameLength, (int) nameBound.get(i).getWidth(), margin), y);
            g2.drawString(values.get(i),
                    nameLength + valueAlign.compute(valueLength, (int) valueBound.get(i).getWidth(), margin), y);
        }

        return image;
    }

    public static BufferedImage renderText(String title, List<String> lines) {
        Font font = IMAGE_FONT;
        Font fontBold = IMAGE_FONT_BOLD;

        int margin = font.getSize() / 2;
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, false);

        List<Rectangle2D> lineBound = lines.stream().map(str -> font.getStringBounds(str, frc))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        lineBound.add(fontBold.getStringBounds(title, frc));

        int width = (int) lineBound.stream().mapToDouble(RectangularShape::getWidth).max().getAsDouble() + 2 * margin;
        int charHeight = font.getSize();
        int height = fontBold.getSize() + charHeight * lines.size() + 2 * margin * (lines.size() + 1);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2.setColor(Color.BLACK);

        MutableInt y = new MutableInt(margin + fontBold.getSize());
        g2.setFont(fontBold);
        g2.drawString(title, margin, y.getValue());

        g2.setFont(font);
        lines.forEach(line -> {
            y.setValue(y.getValue() + charHeight + 2 * margin);
            g2.drawString(line, margin, y.getValue());
        });

        return image;
    }

    private interface AlignmentComputer {

        int compute(int cell, int strW, int margin);
    }

    public enum Alignment implements AlignmentComputer {

        LEFT {
            @Override
            public int compute(int cell, int strW, int margin) {
                return margin;
            }
        },
        CENTER {
            @Override
            public int compute(int cell, int strW, int margin) {
                return (cell - strW) / 2;
            }
        },
        RIGHT {
            @Override
            public int compute(int cell, int strW, int margin) {
                return cell - margin - strW;
            }
        }
    }
}
