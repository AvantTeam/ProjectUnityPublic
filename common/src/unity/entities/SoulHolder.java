package unity.entities;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import unity.gen.entities.*;
import unity.mod.*;

/**
 * An entities attribute of {@linkplain Faction#monolith monolith} soul holders.
 * @author GlennFolker
 */
public interface SoulHolder extends Position, Sized{
    int souls();

    int maxSouls();

    Team team();

    default boolean canJoin(){
        return souls() < maxSouls();
    }

    default boolean hasSouls(){
        return souls() > 0;
    }

    default int acceptSoul(Entityc other){
        SoulHolder soul = toSoul(other);
        return soul != null ? acceptSoul(soul) : 0;
    }

    default int acceptSoul(SoulHolder other){
        return acceptSoul(other.souls());
    }

    default int acceptSoul(int amount){
        return Math.min(maxSouls() - souls(), amount);
    }

    int transferSoul(int amount);

    int withdrawSoul(int amount);

    default void transferredSoul(){}

    default float soulf(){
        return souls() / (float)maxSouls();
    }

    /** Spreads the nesting souls in this soul holder, typically at death. Called server-side */
    default void spreadSouls(){
        boolean[] transferred = {false};
        spread(team(), souls(), soul -> transferred[0] |= apply(soul, transferred[0]));
    }

    /** @return If this entity was controlled by a player, returns whether the player has been transferred to a spawned soul. */
    default boolean apply(MonolithSoul soul, boolean transferred){
        Tmp.v1.trns(Mathf.random(360f), Mathf.random(hitSize()));
        soul.set(getX() + Tmp.v1.x, getY() + Tmp.v1.y);

        Tmp.v1.trns(Mathf.random(360f), Mathf.random(6f, 12f));
        soul.rotation = Tmp.v1.angle();
        soul.vel.set(Tmp.v1.x, Tmp.v1.y);
        soul.add();

        if(this instanceof Unitc unit && unit.isPlayer()){
            if(!transferred) soul.controller(unit.getPlayer());
            return true;
        }else{
            return false;
        }
    }

    static boolean isSoul(Object e){
        return toSoul(e) != null;
    }

    static SoulHolder toSoul(Object e){
        if(e instanceof UnitController cont) e = cont.unit();
        if(e instanceof BlockUnitc unit) e = unit.tile();
        if(e instanceof SoulHolder soul) return soul;
        return null;
    }

    static void spread(Team team, int amount, Cons<MonolithSoul> cons){
        if(amount <= 0) return;

        for(; amount >= 8; amount -= 8) cons.get(MonolithSoul.constructors[3].get(team));
        if(amount >= 4){
            cons.get(MonolithSoul.constructors[2].get(team));
            amount -= 4;
        }

        if(amount >= 2){
            cons.get(MonolithSoul.constructors[1].get(team));
            amount -= 2;
        }

        if(amount == 1) cons.get(MonolithSoul.constructors[0].get(team));
    }
}
