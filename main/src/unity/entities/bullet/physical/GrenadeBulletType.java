package unity.entities.bullet.physical;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class GrenadeBulletType extends BulletType{
    public float width,height;
    public Color frontColor=Color.gray,backColor=Pal.bulletYellowBack,mixColorFrom = new Color(1.0f, 1.0f, 1.0f, 0.0f),mixColorTo= new Color(1.0f, 1.0f, 1.0f, 0.0f);
    public TextureRegion frontRegion,backRegion;
    @Override
    public void load(){
        super.load();
        this.trailEffect = Fx.artilleryTrail;
        this.width=6;
        this.height=4;
        this.hitEffect= Fx.blastExplosion;
        this.frontRegion = Core.atlas.find("bullet");
        this.backRegion = Core.atlas.find("bullet-back");
    }
    public boolean justBounced(Bullet b){
        var x = b.fin();
        var px = (b.time-Time.delta*2.0)/b.lifetime;
        return Math.floor(5*x)!= Math.floor(5*px);
    }
    @Override
    public void update(Bullet b){
        super.update(b);

        if(this.justBounced(b)){
            b.vel.x *= 0.8;
            b.vel.y *= 0.8;
        }

        float zh = this.getZ(b);
        var tile = Vars.world.tileWorld(b.x, b.y);
        if(tile == null || tile.build == null|| zh>0.2 || b.fin()<0.05) return;

        if(tile.solid()){
            b.trns(-b.vel.x, -b.vel.y);
            float penX = Math.abs(tile.build.x - b.x);
            float penY = Math.abs(tile.build.y - b.y);
             if(penX > penY){
                 b.vel.x *= -0.5;
             }else{
                 b.vel.y *= -0.5;
             }
        }

    }

    @Override
    public float calculateRange(){
        //float a = 1.0f/0.3f;
        float t = this.speed*0.8f;
        float acc = 0;
        for(int i =0;i<5;i++){
            t*=0.8;
            acc+=this.lifetime*0.2f*t;
        }
        //return Math.max(this.speed * (a*this.lifetime/(a+this.lifetime)), this.maxRange);
        return Math.max(acc, this.maxRange);
    }

    public float getZ(Bullet b){
        var x = b.fin();
        return Math.abs(Mathf.sin(5*x*Mathf.pi))/(Mathf.floor(x*5)*Mathf.floor(x*5)+1);
    }

    @Override
    public boolean testCollision(Bullet bullet, Building tile){
        return super.testCollision(bullet,tile) && this.getZ(bullet)<0.2;
    }

    @Override
    public void draw(Bullet b){
        if(b.fin()<0.15&&b.timer.get(0, (3 + b.fslope() * 2))){
            this.trailEffect.at(b.x, b.y, b.fslope() * 4.0f* Mathf.clamp(b.fout()), this.backColor);
        }

        var scl = this.getZ(b)+1;
        var offset = Time.time*3.0f;
        var height = this.height*scl;
        var width = this.width*scl;
        var flash = Mathf.pow(2,5*b.fin()-1)%1.0>0.5;

        var mix = Tmp.c1.set(this.mixColorFrom).lerp(this.mixColorTo, b.fin());

        Draw.mixcol(mix, mix.a);

        Draw.color(this.backColor);
        Draw.rect(this.backRegion, b.x, b.y, width, height, b.rotation()-90+offset);
        Draw.color(this.frontColor.cpy().lerp(Color.white,flash?1:0));
        Draw.rect(this.frontRegion, b.x, b.y, width, height, b.rotation()-90+offset);

        Draw.reset();
    }
}
