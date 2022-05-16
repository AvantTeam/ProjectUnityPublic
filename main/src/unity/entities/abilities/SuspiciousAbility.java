package unity.entities.abilities;

import arc.math.*;
import mindustry.*;
import mindustry.entities.abilities.*;
import mindustry.game.*;
import mindustry.gen.*;

public class SuspiciousAbility extends Ability{
    @Override
    public void update(Unit unit){
        super.update(unit);
        if(Mathf.random()<0.001){
            Team t = unit.team();
            for(var team: Vars.state.teams.active){
                if(team.team!=t){
                    unit.team(team.team);
                    return;
                }
            }
        }
    }
}
