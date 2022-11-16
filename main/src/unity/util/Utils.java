package unity.util;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.Interp.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import arc.util.pooling.Pool.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.world.*;

import java.io.*;
import java.util.zip.*;

import static java.lang.Math.exp;
import static mindustry.Vars.*;

public final class Utils{
    public static final PowIn pow6In = new PowIn(6);
    public static final PowOut pow25Out = new PowOut(25);

    public static final float sqrtHalf = Mathf.sqrt(0.5f);

    public static float interp(float x,float x2,float t){
        return (float)(1-(1.0/(1+exp((t*2-1)/0.2f))))*(x2-x)+x;
    }
    public static float sqinterp(float x,float x2,float t){
        return (float)(t*t)*(x2-x)+x;
    }

    public static final Quat
        q1 = new Quat(),
        q2 = new Quat(),
        q3 = new Quat();

    // Temporary static fields to be used in lambdas.
    private static Posc result;
    private static float cdist;
    private static int idx;
    private static final Vec2 tV = new Vec2(), tV2 = new Vec2();
    private static final IntSet collidedBlocks = new IntSet();
    private static final Rect rect = new Rect(), rectAlt = new Rect(), hitRect = new Rect();
    private static final BoolGrid collideLineCollided = new BoolGrid();
    private static final IntSeq lineCast = new IntSeq(), lineCastNext = new IntSeq();
    private static final Seq<Hit> hitEffects = new Seq<>();
    private static Building tmpBuilding;
    private static Unit tmpUnit;
    private static boolean hit, hitB;

    private Utils(){
        throw new AssertionError();
    }

    public static void init(){
        Events.on(WorldLoadEvent.class, event -> collideLineCollided.resize(world.width(), world.height()));
    }

    /** @author EyeOfDarkness */
    public static Bullet nearestBullet(float x, float y, float range, Boolf<Bullet> boolf){
        result = null;
        cdist = range;
        Tmp.r1.setCentered(x, y, range * 2);
        Groups.bullet.intersect(Tmp.r1.x, Tmp.r1.y, Tmp.r1.width, Tmp.r1.height, b -> {
            float dst = b.dst(x, y);
            if(boolf.get(b) && b.within(x, y, range + b.hitSize) && (result == null || dst < cdist)){
                result = b;
                cdist = dst;
            }
        });

        return (Bullet)result;
    }

    /** @author EyeOfDarkness */
    public static float angleDistSigned(float a, float b){
        a = (a + 360f) % 360f;
        b = (b + 360f) % 360f;

        float d = Math.abs(a - b) % 360f;
        int sign = (a - b >= 0f && a - b <= 180f) || (a - b <= -180f && a - b >= -360f) ? 1 : -1;

        return (d > 180f ? 360f - d : d) * sign;
    }

    /** @author EyeOfDarkness */
    public static float angleDistSigned(float a, float b, float start){
        float dst = angleDistSigned(a, b);
        if(Math.abs(dst) > start){
            return dst > 0 ? dst - start : dst + start;
        }

        return 0f;
    }

    /** @author EyeOfDarkness */
    public static float angleDist(float a, float b){
        float d = Math.abs(a - b) % 360f;
        return (d > 180f ? 360f - d : d);
    }

    /** @author EyeOfDarkness */
    public static float clampedAngle(float angle, float relative, float limit){
        if(limit >= 180) return angle;
        if(limit <= 0) return relative;
        float dst = angleDistSigned(angle, relative);
        if(Math.abs(dst) > limit){
            float val = dst > 0 ? dst - limit : dst + limit;
            return (angle - val) % 360f;
        }
        return angle;
    }

    /** @author EyeOfDarkness */
    public static void shotgunRange(int points, float range, float angle, Floatc cons){
        if(points <= 1){
            cons.get(angle);
            return;
        }

        for(int i = 0; i < points; i++){
            float in = Mathf.lerp(-range, range, i / (points - 1f));
            cons.get(in + angle);
        }
    }

    /** @author EyeOfDarkness */
    public static float[] castConeTile(float wx, float wy, float range, float angle, float cone, int rays, Cons2<Building, Tile> consBuilding, Boolf<Tile> insulator){
        return castConeTile(wx, wy, range, angle, cone, consBuilding, insulator, new float[rays]);
    }

