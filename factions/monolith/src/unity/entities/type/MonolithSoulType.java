package unity.entities.type;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import unity.content.*;
import unity.entities.prop.*;
import unity.gen.entities.*;
import unity.graphics.*;
import unity.util.*;

import static mindustry.Vars.headless;
import static unity.graphics.MonolithPal.*;

/**
 * Convenience class for {@link MonolithSoul} types.
 * @author GlennFolker
 */
@SuppressWarnings("unchecked")
public class MonolithSoulType extends PUUnitType{
    public Func<MonolithSoul, Trail> corporealTrail = soul -> trailType.get(soul);

    public float trailChance = -1f, formChance = -1f, formTileChance = -1f, formAbsorbChance = -1f, joinChance = -1f;
    public Effect trailEffect = Fx.none, formEffect = Fx.none, formTileEffect = Fx.none, formAbsorbEffect = Fx.none, joinEffect = Fx.none;

    public MonolithSoulType(String name, MonolithSoulProps props){
        super(name);

        fallSpeed = 1f;

        lowAltitude = true;
        flying = true;
        omniMovement = false;
        playerControllable = false;

        prop(props);
    }

    @Override
    public StateTrail createTrail(Unit unit){
        return super.createTrail(unit);
    }

    @Override
    public void init(){
        boolean substitute = Core.atlas == null;
        if(substitute) Core.atlas = DrawUtils.emptyAtlas;

        if(!(trailType.get(sample) instanceof StateTrail)){
            Func<Unit, Trail> type = trailType;
            trailType = unit -> new StateTrail(type.get(unit));
        }

        if(substitute) Core.atlas = null;
        super.init();
    }

    @Override
    public void update(Unit unit){
        if(unit instanceof MonolithSoul soul){
            if(soul.trail instanceof StateTrail trail){
                float width = (engineSize + Mathf.absin(Time.time, 2f, engineSize / 4f) * soul.elevation);
                if(soul.corporeal && !trail.corporeal){
                    Trail copy = trail.trail.copy();
                    if(copy instanceof BaseTrail t) t.rot = BaseTrail::rot;

                    MonolithFx.trailFadeLow.at(soul.x, soul.y, width, monolithLighter, copy);
                    trail.trail = corporealTrail.get(soul);
                    trail.corporeal = true;
                }else if(!soul.corporeal && trail.corporeal){
                    Trail copy = trail.trail.copy();
                    if(copy instanceof BaseTrail t) t.rot = BaseTrail::rot;

                    MonolithFx.trailFadeLow.at(soul.x, soul.y, width, monolithLighter, copy);
                    trail.trail = kickstartTrail(soul, createTrail(soul));
                    trail.corporeal = false;
                }
            }

            if(!soul.corporeal){
                if(trailChance > 0f && Mathf.chanceDelta(trailChance)) trailEffect.at(soul.x, soul.y, Time.time, new Vec2(soul.vel).scl(-0.3f / Time.delta));
                if(soul.forming()){
                    if(formChance > 0f || formTileChance > 0f || formAbsorbChance > 0f) for(Tile form : soul.forms){
                        if(formChance > 0f && Mathf.chanceDelta(formChance)) formEffect.at(soul);
                        if(formTileChance > 0f && Mathf.chanceDelta(formTileChance)) formTileEffect.at(form.drawx(), form.drawy(), 4f);
                        if(formAbsorbChance > 0f && Mathf.chanceDelta(formAbsorbChance)) formAbsorbEffect.at(form.drawx(), form.drawy(), 0f, soul);
                    }
                }else if(soul.joining() && joinChance > 0f && Mathf.chanceDelta(joinChance)){
                    joinEffect.at(soul.x + Mathf.range(6f), soul.y + Mathf.range(6f), 0f, soul.joinTarget);
                }
            }
        }

        super.update(unit);
    }

