package unity.content.effects;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.entities.*;
import mindustry.graphics.*;

import static arc.graphics.g2d.Draw.*;
import static arc.math.Angles.*;

public final class ShootFx{
    public static final Effect

    sapPlasmaShoot = new Effect(25f, e -> {
        color(Color.white, Pal.sapBullet, e.fin());
        randLenVectors(e.id, 13, e.finpow() * 20f, e.rotation, 23f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 5f);
            Fill.circle(e.x + x / 1.2f, e.y + y / 1.2f, e.fout() * 3f);
        });
    }),
    tonkCannon = new Effect(35f, e -> {
        color(Pal.accent);
        for(int sus : Mathf.signs){
            Drawf.tri(e.x, e.y, 8 * e.fout(Interp.pow3Out), 80, e.rotation + 20 * sus);
            Drawf.tri(e.x, e.y, 4 * e.fout(Interp.pow3Out), 30, e.rotation + 60 * sus); 
        };
    }),
    tonkCannonSmoke = new Effect(45f, e -> {
        color(Pal.lighterOrange, Color.lightGray, Color.gray, e.fin());
        randLenVectors(e.id, 14, 0f + 55f * e.finpow(), e.rotation, 25f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout(Interp.pow5Out) * 4.5f);
        });
    });

    private ShootFx(){
        throw new AssertionError();
    }
}
