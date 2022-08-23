package unity.world.blocks.units;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

public class MechPad extends Block{
    public UnitType unitType = UnitTypes.dagger;
    public float craftTime = 100f;
    public float cooldown = 0.1f;
    public float spawnRot = 90f;
    public float spawnForce = 3f;

    protected TextureRegion arrowRegion;
    
    public MechPad(String name){
        super(name);
        update = configurable = true;
        hasItems = solid = false;
        ambientSound = Sounds.respawn;
        ambientSoundVolume = 0.08f;
    }
    
    @Override
    public void setStats(){
        //TODO
        super.setStats();
    }
    
    @Override
    public void load(){
        super.load();
        arrowRegion = Core.atlas.find("transfer-arrow");
    }
    
    @Override
    public boolean canReplace(Block other){
        //TODO
        return other.alwaysReplace;
    }
    
    public class MechPadBuild extends Building implements ControlBlock{
        protected @Nullable BlockUnitc thisU;
        protected float time;
        protected float heat;
        protected boolean revert;
        
        @Override
        public boolean canControl(){
            return false;
        }
        
        public boolean inRange(Player player){
            return player.unit() != null && !player.unit().dead && Math.abs(player.unit().x - x) <= 2.5f * tilesize && Math.abs(player.unit().y - y) <= 2.5f * tilesize;
        }
        
        @Override
        public void drawSelect(){
            Draw.color(canConsume() ? (inRange(player) ? Color.orange : Pal.accent) : Pal.darkMetal);
            float length = tilesize * size / 2f + 3f + Mathf.absin(Time.time, 5f, 2f);
            
            Draw.rect(arrowRegion, x + length, y, (0f + 2f) * 90f);
            Draw.rect(arrowRegion, x, y + length, (1f + 2f) * 90f);
            Draw.rect(arrowRegion, x + -1 * length, y, (2f + 2f) * 90f);
            Draw.rect(arrowRegion, x, y + -1 * length, (3f + 2f) * 90f);

            Draw.color();
        }
        
        @Override
        public boolean shouldShowConfigure(Player player){
            return canConsume() && inRange(player);
        }
        
        @Override
        public Unit unit(){
            if(thisU == null){
                thisU = (BlockUnitc)UnitTypes.block.create(team);
                thisU.tile(self());
            }
            return (Unit)thisU;
        }
        
        @Override
        public boolean configTapped(){
            if(!canConsume() || !inRange(player)) return false;
            configure(null);
            //Sounds.click.at(self());
            return false;
        }
        
        @Override
        public void configured(@Nullable Unit unit, @Nullable Object value){
            if(unit != null && unit.isPlayer() && !(unit instanceof BlockUnitc)){
                time = 0;
                revert = unit.type == unitType;
                if(!net.client()){
                    unit.getPlayer().unit(unit());
                }
            }
        }
        
        @Override
        public boolean shouldAmbientSound(){
            return inProgress();
        }
        
        @Override
        public void updateTile(){
            if(inProgress()){
                time += edelta() * (canConsume() ? 1 : 0) * state.rules.unitBuildSpeedMultiplier;
                if(time >= craftTime) finishUnit();
            }
            heat = Mathf.lerpDelta(heat, inProgress() ? 1 : 0, cooldown);
        }
        
        public UnitType getResultUnit(){
            return revert ? bestCoreUnit() : unitType;
        }

        public UnitType bestCoreUnit(){
            return ((CoreBlock)thisU.getPlayer().bestCore().block).unitType;
        }
        
        public boolean inProgress(){
            return thisU != null && isControlled();
        }
        
        public void finishUnit(){
            Player thisP = thisU.getPlayer();
            if(thisP == null) return;
            Fx.spawn.at(self());
            
            if(!net.client()){
                Unit unit = getResultUnit().create(team);
                unit.set(self());
                unit.rotation = spawnRot;
                unit.impulse(0, spawnForce);
                unit.set(getResultUnit(), thisP);
                unit.spawnedByCore = true;
                unit.add();
            }
            
            if(state.isCampaign() && thisP == player) getResultUnit().unlock();
            
            consume();
            time = 0;
            revert = false;
        }
        
        @Override
        public void draw(){
            super.draw();
            if(!inProgress()) return;
            float progress = Mathf.clamp(time / craftTime);
            
            Draw.color(Pal.darkMetal);
            Lines.stroke(2f * heat);
            Fill.poly(x, y, 4, 10f * heat);
            Draw.reset();
            TextureRegion region = getResultUnit().fullIcon;

            //Draw.rect(from, x, y);
            Draw.color(0, 0, 0, 0.4f * progress);
            Draw.rect("circle-shadow", x, y, region.width / 3f, region.width / 3f);
            Draw.color();
            Draw.draw(Layer.blockOver, () -> {
                try{
                    Drawf.construct(x, y, region, 0f, progress, state.rules.unitBuildSpeedMultiplier, time);
                    Lines.stroke(heat, Pal.accentBack);
                    float pos = Mathf.sin(time, 6f, 8f);
                    Lines.lineAngleCenter(x + pos, y, 90f, 16f - Math.abs(pos) * 2f);
                    Draw.color();
                }
                catch(Throwable bruh){
                    //why.
                }
            });

            Lines.stroke(1.5f * heat);
            Draw.color(Pal.accentBack);
            Lines.poly(x, y, 4, 8f * heat);

            float oy = -7f;
            float len = 6f * heat;
            Lines.stroke(5f);
            Draw.color(Pal.darkMetal);
            Lines.line(x - len, y + oy, x + len, y + oy, false);

            Fill.tri(x + len, y + oy - Lines.getStroke() / 2f, x + len, y + oy + Lines.getStroke() / 2f, x + (len + Lines.getStroke() * heat), y + oy);
            Fill.tri(x + len * -1, y + oy - Lines.getStroke() / 2f, x + len * -1, y + oy + Lines.getStroke() / 2f, x + (len + Lines.getStroke() * heat) * -1f, y + oy);

            Lines.stroke(3);
            Draw.color(Pal.accent);
            Lines.line(x - len, y + oy, x - len+ len * 2f * progress, y + oy, false);

            Fill.tri(x + len, y + oy - Lines.getStroke() / 2f, x + len, y + oy + Lines.getStroke() / 2f, x + (len + Lines.getStroke() * heat), y + oy);
            Fill.tri(x + len * -1f, y + oy - Lines.getStroke() / 2f, x + len * -1f, y + oy + Lines.getStroke() / 2f, x + (len + Lines.getStroke() * heat) * -1f, y + oy);

            Draw.reset();
        }
        
        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            time = read.f();
            revert = read.bool();
        }
        
        @Override
        public void write(Writes write){
            super.write(write);
            write.f(time);
            write.bool(revert);
        }
    }
}