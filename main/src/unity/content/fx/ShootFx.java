package unity.content.fx;

import arc.graphics.*;
import arc.graphics.g2d.*;
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
    });

    private ShootFx(){
        throw new AssertionError();
    }
}
