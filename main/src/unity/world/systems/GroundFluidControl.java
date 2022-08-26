package unity.world.systems;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.io.SaveFileReader.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import unity.content.blocks.*;
import unity.world.blocks.*;

import java.io.*;
import java.util.*;

import static mindustry.Vars.*;

//fluid on the ground :0
public class GroundFluidControl implements CustomChunk{
    static final int D_BOTTOM = 0;
    static final int D_LEFT = 1;
    static final int D_TOP = 2;
    static final int D_RIGHT = 3;

    //content

    public static final Seq<GroundLiquidProperties> liquidProperties = new Seq<>();
    static final IntMap<GroundLiquidProperties> liquidAssociationMap = new IntMap<>();
    static LiquidInteraction[] interactions;
    static GroundLiquidProperties[] liquidPropertiesArray;
    public static GroundLiquidProperties water, sulfur, neoplasm;

    public static void initialiseContent(){
        water = new GroundLiquidProperties(Liquids.water);
        sulfur = new GroundLiquidProperties("", Color.rgb(255, 180, 30));
        sulfur.setDensity(2); // twice as dense as water
        sulfur.setFlowspd(0.1f); // more viscous
        sulfur.minamount = 0.04f;
        sulfur.maxLiquidOutflow = 0.7f;
        neoplasm = new GroundLiquidProperties(Liquids.neoplasm);
        neoplasm.setDensity(3);
        neoplasm.setFlowspd(0.2f);
        neoplasm.minamount = 0.03f;
        neoplasm.maxLiquidOutflow = 1f;
        //blah blah
        liquidPropertiesArray = liquidProperties.toArray(GroundLiquidProperties.class);
        interactions = new LiquidInteraction[liquidProperties.size * liquidProperties.size];
        //interactions after etc
        addInteraction(neoplasm.id,water.id,(neo, water, am) -> {
            float n = am[water];
            n = Math.min(n,n*0.5f+0.1f)*dt;
            am[neo]+=n*0.5; // nom nom
            am[water]-=n;
        });


    }

    //local
    float[] previousLiquidAmount;
    float[] liquidAmount;
    int[] previousFluidType;
    int[] fluidType;
    float[] terrainHeight;
    float[] pressure;

    boolean[] terrainImpassable;
    boolean[] terrainAbsorber;

    float[] tmpvel;
    float[] vel;

    volatile int w, h, stride;

    boolean loadedStatic = false;
    static float dt = 0.25f;

    //temp
    float[] tmp = new float[4];

    //renderer, idk where to put it
    public GroundFluidRenderer renderer;

    //with the power of among us, no touchy
    private FluidRunnerThread fluidThread;


    static void addInteraction(int l1, int l2, LiquidInteraction a){
        interactions[(l1 - 1) + liquidProperties.size * (l2 - 1)] = a;
        interactions[(l2 - 1) + liquidProperties.size * (l1 - 1)] = (x, y, am) -> a.interact(y, x, am);
    }

