package unity.world.blocks.exp;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.content.effects.*;
import unity.entities.*;
import unity.graphics.*;

import static arc.Core.atlas;
import static mindustry.Vars.*;

public class ExpHub extends ExpTank{
    public float ratio = 0.3f;
    public float reloadTime = 30f;
    public float range = 40f;

    public int maxLinks = 4;

    public Effect transferEffect = UnityFx.expLaser;
    public TextureRegion laser, laserEnd;
    private final Seq<Building> tmpe = new Seq<>();

    public ExpHub(String name){
        super(name);
        rotate = true;
        configurable = true;
        solid = false;
        schematicPriority = -16;

        config(Integer.class, (ExpHubBuild entity, Integer value) -> {
            Building other = world.build(value);
            boolean contains = entity.links.contains(value);
            //if(entity.occupied == null) entity.occupied = new boolean[maxLinks];

            if(contains){
                //unlink
                int i = entity.links.indexOf(value);
                entity.links.removeIndex(i);
            }else if(linkValid(entity, other, true) && entity.links.size < maxLinks){
                if(!entity.links.contains(other.pos())){
                    entity.links.add(other.pos());
                }
            }
            else{
                return;
            }
            entity.sanitize();
        });
        configClear((ExpHubBuild entity) -> {
            entity.links.clear();
        });
        config(Point2[].class, (ExpHubBuild tile, Point2[] value) -> {
            IntSeq old = new IntSeq(tile.links);

            //clear old
            for(int i = 0; i < old.size; i++){
                int cur = old.get(i);
                configurations.get(Integer.class).get(tile, cur);
            }

            //set new
            for(Point2 p : value){
                int newPos = Point2.pack(p.x + tile.tileX(), p.y + tile.tileY());
                configurations.get(Integer.class).get(tile, newPos);
            }
        });
    }

    @Override
    public void init(){
        super.init();
        clipSize = Math.max(clipSize, range * 2f + 8f);
    }

