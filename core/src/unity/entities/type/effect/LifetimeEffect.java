package unity.entities.type.effect;

import arc.func.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.entities.effect.*;
import mindustry.gen.*;
import unity.entities.type.*;

/**
 * A type of effect that's able to dynamically modify its lifetime without its lifetime scale going backwards.
 * Will not work with {@link SeqEffect}.
 * @author GlennFolker
 */
public class LifetimeEffect extends PUEffect{
    private static final Adjuster adjust = new Adjuster();

    public LifetimeEffect(float lifetime, Cons2<EffectContainer, Adjuster> e){
        this(LifetimeEffectState::create, null, lifetime, 50f, e);
    }

    public LifetimeEffect(float lifetime, float clip, Cons2<EffectContainer, Adjuster> e){
        this(LifetimeEffectState::create, null, lifetime, clip, e);
    }

    public LifetimeEffect(Prov<? extends LifetimeEffectState> constructor, float lifetime, Cons2<EffectContainer, Adjuster> e){
        this(constructor, null, lifetime, 50f, e);
    }

    public LifetimeEffect(Prov<? extends LifetimeEffectState> constructor, float lifetime, float clip, Cons2<EffectContainer, Adjuster> e){
        this(constructor, null, lifetime, clip, e);
    }

    public LifetimeEffect(Prov<? extends LifetimeEffectState> constructor, @Nullable Prov<Object> data, float lifetime, Cons2<EffectContainer, Adjuster> e){
        this(constructor, data, lifetime, 50f, e);
    }

    public LifetimeEffect(Prov<? extends LifetimeEffectState> constructor, @Nullable Prov<Object> data, float lifetime, float clip, Cons2<EffectContainer, Adjuster> e){
        super(constructor, data, lifetime, clip, null);
        renderer = createRenderer(e);
    }

    public Cons<EffectContainer> createRenderer(Cons2<EffectContainer, Adjuster> cons){
        return e -> {
            adjust.set(this, e);
            cons.get(e, adjust);
        };
    }

    public static class Adjuster{
        private EffectContainer container;
        private float defLifetime;
        private float lifetime;

        public void set(LifetimeEffect effect, EffectContainer container){
            this.container = container;
            defLifetime = effect.lifetime;
        }

        public float lifetime(float newLifetime){
            float prev = lifetime;
            lifetime = container.lifetime = newLifetime;
            container.time = lifetime * (container.time / prev);

            return scl();
        }

        public float scl(){
            return lifetime / defLifetime;
        }

        public void remove(){
            lifetime = container.lifetime = -1f;
        }
    }

    public static class LifetimeEffectState extends EffectState{
        public static LifetimeEffectState create(){
            return Pools.obtain(LifetimeEffectState.class, LifetimeEffectState::new);
        }

        @Override
        public void draw(){
            float prev = lifetime;
            lifetime = effect.render(id, color, time, lifetime, rotation, x, y, data);
            time = lifetime * (time / prev);
        }
    }
}
