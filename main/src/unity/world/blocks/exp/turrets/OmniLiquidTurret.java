package unity.world.blocks.exp.turrets;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import unity.entities.bullet.exp.*;
import unity.world.blocks.exp.*;

import static arc.Core.atlas;
import static mindustry.Vars.*;

public class OmniLiquidTurret extends ExpTurret {
    public TextureRegion liquidRegion;
    public TextureRegion topRegion;
    public boolean extinguish = true;
    public BulletType shootType;
    public float shootAmount = 0.5f;

    public OmniLiquidTurret(String name){
        super(name);
        hasLiquids = true;
        loopSound = Sounds.spray;
        shootSound = Sounds.none;
        smokeEffect = Fx.none;
        shootEffect = Fx.none;
        outlinedIcon = 1;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.ammo, ammo(0));
    }

    @Override
    public void load(){
        super.load();
        liquidRegion = atlas.find(name + "-liquid");
        topRegion = atlas.find(name + "-top");
    }

    public static boolean friendly(Liquid l){
        return l.effect != StatusEffects.none && l.effect.damage <= 0.1f && (l.effect.damage < -0.01f || l.effect.healthMultiplier > 1.01f || l.effect.damageMultiplier > 1.01f);
    }

    public StatValue ammo(int indent){
        if(!(shootType instanceof GeyserLaserBulletType g)) return table -> {};
        GeyserBulletType type = (GeyserBulletType) g.geyser;
        return table -> {
            table.row();

            for(Liquid t : content.liquids()){
                boolean compact = indent > 0;

                table.image(t.uiIcon).size(3 * 8).padRight(4).right().top();
                table.add(t.localizedName).padRight(10).left().top();

                table.table(bt -> {
                    bt.left().defaults().padRight(3).left();

                    //damage of geyser
                    float damage = type.damage * GeyserBulletType.damageScale(t) * 60f;
                    if(damage > 0f) sep(bt, Core.bundle.format("bullet.splashdamage", Strings.autoFixed(damage, 1), Strings.fixed(type.radius / tilesize, 1)));
                    else sep(bt, Core.bundle.format("bullet.splashheal", Strings.autoFixed(-damage, 1), Strings.fixed(type.radius / tilesize, 1)));

                    float kn = GeyserBulletType.knockbackScale(t) * type.knockback;
                    if(kn > 0){
                        sep(bt, Core.bundle.format("bullet.knockback", Strings.autoFixed(kn, 2)));
                    }

                    if(t.temperature > 0.8f){
                        sep(bt, "@bullet.incendiary");
                    }

                    if(GeyserBulletType.hasLightning(t)){
                        sep(bt, Core.bundle.format("bullet.lightning", (int)(1 + t.heatCapacity * 5), damage * 0.5f));
                    }

                    if(t.effect != StatusEffects.none){
                        sep(bt, (t.effect.minfo.mod == null ? t.effect.emoji() : "") + "[stat]" + t.effect.localizedName);
                    }
                }).padTop(compact ? 0 : -9).padLeft(indent * 8).left().get().background(compact ? null : Tex.underline);

                table.row();
            }
        };
    }

    //for AmmoListValue
    private static void sep(Table table, String text){
        table.row();
        table.add(text);
    }

    public class OmniLiquidTurretBuild extends ExpTurretBuild{

        @Override
        public boolean shouldActiveSound(){
            return wasShooting && enabled;
        }

        @Override
        public void updateTile(){
            unit.ammo(unit.type().ammoCapacity * liquids.currentAmount() / liquidCapacity);

            super.updateTile();
        }

        @Override
        protected void findTarget(){
            if(extinguish && liquids.current().canExtinguish()){
                int tx = World.toTile(x), ty = World.toTile(y);
                Fire result = null;
                float mindst = 0f;
                int tr = (int)(range / tilesize);
                for(int x = -tr; x <= tr; x++){
                    for(int y = -tr; y <= tr; y++){
                        Tile other = world.tile(x + tx, y + ty);
                        var fire = Fires.get(x + tx, y + ty);
                        float dst = fire == null ? 0 : dst2(fire);
                        //do not extinguish fires on other team blocks
                        if(other != null && fire != null && Fires.has(other.x, other.y) && dst <= range * range && (result == null || dst < mindst) && (other.build == null || other.team() == team)){
                            result = fire;
                            mindst = dst;
                        }
                    }
                }

                if(result != null){
                    target = result;
                    //don't run standard targeting
                    return;
                }
            }

            super.findTarget();
        }

        @Override
        protected boolean canHeal(){
            return liquids.current() != null && friendly(liquids.current());
        }

        @Override
        public BulletType useAmmo(){
            if(cheating()) return shootType;
            liquids.remove(liquids.current(), shootAmount);
            return shootType;
        }

        @Override
        public BulletType peekAmmo(){
            return shootType;
        }

        @Override
        public boolean hasAmmo(){
            return liquids.currentAmount() >= shootAmount;
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return false;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            if(!hasLiquids) return false;
            return liquids.current() == liquid || liquids.currentAmount() < 0.2f;
        }

        //TODO as far as I can tell, the old turret just set the bullet data to the liquid. but I could be wrong -Anuke
        @Override
        protected void handleBullet(@Nullable Bullet bullet, float offsetX, float offsetY, float angleOffset){
            if(bullet != null){
                bullet.data = liquids.current();
            }
        }
    }
}