package unity.world.blocks.units;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.world.*;
import unity.graphics.*;
import unity.world.blocks.exp.*;

import static arc.Core.atlas;

public class SkillCenter extends ExpTank {
    public int sprites = 19;
    public TextureRegion underRegion;
    public TextureRegion[] animRegion;
    public float animspeed = 0.006f;
    public float animwidth = 80;

    public SkillCenter(String name){
        super(name);
        hasItems = true;
        itemCapacity = 300;
        drawDisabled = false;
        noUpdateDisabled = false;
    }

    @Override
    public void load(){
        super.load();
        underRegion = atlas.find(name + "-under");
        animRegion = new TextureRegion[sprites];
        for(int i = 0; i < sprites; i++){
            animRegion[i] = atlas.find(name + "-" + i, name);
        }
    }

    @Override
    protected TextureRegion[] icons(){
        return new TextureRegion[]{region, underRegion, topRegion};
    }

    public class SkillCenterBuild extends ExpTankBuild {
        private float time = 0f;
        public float heat;

        @Override
        public void updateTile(){
            heat = Mathf.lerp(heat, efficiency(), 0.05f);
            time += heat * Time.delta;
        }

        @Override
        public void draw(){
            Draw.rect(animRegion[Mathf.floorPositive(animwidth+2+animwidth*Mathf.sin(time * animspeed))%sprites], x, y);
            Draw.rect(underRegion, x, y);
            Draw.color(UnityPal.exp, Color.white, Mathf.absin(20, 0.6f));
            Draw.alpha(expf());
            Draw.rect(expRegion, x, y);
            Draw.color();
            Draw.rect(topRegion, x, y);

            drawTeamTop();
        }

        @Override
        public boolean canPickup(){
            return false;
        }
    }
}