    @Override
    public void load(){
        super.load();
        laser = atlas.find("unity-exp-laser");
        laserEnd = atlas.find("unity-exp-laser-end");
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("links", (ExpHubBuild entity) -> new Bar(() -> Core.bundle.format("bar.iconlinks", entity.links.size, maxLinks, Iconc.turret), () -> Pal.accent, () -> entity.links.size / (float)maxLinks));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.powerRange, (int)(range / tilesize) + 1, StatUnit.blocks);
        stats.add(Stat.powerConnections, maxLinks, StatUnit.none);
    }

    public boolean linkValid(Building tile, Building link, boolean checkHub){
        if(tile == link || link == null || tile.team != link.team || link.dead) return false;

        return tile.dst2(link) <= range * range &&
                (link instanceof ExpHolder e && e.hubbable() && (!checkHub || e.canHub(tile)));
    }

    @Override
    protected TextureRegion[] icons(){
        return new TextureRegion[]{region};
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Tile tile = world.tile(x, y);

        if(tile == null) return;
        Lines.stroke(1f);
        Drawf.circles(x * tilesize + offset, y * tilesize + offset, range, UnityPal.exp);
        if(valid){
            tmpe.clear();
            int linkRange = (int)(range / tilesize) + 1;
            for(int xx = x - linkRange; xx <= x + linkRange; xx++){
                for(int yy = y - linkRange; yy <= y + linkRange; yy++){
                    if(xx == x && yy == y) continue;
                    Building link = world.build(xx, yy);

                    if(link != null && link.team == player.team() && !tmpe.contains(link) && link.dst2(Tmp.v1.set(x * tilesize + offset, y * tilesize + offset)) <= range * range && link instanceof ExpHolder e && e.hubbable() && e.canHub(null)){
                        tmpe.add(link);
                        Drawf.square(link.x, link.y, link.block.size * tilesize / 2f + 2f, Pal.place);
                    }
                }
            }
        }

        Draw.reset();
    }

    @Override
    public void drawPlanRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.rect(topRegion, req.drawx(), req.drawy(), req.rotation * 90);
    }

    protected void getPotentialLinks(Tile tile, Team team, Cons<Building> others, boolean checkHub){
        if(tile == null || tile.build == null) return;
        tmpe.clear();
        int linkRange = (int)(range / tilesize) + 1;
        for(int x = tile.x - linkRange; x <= tile.x + linkRange; x++){
            for(int y = tile.y - linkRange; y <= tile.y + linkRange; y++){
                Building link = world.build(x, y);

                if(link != null && link.team == team && link != tile.build && !tmpe.contains(link) && linkValid(tile.build, link, checkHub)){
                    tmpe.add(link);
                    others.get(link);
                }
            }
        }
    }

    public class ExpHubBuild extends ExpTankBuild {
        public float reload = reloadTime;
        public IntSeq links = new IntSeq();

        public int takeAmount(int e, Building source){
            if(e <= 0) return 0;
            int prefa = Mathf.ceilPositive(ratio * e);
            int r = handleExp(prefa);
            if(r > 0) transferEffect.at(x, y, 0f, Color.white, source);
            return r;
        }

        public void sanitize(){
            for(int i = 0; i < links.size; i++){
                Building b = world.build(links.get(i));

                if(!linkValid(this, b, true) || links.get(i) != b.pos()){
                    links.removeIndex(i);
                    i--;
                }
                else if(b instanceof ExpHolder e && e.hubbable() && e.canHub(this)) e.setHub(this);
            }
        }

        @Override
        public void placed(){
            if(net.client()) return;

            getPotentialLinks(tile, team, other -> {
                if(!links.contains(other.pos())){
                    configureAny(other.pos());
                }
            }, true);

            super.placed();
        }

        @Override
        public void dropped(){
            links.clear();
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
            sanitize();
        }

        @Override
        public void updateTile(){
            reload += edelta();
            if(reload >= reloadTime && ExpOrbs.orbs(exp) > 0){
                int a = handleExp(-ExpOrbs.oneOrb(exp));
                if(a < 0) ExpOrbs.dropExp(x, y, rotation * 90f, 4f, -a);
                reload = 0f;
            }
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            Draw.color(UnityPal.exp, Color.white, Mathf.absin(20, 0.6f));
            Draw.alpha(expf() * 0.6f);
            Draw.rect(expRegion, x, y);
            Draw.color();
            Draw.rect(topRegion, x, y, rotdeg());

            drawLinks();
        }

        protected void drawLinks(){
            if(Mathf.zero(Renderer.laserOpacity) || links.size == 0) return;
            Draw.z(Layer.power + 1f);
            Draw.alpha(Renderer.laserOpacity * (Mathf.absin(5f, 0.3f) + 0.1f));
            for(int i = 0; i < links.size; i++){
                Building b = world.build(links.get(i));
                if(!linkValid(this, b, true) || links.get(i) != b.pos()) continue;

                Tmp.v2.set(b);
                Tmp.v1.set(Tmp.v2).sub(this).nor().scl(size * tilesize / 2f);
                Tmp.v2.sub(Tmp.v1);
                Tmp.v1.add(this);
                Drawf.laser(laser, laserEnd, Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y, 0.3f);
            }
            Draw.reset();
        }

        @Override
        public void drawLight(){
            super.drawLight();
            Drawf.light(x, y, lightRadius * expf(), UnityPal.exp, 0.5f);
        }

        @Override
        public void drawConfigure(){
            Lines.stroke(1f);
            Drawf.circles(x, y, tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f), UnityPal.exp);
            Drawf.circles(x, y, range, UnityPal.exp);

            int linkRange = (int)(range / tilesize) + 1;
            for(int x = tile.x - linkRange; x <= tile.x + linkRange; x++){
                for(int y = tile.y - linkRange; y <= tile.y + linkRange; y++){
                    Building link = world.build(x, y);

                    if(link != null && link != this && linkValid(this, link, true)){
                        boolean linked = links.indexOf(link.pos()) >= 0;

                        if(linked){
                            Drawf.square(link.x, link.y, link.block.size * tilesize / 2f + 1f, Pal.accent);
                        }
                    }
                }
            }

            Draw.reset();
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(linkValid(this, other, false)){
                configure(other.pos());
                return false;
            }

            if(this == other){
                if(links.size == 0){
                    getPotentialLinks(tile, team, b -> configure(b.pos()), true);
                }else{
                    configure(null);
                }
                deselect();
                return false;
            }

            return true;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.s(links.size);
            for(int i = 0; i < links.size; i++){
                write.i(links.get(i));
            }
            write.f(reload);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            links.clear();
            short amount = read.s();
            for(int i = 0; i < amount; i++){
                links.add(read.i());
            }
            reload = read.f();
        }

        @Override
        public Point2[] config(){
            Point2[] out = new Point2[links.size];
            for(int i = 0; i < out.length; i++){
                out[i] = Point2.unpack(links.get(i)).sub(tile.x, tile.y);
            }
            return out;
        }

        @Override
        public boolean acceptOrb(){
            return false;
        }

        @Override
        public int handleTower(int amount, float angle){
            return 0;
        }
    }
}
