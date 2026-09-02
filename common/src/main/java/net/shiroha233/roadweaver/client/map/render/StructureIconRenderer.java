package net.shiroha233.roadweaver.client.map.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaver.core.model.MapLocationIcon;
import net.shiroha233.roadweaver.core.model.StructureInfo;

import java.util.Locale;

/**
 * 结构点图标的分类与扁平化绘制。
 */
public final class StructureIconRenderer {
    private static final ResourceLocation VILLAGER_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/villager.png");
    private static final int BORDER_COLOR = 0xE0101214;
    private static final int UNKNOWN_BACKGROUND = 0xE0282C31;
    private static final int UNKNOWN_TEXT = 0xFFF4F1E8;

    private StructureIconRenderer() {}

    public static void render(GuiGraphics graphics,
                              Font font,
                              StructureInfo structure,
                              int centerX,
                              int centerY,
                              int size,
                              int left,
                              int top,
                              int right,
                              int bottom) {
        int iconSize = Math.max(10, size);
        int drawX = centerX - iconSize / 2;
        int drawY = centerY - iconSize / 2;
        if (drawX > right || drawX + iconSize < left || drawY > bottom || drawY + iconSize < top) return;

        graphics.fill(drawX - 1, drawY - 1, drawX + iconSize + 1, drawY + iconSize + 1, BORDER_COLOR);
        String structureId = structure == null ? null : structure.structureId();
        MapLocationIcon icon = structure == null ? null : structure.mapIcon();
        if (icon != null && icon.kind() != MapLocationIcon.Kind.DEFAULT) {
            drawSemanticIcon(graphics, font, icon, centerX, drawX, drawY, iconSize);
            return;
        }
        if (isVillageStructure(structureId)) {
            drawVillagerFace(graphics, drawX, drawY, iconSize);
            return;
        }
        drawUnknown(graphics, font, centerX, drawX, drawY, iconSize);
    }

    public static void renderOutline(GuiGraphics graphics,
                                     int centerX,
                                     int centerY,
                                     int size,
                                     int color,
                                     int left,
                                     int top,
                                     int right,
                                     int bottom) {
        int half = Math.max(6, size / 2);
        int x0 = Math.max(left, centerX - half);
        int y0 = Math.max(top, centerY - half);
        int x1 = Math.min(right, centerX + half);
        int y1 = Math.min(bottom, centerY + half);
        if (x0 > x1 || y0 > y1) return;
        graphics.fill(x0, y0, x1 + 1, y0 + 1, color);
        graphics.fill(x0, y1, x1 + 1, y1 + 1, color);
        graphics.fill(x0, y0, x0 + 1, y1 + 1, color);
        graphics.fill(x1, y0, x1 + 1, y1 + 1, color);
    }

    private static boolean isVillageStructure(String structureId) {
        return structureId != null
                && !structureId.isBlank()
                && structureId.toLowerCase(Locale.ROOT).contains("village");
    }

    private static void drawVillagerFace(GuiGraphics graphics, int x, int y, int size) {
        RenderSystem.enableBlend();
        graphics.blit(VILLAGER_TEXTURE, x, y, size, size,
                8.0F, 8.0F,
                8, 10,
                64, 64);
        RenderSystem.disableBlend();
    }

