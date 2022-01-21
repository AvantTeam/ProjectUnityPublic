package unity.tools.proc;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.gen.*;
import unity.gen.*;
import unity.tools.*;
import unity.tools.GenAtlas.*;
import unity.type.*;
import unity.util.*;

import java.util.concurrent.*;

import static mindustry.Vars.*;
import static unity.tools.Tools.*;

/**
 * A processor to generate unit sprites such as:
 * <ul>
 *     <li> Separate outline regions. </li>
 *     <li> Override outline regions, such as leg or joint sprites. </li>
 *     <li> {@code -full} icons. </li>
 *     <li> Weapons, rotors, tentacles, and other decoration outline regions. </li>
 *     <li> Rotor shade and ghost sprites. </li>
 *     <li> Wreck regions. </li>
 * </ul>
 * @author GlennFolker
 */
public class UnitProcessor implements Processor{
    private final ObjectSet<String> outlined = new ObjectSet<>();

    private boolean outline(String region){
        synchronized(outlined){
            return outlined.add(region);
        }
    }

    @Override
    @SuppressWarnings("SuspiciousNameCombination") // sus
    public void process(ExecutorService exec){
        content.units().each(type -> type instanceof UnityUnitType && !type.isHidden(), (UnityUnitType type) -> submit(exec, type.name, () -> {
            init(type);
            load(type);

            float scl = Draw.scl / 4f;

            Seq<String> optional = Seq.with("-joint", "-joint-base", "-leg-back", "-leg-base-back", "-foot");
            Boolf<GenRegion> opt = r -> !optional.contains(e -> r.name.contains(e)) || r.found();

            Cons3<GenRegion, String, Pixmap> add = (relative, name, pixmap) -> {
                if(!relative.found()) throw new IllegalArgumentException("Cannot use a non-existent region as a relative point: " + relative);

                GenRegion reg = new GenRegion(name, pixmap);
                reg.relativePath = relative.relativePath;
                reg.save();
            };

            Func<TextureRegion, TextureRegion> outliner = t -> {
                if(!(t instanceof GenRegion at)) return t;
                if(opt.get(at) && outline(at.name)){
                    GenRegion reg = new GenRegion(at.name, Pixmaps.outline(new PixmapRegion(at.pixmap()), type.outlineColor, type.outlineRadius));
                    reg.relativePath = at.relativePath;
                    reg.save();

                    return reg;
                }else{
                    return atlas.find(at.name);
                }
            };

            Cons2<TextureRegion, String> outlSeparate = (t, suffix) -> {
                if(t instanceof GenRegion at && opt.get(at)){
                    GenRegion reg = new GenRegion(at.name + "-" + suffix, Pixmaps.outline(new PixmapRegion(at.pixmap()), type.outlineColor, type.outlineRadius));
                    reg.relativePath = at.relativePath;
                    reg.save();
                }
            };

            Unit unit = type.constructor.get();

            if(unit instanceof Legsc){
                outliner.get(type.jointRegion);
                outliner.get(type.footRegion);
                outliner.get(type.legBaseRegion);
                outliner.get(type.baseJointRegion);
                outliner.get(type.legRegion);
            }

            if(unit instanceof Mechc) outliner.get(type.legRegion);

            Pixmap icon = Pixmaps.outline(new PixmapRegion(conv(type.region).pixmap()), type.outlineColor, type.outlineRadius);
            add.get(conv(type.region), type.name + "-outline", icon.copy());

            icon.draw(Pixmaps.outline(new PixmapRegion(conv(type.region).pixmap()), type.outlineColor, type.outlineRadius), true);

            if(unit instanceof Mechc){
                GraphicUtils.drawCenter(icon, conv(type.baseRegion).pixmap());
                GraphicUtils.drawCenter(icon, conv(type.legRegion).pixmap());

                Pixmap flip = conv(type.legRegion).pixmap().flipX();
                GraphicUtils.drawCenter(icon, flip);
                flip.dispose();

                icon.draw(conv(type.region).pixmap(), true);
            }

            for(var weapon : type.weapons){
                if(weapon.name.isEmpty()) continue;

                GenRegion reg = conv(weapon.region);
                add.get(reg, weapon.name + "-outline", Pixmaps.outline(new PixmapRegion(reg.pixmap()), type.outlineColor, type.outlineRadius));

                // TODO since the old `bottomWeapons` are removed and now replaced with layer offsets, ...rework this.
                weapon.load();
            }

            icon.draw(conv(type.region).pixmap(), true);
            int baseColor = Color.valueOf("ffa665").rgba();

            Pixmap baseCell = conv(type.cellRegion).pixmap();
            Pixmap cell = new Pixmap(type.cellRegion.width, type.cellRegion.height);
            cell.each((x, y) -> cell.setRaw(x, y, Color.muli(baseCell.getRaw(x, y), baseColor)));

            icon.draw(cell, icon.width / 2 - cell.width / 2, icon.height / 2 - cell.height / 2, true);

            for(var weapon : type.weapons){
                if(weapon.name.isEmpty()) continue;

                GenRegion wepReg = weapon.top ? atlas.find(weapon.name + "-outline") : conv(weapon.region);
                Pixmap pix = wepReg.pixmap().copy();

                if(weapon.flipSprite){
                    Pixmap newPix = pix.flipX();
                    pix.dispose();
                    pix = newPix;
                }

                icon.draw(pix,
                    (int)(weapon.x / scl + icon.width / 2f - weapon.region.width / 2f),
                    (int)(-weapon.y / scl + icon.height / 2f - weapon.region.height / 2f),
                    true
                );

                if(weapon.mirror){
                    Pixmap mirror = pix.flipX();

                    icon.draw(mirror,
                        (int)(-weapon.x / scl + icon.width / 2f - weapon.region.width / 2f),
                        (int)(-weapon.y / scl + icon.height / 2f - weapon.region.height / 2f),
                        true
                    );

                    mirror.dispose();
                }

                pix.dispose();
                weapon.load();
            }

            add.get(conv(type.region), type.name + "-full", icon);

            // Only generate wreck regions if it is larger than zenith
            if(type.hitSize > UnitTypes.zenith.hitSize){
                Rand rand = new Rand();
                rand.setSeed(type.name.hashCode());

                int splits = 3;
                float degrees = rand.random(360f);
                float offsetRange = Math.max(icon.width, icon.height) * 0.15f;
                Vec2 offset = new Vec2(1, 1).rotate(rand.random(360f)).setLength(rand.random(0, offsetRange)).add(icon.width / 2f, icon.height / 2f);

                Pixmap[] wrecks = new Pixmap[splits];
                for(int i = 0; i < wrecks.length; i++){
                    wrecks[i] = new Pixmap(icon.width, icon.height);
                }

                VoronoiNoise vn = new VoronoiNoise(type.id, true);

                icon.each((x, y) -> {
                    boolean rValue = Math.max(Ridged.noise2d(1, x, y, 3, 1f / (20f + icon.width / 8f)), 0) > 0.16f;
                    boolean vval = vn.noise(x, y, 1f / (14f + icon.width/40f)) > 0.47;

                    float dst =  offset.dst(x, y);
                    float noise = (float)Noise.rawNoise(dst / (9f + icon.width / 70f)) * (60 + icon.width / 30f);
                    int section = (int)Mathf.clamp(Mathf.mod(offset.angleTo(x, y) + noise + degrees, 360f) / 360f * splits, 0, splits - 1);
                    if(!vval) wrecks[section].setRaw(x, y, Color.muli(icon.getRaw(x, y), rValue ? 0.7f : 1f));
                });

                for(int i = 0; i < wrecks.length; i++){
                    add.get(conv(type.region), type.name + "-wreck" + i, wrecks[i]);
                }
            }
        }));
    }

