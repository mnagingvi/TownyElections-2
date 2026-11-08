package com.townyelections.night;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.Random;

/**
 * Party-coloured fireworks for the Election Night winner reveal.
 *
 * <p>Fireworks are launched a few blocks above and around a viewer so the
 * celebration happens wherever that resident is standing, rather than at a town
 * spawn that may be unloaded or far away. Only chunks that are already loaded
 * are used, so the show can never force-load the world.
 */
public final class FireworkCelebration {

    private static final Color[] FALLBACK_COLORS = {
            Color.fromRGB(0xFF5555),
            Color.fromRGB(0xFFAA00),
            Color.fromRGB(0x55FF55),
            Color.fromRGB(0x55FFFF),
            Color.fromRGB(0x5599FF),
            Color.fromRGB(0xFF55FF)
    };

    private FireworkCelebration() {
    }

    /** Launch {@code count} fireworks above a player, coloured from a party colour code. */
    public static void launch(org.bukkit.entity.Player viewer, String colorCode, int count, Random random) {
        if (viewer == null || count <= 0) {
            return;
        }
        Location origin = viewer.getLocation();
        World world = viewer.getWorld();
        if (world == null) {
            return;
        }
        Color primary = colorFor(colorCode, random);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 1.5 + random.nextDouble() * 4.5;
            double x = origin.getX() + Math.cos(angle) * radius;
            double z = origin.getZ() + Math.sin(angle) * radius;
            double y = origin.getY() + 1.0 + random.nextDouble() * 4.0;
            Location target = new Location(world, x, y, z);
            if (!world.isChunkLoaded(target.getBlockX() >> 4, target.getBlockZ() >> 4)) {
                continue;
            }
            spawn(world, target, primary, random);
        }
    }

    private static void spawn(World world, Location location, Color primary, Random random) {
        try {
            Firework firework = world.spawn(location, Firework.class);
            FireworkMeta meta = firework.getFireworkMeta();
            FireworkEffect.Builder effect = FireworkEffect.builder()
                    .with(random.nextBoolean() ? FireworkEffect.Type.BALL_LARGE : FireworkEffect.Type.BURST)
                    .withColor(primary)
                    .withFade(Color.WHITE)
                    .withTrail();
            if (random.nextInt(3) == 0) {
                effect.withFlicker();
            }
            meta.addEffect(effect.build());
            meta.setPower(1);
            firework.setFireworkMeta(meta);
        } catch (Throwable ignored) {
            // A single failed firework should never break the show.
        }
    }

    /**
     * Convert a party colour code ({@code &c}, {@code &#aabbcc}) into an RGB
     * colour, falling back to a bright palette when the party has no colour.
     */
    public static Color colorFor(String colorCode, Random random) {
        if (colorCode != null && colorCode.startsWith("&#") && colorCode.length() >= 8) {
            try {
                return Color.fromRGB(Integer.parseInt(colorCode.substring(2, 8), 16));
            } catch (NumberFormatException ignored) {
                // Fall through to the palette.
            }
        }
        if (colorCode != null && !colorCode.isEmpty()) {
            char code = Character.toLowerCase(colorCode.charAt(colorCode.length() - 1));
            Color color = switch (code) {
                case '0' -> Color.fromRGB(0x404040);
                case '1' -> Color.fromRGB(0x0000AA);
                case '2' -> Color.fromRGB(0x00AA00);
                case '3' -> Color.fromRGB(0x00AAAA);
                case '4' -> Color.fromRGB(0xAA0000);
                case '5' -> Color.fromRGB(0xAA00AA);
                case '6' -> Color.fromRGB(0xFFAA00);
                case '7' -> Color.fromRGB(0xAAAAAA);
                case '8' -> Color.fromRGB(0x555555);
                case '9' -> Color.fromRGB(0x5555FF);
                case 'a' -> Color.fromRGB(0x55FF55);
                case 'b' -> Color.fromRGB(0x55FFFF);
                case 'c' -> Color.fromRGB(0xFF5555);
                case 'd' -> Color.fromRGB(0xFF55FF);
                case 'e' -> Color.fromRGB(0xFFFF55);
                case 'f' -> Color.fromRGB(0xFFFFFF);
                default -> null;
            };
            if (color != null) {
                return color;
            }
        }
        return FALLBACK_COLORS[random.nextInt(FALLBACK_COLORS.length)];
    }
}
