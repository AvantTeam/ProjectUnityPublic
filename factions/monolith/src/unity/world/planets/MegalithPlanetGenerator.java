package unity.world.planets;

import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.graphics.g3d.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.maps.generators.*;
import mindustry.type.*;
import mindustry.type.Sector.*;
import mindustry.world.*;
import unity.content.*;
import unity.graphics.g3d.PUMeshBuilder.*;
import unity.util.*;

import static unity.content.MonolithBlocks.*;
import static unity.graphics.MonolithPal.*;

/**
 * Planet mesh and sector generator for the {@linkplain MonolithPlanets#megalith megalith} planet:
 * <ul>
 *     <li>Streams of (hot) liquefied eneraphyte from both poles, goes thinner as it approaches the equator.</li>
 *     <li>The eneraphyte stream gradually vaporizes as it approaches the equator.</li>
 *     <li>Temperature is cold around the poles, hot across the equator.</li>
 *     <li>Air pressure is more intense across the equator, due to the amount of eneraphyte in the air.</li>
 *     <li>Terrain is dominated by rocks, mostly slates. Most of them erodes and discolors due to temperature and pressure.</li>
 *     <li>Crater... somewhere. Barely any exposed ground liquid. Lots of eneraphyte infusion, emission, and crystals.</li>
 *     <li>Ruinous structures, progressively more common near the crater.</li>
 * </ul>
 * @author GlennFolker
 */
public class MegalithPlanetGenerator extends PlanetGenerator implements PUHexMesher{
    protected static final Color col1 = new Color(), col2 = new Color();
    protected static final Vec3
    rad = new Vec3(), vert = new Vec3(), o = new Vec3(), d = new Vec3(),
    v31 = new Vec3(), v32 = new Vec3(), v33 = new Vec3();
    protected static final Vec2
    v1 = new Vec2();

    protected static final float
    volcanoRadius = 0.16f, volcanoFalloff = 0.3f, volcanoEdgeDeviation = 0.1f, volcanoEdge = 0.06f,
    volcanoHeight = 0.7f, volcanoHeightDeviation = 0.2f, volcanoHeightInner = volcanoHeight - 0.05f;

    protected static final int flowMin = 24, flowMax = 48;

    protected Block[] terrain = {sharpSlate};

    protected boolean initialized = false;
    protected Planet planet;

    protected PlanetGrid grid;
    protected float[] heights;
    protected long[] tileFlags;
    protected int[][] secTiles;
    protected Vec3[] secTilePos;

    protected final FloatSeq vertices = new FloatSeq();
    protected final IntMap<short[]> indices = new IntMap<>();

    protected static final int
    flagFlow = 1; // Eneraphyte stream flow. Contains an int value.

    protected static long tile(long target, int flag, int val){
        return flag(target) | tile(flag, val);
    }

    protected static long tile(int flag, int val){
        return ((long)flag << 32L) | val;
    }

    protected static int flag(long tile){
        return (int)(tile >>> 32L);
    }

    protected static boolean hasFlag(long tile, int flag){
        return (flag(tile) & flag) == flag;
    }

    protected static int val(long tile){
        return (int)tile;
    }

    @Override
    public void init(int divisions, boolean lines, float radius, float intensity){
        Log.debug("Generating volcano flow...");
        Time.mark();

        planet = MonolithPlanets.megalith;
        grid = PlanetGrid.create(divisions);

        heights = new float[grid.tiles.length];
        tileFlags = new long[grid.tiles.length];

        calculateHeight(intensity);
        Cons2<Ptile, Float> flow = (init, pole) -> {
            IntSet used = new IntSet();
            Seq<Ptile> queue = new Seq<>();
            Seq<Ptile> candidates = new Seq<>();

            queue.add(init);
            used.add(init.id);

            int flowIndex = -1, flowCount = Mathf.randomSeed(init.id, flowMin, flowMax);
            while(!queue.isEmpty() && ++flowIndex < flowCount){
                candidates.clear();

                Ptile current = queue.pop();
                tileFlags[current.id] = tile(tileFlags[current.id], flagFlow, flowCount - flowIndex);

                float prog = flowIndex / (flowCount - 1f), heightDeviation = 0.2f, dotDeviation = 0.3f;
                int maxFlood = Mathf.ceilPositive(current.edgeCount * Interp.pow2In.apply(1f - prog));

                float height = heights[current.id] * 1.1f;
                for(Ptile neighbor : current.tiles){
                    if(!used.contains(neighbor.id) && heights[neighbor.id] <= height + Mathf.randomSeed(neighbor.id, heightDeviation) && v31.set(neighbor.v).sub(init.v).dot(0f, pole, 0f) <= 0.5f + Mathf.randomSeedRange(neighbor.id + 1, dotDeviation / 2f)){
                        candidates.add(neighbor);
                    }
                }

                if(candidates.isEmpty()){
                    candidates.add(Structs.findMin(current.tiles, tile -> used.contains(tile.id) ? Float.MAX_VALUE : heights[tile.id]));
                    if(v31.set(candidates.first().v).sub(init.v).dot(0f, pole, 0f) > 0.5f + Mathf.randomSeed(candidates.first().id + 1, dotDeviation / 2f)) candidates.clear();
                }else{
                    candidates.sort(tile -> heights[tile.id] * -(v31.set(tile.v).sub(init.v).dot(0f, pole, 0f) + Mathf.randomSeed(tile.id + 1, dotDeviation / 2f)));
                }

                for(int i = 0, max = Math.min(maxFlood, candidates.size); i < max; i++){
                    Ptile tile = candidates.get(i);
                    if(used.add(tile.id)) queue.add(tile);
                }
            }
        };

        for(int sign : Mathf.signs){
            for(long i = 0; i < 12; i++){
                Tmp.v1.trns(i / 12f * 360f + Mathf.randomSeed(seed + i * sign, 1 / 12f * 360f * 0.67f), 1f).setLength(volcanoRadius(v31.set(Tmp.v1.x, 0.25f * sign, Tmp.v1.y)));
                v31.set(Tmp.v1.x, sign, Tmp.v1.y);

                flow.get(Structs.findMin(grid.tiles, t -> t.v.dst2(v31)), (float)sign);
            }
        }

        initialized = true;
        calculateHeight(intensity);

        Log.debug("Volcano flow generated in @ms.", Time.elapsed());
        Log.debug("Generating sector-divided planet tiles.");
        Time.mark();

        int secSize = planet.sectors.size;
        secTiles = new int[secSize][];
        secTilePos = new Vec3[secSize];

        IntSeq array = new IntSeq();
        for(int i = 0; i < secSize; i++){
            Sector sector = planet.sectors.get(i);

            array.clear();
            for(Ptile tile : grid.tiles) if(in(tile, sector.tile)) array.add(tile.id);

            secTiles[i] = array.toArray();
            secTilePos[i] = new Vec3(sector.tile.v).nor();
        }

        Log.debug("Sector-divided planet tiles generated in @ms.", Time.elapsed());
    }

