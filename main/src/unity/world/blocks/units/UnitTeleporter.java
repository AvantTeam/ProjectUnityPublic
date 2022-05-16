package unity.world.blocks.units;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import unity.content.*;
import unity.content.effects.*;
import unity.graphics.*;

import static arc.Core.atlas;
import static mindustry.Vars.*;

/** Shamelessly stolen from BM (well actually I stole it from the private branch which is also stolen from CB so uhhhhhhh)
 * well screw you i can and will steal my own content
 * @author sunny
 */
public class UnitTeleporter extends Block {
    private static boolean eventInit = false, nearBool = false;
    public static final OrderedSet<UnitTeleporterBuild> pads = new OrderedSet<>();

    public TextureRegion topRegion, lightRegion, arrowRegion;
    public Color disabledColor = Color.coral;
    public float heatLerp = 0.04f;
    public boolean animateNear = true;

    public Effect teleportIn = UnityFx.tpIn;
    public Effect teleportOut = UnityFx.tpOut;
    public Effect teleportUnit = UnityFx.tpFlash;
    public Sound inSound = Sounds.plasmadrop;
    public Sound outSound = Sounds.lasercharge2;

    public UnitTeleporter(String name){
        super(name);
        update = configurable = true;
        solid = false;
        lightColor = Pal.lancerLaser;
        lightRadius = 80f;
        if(!eventInit){
            eventInit = true;
            pads.orderedItems().ordered = false;
            Events.run(EventType.WorldLoadEvent.class, pads::clear);
        }
    }

    @Override
    public void load(){
        super.load();
        topRegion = atlas.find(name + "-top");
        lightRegion = atlas.find(name + "-light");
        arrowRegion = atlas.find("transfer-arrow");
    }

    public class UnitTeleporterBuild extends Building {
        private boolean preLoad = true;
        public float heat, warmup;

        public void teleport(Unit unit, @Nullable Player p){
            UnitTeleporterBuild dest = nextPad(this);
            if(dest == this) return;
            if(!headless) teleportIn.at(unit.x, unit.y, unit.rotation, lightColor, unit.type);
            unit.set(dest.x, dest.y);
            unit.snapInterpolation();
            unit.set(dest.x, dest.y);
            if(!headless){
                effects(dest, unit.hitSize, p == player, unit);
                if(p == player){
                    Core.camera.position.set(dest.x, dest.y);
                    Core.app.post(() -> Core.camera.position.set(dest.x, dest.y));
                }
                dest.warmup = 1f;
                warmup = 1f;
            }
        }

        public UnitTeleporterBuild nextPad(UnitTeleporterBuild prev){
            if(pads.isEmpty()) return prev;
            Seq<UnitTeleporterBuild> arr = pads.orderedItems();
            int gid = prev.power == null ? -1 : prev.power.graph.getID();
            UnitTeleporterBuild dest = prev;
            int dpos = prev.pos();
            int ppos = dpos;
            for(int i = 0; i < arr.size; i++){
                UnitTeleporterBuild next = arr.get(i);
                if(next.id != prev.id && next.team == prev.team && !next.dead() && next.enabled){
                    if(prev.power == null){
                        if(next.power == null && scorePos(ppos, next.pos(), dpos)){
                            dest = next;
                            dpos = next.pos();
                        }
                    }
                    else if(next.power != null && gid == next.power.graph.getID() && scorePos(ppos, next.pos(), dpos)){
                        dest = next;
                        dpos = next.pos();
                    }
                }
            }
            return dest;
        }

        public boolean scorePos(int prev, int next, int dest){
            if(dest > prev) return next < dest && next > prev;
            return next < dest || next > prev;
        }

        @Override
        public void created(){
            super.created();
            //Log.info("CREATED:" + id);
            pads.add(this);
        }

        @Override
        public void onRemoved(){
            super.onRemoved();
            pads.remove(this);
        }