    public GroundFluidControl(){
        SaveVersion.addCustomChunk("ground-fluid-data", this);

        Events.on(ResetEvent.class, e -> {
            stop();
        });

        Events.on(WorldLoadEvent.class, e -> {
            stop();

            loadedStatic = false;
            w = world.width();
            h = world.height();
            stride = w + 2;

            terrainHeight = new float[getArrayLength()];
            liquidAmount = new float[getArrayLength()];
            previousLiquidAmount = new float[getArrayLength()];
            pressure = new float[getArrayLength()];
            previousFluidType = new int[getArrayLength()];
            fluidType = new int[getArrayLength()];
            terrainImpassable = new boolean[getArrayLength()];
            terrainAbsorber = new boolean[getArrayLength()];
            vel = new float[getArrayLength() * 4];
            tmpvel = new float[getArrayLength() * 4];
            reloadOffsets();
            Log.info("fluid array size:" + liquidAmount.length);
            Log.info("vel array size:" + vel.length);
            pushStaticBlocks(true);
            loadedStatic = true;


            Log.info("Loaded world");
        });


        Events.on(TileChangeEvent.class, event -> {
            if(loadedStatic){
                updateTerrain(event.tile.x,event.tile.y);
            }
        });

        Events.run(Trigger.update, () -> {
            if(state.isGame()){
                if(!state.isPaused()){
                    if(fluidThread == null){
                        fluidThread = new FluidRunnerThread();
                        fluidThread.start();
                        Log.info("Started Fluid");
                    }
                    fluidThread.updateTime(Time.delta * 0.2f);
                }
            }
        });

        if(!net.server()){
            Log.info("Activated Fluid Drawer");

            Events.run(Trigger.draw, () -> {
                Draw.draw(Layer.blockOver - 1f, () -> {
                    if(loadedStatic){
                        renderer.draw();
                    }
                });

            });
            renderer = new GroundFluidRenderer();
        }
    }

    public float getTransition(){
        return fluidThread.transition;
    }

    public static boolean supportsLiquid(Liquid g){
        return (liquidAssociationMap.containsKey(g.id));
    }

    public void addFluid(Liquid g, Tile t, float amount){
        if(liquidAssociationMap.containsKey(g.id)){
            addFluid(liquidAssociationMap.get(g.id), t.x, t.y, amount);
        }
    }

    public void addFluid(GroundLiquidProperties g, Tile t, float amount){
        addFluid(g, t.x, t.y, amount);
    }

    public void addFluid(GroundLiquidProperties g, int x, int y, float amount){
        int ind = tileIndexOf(x, y);
        if(amount <= 0 || terrainImpassable[ind]){
            return;
        }

        liquidAmount[ind] += amount;
        fluidType[ind] = g == null ? water.id : g.id;
    }

    public void updateTerrain(int x, int y){
        int ind = tileIndexOf(x, y);
        var tile = world.tile(x, y);
        //how to save floor data?
        float theight = 1;
        if(tile.floor().liquidDrop != null || tile.floor()== YoungchaBlocks.pit){
            theight = 0;
            terrainImpassable[ind] = true;
            terrainAbsorber[ind] = true;
        }
        if(tile.block()!=Blocks.air){
            terrainAbsorber[ind] = false;
        }
        if(tile.block() instanceof StaticWall){
            theight = 99;
            terrainImpassable[ind] = true;
        }
        if(tile.build != null){
            if(tile.build instanceof GroundFluidTerrainBuild g){
                theight += g.terrainHeight();
            }else{
                theight += tile.block().solid ? 2 : 0.5;
            }
        }
        terrainHeight[ind] = theight;
        ind++;

    }

