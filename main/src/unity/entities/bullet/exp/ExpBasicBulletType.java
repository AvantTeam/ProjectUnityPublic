package unity.entities.bullet.exp;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class ExpBasicBulletType extends ExpBulletType {
    public Color backColor = Pal.bulletYellowBack, frontColor = Pal.bulletYellow;
    public Color mixColorFrom = new Color(1f, 1f, 1f, 0f), mixColorTo = new Color(1f, 1f, 1f, 0f);
    public float width = 5f, height = 7f;
    public float shrinkX = 0f, shrinkY = 0.5f;
    public float spin = 0;
    public String sprite;

    public TextureRegion backRegion;
    public TextureRegion frontRegion;

    public ExpBasicBulletType(float speed, float damage, String bulletSprite){
        super(speed, damage);
        this.sprite = bulletSprite;
        expOnHit = true;
    }

    public ExpBasicBulletType(float speed, float damage){
        this(speed, damage, "bullet");
    }

    /** For mods. */
    public ExpBasicBulletType(){
        this(1f, 1f, "bullet");
    }

    @Override
    public void init(){
        super.init();
        despawnHit = !expOnHit;
    }

    @Override
    public void load(){
        backRegion = Core.atlas.find(sprite + "-back");
        frontRegion = Core.atlas.find(sprite);
    }

    @Override
    public void draw(Bullet b){
        super.draw(b);
        float height = this.height * ((1f - shrinkY) + shrinkY * b.fout());
        float width = this.width * ((1f - shrinkX) + shrinkX * b.fout());
        float offset = -90 + (spin != 0 ? Mathf.randomSeed(b.id, 360f) + b.time * spin : 0f);

        Color mix = Tmp.c1.set(mixColorFrom).lerp(mixColorTo, b.fin());

        Draw.mixcol(mix, mix.a);

        Draw.color(getColor(b));
        Draw.rect(backRegion, b.x, b.y, width, height, b.rotation() + offset);
        Draw.color(frontColor);
        Draw.rect(frontRegion, b.x, b.y, width, height, b.rotation() + offset);

        Draw.reset();
    }
}
