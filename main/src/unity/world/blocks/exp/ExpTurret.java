package unity.world.blocks.exp;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;
import unity.annotations.Annotations.*;
import unity.content.effects.*;
import unity.entities.*;
import unity.graphics.*;
import unity.ui.*;
import unity.world.draw.*;

import static mindustry.Vars.*;

/** Identical to {@link Turret} but repurposed as a base for exp. Used to generate other exp blocks via @Dupe.
 * @author sunny
 */
@Dupe(base = ExpTurret.class, parent = Block.class, name = "ExpLBase")
public class ExpTurret extends Turret {
    public int maxLevel = 10; //must be below 200
    public int maxExp;
    public EField<?>[] expFields;
    public boolean passive = false;
    public boolean updateExpFields = true;

    public @Nullable ExpTurret pregrade = null;
    public int pregradeLevel = -1;

    public float orbScale = 0.8f;
    public int expScale = -1;
    public Effect upgradeEffect = UnityFx.expPoof, upgradeBlockEffect = UnityFx.expShineRegion;
    public Sound upgradeSound = Sounds.message;
    public Color fromColor = Pal.lancerLaser, toColor = UnityPal.exp;
    public Color[] effectColors;

    protected @Nullable EField<Float> rangeField = null;//special field, it is special because it's the only one used for drawing stuff
    protected float rangeStart, rangeEnd;
    private final Seq<Building> seqs = new Seq<>();//uwagh

    //damage resist feature for all blocks
    public EField<Float> damageReduction;
    //optional drawer
    public @Nullable DrawLevel draw = null;

    public ExpTurret(String name){
        super(name);
    }

    @Override
    public void init(){
        super.init();
        if(expScale < 0) expScale = passive ? 5 : 2;
        if(expFields == null) expFields = new EField[]{};
        maxExp = requiredExp(maxLevel);
        if(expLevel(maxExp) < maxLevel) maxExp++; //floating point error

        //check for range field
        for(EField<?> f : expFields){
            if(f.stat == Stat.shootRange || f.stat == Stat.range){
                rangeField = (EField<Float>) f;
                break;
            }
        }
        if(rangeField == null){
            rangeStart = rangeEnd = getRange();
        }
        else{
            rangeEnd = rangeField.fromLevel(maxLevel);
            rangeStart = rangeField.fromLevel(0);
        }
        setEFields(0);

        if(pregrade != null && pregradeLevel < 0) pregradeLevel = pregrade.maxLevel;
        if(damageReduction == null) damageReduction = new EField.EExpoZero(f -> {}, 0.1f, Mathf.pow(4f + size, 1f / maxLevel), true, null, v -> Strings.autoFixed(Mathf.roundPositive(v * 10000) / 100f, 2)+ "%");
    }

    @Override
    public void load(){
        super.load();
        if(draw != null) draw.load(this);
    }

    @Actually("return 0;")
    public float getRange(){
        return range;
    }

    //setStats is untouched
    @Override
    public void checkStats(){
        if(!stats.intialized){
            setStats();
            addExpStats();
            stats.intialized = true;
        }
    }