    /** @author EyeOfDarkness */
    public static float[] castConeTile(float wx, float wy, float range, float angle, float cone, Cons2<Building, Tile> consBuilding, Boolf<Tile> insulator, float[] ref){
        collidedBlocks.clear();
        idx = 0;
        float expand = 3;
        rect.setCentered(wx, wy, expand);

        shotgunRange(3, cone, angle, con -> {
            tV.trns(con, range).add(wx, wy);
            rectAlt.setCentered(tV.x, tV.y, expand);
            rect.merge(rectAlt);
        });

        if(insulator != null){
            shotgunRange(ref.length, cone, angle, con -> {
                tV.trns(con, range).add(wx, wy);
                ref[idx] = range * range;
                world.raycastEachWorld(wx, wy, tV.x, tV.y, (x, y) -> {
                    Tile tile = world.tile(x, y);
                    if(tile != null && insulator.get(tile)){
                        ref[idx] = Mathf.dst2(wx, wy, x * tilesize, y * tilesize);
                        return true;
                    }
                    return false;
                });
                idx++;
            });
        }

        int tx = Mathf.round(rect.x / tilesize);
        int ty = Mathf.round(rect.y / tilesize);
        int tw = tx + Mathf.round(rect.width / tilesize);
        int th = ty + Mathf.round(rect.height / tilesize);
        for(int x = tx; x <= tw; x++){
            for(int y = ty; y <= th; y++){
                float ofX = (x * tilesize) - wx, ofY = (y * tilesize) - wy;
                int angIdx = Mathf.clamp(Mathf.round(((angleDistSigned(Angles.angle(ofX, ofY), angle) + cone) / (cone * 2f)) * (ref.length - 1)), 0, ref.length - 1);
                float dst = ref[angIdx];
                float dst2 = Mathf.dst2(ofX, ofY);
                if(dst2 < dst && dst2 < range * range && angleDist(Angles.angle(ofX, ofY), angle) < cone){
                    Tile tile = world.tile(x, y);
                    Building building = null;
                    if(tile != null){
                        Building b = world.build(x, y);
                        if(b != null && !collidedBlocks.contains(b.id)){
                            building = b;
                            collidedBlocks.add(b.id);
                        }

                        consBuilding.get(building, tile);
                    }
                }
            }
        }

        collidedBlocks.clear();
        return ref;
    }

    /** @author EyeOfDarkness */
    public static void castCone(float wx, float wy, float range, float angle, float cone, Cons4<Tile, Building, Float, Float> consTile){
        castCone(wx, wy, range, angle, cone, consTile, null);
    }

    /** @author EyeOfDarkness */
    public static void castCone(float wx, float wy, float range, float angle, float cone, Cons3<Unit, Float, Float> consUnit){
        castCone(wx, wy, range, angle, cone, null, consUnit);
    }

    /** @author EyeOfDarkness */
    public static void castCone(float wx, float wy, float range, float angle, float cone, Cons4<Tile, Building, Float, Float> consTile, Cons3<Unit, Float, Float> consUnit){
        collidedBlocks.clear();
        float expand = 3;
        float rangeSquare = range * range;
        if(consTile != null){
            rect.setCentered(wx, wy, expand);
            for(int i = 0; i < 3; i++){
                float angleC = (-1 + i) * cone + angle;
                tV.trns(angleC, range).add(wx, wy);
                rectAlt.setCentered(tV.x, tV.y, expand);
                rect.merge(rectAlt);
            }

            int tx = Mathf.round(rect.x / tilesize);
            int ty = Mathf.round(rect.y / tilesize);
            int tw = tx + Mathf.round(rect.width / tilesize);
            int th = ty + Mathf.round(rect.height / tilesize);
            for(int x = tx; x <= tw; x++){
                for(int y = ty; y <= th; y++){
                    float temp = Angles.angle(wx, wy, x * tilesize, y * tilesize);
                    float tempDst = Mathf.dst(x * tilesize, y * tilesize, wx, wy);
                    if(tempDst >= rangeSquare || !Angles.within(temp, angle, cone)) continue;

                    Tile other = world.tile(x, y);
                    if(other == null) continue;
                    if(!collidedBlocks.contains(other.pos())){
                        float dst = 1f - tempDst / range;
                        float anDst = 1f - Angles.angleDist(temp, angle) / cone;
                        consTile.get(other, other.build, dst, anDst);
                        collidedBlocks.add(other.pos());
                    }
                }
            }
        }

        if(consUnit != null){
            Groups.unit.intersect(wx - range, wy - range, range * 2f, range * 2f, e -> {
                float temp = Angles.angle(wx, wy, e.x, e.y);
                float tempDst = Mathf.dst(e.x, e.y, wx, wy);
                if(tempDst >= rangeSquare || !Angles.within(temp, angle, cone)) return;

                float dst = 1f - tempDst / range;
                float anDst = 1f - Angles.angleDist(temp, angle) / cone;
                consUnit.get(e, dst, anDst);
            });
        }
    }

