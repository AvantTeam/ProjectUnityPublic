package unity.entities.type;

import arc.func.*;
import arc.graphics.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;

/**
 * Effect entity types that support custom state and data provider.
 * @author EyeOfDarkness
 * @author GlennFolker
 */
public class PUEffect extends Effect{
    public Prov<? extends EffectState> constructor;
    public @Nullable Prov<Object> data;

    public PUEffect(float lifetime, Cons<EffectContainer> e){
        this(EffectState::create, lifetime, e);
    }

    public PUEffect(float lifetime, float clip, Cons<EffectContainer> e){
        this(EffectState::create, lifetime, clip, e);
    }

    public PUEffect(Prov<? extends EffectState> constructor, float lifetime, Cons<EffectContainer> e){
        this(constructor, null, lifetime, e);
    }

    public PUEffect(Prov<? extends EffectState> constructor, float lifetime, float clip, Cons<EffectContainer> e){
        this(constructor, null, lifetime, clip, e);
    }

    public PUEffect(Prov<? extends EffectState> constructor, @Nullable Prov<Object> data, float lifetime, Cons<EffectContainer> e){
        this(constructor, data, lifetime, 50f, e);
    }

    public PUEffect(Prov<? extends EffectState> constructor, @Nullable Prov<Object> data, float lifetime, float clip, Cons<EffectContainer> e){
        super(lifetime, clip, e);
        this.constructor = constructor;
        this.data = data;
    }

    @Override
    protected void add(float x, float y, float rotation, Color color, Object data){
        inst(x, y, rotation, color, data).add();
    }

    protected EffectState inst(float x, float y, float rotation, Color color, Object data){
        EffectState e = constructor.get();
        e.effect = this;
        e.rotation = baseRotation + rotation;
        e.data = this.data != null ? this.data.get() : data;
        e.lifetime = lifetime;
        e.set(x, y);
        e.color.set(color);
        if(followParent && data instanceof Posc p){
            e.parent = p;
            e.rotWithParent = rotWithParent;
        }

        return e;
    }
}
