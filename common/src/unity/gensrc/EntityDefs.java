package unity.gensrc;

import mindustry.gen.*;
import unity.annotations.Annotations.*;
import unity.gen.entities.*;

final class EntityDefs{
    /** Monolith unit. */
    @EntityDef({Unitc.class, Monolithc.class}) Object monolithUnit;
    /** Monolith mech + unit. */
    @EntityDef({Unitc.class, Mechc.class, Monolithc.class}) Object monolithMechUnit;
    /** Monolith legs + unit. */
    @EntityDef({Unitc.class, Legsc.class, Monolithc.class}) Object monolithLegsUnit;

    private EntityDefs(){
        throw new AssertionError();
    }
}