    private int populateColorArray(int[] heightAverageColors, Pixmap bladeSprite, int halfHeight){
        int c1 = 0,
            c2 = 0;

        float hits = 0f;
        int length = 0;

        for(int y = halfHeight - 1; y >= 0; y--){
            for(int x = 0; x < bladeSprite.width; x++){
                int color = bladeSprite.get(x, y);

                if(SColor.a(color) > 0.01f){
                    hits++;
                    float prevhit = hits - 1;

                    c1 = SColor.set(
                        c1,
                        ((SColor.r(c1) * prevhit) + SColor.r(color)) / hits,
                        ((SColor.g(c1) * prevhit) + SColor.g(color)) / hits,
                        ((SColor.b(c1) * prevhit) + SColor.b(color)) / hits,
                        1f
                    );
                }
            }

            if(hits > 0f){
                length = Math.max(length, halfHeight - y);
                c2 = SColor.a(c1, 0f);
            }else{
                // Use color from previous row with alpha 0. This avoids alpha bleeding when interpolating later
                c1 = c2;
            }

            heightAverageColors[halfHeight - y] = c1;
            c1 = 0;
            hits = 0f;
        }

        heightAverageColors[length + 1] = SColor.a(heightAverageColors[length], 0f); // Set final entry to be fully transparent
        return length;
    }