        @Override
        public void updateTile(){
            if(preLoad){ //WLE is called after created(), so re-invoke this after WLE once
                pads.add(this);
                preLoad = false;
            }

            heat = Mathf.lerpDelta(heat, canConsume() ? 1f : 0f, heatLerp);
            if(animateNear && !headless){
                nearBool = false;
                Units.nearby(team, x, y, 80f, u -> {
                    if(nearBool) return;
                    if(u.isPlayer() || !u.isFlying()) nearBool = true;
                });
                warmup = Mathf.lerpDelta(warmup, nearBool ? 1f : 0f, 0.05f);
            }
        }

        @Override
        public void unitOn(Unit unit){
            if(!canConsume()) return;
            if(unit.hasEffect(UnityStatusEffects.tpCoolDown) || unit.isPlayer()) return;
            unit.apply(UnityStatusEffects.tpCoolDown, 120f);
            teleport(unit, null);
        }

        protected void effects(UnitTeleporterBuild dest, float hitSize, boolean isPlayer, Unit unit){
            if(isPlayer){
                inSound.at(dest, Mathf.random() * 0.2f + 1f);
                outSound.at(this, Mathf.random() * 0.2f + 0.7f);
            }else{
                inSound.at(this, Mathf.random() * 0.2f + 1f);
                outSound.at(dest, Mathf.random() * 0.2f + 0.7f);
            }
            teleportOut.at(dest.x, dest.y, hitSize, lightColor);
            if(teleportUnit != Fx.none) teleportUnit.at(dest.x, dest.y, 0f, lightColor, unit);
        }

        protected boolean inRange(Player player){
            return player.unit() != null && player.unit().isValid() && Math.abs(player.unit().x - x) <= 2.5f * tilesize && Math.abs(player.unit().y - y) <= 2.5f * tilesize;
        }

        @Override
        public void draw(){
            super.draw();
            Draw.color(Color.white);
            Draw.alpha(0.45f + Mathf.absin(7f, 0.26f));
            Draw.rect(topRegion, x, y);
            if(heat >= 0.001f){
                Draw.z(Layer.bullet);
                Draw.color(UnityPal.dirium, team.color, Mathf.absin(19f, 1f));
                Lines.stroke((Mathf.absin(62f, 0.5f) + 0.5f) * heat);
                Lines.square(x, y, 10.5f, 45f);
                if(warmup >= 0.001f){
                    Lines.stroke((Mathf.absin(62f, 1f) + 1f) * warmup);
                    Lines.square(x, y, 8.5f, Time.time / 2f);
                    Lines.square(x, y, 8.5f, -1 * Time.time / 2f);
                }
            }
            Draw.reset();
        }

        @Override
        public void drawSelect(){
            Draw.color(canConsume() ? (inRange(player) ? Color.orange : Pal.accent) : Pal.darkMetal);
            float length = tilesize * size / 2f + 3f + Mathf.absin(5f, 2f);
            Draw.rect(arrowRegion, x + length, y, 180f);
            Draw.rect(arrowRegion, x, y + length, 270f);
            Draw.rect(arrowRegion, x - length, y, 0f);
            Draw.rect(arrowRegion, x, y - length, 90f);
            Draw.color();
        }

        @Override
        public void drawLight(){
            Drawf.light(x, y, lightRadius * (animateNear ? 0.5f + 0.5f * warmup : 1f), lightColor, 0.8f * heat);
        }

        @Override
        public boolean canConsume(){
            return power == null ? enabled : power.status > 0.98f;
        }

        @Override
        public void configured(Unit builder, Object value){
            if(builder != null && builder.isPlayer() && !(builder instanceof BlockUnitc)) teleport(builder, builder.getPlayer());
        }

        @Override
        public boolean shouldShowConfigure(Player player){
            return canConsume() && inRange(player);
        }

        @Override
        public boolean configTapped(){
            if(!canConsume() || !inRange(player)) return false;
            configure(null);
            Sounds.click.at(this);
            return false;
        }

        @Override
        public boolean canPickup(){
            return false;
        }
    }
}