    protected boolean in(Ptile planetTile, Ptile sectorTile){
        vertices.clear();
        for(Corner corner : sectorTile.corners){
            vert.set(corner.v).nor().scl(0.98f);
            vertices.add(vert.x, vert.y, vert.z);
        }

        vert.setZero();
        int len = vertices.size;
        float[] items = vertices.items;

        for(int i = 0; i < len; i += 3) vert.add(items[i], items[i + 1], items[i + 2]);
        vert.scl((1f / (len / 3f)) * 0.98f);
        vertices.add(vert.x, vert.y, vert.z);

        int edgeCount = sectorTile.edgeCount;
        return Intersector3D.intersectRayTriangles(
        MathUtils.ray1.set(o.setZero(), d.set(planetTile.v).nor()),
        vertices.items, indices.get(edgeCount, () -> {
            short[] indices = new short[edgeCount * 3];
            for(short i = 0, end = (short)edgeCount; i < end; i++){
                short next = (short)(i + 1);
                if(next == end) next = 0;

                int ind = i * 3;
                indices[ind] = i;
                indices[ind + 1] = next;
                indices[ind + 2] = end;
            }

            return indices;
        }),
        3, null
        );
    }

    //might not be necessary, but let's see...
    /*protected boolean edge(Ptile planetTile, Ptile sectorTile){
        int[] tiles = secTiles[sectorTile.id];
        return
            // Must be in the sector tile.
            MathUtils.contains(planetTile.id, tiles) &&
            // Must have a neighbor that is *not* in the sector tile.
            Structs.contains(planetTile.tiles, t -> !MathUtils.contains(t.id, tiles));
    }*/

    protected void calculateHeight(float intensity){
        for(int i = 0, tlen = grid.tiles.length; i < tlen; i++){
            Ptile tile = grid.tiles[i];
            float height = 0f;

            int clen = tile.corners.length;
            for(int j = 0; j < clen; j++){
                Corner corner = tile.corners[j];
                height += (1f + getHeight(corner, v31.set(corner.v).nor())) * intensity;
            }

            heights[i] = height / clen;
        }
    }

    protected float volcanoRadius(Vec3 position){
        float pole = position.dst2(0f, 1f, 0f) < position.dst2(0f, -1f, 0f) ? 1f : -1f;
        rad.set(position).sub(0f, pole, 0f).setLength(volcanoRadius).add(0f, pole, 0f);

        return volcanoRadius + Simplex.noise3d(seed + 1, 3d, 0.5d, 0.7d, rad.x, rad.y, rad.z) * volcanoEdgeDeviation;
    }

    protected boolean inVolcano(Vec3 position){
        return inVolcano(position, volcanoRadius(position));
    }

    protected boolean inVolcano(Vec3 position, float radius){
        return Math.min(position.dst(0f, 1f, 0f), position.dst(0f, -1f, 0f)) <= radius;
    }

    protected Block getBlock(@Nullable Ptile tile, Vec3 position){
        // Raw block, yet to be processed further.
        //TODO floor noise
        Block block = erodedSlate;

        // Volcano stream.
        if(tile != null){
            if(hasFlag(tileFlags[tile.id], flagFlow) || inVolcano(position)) block = liquefiedEneraphyte;
        }

        return block;
    }