    private static void drawUnknown(GuiGraphics graphics, Font font, int centerX, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, UNKNOWN_BACKGROUND);
        String text = "?";
        int textX = centerX - font.width(text) / 2;
        int textY = y + Math.max(0, (size - font.lineHeight) / 2);
        graphics.drawString(font, text, textX, textY, UNKNOWN_TEXT, false);
    }

    /** Draws compact, texture-free semantic icons so integrations need no client asset bundle. */
    private static void drawSemanticIcon(GuiGraphics graphics,
                                         Font font,
                                         MapLocationIcon icon,
                                         int centerX,
                                         int x,
                                         int y,
                                         int size) {
        int accent = icon.accentColor() == 0 ? MapLocationIcon.DEFAULT_ACCENT_COLOR : icon.accentColor();
        switch (icon.kind()) {
            case GYM -> drawPokeBall(graphics, font, icon.marker(), centerX, x, y, size, accent, false);
            case ELITE_FOUR -> drawPokeBall(graphics, font, "M", centerX, x, y, size, 0xFF7655B8, true);
            case STARTER -> drawPokedex(graphics, font, centerX, x, y, size, accent);
            case TOWN -> drawTown(graphics, font, icon.marker(), centerX, x, y, size, accent);
            case OUTPOST -> drawOutpost(graphics, font, icon.marker(), centerX, x, y, size, accent);
            case LANDMARK -> drawLandmark(graphics, font, icon.marker(), centerX, x, y, size, accent);
            case CAVE -> drawCave(graphics, font, icon.marker(), centerX, x, y, size, accent);
            case VILLAGE -> drawVillagerFace(graphics, x, y, size);
            default -> drawUnknown(graphics, font, centerX, x, y, size);
        }
    }

    private static void drawPokeBall(GuiGraphics graphics, Font font, String marker,
                                     int centerX, int x, int y, int size, int accent, boolean master) {
        int inset = Math.max(1, size / 6);
        int middle = y + size / 2;
        graphics.fill(x + inset, y, x + size - inset, y + 1, accent);
        graphics.fill(x + 1, y + inset, x + size - 1, middle, accent);
        graphics.fill(x, y + inset + 1, x + size, middle, accent);
        graphics.fill(x + 1, middle + 1, x + size - 1, y + size - inset, 0xFFF0F4F7);
        graphics.fill(x + inset, y + size - inset, x + size - inset, y + size, 0xFFF0F4F7);
        graphics.fill(x, middle - 1, x + size, middle + 1, 0xFF20242A);
        int knob = Math.max(3, size / 3);
        int knobX = centerX - knob / 2;
        graphics.fill(knobX - 1, middle - knob / 2 - 1, knobX + knob + 1, middle + knob / 2 + 1, 0xFF20242A);
        graphics.fill(knobX, middle - knob / 2, knobX + knob, middle + knob / 2, 0xFFE9EDF2);
        if (master) {
            int arc = Math.max(1, size / 5);
            graphics.fill(x + arc, y + arc, x + arc + 2, middle - 1, 0xFFE7D8FF);
            graphics.fill(x + size - arc - 2, y + arc, x + size - arc, middle - 1, 0xFFE7D8FF);
        }
        drawMarker(graphics, font, marker, x + size - Math.max(7, size / 3), y + size - Math.max(7, size / 3), Math.max(7, size / 3), accent);
    }

    private static void drawPokedex(GuiGraphics graphics, Font font, int centerX, int x, int y, int size, int accent) {
        graphics.fill(x + 1, y, x + size - 1, y + size, accent);
        graphics.fill(x + 2, y + Math.max(2, size / 4), x + size - 2, y + size - 2, 0xFF3B5665);
        int screen = Math.max(4, size / 2);
        int screenX = centerX - screen / 2;
        graphics.fill(screenX, y + Math.max(3, size / 3), screenX + screen, y + Math.max(3, size / 3) + screen / 2, 0xFFB9EDF1);
        drawCentered(graphics, font, "D", centerX, y + size - font.lineHeight - 1, 0xFFFFFFFF);
    }

    private static void drawTown(GuiGraphics graphics, Font font, String marker, int centerX, int x, int y, int size, int accent) {
        int roof = Math.max(3, size / 3);
        graphics.fill(x + roof, y, x + size - roof, y + 2, accent);
        graphics.fill(x + roof - 2, y + 2, x + size - roof + 2, y + 4, accent);
        graphics.fill(x + 2, y + 4, x + size - 2, y + size - 1, 0xFFF1E8D5);
        graphics.fill(centerX - 1, y + size / 2, centerX + 2, y + size - 1, 0xFF72523B);
        drawMarker(graphics, font, marker, x + size - Math.max(7, size / 3), y + size - Math.max(7, size / 3), Math.max(7, size / 3), accent);
    }

    private static void drawOutpost(GuiGraphics graphics, Font font, String marker, int centerX, int x, int y, int size, int accent) {
        graphics.fill(centerX - 1, y + 1, centerX + 1, y + size - 1, 0xFFF1E8D5);
        graphics.fill(centerX + 1, y + 2, x + size - 1, y + size / 2, accent);
        drawMarker(graphics, font, marker, x, y + size - Math.max(7, size / 3), Math.max(7, size / 3), accent);
    }

    private static void drawLandmark(GuiGraphics graphics, Font font, String marker, int centerX, int x, int y, int size, int accent) {
        int half = size / 2;
        for (int row = 0; row < size; row++) {
            int inset = Math.abs(half - row) / 2;
            graphics.fill(x + inset, y + row, x + size - inset, y + row + 1, accent);
        }
        drawMarker(graphics, font, marker, x + size - Math.max(7, size / 3), y + size - Math.max(7, size / 3), Math.max(7, size / 3), accent);
    }

    private static void drawCave(GuiGraphics graphics, Font font, String marker, int centerX, int x, int y, int size, int accent) {
        graphics.fill(x + 1, y + size / 3, x + size - 1, y + size, accent);
        graphics.fill(x + size / 4, y + 1, x + size - size / 4, y + size, accent);
        graphics.fill(x + size / 3, y + size / 2, x + size - size / 3, y + size, 0xFF1C2028);
        drawMarker(graphics, font, marker, x + size - Math.max(7, size / 3), y + size - Math.max(7, size / 3), Math.max(7, size / 3), accent);
    }

    private static void drawMarker(GuiGraphics graphics, Font font, String marker, int x, int y, int size, int color) {
        if (marker == null || marker.isBlank()) return;
        String text = marker.substring(0, 1).toUpperCase(Locale.ROOT);
        graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF101214);
        graphics.fill(x, y, x + size, y + size, color);
        drawCentered(graphics, font, text, x + size / 2, y + Math.max(0, (size - font.lineHeight) / 2), 0xFF101214);
    }

    private static void drawCentered(GuiGraphics graphics, Font font, String text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }
}
