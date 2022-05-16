package unity.world.blocks.payloads;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.payloads.Constructor.*;
import mindustry.world.blocks.payloads.PayloadBlock.*;
import mindustry.world.blocks.storage.*;
import unity.graphics.*;
import unity.util.*;
import unity.world.blocks.*;
import unity.world.blocks.production.*;
import unity.world.meta.*;

import static mindustry.Vars.*;

//despite its looks, its not a payload block bc im too lazy.
public class PayloadCaster extends GenericCaster{
    TextureRegion floor,platter,platterside,payloadExit,spout,liquid;
    TextureRegion[] base;

    public PayloadCaster(String name){
        super(name);
        configurable = true;
        outputsPayload = true;

        configClear((PayloadCasterBuild tile) -> tile.cast = null);
        config(Block.class, (PayloadCasterBuild tile, Block block) -> {
            if(tile.cast != block) tile.progress = 0f;
            if(canProduce(block)){
                tile.cast = block;
            }
        });
    }
    @Override
    public void load(){
        super.load();
        floor = loadTex("floor");
        platter = loadTex("platter");
        platterside = loadTex("platterside");
        spout = loadTex("spout");
        liquid = loadTex("liquid");
        payloadExit = loadTex("payloadExit");
        base = new TextureRegion[4];
        base[0] = loadTex("base1");
        base[1] = loadTex("base2");
        base[2] = loadTex("base3");
        base[3] = loadTex("base4");
    }

    public boolean canProduce(Block b){
       var req =  b.requirements;
       for(var itemstack:req){
           if(!CrucibleRecipes.items.containsKey(itemstack.item)){
               return false;
           }
       }
       return b.size == 1  && ((b instanceof ModuleBlock mb && mb.castable) || b instanceof Wall || b.requirements.length==1) && !state.rules.bannedBlocks.contains(b);
    }



    public class PayloadCasterBuild extends GenericCasterBuild{
        public @Nullable Block cast;
        public @Nullable Block currentlycasting = null;
        public boolean casting = false;
        public BuildPayload waiting = null;

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(PayloadCaster.this, table, content.blocks().select(PayloadCaster.this::canProduce), () -> cast, this::configure);
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(this == other){
                deselect();
                configure(null);
                return false;
            }

            return true;
        }

        @Override
        public Object config(){
            return cast;
        }

        @Override
        public boolean isCasting(){
            return casting;
        }

        @Override
        public Payload takePayload(){
            var t = waiting;
            if(waiting!=null){
                waiting = null;
                casting = false;
                progress = 0;
            }
            return t;
        }

        @Override
        public Payload getPayload(){
            return waiting;
        }

        @Override
        public void tryStartCast(){
            var crucible = crucibleNode();
            if(cast==null){
                return;
            }
            for(var i: cast.requirements){
                if(crucible.getFluid(CrucibleRecipes.items.get(i.item)).melted<i.amount){
                    return; // not enough items.
                }
            }
            //begin cast
            for(var i: cast.requirements){
                crucible.getFluid(CrucibleRecipes.items.get(i.item)).melted-=i.amount;
            }
            casting = true;
            currentlycasting = cast;
        }

        @Override
        public void offloadCast(){
            if(waiting ==null ){
                return;
            }
            var build = front();
            if(build!=null){
                build.handlePayload(this,waiting);
            }
        }

        @Override
        public void resetCast(){
            casting = false;
            waiting = null;
        }