    public void addExpStats(){
        var map = stats.toMap();
        boolean removeAbil = false;
        for(EField<?> f : expFields){
            if(f.stat == null) continue;
            if(map.containsKey(f.stat.category) && map.get(f.stat.category).containsKey(f.stat)){
                if(f.stat == Stat.abilities){
                    if(!removeAbil){
                        stats.remove(f.stat);
                        removeAbil = true;
                    }
                }
                else{
                    stats.remove(f.stat);
                }
            }
            if(f.hasTable){
                stats.add(f.stat, t -> {
                    buildGraphTable(t, f);
                    t.row();
                });
            }
            else stats.add(f.stat, f.toString());
        }

        if(pregrade != null){
            stats.add(Stat.buildCost, "[#84ff00]" + Iconc.up + Core.bundle.format("exp.upgradefrom", pregradeLevel, pregrade.localizedName) + "[]");
            stats.add(Stat.buildCost, t -> {
                t.button(Icon.infoCircleSmall, Styles.cleari, 20f, () -> ui.content.show(pregrade)).size(26).color(UnityPal.exp);
            });
        }

        //stats.add(Stat.itemCapacity, "@", Core.bundle.format(passive ? "exp.lvlAmountP" : "exp.lvlAmount", maxLevel));
        if(!passive) stats.add(Stat.itemCapacity, "@", Core.bundle.format("exp.expAmount", maxExp));
        stats.add(Stat.itemCapacity, t -> {
            t.add(Core.bundle.format(passive ? "exp.lvlAmountP" : "exp.lvlAmount", maxLevel)).tooltip(Core.bundle.get("exp.tooltip"));
        });
        stats.add(Stat.armor, t -> buildGraphTable(t, damageReduction));
    }

    protected void buildGraphTable(Table t, EField<?> f){
        Label l = t.add(f.toString()).get();
        Collapser c = new Collapser(tc -> {
            f.buildTable(tc, maxLevel);
        }, true);

        Runnable toggle = () -> {
            c.toggle(false);
        };
        l.clicked(toggle);
        t.button(Icon.downOpenSmall, Styles.clearTogglei, 20f, toggle).size(26f).color(UnityPal.exp).padLeft(8);
        t.row();
        t.add(c).colspan(2).left(); //label + button
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("health");
    }