    /** @author EyeOfDarkness */
    public static float[] castCircle(float wx, float wy, float range, int rays, Boolf<Building> filter, Cons<Building> cons, Boolf<Tile> insulator){
        collidedBlocks.clear();
        float[] cast = new float[rays];

        for(int i = 0; i < cast.length; i++){
            cast[i] = range;
            float ang = i * (360f / cast.length);
            tV.trns(ang, range).add(wx, wy);
            int s = i;

            world.raycastEachWorld(wx, wy, tV.x, tV.y, (cx, cy) -> {
                Tile t = world.tile(cx, cy);
                if(t != null && t.block() != null && insulator.get(t)){
                    float dst = t.dst(wx, wy);
                    cast[s] = dst;
                    return true;
                }

                return false;
            });
        }

        indexer.allBuildings(wx, wy, range, build -> {
            if(!filter.get(build)) return;
            float ang = Angles.angle(wx, wy, build.x, build.y);
            float dst = build.dst2(wx, wy) - ((build.hitSize() * build.hitSize()) / 2f);

            float d = cast[Mathf.mod(Mathf.round((ang % 360f) / (360f / cast.length)), cast.length)];
            if(dst <= d * d) cons.get(build);
        });

        return cast;
    }

    /** @author EyeOfDarkness */
    public static void collideLineRawEnemyRatio(Team team, float x, float y, float x2, float y2, float width, Boolf3<Building, Float, Boolean> buildingCons, Boolf2<Unit, Float> unitCons, Floatc2 effectHandler){
        float minRatio = 0.05f;
        collideLineRawEnemy(team, x, y, x2, y2, width, (building, direct) -> {
            float size = (building.block.size * tilesize / 2f);
            float dst = Mathf.clamp(1f - ((Intersector.distanceSegmentPoint(x, y, x2, y2, building.x, building.y) - width) / size), minRatio, 1f);
            return buildingCons.get(building, dst, direct);
        }, unit -> {
            float size = (unit.hitSize / 2f);
            float dst = Mathf.clamp(1f - ((Intersector.distanceSegmentPoint(x, y, x2, y2, unit.x, unit.y) - width) / size), minRatio, 1f);
            return unitCons.get(unit, dst);
        }, effectHandler, true);
    }

    /** @author EyeOfDarkness */
    public static void collideLineRawEnemy(Team team, float x, float y, float x2, float y2, float width, boolean hitTiles, boolean hitUnits, boolean stopSort, HitHandler handler){
        collideLineRaw(x, y, x2, y2, width, width, b -> b.team != team, u -> u.team != team, hitTiles, hitUnits, h -> h.dst2(x, y), handler, stopSort);
    }

    /** @author EyeOfDarkness */
    public static void collideLineRawEnemy(Team team, float x, float y, float x2, float y2, Boolf2<Building, Boolean> buildingCons, Cons<Unit> unitCons, Floatc2 effectHandler, boolean stopSort){
        collideLineRaw(x, y, x2, y2, 3f, b -> b.team != team, u -> u.team != team, buildingCons, unitCons, healthc -> healthc.dst2(x, y), effectHandler, stopSort);
    }

    /** @author EyeOfDarkness */
    public static void collideLineRawEnemy(Team team, float x, float y, float x2, float y2, float width, Boolf2<Building, Boolean> buildingCons, Boolf<Unit> unitCons, Floatc2 effectHandler, boolean stopSort){
        collideLineRaw(x, y, x2, y2, width, width, b -> b.team != team, u -> u.team != team, buildingCons, unitCons, healthc -> healthc.dst2(x, y), effectHandler, stopSort);
    }