    public void pushStaticBlocks(boolean initial){
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                updateTerrain(x, y);
            }
        }
    }

    public void step(){
        c_amcount = new float[liquidProperties.size];
        int originIndex = tileIndexOf(0, 0);
        int ind = originIndex;

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                pressure[ind] = updatePressure(ind);
                ind++;
            }
            ind += 2;
        }
        ind = originIndex;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                updateOutflow(ind);
                ind++;
            }
            ind += 2;
        }

        ind = originIndex;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                previousFluidType[ind] = fluidType[ind];
                previousLiquidAmount[ind] = liquidAmount[ind];
                if(!terrainImpassable[ind]){
                    liquidAmount[ind] += depthChange(ind);
                    liquidAmount[ind] = Math.max(0, liquidAmount[ind]);
                    if(liquidAmount[ind] == 0){
                        fluidType[ind] = 0;
                    }
                }else{
                    liquidAmount[ind] = 0;
                }
                ind++;
            }
            ind += 2;
        }
    }

    private void stop(){
        Log.info("Stopped Fluid");
        if(fluidThread != null){
            fluidThread.terminate = true;
            fluidThread.interrupt();
        }
        fluidThread = null;
    }

    @Override
    public void write(DataOutput stream) throws IOException{

    }

    @Override
    public void read(DataInput stream) throws IOException{

    }

    float[] c_amcount = new float[4];
    int[] c_v = new int[4];

    float depthChange(int index){
        float incomingtotal = 0;
        float outgoingtotal = 0;
        int acceptedFluid = fluidType[index];
        int vindex = index * 4;
        c_v[D_BOTTOM] = bottomVel(vindex) + D_TOP;
        c_v[D_LEFT] = leftVel(vindex) + D_RIGHT;
        c_v[D_TOP] = topVel(vindex) + D_BOTTOM;
        c_v[D_RIGHT] = rightVel(vindex) + D_LEFT;
        if(acceptedFluid == 0){
            int t = 0;
            float best = 0;
            Arrays.fill(c_amcount, 0);
            for(int dir = 0; dir < 4; dir++){
                t = fluidType[offset(index, dir)];
                if(t == 0){
                    continue;
                }
                c_amcount[t - 1] += tmpvel[c_v[dir]] * liquidPropertiesArray[t - 1].density;
                if(c_amcount[t - 1] > best){
                    best = c_amcount[t - 1];
                    acceptedFluid = t;
                }
            }
            if(acceptedFluid == 0){
                return 0; //? nothing to do
            }
            for(int dir = 0; dir < 4; dir++){
                if(fluidType[offset(index, dir)] != acceptedFluid){
                    tmpvel[c_v[dir]] = 0;
                }
            }
        }
        incomingtotal += tmp[D_BOTTOM] = tmpvel[bottomVel(vindex) + D_TOP];
        incomingtotal += tmp[D_LEFT] = tmpvel[leftVel(vindex) + D_RIGHT];
        incomingtotal += tmp[D_TOP] = tmpvel[topVel(vindex) + D_BOTTOM];
        incomingtotal += tmp[D_RIGHT] = tmpvel[rightVel(vindex) + D_LEFT];


        GroundLiquidProperties prop = liquidPropertiesArray[acceptedFluid - 1];
        for(int dir = 0; dir < 4; dir++){
            outgoingtotal += tmpvel[vindex + dir];
        }
        float netincome = incomingtotal - outgoingtotal;
        float evaporationAm = liquidAmount[index] < prop.minamount ? prop.fast_evaporation : prop.evaporation;
        for(int dir = 0; dir < 4; dir++){
            vel[vindex + dir] = Math.max(0, tmpvel[vindex + dir] - tmp[dir] * prop.outflowMomentumDamp);
        }
        fluidType[index] = acceptedFluid;
        return (netincome - evaporationAm) * dt;
    }


    LiquidInteraction getInteraction(int l1, int l2){
        return interactions[(l1 - 1) + liquidProperties.size * (l2 - 1)];
    }

    float updatePressure(int index){
        if(liquidAmount[index] == 0){
            return 0;
        }
        float olevel = liquidAmount[index] + terrainHeight[index];
        int ot = fluidType[index], tt;
        float p = 0;
        int t;
        LiquidInteraction it;
        for(int i = 0; i < 4; i++){
            t = offset(index, i);
            tt = fluidType[t];
            if(ot != tt && tt != 0){
                it = getInteraction(ot, tt);
                if(it != null){
                    it.interact(index, t, liquidAmount);
                }
                p += Math.max(0, liquidAmount[t] + terrainHeight[t] - olevel) * liquidPropertiesArray[tt - 1].fluxDensity;
            }
        }
        return p;
    }

    void reset(float[] t, int index){
        t[index] = 0;
        t[index + 1] = 0;
        t[index + 2] = 0;
        t[index + 3] = 0;
    }


    void updateOutflow(int index){
        if(liquidAmount[index] == 0){
            reset(tmpvel, index * 4);
            return;
        }
        int vindex = index * 4;
        float total = 0;
        float originHeight = liquidAmount[index] + terrainHeight[index] + pressure[index];
        GroundLiquidProperties prop = liquidPropertiesArray[fluidType[index] - 1];
        total += tmp[0] = outflow(prop, index, originHeight, bottom(index), vel[vindex + D_BOTTOM]);
        total += tmp[1] = outflow(prop, index, originHeight, left(index), vel[vindex + D_LEFT]);
        total += tmp[2] = outflow(prop, index, originHeight, top(index), vel[vindex + D_TOP]);
        total += tmp[3] = outflow(prop, index, originHeight, right(index), vel[vindex + D_RIGHT]);

        if(total == 0){
            reset(tmpvel, index * 4);
            return;
        }
        float weightTotal = liquidAmount[index] / (total * dt);
        if(weightTotal > 1f){
            weightTotal = 1f;
        }
        for(int dir = 0; dir < 4; dir++){
            tmpvel[vindex + dir] = tmp[dir] * weightTotal;
        }
    }


    float outflow(GroundLiquidProperties prop, int origin, float originHeight, int target, float oldflow){
        if(prop.id != fluidType[target] && fluidType[target] != 0){
            return 0;
        }
        float terrainO = terrainHeight[origin];
        float terrainT = terrainHeight[target];
        float hdiff = originHeight - liquidAmount[target] - terrainT-  pressure[target];
        if(liquidAmount[target] == 0){
            hdiff -= prop.minamount;
        }

        float addedFlow = dt * prop.flowspd * hdiff;
        float newFlow = oldflow + addedFlow;
        if(terrainO > terrainT && newFlow > prop.maxLiquidOutflow){
            return prop.maxLiquidOutflow;
        }
        if(newFlow > 0){
            return newFlow;
        }
        return 0;
    }


    int getArrayLength(){
        return (w + 2) * (h + 2);
    }

    static final int[] offsets = new int[4];

    void reloadOffsets(){
        offsets[D_BOTTOM] = -stride;
        offsets[D_LEFT] = -1;
        offsets[D_TOP] = stride;
        offsets[D_RIGHT] = 1;
    }

    int offset(int index, int dir){
        return index + offsets[dir];
    }

    int top(int index){
        return index + stride;
    }

    int left(int index){
        return index - 1;
    }

    int bottom(int index){
        return index - stride;
    }

    int right(int index){
        return index + 1;
    }

    int topVel(int index){
        return index + stride * 4;
    }

    int leftVel(int index){
        return index - 4;
    }

    int bottomVel(int index){
        return index - stride * 4;
    }

    int rightVel(int index){
        return index + 4;
    }

    public int tileIndexOf(int x, int y){
        return (x + 1) + (y + 1) * stride;
    }

    public float fluidAmount(int x, int y){
        return liquidAmount[tileIndexOf(x, y)];
    }

    public float fluidVelX(int x, int y){
        int t = tileIndexOf(x, y) * 4;
        return vel[t + D_RIGHT] - vel[t + D_LEFT];
    }
    public float fluidVelX(int index){
        return vel[index * 4 + D_RIGHT] - vel[index * 4 + D_LEFT];
    }
    public float tmpfluidVelX(int index){
        return tmpvel[index * 4 + D_RIGHT] - tmpvel[index * 4 + D_LEFT];
    }

    public float fluidVelY(int x, int y){
        int t = tileIndexOf(x, y) * 4;
        return vel[t + D_TOP] - vel[t + D_BOTTOM];
    }
    public float fluidVelY(int index){
        return vel[index * 4 + D_TOP] - vel[index * 4 + D_BOTTOM];
    }
    public float tmpfluidVelY(int index){
        return tmpvel[index * 4 + D_TOP] - tmpvel[index * 4 + D_BOTTOM];
    }

    public GroundLiquidProperties fluidType(int x, int y){
        return liquidProperties.get(fluidType[tileIndexOf(x, y)]);
    }

    GroundLiquidProperties getFluidType(int tileindex){
        return liquidProperties.get(fluidType[tileindex] - 1);
    }
    // used for drawin
    GroundLiquidProperties getVisualFluidType(int tileindex){
       return liquidProperties.get(Math.max(fluidType[tileindex],previousFluidType[tileindex]) - 1);
   }

    //Unity.groundFluidControl.addFluid(null,85,101,5)
    //Unity.groundFluidControl.fluidAmount(85,101)

    public static class GroundLiquidProperties{
        public String name;
        // minimum amount in tile before it can spread to adjacent ones
        public float minamount = 0.02f;

        public float maxLiquidOutflow = 1.9f;
        public float evaporation = 0.00005f;
        public float fast_evaporation = 0.0005f;
        private float density = 1;
        private float flowspd = 0.9f;
        private float fluxDensity = 0.5f;
        float outflowMomentumDamp = 0.8f;
        public Liquid association;
        private static int idAcc = 0;
        public Color color;
        //texture as well probably

        int id = ++idAcc;

        public GroundLiquidProperties(String name, Color color){
            this.name = name;
            this.color = color;
            liquidProperties.add(this);
        }

        public GroundLiquidProperties(Liquid l){
            this(l.name, l.color);
            association = l;
            liquidAssociationMap.put(l.id,this);
        }

        public float getFlowspd(){
            return flowspd;
        }

        public float getDensity(){
            return density;
        }

        public void setFlowspd(float flowspd){
            this.flowspd = flowspd;
            fluxDensity = flowspd * density;
        }

        public void setDensity(float density){
            this.density = density;
            fluxDensity = flowspd * density;
        }

        public float getFluxDensity(){
            return fluxDensity;
        }
    }

    public interface LiquidInteraction{
        public void interact(int index, int index2, float[] am);
    }

    class FluidRunnerThread extends Thread{
        public volatile float transition;
        float targetTime;
        int currentTime;
        final Object waitSync = new Object();
        boolean terminate = false; // not really used, but if you want to 'play it safe' ig?

        public void run(){

            while(!terminate){
                try{
                    synchronized(waitSync){
                        try{
                            waitSync.wait();
                        }catch(InterruptedException e){
                            return;
                        }
                    }
                    while(currentTime < targetTime){
                        step();
                        synchronized(waitSync){
                            currentTime++;
                            updateTransition();
                        }
                    }
                }catch(Exception e){
                    Log.debug(e);
                    Log.debug(Arrays.asList(e.getStackTrace()).toString());
                    Call.sendChatMessage(e.toString());
                }
            }
        }

        private void updateTransition(){
            transition = 1f-Mathf.clamp(currentTime-targetTime);

        }

        public void updateTime(float delta){
            targetTime += delta;
            synchronized(waitSync){
                updateTransition();
                if(targetTime > currentTime){
                    waitSync.notify();
                }
            }
        }


    }


    /*
    Probably not needed rn.
    //well its what the struct annotation does (debateful if its faster single threaded, but will defs carry good multithreaded performance (less data being moved)
    public static final class GroundFluidData{
        public static final long bitMaskAmount = 0x00000000FFFFFFFFL; //the last part is allocated to this float.
        public static final long bitMaskFluid =  0x0000FFFF00000000L; //another part is allocated to the fluid id

        public static float amount(long g){ return Float.intBitsToFloat((int)(bitMaskAmount&g));}
        public static long amountSet(long g,float am){ return (g&~bitMaskAmount) | Float.floatToIntBits(am);}

        public static int liquid(long g){ return (int)(bitMaskFluid&g);}
        public static long liquid(long g, int fluid){ return (g&~bitMaskFluid)|((long)fluid <<32L);}

        public static long setLiquid(int fluid, float amount){return ((long)fluid <<32L)|Float.floatToIntBits(amount);}
    }
    */
}