    // Instead of ACTUALLY accounting for the insanity that is the variation of rotor configurations
    // including counter-rotating propellers and that jazz, number 4 will be used instead.
    private void drawRadial(Pixmap sprite, int[] colorTable, int tableLimit){
        float spriteCenter = 0.5f - (sprite.height >> 1);

        sprite.each((x, y) -> {
            // 0.5f is required since mathematically it'll put the position at an intersection between 4 pixels, since the sprites are even-sized
            float positionLength = Mathf.len(x + spriteCenter, y + spriteCenter);

            if(positionLength < tableLimit){
                int arrayIndex = Mathf.clamp((int)positionLength, 0, tableLimit);
                float a = Mathf.cos(Mathf.atan2(x + spriteCenter, y + spriteCenter) * 8) * 0.05f + 0.95f;
                a *= a;

                sprite.set(x, y, SColor.mul(
                    GraphicUtils.colorLerp(
                        colorTable[arrayIndex],
                        colorTable[arrayIndex + 1], positionLength % 1f
                    ), a, a, a, a * (1 - 0.5f / (tableLimit - positionLength + 0.5f)))
                );
            }else{
                sprite.set(x, y, 0);
            }
        });
    }

    // To help visualize the expected output of this algorithm:
    // - Divide the circle of the rotor's blade into rings, with a new ring every 4 pixels.
    // - Within each band exists a circumferential parallelogram, which the upper and bottom lines are offset differently.
    // - Entire parallelograms are offset as well.
    // The resulting drawing looks like a very nice swooshy hourglass. It must be anti-aliased afterwards.
    private void drawShade(Pixmap sprite, int length){
        float spriteCenter = 0.5f - (sprite.height >> 1);
        // Divide by 2 then round down to nearest even positive number. This array will be accessed by pairs, hence the even number size.
        float[] offsets = new float[length >> 2 & 0xEFFFFFFE];
        for(int i = 0; i < offsets.length; i++){
            // The output values of the noise functions from the noise class are awful that
            // every integer value always result in a 0. Offsetting by 0.5 results in delicious good noise.
            // The additional offset is only that the noise values close to origin make for bad output for the sprite.
            offsets[i] = (float)Noise.rawNoise(i + 2.5f);
        }

        sprite.each((x, y) -> {
            float positionLength = Mathf.len(x + spriteCenter, y + spriteCenter);

            int arrayIndex = Mathf.clamp((int)positionLength >> 2 & 0xEFFFFFFE, 0, offsets.length - 2);
            float offset = GraphicUtils.pythagoreanLerp(offsets[arrayIndex], offsets[arrayIndex + 1], (positionLength / 8f) % 1);

            float a = Mathf.sin(Mathf.atan2(x + spriteCenter, y + spriteCenter) + offset);
            a *= a; // Square the sine wave to make it all positive values
            a *= a; // Square sine again to thin out intervals of value increases
            a *= a; // Sine to the 8th power - Perfection

            // To maintain the geometric-sharpness, the resulting alpha fractional is rounded to binary integer.
            sprite.setRaw(x, y, SColor.construct(1f, 1f, 1f, Mathf.round(a) * Mathf.clamp(length - positionLength, 0f, 1f)));
        });
    }
}