    /** @author EyeOfDarkness */
    public static void collideLineRawEnemy(Team team, float x, float y, float x2, float y2, float width, Boolf2<Building, Boolean> buildingCons, Boolf<Unit> unitCons, Floatf<Healthc> sort, Floatc2 effectHandler, boolean stopSort){
        collideLineRaw(x, y, x2, y2, width, width, b -> b.team != team, u -> u.team != team, buildingCons, unitCons, sort, effectHandler, stopSort);
    }

    /** @author EyeOfDarkness */
    public static void collideLineRawEnemy(Team team, float x, float y, float x2, float y2, float unitWidth, float tileWidth, Boolf2<Building, Boolean> buildingCons, Boolf<Unit> unitCons, Floatc2 effectHandler, boolean stopSort){
        collideLineRaw(x, y, x2, y2, unitWidth, tileWidth, b -> b.team != team, u -> u.team != team, buildingCons, unitCons, healthc -> healthc.dst2(x, y), effectHandler, stopSort);
    }

    /** @author EyeOfDarkness */
    public static void collideLineRawEnemy(Team team, float x, float y, float x2, float y2, Boolf2<Building, Boolean> buildingCons, Boolf<Unit> unitCons, Floatc2 effectHandler, boolean stopSort){
        collideLineRaw(x, y, x2, y2, 3f, b -> b.team != team, u -> u.team != team, buildingCons, unitCons, healthc -> healthc.dst2(x, y), effectHandler, stopSort);
    }

    /** @author EyeOfDarkness */
    public static void collideLineRawEnemy(Team team, float x, float y, float x2, float y2, Boolf2<Building, Boolean> buildingCons, Cons<Unit> unitCons, Floatf<Healthc> sort, Floatc2 effectHandler){
        collideLineRaw(x, y, x2, y2, 3f, b -> b.team != team, u -> u.team != team, buildingCons, unitCons, sort, effectHandler);
    }

    public static void collideLineRawEnemy(Team team, float x, float y, float x2, float y2, float width, Boolf<Healthc> pred, Boolf2<Building, Boolean> buildingCons, Boolf<Unit> unitCons, Floatc2 effectHandler, boolean stopSort){
        collideLineRaw(x, y, x2, y2, width, width, b -> b.team != team && pred.get(b), u -> u.team != team && pred.get(u), buildingCons, unitCons, healthc -> healthc.dst2(x, y), effectHandler, stopSort);
    }

    /** @author EyeOfDarkness */
    public static void collideLineRaw(float x, float y, float x2, float y2, float unitWidth, Boolf<Building> buildingFilter, Boolf<Unit> unitFilter, Boolf2<Building, Boolean> buildingCons, Cons<Unit> unitCons, Floatf<Healthc> sort, Floatc2 effectHandler){
        collideLineRaw(x, y, x2, y2, unitWidth, buildingFilter, unitFilter, buildingCons, unitCons, sort, effectHandler, false);
    }

    /** @author EyeOfDarkness */
    public static void collideLineRaw(float x, float y, float x2, float y2, float unitWidth, Boolf<Building> buildingFilter, Boolf<Unit> unitFilter, Boolf2<Building, Boolean> buildingCons, Cons<Unit> unitCons, Floatf<Healthc> sort, Floatc2 effectHandler, boolean stopSort){
        collideLineRaw(x, y, x2, y2, unitWidth, buildingFilter, unitFilter, buildingCons, unitCons == null ? null : unit -> {
            unitCons.get(unit);
            return false;
        }, sort, effectHandler, stopSort);
    }

    /** @author EyeOfDarkness */
    public static void collideLineRaw(float x, float y, float x2, float y2, float unitWidth, Boolf<Building> buildingFilter, Boolf<Unit> unitFilter, Boolf2<Building, Boolean> buildingCons, Boolf<Unit> unitCons, Floatf<Healthc> sort, Floatc2 effectHandler, boolean stopSort){
        collideLineRaw(x, y, x2, y2, unitWidth, 0f, buildingFilter, unitFilter, buildingCons, unitCons, sort, effectHandler, stopSort);
    }

