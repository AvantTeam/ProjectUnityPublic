package unity.entities.type;

import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;

/**
 * Common form of {@link UnitType}s. Stores composable properties.
 * @author GlennFolker
 */
public abstract class PUUnitTypeCommon extends UnitType{
    public PUUnitTypeCommon(String name){
        super(name);
    }

    public abstract ObjectMap<Class<? extends Props>, Props> properties();

    public abstract void prop(Props prop);

    public abstract <T extends Props> T prop(Class<T> type);

    public abstract <T extends Props> boolean hasProp(Class<T> type);

    public <T extends Props> T propReq(Class<T> type){
        T prop = prop(type);
        if(prop == null) throw new IllegalArgumentException("Property does not exist: '" + type.getSimpleName() + "'.");
        return prop;
    }

    /** Unit type properties, e.g. copter regions, soul amounts, etc. */
    public abstract static class Props{
        public final boolean drawEarly;

        public Props(){
            this.drawEarly = false;
        }

        public Props(boolean drawEarly){
            this.drawEarly = drawEarly;
        }

        public void preInit(){}

        public void init(){}

        public void preLoad(){}

        public void load(){}
    }
}
