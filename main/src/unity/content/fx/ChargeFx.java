package unity.content.fx;

import arc.graphics.g2d.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import unity.entities.effects.*;

import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;

public final class ChargeFx{
    public static final Effect

    greenLaserChargeSmallParent = new ParentEffect(40f, 100f, e -> {
        color(Pal.heal);
        stroke(e.fin() * 2f);
        Lines.circle(e.x, e.y, e.fout() * 50f);
    }),

    greenLaserChargeParent = new ParentEffect(80f, 100f, e -> {
        color(Pal.heal);
        stroke(e.fin() * 2f);
        Lines.circle(e.x, e.y, 4f + e.fout() * 100f);

        Fill.circle(e.x, e.y, e.fin() * 20);

        randLenVectors(e.id, 20, 40f * e.fout(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fin() * 5f);
            Drawf.light(e.x + x, e.y + y, e.fin() * 15f, Pal.heal, 0.7f);
        });

        color();

        Fill.circle(e.x, e.y, e.fin() * 10);
        Drawf.light(e.x, e.y, e.fin() * 20f, Pal.heal, 0.7f);
    });

    private ChargeFx(){
        throw new AssertionError();
    }
}