    @Override
    @Ignore
    public void drawPlace(int x, int y, int rotation, boolean valid){
        drawPotentialLinks(x, y);

        if(rangeStart != rangeEnd) Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, rangeEnd, UnityPal.exp);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, rangeStart, Pal.placing);

        if(!valid && checkPregrade()) drawPlaceText(Core.bundle.format("exp.pregrade", pregradeLevel, pregrade.localizedName), x, y, false);
    }

    @Override
    public boolean canReplace(Block other){
        return super.canReplace(other) || (pregrade != null && other == pregrade);
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        if(tile == null) return false;
        if(pregrade == null) return super.canPlaceOn(tile, team, rotation);

        //don't bother checking requirements in sandbox or editor
        if(!checkPregrade()) return true;

        CoreBlock.CoreBuild core = team.core();
        //must have all requirements
        if(core == null || (!core.items.has(requirements, state.rules.buildCostMultiplier))) return false;

        //check is there is ONLY a single pregrade block INSIDE all the tiles it will replace - by tracking SEGGS. This protocol is also known as UWAGH standard.
        seqs.clear();
        tile.getLinkedTilesAs(this, inside -> {
            if(inside.build == null || seqs.contains(inside.build) || seqs.size > 1) return; //no point of checking if there are already two in seqs
            if(inside.block() == pregrade && ((ExpTurretBuild) inside.build).level() >= pregradeLevel) seqs.add(inside.build);
        });
        return seqs.size == 1; //no more, no less; a healthy monogamous relationship.
    }

    public boolean checkPregrade(){
        return pregrade != null && !state.rules.infiniteResources && !state.isEditor();
    }

    @Override
    public void placeBegan(Tile tile, Block previous){
        //finish placement immediately when a block is replaced.
        if(pregrade != null && previous == pregrade){
            tile.setBlock(this, tile.team());
            UnityFx.placeShine.at(tile.drawx(), tile.drawy(), tile.block().size * tilesize, UnityPal.exp);
            Fx.upgradeCore.at(tile, tile.block().size);
        }
        else super.placeBegan(tile, previous);
    }

    public int expLevel(int e){
        return Math.min(maxLevel, (int)(Mathf.sqrt(e / (float)(expScale))));
    }

    public float expCap(int l){
        if(l < 0) return 0f;
        if(l > maxLevel) l = maxLevel;
        return requiredExp(l + 1);
    }

    public int requiredExp(int l){
        return l * l * expScale;
    }

    public void setEFields(int l){
        for(EField<?> f : expFields){
            f.setLevel(l);
        }
    }

    public class ExpTurretBuild extends TurretBuild implements ExpHolder, LevelHolder {
        public int exp;
        public @Nullable ExpHub.ExpHubBuild hub = null;

        public int incExp(int amount, boolean hub){
            int ehub = (hub && hubValid()) ? this.hub.takeAmount(amount, this) : 0;

            int e = Math.min(amount - ehub, maxExp - exp);
            if(e == 0) return 0;

            int before = level();
            exp += e;
            int after = level();

            if(exp > maxExp) exp = maxExp;
            if(exp < 0) exp = 0;

            if(after > before) levelup();
            return e;
        }

        @Override
        public int getExp(){
            return exp;
        }

        @Override
        public int handleExp(int amount){
            return incExp(amount, true);
        }

        @Override
        public int unloadExp(int amount){
            if(passive) return 0;
            int e = Math.min(amount, exp);
            exp -= e;
            return e;
        }

        @Override
        public boolean acceptOrb(){
            return !passive && exp < maxExp;
        }

        @Override
        public boolean handleOrb(int orbExp){
            int a = (int)(orbScale * orbExp);
            if(a < 1) return false;
            incExp(a, false);
            return true;
        }

        @Override
        public int handleTower(int amount, float angle){
            if(passive) return 0;
            return incExp(amount, false);
        }

        @Override
        public int level(){
            return expLevel(exp);
        }

        @Override
        public int maxLevel(){
            return maxLevel;
        }

        /** @return the current expf. May be either in exp context or level context.
         */
        public float expf(){
            return lvlexpf();
        }

        /** @return the current expf. Always in level context.
         */
        public float lvlexpf(){
            int lv = level();
            if(lv >= maxLevel) return 1f;
            float lb = expCap(lv - 1);
            float lc = expCap(lv);
            return ((float) exp - lb) / (lc - lb);
        }

        @Override
        public float levelf(){
            return level() / (float)maxLevel;
        }

        public void levelup(){
            upgradeSound.at(this);
            upgradeEffect.at(this);
            if(upgradeBlockEffect != Fx.none) upgradeBlockEffect.at(x, y, rotation - 90, Color.white, region);
        }

        public Color shootColor(Color tmp){
            return tmp.set(fromColor).lerp(toColor, exp / (float)maxExp);
        }

        /**
         * @return a color picked from effectColors[]. Cannot be modified.
         */
        public Color effectColor(){
            if(effectColors == null) return Color.white;
            return effectColors[Math.min((int)(levelf() * effectColors.length), effectColors.length - 1)];
        }

        //updateTile is untouched
        @Override
        public void update(){
            if(updateExpFields) setEFields(level());
            super.update();
        }

        @Override
        public void draw(){
            if(draw != null) draw.draw(this);
            super.draw();
        }

        @Override
        public void drawLight(){
            if(draw != null) draw.drawLight(this);
            super.drawLight();
        }

        //TODO broken -Anuke
        /*
        @Override
        @Ignore
        protected void effects(){
            Effect fshootEffect = shootEffect == Fx.none ? peekAmmo().shootEffect : shootEffect;
            Effect fsmokeEffect = smokeEffect == Fx.none ? peekAmmo().smokeEffect : smokeEffect;
            Color effectc = effectColor();

            fshootEffect.at(x + tr.x, y + tr.y, rotation, effectc);
            fsmokeEffect.at(x + tr.x, y + tr.y, rotation, effectc);
            shootSound.at(x + tr.x, y + tr.y, Mathf.random(0.9f, 1.1f));

            if(shootShake > 0){
                Effect.shake(shootShake, shootShake, this);
            }

            recoil = recoilAmount;
        }*/

        @Override
        @Ignore
        public void drawSelect(){
            Drawf.dashCircle(x, y, rangeField == null ? range : rangeField.fromLevel(level()), team.color);
        }

        @Override
        public void displayBars(Table table){
            table.table(this::buildHBar).pad(0).growX().padTop(8).padBottom(4);
            table.row();

            super.displayBars(table);

            table.table(t -> {
                t.defaults().height(18f).pad(4);
                t.label(() -> "Lv " + level()).color(passive ? UnityPal.passive : Pal.accent).width(65f);
                t.add(new Bar(() -> level() >= maxLevel ? "MAX" : Core.bundle.format("bar.expp", (int)(lvlexpf() * 100f)), () -> UnityPal.exp, this::lvlexpf)).growX();
            }).pad(0).growX().padTop(4).padBottom(4);
            table.row();
        }

        protected void buildHBar(Table t){
            t.clearChildren();
            t.defaults().height(18f).pad(4);
            final int l = level();
            if(damageReduction.fromLevel(level()) >= 0.01f){
                Image ii = new Image(Icon.defense, Pal.health);
                ii.setSize(14f);
                Label ll = new Label(() -> Mathf.roundPositive(damageReduction.fromLevel(level()) * 100) + "");
                ll.setStyle(new Label.LabelStyle(Styles.outlineLabel));
                //ll.setColor(UnityPal.armor);
                ll.setSize(26f, 18f);
                ll.setAlignment(Align.center);
                t.stack(ii, ll).size(26f, 18f).pad(4).padRight(8).center();
            }
            else t.update(() -> {
                if(level() != l) buildHBar(t);
            });
            t.add(new Bar("stat.health", Pal.health, this::healthf).blink(Color.white)).growX();
        }

        @Override
        public void killed(){
            if(!passive) ExpOrbs.spreadExp(x, y, exp * 0.3f, 3f * size);
            super.killed();
        }

        @Override
        public float handleDamage(float amount){
            return super.handleDamage(amount) * Mathf.clamp(1f - damageReduction.fromLevel(level()));
        }

        @Override
        public boolean canPickup(){
            return pregrade == null && super.canPickup();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(exp);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            exp = read.i();
            if(exp > maxExp) exp = maxExp;
        }

        //hub methods
        @Override
        public boolean hubbable(){
            return !passive;
        }

        @Override
        public boolean canHub(Building build){
            return !hubValid() || (build != null && build == hub);
        }

        @Override
        public void setHub(ExpHub.ExpHubBuild hub){
            this.hub = hub;
        }

        public boolean hubValid(){
            boolean val = hub != null && hub.isValid() && !hub.dead && hub.links.contains(pos());
            if(!val) hub = null;
            return val;
        }
    }

    //reloadtime calculation sucks
    public class LinearReloadTime extends EField<Float> {
        public Floatc set;
        public float start, scale;

        public LinearReloadTime(Floatc set, float start, float scale){
            super(Stat.reload);
            this.start = start;
            this.scale = scale;
            this.set = set;
        }

        @Override
        public Float fromLevel(int l){
            return start + l * scale;
        }

        @Override
        public void setLevel(int l){
            set.get(fromLevel(l));
        }

        @Override
        public String toString(){
            return Core.bundle.format("field.linearreload", Strings.autoFixed(shoot.shots * 60f / start, 2), Strings.autoFixed(shoot.shots * 60f / (start + scale * maxLevel), 2));
        }

        @Override
        public void buildTable(Table table, int end){
            table.left();
            Graph g = new Graph(i -> shoot.shots * 60f / fromLevel(i), end, UnityPal.exp);
            table.add(g).size(graphWidth, graphHeight).left();
            table.row();
            table.label(() -> g.lastMouseOver ? Core.bundle.format("ui.graph.label", g.lastMouseStep, Strings.autoFixed(g.mouseValue(), 2) + "/s") : Core.bundle.get("ui.graph.hover"));
        }
    }
}
