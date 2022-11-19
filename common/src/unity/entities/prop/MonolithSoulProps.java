package unity.entities.prop;

import arc.audio.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import unity.entities.type.PUUnitTypeCommon.*;
import unity.mod.*;

/**
 * {@linkplain Faction#monolith Monolith} soul properties.
 * @author GlennFolker
 */
public class MonolithSoulProps extends Props{
    public int transferAmount = 1, formAmount = 5;

    public float
    formDelta = 1.8f,
    healthJoinDelta = -0.2f, transferDelay = 0f,
    joinWarmup = 0.008f, joinCooldown = 0.1f, formWarmup = 0.17f,
    splitVelMin = 6f, splitVelMax = 12f,
    ringRotateSpeed = 0.08f,

    crackPitchMin = 0.9f, crackPitchMax = 1.1f,
    joinPitchMin = 0.8f, joinPitchMax = 1.2f,
    formPitchMin = 0.9f, formPitchMax = 1.1f;

    public Sound crackSound = Sounds.none, joinSound = Sounds.none, formSound = Sounds.none;
    public Effect crackEffect = Fx.none, joinEffect = Fx.none, formEffect = Fx.none, transferEffect = Fx.none;
}