    @Override
    public float getHeight(Corner corner, Vec3 position){
        // Raw terrain height, yet to be processed further.
        float height = Simplex.noise3d(seed, 4d, 0.6d, 1.1d, position.x, position.y, position.z);

        // Volcano height.
        float
        volcanoRad = volcanoRadius(position),
        volcanoDst = Math.min(position.dst(0f, 1f, 0f), position.dst(0f, -1f, 0f));
        if(volcanoDst <= volcanoRad + volcanoFalloff){
            // 0 -> near, 1 -> far.
            float volcanoProg = (volcanoDst - volcanoRad) / volcanoFalloff;
            if(initialized) volcanoProg = Mathf.clamp(volcanoProg);

            // Raw terrain height goes down, the volcano height goes up.
            height = Mathf.lerp(
            height * Interp.pow5Out.apply(volcanoProg),
            (volcanoHeight + Simplex.noise3d(seed, 3d, 0.5d, 0.56d, position.x, position.y, position.z) * volcanoHeightDeviation) * Interp.smoother.apply(1f - volcanoProg),
            1f - volcanoProg
            );

            if(initialized && volcanoDst <= volcanoRad) height = Mathf.lerp(height, volcanoHeightInner, Interp.smoother.apply(1f - Mathf.clamp((volcanoDst - volcanoRad) / volcanoEdge)));
        }

        return height * height * 1.3f;
    }

    @Override
    public Color getColor(Ptile tile, Vec3 position){
        Block block = getBlock(tile, position);
        Color col = block.mapColor;

        long t = tileFlags[tile.id];
        if(inVolcano(position)){
            col = monolithLighter;
        }else if(hasFlag(t, flagFlow)){
            float in = val(t) / (flowMax - 1f);
            col = col2.set(monolithMid).lerp(monolithLight, Mathf.curve(in, 0f, 0.5f)).lerp(monolithLighter, Mathf.curve(in, 0.5f));
        }

        return col1.set(col).a(1f - block.albedo);
    }

    @Override
    public float getHeight(Vec3 position){
        v33.set(position).nor();
        return getHeight(Structs.findMin(grid.corners, c -> v32.set(c.v).nor().dst2(v33)), position);
    }

    @Override
    public Color getColor(Vec3 position){
        v33.set(position).nor();
        return getColor(Structs.findMin(grid.tiles, t -> v32.set(t.v).nor().dst2(v33)), position);
    }

    @Override
    public boolean allowLanding(Sector sector){
        //TODO avoid landing on crater sectors until the "requirements" have been fulfilled.
        return super.allowLanding(sector);
    }

    @Override
    public void generateSector(Sector sector){
        //TODO i don't know how the bases should be...
    }

    @Override
    public void addWeather(Sector sector, Rules rules){
        //TODO i don't know how any of this works (yet), when i do i'll make the sectors have a "very cool" megalith climate
        super.addWeather(sector, rules);
    }

    @Override
    protected void genTile(Vec3 position, TileGen tile){
        v33.set(position).nor();
        tile.floor = getBlock(null, position);
        //tile.block = tile.floor.asFloor().wall;

        if(Ridged.noise3d(seed + 1, position.x, position.y, position.z, 2, 20f) > 0.67f) tile.block = Blocks.air;
    }

    @Override
    protected void generate(){
        class FlowData{
            final int x;
            final int y;
            final int level;

            FlowData(int x, int y, int level){
                this.x = x;
                this.y = y;
                this.level = level;
            }
        }

        Seq<FlowData> river = new Seq<>();
        SectorRect srect = sector.rect;

        int[] ptiles = secTiles[sector.id];
        for(int tileIndex : ptiles){
            Ptile ptile = grid.tiles[tileIndex];
            long t = tileFlags[tileIndex];

            if(hasFlag(t, flagFlow)){
                unproject(srect, ptile.v, v1);
                river.add(new FlowData(wc(v1.x), hc(v1.y), val(t)));

                for(Ptile neighbor : ptile.tiles){
                    int nid = neighbor.id;
                    long nflag = tileFlags[nid];

                    if(!hasFlag(nflag, flagFlow) || MathUtils.contains(nid, ptiles)) continue;

                    unproject(srect, neighbor.v, v1);
                    river.add(new FlowData(w(v1.x), h(v1.y), val(nflag)));
                }
            }
        }

        river.each(d -> {
            Tile tile = tiles.get(d.x, d.y);
            if(tile != null) tile.setFloor(liquefiedEneraphyte.asFloor());
        });
    }

    protected Vec2 unproject(SectorRect rect, Vec3 position, Vec2 out){
        return out.set(
        (position.dot(rect.right) / rect.right.len2() + 1f) / 2f,
        (position.dot(rect.top) / rect.top.len2() + 1f) / 2f
        );
    }

    protected int w(float frac){
        return (int)(frac * width);
    }

    protected int h(float frac){
        return (int)(frac * height);
    }

    protected int wc(float frac){
        return Mathf.clamp(w(frac), 0, width - 1);
    }

    protected int hc(float frac){
        return Mathf.clamp(h(frac), 0, height - 1);
    }
}
