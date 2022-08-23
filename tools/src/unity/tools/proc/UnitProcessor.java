package unity.tools.proc;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.noise.*;
import mindustry.gen.*;
import unity.gen.*;
import unity.tools.*;
import unity.tools.GenAtlas.*;
import unity.type.*;
import unity.type.weapons.*;
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
 * @author Drullkus
 * @author Anuke
 */
public class UnitProcessor implements Processor{
    private final ObjectSet<String> outlined = new ObjectSet<>();

    private boolean outline(String region){
        synchronized(outlined){
            return outlined.add(region);
        }
    }

    @Override
    @SuppressWarnings("SuspiciousNameCombination") // sus :flushed:
    public void process(ExecutorService exec){
        content.units().each(type -> type instanceof UnityUnitType && !type.isHidden(), (UnityUnitType type) -> submit(exec, type.name, () -> {
            init(type);
            load(type);

            float scl = Draw.scl / 4f;

            Seq<String> optional = Seq.with("-joint", "-joint-base", "-leg-back", "-leg-base-back", "-foot");
            Boolf<GenRegion> opt = r -> !optional.contains(e -> r.name.contains(e)) || r.found();

            Func3<GenRegion, String, Pixmap, GenRegion> add = (relative, name, pixmap) -> {
                if(!relative.found()) throw new IllegalArgumentException("Cannot use a non-existent region as a relative point: " + relative);

                GenRegion reg = new GenRegion(name, pixmap);
                reg.relativePath = relative.relativePath;
                reg.save();
                return reg;
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

            if(unit instanceof Copterc){
                for(var rotor : type.rotors){
                    GenRegion region = conv(rotor.bladeRegion);

                    outlSeparate.get(region, "outline");
                    outliner.get(rotor.topRegion);

                    if(atlas.has(rotor.name + "-blade-ghost") || !atlas.has(rotor.name + "-blade")){
                        rotor.load();
                        continue;
                    }

                    Pixmap bladeSprite = region.pixmap();

                    // This array is to be written in the order where colors at index 0 are located towards the center,
                    // and colors at the end of the array is located towards at the edge.
                    int[] heightAverageColors = new int[(bladeSprite.height >> 1) + 1]; // Go one extra so it becomes transparent especially if blade is full length
                    int bladeLength = populateColorArray(heightAverageColors, bladeSprite, bladeSprite.height >> 1);

                    Pixmap ghostSprite = new Pixmap(bladeSprite.height, bladeSprite.height);
                    drawRadial(ghostSprite, heightAverageColors, bladeLength);
                    add.get(region, rotor.name + "-blade-ghost", ghostSprite);

                    if(atlas.has(rotor.name + "-blade-shade")){
                        rotor.load();
                        continue;
                    }

                    Pixmap shadeSprite = new Pixmap(bladeSprite.height, bladeSprite.height);
                    drawShade(shadeSprite, bladeLength);
                    add.get(region, rotor.name + "-blade-shade", shadeSprite);

                    rotor.load();
                }
            }

            Pixmap icon = Pixmaps.outline(new PixmapRegion(conv(type.region).pixmap()), type.outlineColor, type.outlineRadius);
            add.get(conv(type.region), type.name, icon.copy());

            if(unit instanceof Mechc){
                GraphicUtils.drawCenter(icon, conv(type.baseRegion).pixmap());
                GraphicUtils.drawCenter(icon, conv(type.legRegion).pixmap());

                Pixmap flip = conv(type.legRegion).pixmap().flipX();
                GraphicUtils.drawCenter(icon, flip);
                flip.dispose();

                icon.draw(conv(type.region).pixmap(), true);
            }

            type.weapons.sort(w -> w.layerOffset);
            for(var weapon : type.weapons){
                GenRegion reg = conv(weapon.region);
                //TODO: outline regions are still needed for certain weapons, e.g. mech guns; look into this -Anuke
                Pixmap pix = weapon.name.isEmpty() ? null : add.get(reg, weapon.name, Pixmaps.outline(new PixmapRegion(reg.pixmap()), type.outlineColor, type.outlineRadius)).pixmap();
                if(pix != null && weapon.flipSprite) pix = pix.flipX();

                if(weapon instanceof MultiBarrelWeapon w){
                    if(pix != null){
                        icon.draw(pix,
                            (int)(weapon.x / scl + icon.width / 2f - pix.width / 2f),
                            (int)(-weapon.y / scl + icon.height / 2f - pix.height / 2f),
                            true
                        );
                    }

                    GenRegion barrelReg = conv(w.barrelRegion);
                    Pixmap barrelPix = add.get(barrelReg, barrelReg.name + "-outline", Pixmaps.outline(new PixmapRegion(barrelReg.pixmap()), type.outlineColor, type.outlineRadius)).pixmap(), pixFlip = barrelPix.flipX();

                    for(int i = w.flipSprite ? w.barrels - 1 : 0; w.flipSprite ? i >= 0 : i < w.barrels; i += Mathf.sign(!w.flipSprite)){
                        float offset = i * w.barrelSpacing - (w.barrels - 1) * w.barrelSpacing / 2f;
                        boolean s = (!w.mirrorBarrels || offset < 0) != w.flipSprite;

                        icon.draw(w.flipSprite ^ s ? pixFlip : barrelPix,
                            (int)((weapon.x + offset) / scl + icon.width / 2f - w.barrelRegion.width / 2f),
                            (int)((-weapon.y - w.barrelOffset) / scl + icon.height / 2f - w.barrelRegion.width / 2f),
                            true
                        );
                    }

                    if(reg.found()){
                        icon.draw(reg.pixmap(),
                            (int)(weapon.x / scl + icon.width / 2f - pix.width / 2f),
                            (int)(-weapon.y / scl + icon.height / 2f - pix.height / 2f),
                            true
                        );
                    }

                    pixFlip.dispose();
                }else if(pix != null && weapon instanceof LimitedAngleWeapon w && !Mathf.equal(w.defaultAngle, 0f)){
                    PixmapRegion pixReg = new PixmapRegion(pix);
                    float mountX = (w.x / scl + icon.width / 2f) - 0.5f;
                    float mountY = (-w.y / scl + icon.height / 2f) - 0.5f;
                    float spriteX = pixReg.width / 2f - 0.5f;
                    float spriteY = pixReg.height / 2f - 0.5f;

                    float angle = w.defaultAngle * Mathf.sign(w.flipSprite);
                    float cos = Mathf.cosDeg(angle);
                    float sin = Mathf.sinDeg(angle);
                    icon.each((x, y) -> icon.setRaw(x, y, Pixmap.blend(icon.getRaw(x, y), GraphicUtils.getColor(pixReg,
                        spriteX + ((mountX - x) * cos + (mountY - y) * sin),
                        spriteY - ((mountX - x) * sin - (mountY - y) * cos)
                    ))));
                }else if(pix != null){
                    icon.draw(pix,
                        (int)(weapon.x / scl + icon.width / 2f - pix.width / 2f),
                        (int)(-weapon.y / scl + icon.height / 2f - pix.height / 2f),
                        true
                    );
                }

                if(pix != null && weapon.flipSprite) pix.dispose();
                weapon.load();
            }

            icon.draw(conv(type.region).pixmap(), true);
            int baseColor = Color.valueOf("ffa665").rgba();

            Pixmap baseCell = conv(type.cellRegion).pixmap();
            Pixmap cell = new Pixmap(type.cellRegion.width, type.cellRegion.height);
            cell.each((x, y) -> cell.setRaw(x, y, Color.muli(baseCell.getRaw(x, y), baseColor)));

            icon.draw(cell, icon.width / 2 - cell.width / 2, icon.height / 2 - cell.height / 2, true);

            for(var weapon : type.weapons){
                if(weapon.layerOffset < 0f) continue;

                //TODO: needs tweaking, may need to use different region for "top" weapons -Anuke
                Pixmap pix = weapon.name.isEmpty() ? null : conv(weapon.region).pixmap();
                if(pix != null && weapon.flipSprite) pix = pix.flipX();

                if(weapon instanceof MultiBarrelWeapon w){
                    if(pix != null){
                        icon.draw(pix,
                            (int)(weapon.x / scl + icon.width / 2f - pix.width / 2f),
                            (int)(-weapon.y / scl + icon.height / 2f - pix.height / 2f),
                            true
                        );
                    }

                    Pixmap barrelPix = conv(w.top ? w.barrelOutlineRegion : w.barrelRegion).pixmap(), pixFlip = barrelPix.flipX();

                    for(int i = w.flipSprite ? w.barrels - 1 : 0; w.flipSprite ? i >= 0 : i < w.barrels; i += Mathf.sign(!w.flipSprite)){
                        float offset = i * w.barrelSpacing - (w.barrels - 1) * w.barrelSpacing / 2f;
                        boolean s = (!w.mirrorBarrels || offset < 0) != w.flipSprite;

                        icon.draw(w.flipSprite ^ s ? pixFlip : barrelPix,
                            (int)((weapon.x + offset) / scl + icon.width / 2f - w.barrelRegion.width / 2f),
                            (int)((-weapon.y - w.barrelOffset) / scl + icon.height / 2f - w.barrelRegion.width / 2f),
                            true
                        );
                    }

                    if(weapon.region.found()){
                        Pixmap over = conv(weapon.region).pixmap();
                        icon.draw(over,
                            (int)(weapon.x / scl + icon.width / 2f - over.width / 2f),
                            (int)(-weapon.y / scl + icon.height / 2f - over.height / 2f),
                            true
                        );
                    }

                    pixFlip.dispose();
                }else if(pix != null && weapon instanceof LimitedAngleWeapon w && !Mathf.equal(w.defaultAngle, 0f)){
                    PixmapRegion pixReg = new PixmapRegion(pix);
                    float mountX = (w.x / scl + icon.width / 2f) - 0.5f;
                    float mountY = (-w.y / scl + icon.height / 2f) - 0.5f;
                    float spriteX = pixReg.width / 2f - 0.5f;
                    float spriteY = pixReg.height / 2f - 0.5f;

                    float angle = w.defaultAngle * Mathf.sign(w.flipSprite);
                    float cos = Mathf.cosDeg(angle);
                    float sin = Mathf.sinDeg(angle);
                    icon.each((x, y) -> icon.setRaw(x, y, Pixmap.blend(icon.getRaw(x, y), GraphicUtils.getColor(pixReg,
                        spriteX + ((mountX - x) * cos + (mountY - y) * sin),
                        spriteY - ((mountX - x) * sin - (mountY - y) * cos)
                    ))));
                }else if(pix != null){
                    icon.draw(pix,
                        (int)(weapon.x / scl + icon.width / 2f - pix.width / 2f),
                        (int)(-weapon.y / scl + icon.height / 2f - pix.height / 2f),
                        true
                    );
                }

                if(pix != null && weapon.flipSprite) pix.dispose();
            }

            if(unit instanceof Copterc){
                Pixmap propellers = new Pixmap(icon.width, icon.height);
                Pixmap tops = new Pixmap(icon.width, icon.height);

                for(var rotor : type.rotors){
                    Pixmap bladeSprite = conv(rotor.bladeRegion).pixmap();
                    PixmapRegion bladeRegion = new PixmapRegion(bladeSprite);

                    float bladeSeparation = 360f / rotor.bladeCount;

                    float propXCenter = (rotor.x / scl + icon.width / 2f) - 0.5f;
                    float propYCenter = (-rotor.y / scl + icon.height / 2f) - 0.5f;

                    float bladeSpriteXCenter = bladeSprite.width / 2f - 0.5f;
                    float bladeSpriteYCenter = bladeSprite.height / 2f - 0.5f;

                    for(int blade = 0; blade < rotor.bladeCount; blade++){
                        float deg = blade * bladeSeparation;
                        float cos = Mathf.cosDeg(deg);
                        float sin = Mathf.sinDeg(deg);

                        propellers.each((x, y) -> propellers.setRaw(x, y, Pixmap.blend(propellers.getRaw(x, y), GraphicUtils.getColor(bladeRegion,
                            ((propXCenter - x) * cos + (propYCenter - y) * sin) + bladeSpriteXCenter,
                            ((propXCenter - x) * sin - (propYCenter - y) * cos) + bladeSpriteYCenter
                        ))));
                    }

                    Pixmap topSprite = conv(rotor.topRegion).pixmap();
                    int topXCenter = (int)(rotor.x / scl + icon.width / 2f - topSprite.width / 2f);
                    int topYCenter = (int)(-rotor.y / scl + icon.height / 2f - topSprite.height / 2f);

                    tops.draw(topSprite, topXCenter, topYCenter, true);
                }

                Pixmap propOutlined = Pixmaps.outline(new PixmapRegion(propellers), type.outlineColor, type.outlineRadius);
                icon.draw(propOutlined, true);
                icon.draw(tops, true);

                propellers.dispose();
                tops.dispose();

                Pixmap payloadCell = new Pixmap(baseCell.width, baseCell.height);
                int cellCenterX = payloadCell.width / 2;
                int cellCenterY = payloadCell.height / 2;
                int propCenterX = propOutlined.width / 2;
                int propCenterY = propOutlined.height / 2;

                boolean collided = false;
                for(int x = 0; x < payloadCell.width; x++){
                    for(int y = 0; y < payloadCell.height; y++){
                        int cellX = x - cellCenterX;
                        int cellY = y - cellCenterY;

                        int base = baseCell.getRaw(x, y);
                        float alpha = SColor.a(propOutlined.get(cellX + propCenterX, cellY + propCenterY));
                        if(!collided && SColor.a(base) > 0f && alpha > 0f) collided = true;

                        payloadCell.setRaw(x, y, SColor.mul(base, 1f, 1f, 1f, 1f - alpha));
                    }
                }

                propOutlined.dispose();
                if(collided){
                    add.get(conv(type.region), type.name + "-cell-payload", payloadCell);
                }else{
                    payloadCell.dispose();
                }
            }

            add.get(conv(type.region), type.name + "-full", icon);

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

            VoronoiNoise voronoi = new VoronoiNoise(type.id, true);
            icon.each((x, y) -> {
                if(voronoi.noise(x, y, 1f / (14f + icon.width/40f)) <= 0.47d){
                    boolean rValue = Math.max(Ridged.noise2d(1, x, y, 3, 1f / (20f + icon.width / 8f)), 0) > 0.16f;

                    float dst =  offset.dst(x, y);
                    float noise = (float)Noise.rawNoise(dst / (9f + icon.width / 70f)) * (60 + icon.width / 30f);
                    wrecks[(int)Mathf.clamp(Mathf.mod(offset.angleTo(x, y) + noise + degrees, 360f) / 360f * splits, 0, splits - 1)].setRaw(x, y, Color.muli(icon.getRaw(x, y), rValue ? 0.7f : 1f));
                }
            });

            for(int i = 0; i < wrecks.length; i++) add.get(conv(type.region), type.name + "-wreck" + i, wrecks[i]);
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
                a *= a;

                sprite.setRaw(x, y, SColor.mul(
                    GraphicUtils.colorLerp(
                        colorTable[arrayIndex],
                        colorTable[arrayIndex + 1], positionLength % 1f
                    ), a, a, a, a * (1f - 0.5f / (tableLimit - positionLength + 0.5f)))
                );
            }else{
                sprite.setRaw(x, y, 0);
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
            offsets[i] = (float)Noise.rawNoise(i + 3.5d);
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