    /** @author EyeOfDarkness */
    public static void collideLineRaw(float x, float y, float x2, float y2, float unitWidth, float tileWidth, Boolf<Building> buildingFilter, Boolf<Unit> unitFilter, Boolf2<Building, Boolean> buildingCons, Boolf<Unit> unitCons, Floatf<Healthc> sort, Floatc2 effectHandler, boolean stopSort){
        collideLineRaw(x, y, x2, y2, unitWidth, tileWidth,
            buildingFilter, unitFilter, buildingCons != null, unitCons != null,
            sort, (ex, ey, ent, direct) -> {
                boolean hit = false;
                if(unitCons != null && direct && ent instanceof Unit){
                    hit = unitCons.get((Unit)ent);
                }else if(buildingCons != null && ent instanceof Building){
                    hit = buildingCons.get((Building)ent, direct);
                }

                if(effectHandler != null && direct) effectHandler.get(ex, ey);
                return hit;
            }, stopSort
        );
    }

    /** @author EyeOfDarkness */
    public static void collideLineRaw(float x, float y, float x2, float y2, float unitWidth, float tileWidth, Boolf<Building> buildingFilter, Boolf<Unit> unitFilter, boolean hitTile, boolean hitUnit, Floatf<Healthc> sort, HitHandler hitHandler, boolean stopSort){
        hitEffects.clear();
        lineCast.clear();
        lineCastNext.clear();
        collidedBlocks.clear();

        tV.set(x2, y2);
        if(hitTile){
            collideLineCollided.clear();
            Runnable cast = () -> {
                hitB = false;
                lineCast.each(i -> {
                    int tx = Point2.x(i), ty = Point2.y(i);
                    Building build = world.build(tx, ty);

                    boolean hit = false;
                    if(build != null && (buildingFilter == null || buildingFilter.get(build)) && collidedBlocks.add(build.pos())){
                        if(sort == null){
                            hit = hitHandler.get(tx * tilesize, ty * tilesize, build, true);
                        }else{
                            hit = hitHandler.get(tx * tilesize, ty * tilesize, build, false);
                            Hit he = Pools.obtain(Hit.class, Hit::new);
                            he.ent = build;
                            he.x = tx * tilesize;
                            he.y = ty * tilesize;

                            hitEffects.add(he);
                        }

                        if(hit && !hitB){
                            tV.trns(Angles.angle(x, y, x2, y2), Mathf.dst(x, y, build.x, build.y)).add(x, y);
                            hitB = true;
                        }
                    }

                    Vec2 segment = Intersector.nearestSegmentPoint(x, y, tV.x, tV.y, tx * tilesize, ty * tilesize, tV2);
                    if(!hit && tileWidth > 0f){
                        for(Point2 p : Geometry.d8){
                            int newX = (p.x + tx);
                            int newY = (p.y + ty);
                            boolean within = !hitB || Mathf.within(x / tilesize, y / tilesize, newX, newY, tV.dst(x, y) / tilesize);
                            if(segment.within(newX * tilesize, newY * tilesize, tileWidth) && collideLineCollided.within(newX, newY) && !collideLineCollided.get(newX, newY) && within){
                                lineCastNext.add(Point2.pack(newX, newY));
                                collideLineCollided.set(newX, newY, true);
                            }
                        }
                    }
                });

                lineCast.clear();
                lineCast.addAll(lineCastNext);
                lineCastNext.clear();
            };

            world.raycastEachWorld(x, y, x2, y2, (cx, cy) -> {
                if(collideLineCollided.within(cx, cy) && !collideLineCollided.get(cx, cy)){
                    lineCast.add(Point2.pack(cx, cy));
                    collideLineCollided.set(cx, cy, true);
                }

                cast.run();
                return hitB;
            });

            while(!lineCast.isEmpty()) cast.run();
        }

        if(hitUnit){
            rect.setPosition(x, y).setSize(tV.x - x, tV.y - y);

            if(rect.width < 0){
                rect.x += rect.width;
                rect.width *= -1;
            }

            if(rect.height < 0){
                rect.y += rect.height;
                rect.height *= -1;
            }

            rect.grow(unitWidth * 2f);
            Groups.unit.intersect(rect.x, rect.y, rect.width, rect.height, unit -> {
                if(unitFilter == null || unitFilter.get(unit)){
                    unit.hitbox(hitRect);
                    hitRect.grow(unitWidth * 2);

                    Vec2 vec = Geometry.raycastRect(x, y, tV.x, tV.y, hitRect);

                    if(vec != null){
                        float scl = (unit.hitSize - unitWidth) / unit.hitSize;
                        vec.sub(unit).scl(scl).add(unit);
                        if(sort == null){
                            hitHandler.get(vec.x, vec.y, unit, true);
                        }else{
                            Hit he = Pools.obtain(Hit.class, Hit::new);
                            he.ent = unit;
                            he.x = vec.x;
                            he.y = vec.y;
                            hitEffects.add(he);
                        }
                    }
                }
            });
        }

        if(sort != null){
            hit = false;
            hitEffects.sort(he -> sort.get(he.ent)).each(he -> {
                if(!stopSort || !hit){
                    hit = hitHandler.get(he.x, he.y, he.ent, true);
                }

                Pools.free(he);
            });
        }

        hitEffects.clear();
    }