    @Override
    public void draw(Unit unit){
        if(!(unit instanceof MonolithSoul soul)) return;
        if(!soul.corporeal){
            if(!headless && soul.trail == null) soul.trail = kickstartTrail(soul, createTrail(soul));

            float z = Draw.z();
            Draw.z(Layer.flyingUnitLow);

            drawSoftShadow(soul);

            float trailSize = (engineSize + Mathf.absin(Time.time, 2f, engineSize / 4f) * soul.elevation) * trailScl;
            soul.trail.drawCap(engineColor, trailSize);
            soul.trail.draw(engineColor, trailSize);

            Draw.z(Layer.effect - 0.01f);
            drawBase(soul);

            Draw.z(Layer.flyingUnit);
            drawEyes(soul);

            Draw.z(Layer.flyingUnit);
            drawForm(soul);

            Draw.z(Layer.flyingUnit);
            drawJoin(soul);

            Draw.reset();
            Draw.z(z);
        }else{
            super.draw(unit);
        }
    }

    public void drawBase(MonolithSoul soul){}

    public void drawEyes(MonolithSoul soul){}

    public void drawForm(MonolithSoul soul){
        for(int i = 0; i < wreckRegions.length; i++){
            TextureRegion reg = wreckRegions[i];
            if(reg == null || !reg.found()) continue;

            float off = (360f / wreckRegions.length) * i;
            float fin = soul.formProgress, fout = 1f - fin;

            Tmp.v1.trns(soul.rotation + off, fout * 24f)
            .add(Tmp.v2.trns((Time.time + off) * 4f, fout * 3f))
            .add(soul);

            Draw.alpha(fin);
            Draw.rect(reg, Tmp.v1.x, Tmp.v1.y, soul.rotation - 90f);
        }
    }

    public void drawJoin(MonolithSoul soul){}

    @Override
    public MonolithSoul create(Team team){
        return (MonolithSoul)super.create(team);
    }

    public static void draw(float x, float y, float rotation, float offSideX, float offSideY, float offFront, float offCenter, float offBack){
        Vec2
        right = Tmp.v1.trns(rotation - 90f, offSideX, offSideY),
        left = Tmp.v2.trns(rotation - 90f, -offSideX, offSideY),
        front = Tmp.v3.trns(rotation, offFront).add(x, y),
        center = Tmp.v4.trns(rotation, offCenter).add(x, y),
        back = Tmp.v5.trns(rotation, offBack).add(x, y);

        Draw.color(monolithMid, monolithLight, 1f - MathUtils.shade(rotation - 45f));
        Fill.tri(center.x, center.y, front.x, front.y, x + right.x, y + right.y);

        Draw.color(monolithMid, monolithLight, 1f - MathUtils.shade(rotation - 135f));
        Fill.tri(center.x, center.y, x + right.x, y + right.y, back.x, back.y);

        Draw.color(monolithMid, monolithLight, 1f - MathUtils.shade(rotation + 45f));
        Fill.tri(center.x, center.y, front.x, front.y, x + left.x, y + left.y);

        Draw.color(monolithMid, monolithLight, 1f - MathUtils.shade(rotation + 135f));
        Fill.tri(center.x, center.y, x + left.x, y + left.y, back.x, back.y);

        Draw.reset();
    }

    public static class StateTrail extends BaseTrail{
        public Trail trail;
        public boolean corporeal = false;

        public StateTrail(Trail trail){
            super(0);
            this.trail = trail;
        }

        @Override
        public int baseSize(){
            return 0;
        }

        @Override
        public void shorten(){
            trail.shorten();
        }

        @Override
        public float update(float x, float y, float width, float angle){
            if(Float.isNaN(lastX) || Float.isNaN(lastY)){
                lastX = x;
                lastY = y;
            }

            trail.update(x, y, width);

            float speed = Mathf.dst(lastX, lastY, x, y) / Time.delta;
            lastX = x;
            lastY = y;
            return speed;
        }

        @Override
        public void draw(Color color, float width){
            trail.draw(color, width);
        }

        @Override
        public void drawCap(Color color, float width){
            trail.drawCap(color, width);
        }

        @Override
        public void forceDrawCap(Color color, float width){
            if(trail instanceof BaseTrail t) t.forceDrawCap(color, width);
        }

        @Override
        public void kickstart(float x, float y){
            if(trail instanceof BaseTrail t) t.kickstart(x, y);
        }

        @Override
        public void length(int length){
            if(trail instanceof BaseTrail t) t.length(length);
        }

        @Override
        public void recalculateAngle(){
            if(trail instanceof BaseTrail t) t.recalculateAngle();
        }

        @Override
        public int size(){
            return trail.size();
        }

        @Override
        public float width(){
            return trail.width();
        }

        @Override
        public void clear(){
            trail.clear();
        }
    }
}
