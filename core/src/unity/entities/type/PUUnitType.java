package unity.entities.type;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import unity.graphics.*;
import unity.util.*;

import static mindustry.Vars.headless;

/**
 * Base implementation of {@link PUUnitTypeCommon}.
 * @author GlennFolker
 */
@SuppressWarnings("unchecked")
public class PUUnitType extends PUUnitTypeCommon{
    /** The common properties of this unit type, mapped by its class. */
    public final OrderedMap<Class<? extends Props>, Props> properties = new OrderedMap<>();

    /** The trail constructor, as a support for custom trails. */
    public Func<Unit, Trail> trailType = unit -> new Trail(trailLength);
    /** Whether to kickstart trails when instantiating. Only valid if the trail is an instanceof {@link BaseTrail}. */
    public boolean kickstartTrail = false;

    public PUUnitType(String name){
        super(name);
        properties.orderedKeys().ordered = false;
    }

    public <T extends Unit> void trail(Func<T, Trail> trailType){
        trail(-2, trailType);
    }

    public <T extends Unit> void trail(int trailLength, Func<T, Trail> trailType){
        this.trailLength = trailLength;
        this.trailType = (Func<Unit, Trail>)trailType;
    }

    @Override
    public void drawTrail(Unit unit){
        if(!headless && unit.trail == null){
            Trail trail = createTrail(unit);
            if(kickstartTrail && trail instanceof BaseTrail t) kickstartTrail(unit, t);

            unit.trail = trail;
        }

        super.drawTrail(unit);
    }

    public <T extends Trail> T createTrail(Unit unit){
        return (T)trailType.get(unit);
    }

    public <T extends BaseTrail> T kickstartTrail(Unit unit, T trail){
        float scale = useEngineElevation ? unit.elevation : 1f;
        float offset = engineOffset / 2f + engineOffset / 2f * scale;

        float
        cx = unit.x + Angles.trnsx(unit.rotation + 180f, offset),
        cy = unit.y + Angles.trnsy(unit.rotation + 180f, offset);

        trail.kickstart(cx, cy);
        return trail;
    }

    @Override
    public OrderedMap<Class<? extends Props>, Props> properties(){
        return properties;
    }

    @Override
    public void prop(Props prop){
        Class<? extends Props> type = prop.getClass();
        if(type.isAnonymousClass()) type = (Class<? extends Props>)type.getSuperclass();

        properties.put(type, prop);
    }

    //Draw order of Prop depends on added order.
    @Override
    public <T extends Props> T prop(Class<T> type){
        return (T)properties.get(type);
    }

    @Override
    public <T extends Props> boolean hasProp(Class<T> type){
        return properties.containsKey(type);
    }

    @Override
    public void init(){
        for(Props prop : properties.values()) prop.preInit();
        super.init();
        for(Props prop : properties.values()) prop.init();

        if(trailLength == -2){
            boolean substitute = Core.atlas == null;
            if(substitute) Core.atlas = DrawUtils.emptyAtlas;

            trailLength = BaseTrail.length(createTrail(sample));

            if(substitute) Core.atlas = null;
        }
    }

    @Override
    public void load(){
        for(Props prop : properties.values()) prop.preLoad();
        super.load();
        for(Props prop : properties.values()) prop.load();
    }
}