    /**
     * Casts forward in a line.
     * @return the first encountered model.
     * There's an issue with the one in 126.2, which I fixed in a pr. This can be removed after the next Mindustry release.
     */
    public static Healthc linecast(Bullet hitter, float x, float y, float angle, float length){
        tV.trns(angle, length);

        tmpBuilding = null;

        if(hitter.type.collidesGround){
            world.raycastEachWorld(x, y, x + tV.x, y + tV.y, (cx, cy) -> {
                Building tile = world.build(cx, cy);
                if(tile != null && tile.team != hitter.team){
                    tmpBuilding = tile;
                    return true;
                }
                return false;
            });
        }

        rect.setPosition(x, y).setSize(tV.x, tV.y);
        float x2 = tV.x + x, y2 = tV.y + y;

        if(rect.width < 0){
            rect.x += rect.width;
            rect.width *= -1;
        }

        if(rect.height < 0){
            rect.y += rect.height;
            rect.height *= -1;
        }

        float expand = 3f;

        rect.y -= expand;
        rect.x -= expand;
        rect.width += expand * 2;
        rect.height += expand * 2;

        tmpUnit = null;

        Units.nearbyEnemies(hitter.team, rect, e -> {
            if((tmpUnit != null && e.dst2(x, y) > tmpUnit.dst2(x, y)) || !e.checkTarget(hitter.type.collidesAir, hitter.type.collidesGround)) return;

            e.hitbox(hitRect);
            Rect other = hitRect;
            other.y -= expand;
            other.x -= expand;
            other.width += expand * 2;
            other.height += expand * 2;

            Vec2 vec = Geometry.raycastRect(x, y, x2, y2, other);

            if(vec != null){
                tmpUnit = e;
            }
        });

        if(tmpBuilding != null && tmpUnit != null){
            if(Mathf.dst2(x, y, tmpBuilding.getX(), tmpBuilding.getY()) <= Mathf.dst2(x, y, tmpUnit.getX(), tmpUnit.getY())){
                return tmpBuilding;
            }
        }else if(tmpBuilding != null){
            return tmpBuilding;
        }

        return tmpUnit;
    }

    static class Hit implements Poolable{
        Healthc ent;
        float x, y;

        @Override
        public void reset(){
            ent = null;
            x = y = 0f;
        }
    }

    public interface HitHandler{
        boolean get(float x, float y, Healthc ent, boolean direct);
    }


    public static void add(ValueMap o,String key, float f){
        if(!o.has(key)){
            o.put(key,f);
            return;
        }
        o.put(key,o.getFloat(key)+f);
    }
    public static float getFloat(ValueMap o,String key, float defaultVal){
        if(!o.has(key)){return defaultVal;}
        return o.getFloat(key);
    }
    public static boolean isCrash = false;
    public static void crash(){
        isCrash = true;
    }

    public static byte[] compress(byte[] in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DeflaterOutputStream defl = new DeflaterOutputStream(out);
            defl.write(in);
            defl.flush();
            defl.close();

            var o = out.toByteArray();
            out.close();
            return o;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static byte[] decompress(byte[] in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InflaterOutputStream infl = new InflaterOutputStream(out);
            infl.write(in);
            infl.flush();
            infl.close();
            var o = out.toByteArray();
            out.close();
            return o;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(150);
            return null;
        }
    }
}
