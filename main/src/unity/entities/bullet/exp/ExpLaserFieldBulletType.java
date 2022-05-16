package unity.entities.bullet.exp;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import unity.content.effects.*;

public class ExpLaserFieldBulletType extends ExpLaserBulletType{
    public BulletType distField;
    public BulletType smallDistField;
    public int fields;
    public float fieldInc;

    public ExpLaserFieldBulletType(float length, float damage){
        super(length, damage);
        speed = 0.01f;
        scaleLife = true;
    }

    int getFields(Bullet b){
        return fields + Mathf.floor(fieldInc * getLevel(b) * b.damageMultiplier());
    }

    @Override
    public void handleExp(Bullet b, float x, float y, int amount){
        super.handleExp(b, x, y, amount);
        distField.create(b.owner, b.team, x, y, 0f);
    }

    @Override
    public void init(Bullet b){
        super.init(b);

        Position vec = (Position) b.data;

        if(!(b.data instanceof Healthc)) smallDistField.create(b.owner, b.team, vec.getX(), vec.getY(), 0f); //if b.data is instanceof healthc, handleExp is called

        Sounds.spark.at(vec.getX(), vec.getY(),0.4f);
        Sounds.spray.at(vec.getX(), vec.getY(),0.4f);
        UnityFx.chainLightning.at(b.x, b.y, 0, getColor(b), vec);

        for(int i = 0; i < getFields(b); i++) {
            final Team team = b.team;
            final var owner = b.owner;
            Time.run(0.1f * 60 * i + 1 + UnityFx.smallChainLightning.lifetime*0.5f, () ->{
                float tx = vec.getX() + Mathf.range(8) * Vars.tilesize;
                float ty = vec.getY() + Mathf.range(8) * Vars.tilesize;
                UnityFx.smallChainLightning.at(vec.getX(), vec.getY(), 0, getColor(b), new Vec2(tx, ty));
                Sounds.spark.at(tx, ty,0.4f);
                Sounds.spray.at(tx, ty,0.4f);
                smallDistField.create(owner, team, tx, ty, 0f);
            });
        }
    }
}