        @Override
        public boolean canOffloadCast(){
            if(currentlycasting==null){
                return false;
            }
            if(casting){
                if(waiting ==null){
                    waiting = new BuildPayload(currentlycasting,this.team);
                    waiting.set(x+Geometry.d4x(rotation)*size*4,y+Geometry.d4y(rotation)*size*4,rotdeg());
                }
                var build = front();
                if(build !=null){
                    if(build.acceptPayload(this,waiting)){
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void draw(){
            var crucible = crucibleNode();
            Draw.rect(floor,x,y);

            Draw.rect(payloadExit,x,y,rotdeg());
            Draw.z(Layer.blockOver);
            var cast = currentlycasting;
            if(isCasting()){
                float castprog = Mathf.curve(progress,0,castTime);
                if(progress<castTime){
                    Draw.rect(platter,x,y);
                    Drawf.shadow(x,y, cast.size * tilesize * 2f, castprog);
                    Draw.rect(cast.region,x,y,castprog*tilesize,castprog*tilesize);
                }else{
                    float moveprog = Mathf.curve(progress,castTime,castTime+moveTime);
                    float keyframe1 = 0.4f;//normally its 0.25
                    float keyframe2 = 0.8f;
                    float keyframe3 = 1f;
                    float plateangle = 45;
                    Vec2 offset = new Vec2(Geometry.d4(rotation).x,Geometry.d4(rotation).y);
                    Vec3 perp = new Vec3(Geometry.d4(rotation+1).x,Geometry.d4(rotation+1).y,0);
                    offset.scl(5); //half block + 4 pixels
                    Vec3 pivot = new Vec3(x+offset.x,y+offset.y,0);

                    if(moveprog<keyframe1){
                        //platter lifts up with the casted thing
                        float prog = Mathf.curve(moveprog,0,keyframe1);
                        float ang = Utils.interp(0,plateangle,prog);
                        UnityDrawf.drawRectOrtho(platter, pivot.x, pivot.y, -5,0,-2, platter.width*0.25f, platter.height*0.25f, ang,rotdeg());
                        UnityDrawf.drawRectOrtho(platterside, pivot.x, pivot.y, 0,0,-12, 4, 16, ang-90,rotdeg());

                        float edgemovescl = Mathf.lerp(1,4/5f,prog*prog);
                        Vec3 cubepos = new Vec3(-offset.x*edgemovescl,-offset.y*edgemovescl,-(2+4)); //rel to pivot
                        cubepos.rotate(perp,ang);
                        Drawf.shadow(pivot.x+cubepos.x, pivot.y+cubepos.y, cast.size * tilesize * 2f, 1);
                        UnityDrawf.drawRectOrtho(cast.region, pivot.x+cubepos.x, pivot.y+cubepos.y, -4, cast.region.width*0.25f, cast.region.height*0.25f, ang,rotdeg());
                        UnityDrawf.drawRectOrtho(cast.region, pivot.x+cubepos.x, pivot.y+cubepos.y, -4, cast.region.width*0.25f, cast.region.height*0.25f, ang-90,rotdeg());
                    }else if(moveprog<keyframe2){
                        //casted thing drops down
                        UnityDrawf.drawRectOrtho(platter, pivot.x, pivot.y, -5,0,-2, platter.width*0.25f, platter.height*0.25f, 45,rotdeg());
                        UnityDrawf.drawRectOrtho(platterside, pivot.x, pivot.y, 0,0,-12, 4, 16, 45-90,rotdeg());

                        float prog = Mathf.curve(moveprog,keyframe1,keyframe2);
                        float edgemovescl = 4/5f;
                        Vec3 cubepos = new Vec3(-offset.x*edgemovescl,-offset.y*edgemovescl,-(2+4)); //rel to pivot
                        float ang = Utils.sqinterp(plateangle,90,prog);
                        cubepos.rotate(perp,ang);
                        Drawf.shadow(pivot.x+cubepos.x, pivot.y+cubepos.y, cast.size * tilesize * 2f, 1);
                        UnityDrawf.drawRectOrtho(cast.region, pivot.x+cubepos.x, pivot.y+cubepos.y, -4, cast.region.width*0.25f, cast.region.height*0.25f, ang,rotdeg());
                        UnityDrawf.drawRectOrtho(cast.region, pivot.x+cubepos.x, pivot.y+cubepos.y, -4, cast.region.width*0.25f, cast.region.height*0.25f, ang-90,rotdeg());

                    }else if(moveprog<=keyframe3){
                        //casted thing moves to end, plates goes back
                        float prog = Mathf.curve(moveprog,keyframe2,keyframe3);
                        float ang = Utils.interp(plateangle,0,prog);
                        UnityDrawf.drawRectOrtho(platter, pivot.x, pivot.y, -5,0,-2, platter.width*0.25f, platter.height*0.25f, ang,rotdeg());
                        UnityDrawf.drawRectOrtho(platterside, pivot.x, pivot.y, 0,0,-12, 4, 16, ang-90,rotdeg());
                        float px = x+offset.x*Mathf.lerp(11,12,prog)/5f;
                        float py = y+offset.y*Mathf.lerp(11,12,prog)/5f;
                        Drawf.shadow(px, py, cast.size * tilesize * 2f, 1);
                        Draw.rect(cast.region,px,py,castprog*tilesize,castprog*tilesize);
                    }
                    //yeh :)
                }
            }else{
                Draw.rect(platter,x,y);
            }
            Draw.rect(base[rotation],x,y);
            Draw.rect(spout,x,y,rotdeg());
            Draw.color(crucible.getColor());
            Draw.rect(liquid,x,y,rotdeg());
            Draw.color();

            drawTeamTop();
        }

        @Override
          public void write(Writes write){
              super.write(write);
              write.s(cast == null ? -1 : cast.id);
              write.s(currentlycasting == null ? -1 : currentlycasting.id);
          }

          @Override
          public void read(Reads read, byte revision){
              super.read(read, revision);
              cast = Vars.content.block(read.s());
              currentlycasting = Vars.content.block(read.s());
          }
    }
}
