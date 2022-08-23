package unity.world.blocks.exp;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import unity.content.*;
import unity.content.effects.*;

import static mindustry.Vars.renderer;

public class MeltingCrafter extends LevelKoruhCrafter{
    public float meltAmount = 0.01f;
    public float cooldown = 0.01f;
    public Liquid lava = UnityLiquids.lava;

    public Color lavaColor1 = Color.coral, lavaColor2 = Color.orange;
    public Effect meltEffect = UnityFx.blockMelt;
    public Effect smokeEffect = UnityFx.longSmoke;

    public MeltingCrafter(String name){
        super(name);
        ignoreExp = true;
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("heat", (MeltingCrafterBuild entity) -> new Bar(() -> Core.bundle.get("bar.heat"), () -> Pal.ammo, () -> Mathf.clamp(entity.melt)));
    }

    public class MeltingCrafterBuild extends LevelKoruhCrafterBuild{
        public float melt = 0f;

        @Override
        public void lackingExp(int missing){
            melt += meltAmount * missing;
        }

        @Override
        public void updateTile(){
            super.updateTile();

            if(expc > 0 && melt > 0){
                melt -= delta() * cooldown;
                if(melt < 0) melt = 0;
            }
            if(Mathf.chance(Mathf.clamp(melt) * 0.1f)) smokeEffect.at(x + Mathf.range(size * 2f), y + Mathf.range(size * 2f));
            if(melt >= 1f && (liquids == null || liquids.get(lava) > 0.1f * liquidCapacity)) kill();
        }

        @Override
        public void draw(){
            super.draw();

            if(melt < 0.1f) return;
            if(melt > 1f) melt = 1f;
            Draw.z(Layer.bullet - 0.01f);
            Draw.color(lavaColor1, lavaColor2, Mathf.absin(3f, 1f));
            TextureRegion region = renderer.blocks.cracks[block.size - 1][Mathf.clamp((int)(melt * BlockRenderer.crackRegions), 0, BlockRenderer.crackRegions-1)];
            Draw.rect(region, x, y, (id%4)*90);
            Draw.color();
        }

        @Override
        public void onDestroyed(){
            super.onDestroyed();
            if(liquids == null || liquids.currentAmount() > 0.1f * liquidCapacity){
                meltEffect.at(x, y, 0f, lavaColor2);
                Puddles.deposit(tile, lava, liquids.get(lava) * 10);
                for(int i=0; i<4; i++){
                    Tile tg = tile.nearby(i);
                    if(tg == null || !tg.solid()) continue;
                    Fires.create(tg);
                }

                float fx = x; float fy = y; int fsize = size;
                for(int i = 0; i < 5; i++){
                    Time.run(Mathf.random(60f), () -> smokeEffect.at(fx + Mathf.range(fsize * 2f), fy + Mathf.range(fsize * 2f)));
                }
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(melt);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            melt = read.f();
        }
    }
}
