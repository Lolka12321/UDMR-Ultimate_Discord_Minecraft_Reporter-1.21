package com.reportsystem.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})>(.+?)</gradient>", Pattern.CASE_INSENSITIVE);

    /**
     * Парсит текст с градиентами используя Adventure API
     */
    public static Component parseColors(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        try {
            // Сначала обрабатываем градиенты через Adventure
            Component result = processGradientsAdvanced(text);
            return result;

        } catch (Exception e) {
            // Фоллбэк - обычная обработка
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }
    }

    /**
     * Обрабатывает текст с градиентами используя Adventure TextColor
     */
    private static Component processGradientsAdvanced(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);

        // Если градиентов нет - просто парсим как legacy
        if (!matcher.find()) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }

        // Сбрасываем matcher для повторного использования
        matcher.reset();

        Component result = Component.empty();
        int lastEnd = 0;

        while (matcher.find()) {
            // Добавляем текст до градиента
            if (matcher.start() > lastEnd) {
                String before = text.substring(lastEnd, matcher.start());
                result = result.append(LegacyComponentSerializer.legacyAmpersand().deserialize(before));
            }

            // Создаём градиент
            String startHex = matcher.group(1);
            String endHex = matcher.group(2);
            String content = matcher.group(3);

            Component gradient = createGradientComponent(content, startHex, endHex);
            result = result.append(gradient);

            lastEnd = matcher.end();
        }

        // Добавляем текст после последнего градиента
        if (lastEnd < text.length()) {
            String after = text.substring(lastEnd);
            result = result.append(LegacyComponentSerializer.legacyAmpersand().deserialize(after));
        }

        return result;
    }

    /**
     * Создаёт градиентный Component
     */
    private static Component createGradientComponent(String text, String startHex, String endHex) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // Убираем форматирование
        String cleanText = text.replaceAll("§[0-9a-fk-orx]", "").replaceAll("&[0-9a-fk-or]", "");
        int length = cleanText.length();

        if (length <= 1) {
            TextColor color = TextColor.fromHexString("#" + startHex);
            return Component.text(cleanText, color);
        }

        try {
            // Парсим цвета
            TextColor startColor = TextColor.fromHexString("#" + startHex);
            TextColor endColor = TextColor.fromHexString("#" + endHex);

            if (startColor == null || endColor == null) {
                return Component.text(cleanText);
            }

            // Получаем RGB значения
            int startR = startColor.red();
            int startG = startColor.green();
            int startB = startColor.blue();

            int endR = endColor.red();
            int endG = endColor.green();
            int endB = endColor.blue();

            Component result = Component.empty();

            // Создаём градиент посимвольно
            for (int i = 0; i < length; i++) {
                float ratio = (float) i / (length - 1);

                int r = (int) (startR + (endR - startR) * ratio);
                int g = (int) (startG + (endG - startG) * ratio);
                int b = (int) (startB + (endB - startB) * ratio);

                TextColor color = TextColor.color(r, g, b);
                result = result.append(Component.text(String.valueOf(cleanText.charAt(i)), color));
            }

            return result;

        } catch (Exception e) {
            return Component.text(cleanText);
        }
    }
}