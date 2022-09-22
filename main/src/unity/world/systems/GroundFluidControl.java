package unity.world.systems;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.io.SaveFileReader.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;
import unity.*;
import unity.content.blocks.*;
import unity.content.effects.*;
import unity.gen.*;
import unity.util.*;
import unity.world.blocks.*;
import unity.world.graph.*;

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
    public static float liquidPerUnit = 10f; // the amount of game liquid needed to fill 1 tile with 1 unit of ground fluid.
    public static float UnitPerLiquid = 1f / liquidPerUnit;
    public static final Seq<GroundLiquidProperties> liquidProperties = new Seq<>();
    static final IntMap<GroundLiquidProperties> liquidAssociationMap = new IntMap<>();
    static LiquidInteraction[] interactions;
    static GroundLiquidProperties[] liquidPropertiesArray;
    public static GroundLiquidProperties water, sulfur, neoplasm, slag, acid, cryofluid, tar; /// thats it >:(

    public static void initialiseContent(){
        water = new GroundLiquidProperties(Liquids.water);

        sulfur = new GroundLiquidProperties("sulfur", Color.rgb(255, 180, 30));
        sulfur.setDensity(1.5f); // twice as dense as water
        sulfur.setFlowspd(0.6f); // more viscous
        sulfur.minamount = 0.02f;
        sulfur.maxLiquidOutflow = 1.5f;
        sulfur.shallowColor.set(211 / 256f, 126 / 256f, 74 / 256f, 0.5f);

        neoplasm = new GroundLiquidProperties(Liquids.neoplasm);
        neoplasm.setDensity(3);
        neoplasm.setFlowspd(0.2f);
        neoplasm.minamount = 0.03f;
        neoplasm.maxLiquidOutflow = 1f;
        neoplasm.shallowColor.set(211 / 256f, 126 / 256f, 74 / 256f, 0.9f);

        slag = new GroundLiquidProperties(Liquids.slag);
        slag.setDensity(4);
        slag.setFlowspd(0.1f);
        slag.minamount = 0.05f;
        slag.maxLiquidOutflow = 0.8f;
        slag.shallowColor.set(1f, 186 / 256f, 124 / 256f, 1f);

        slag.onUnitTouch = (unit, slag) -> {
            if(unit.isImmune(StatusEffects.melting)){
                return;
            }
            float mul = 5;
            if(unit.isImmune(StatusEffects.burning)){
                mul = 1;
            }
            unit.damage(slag * Time.delta* mul);
        };

        slag.onBuildTouch = (build,slag,pos,dir)->{
           if(build instanceof GraphBuild gb){
               var v = gb.heatNode();
               if(v!=null){
                   v.generateHeat(700+HeatGraphNode.celsiusZero,0.01f); // well
                   return;
               }
               if(gb.crucibleNode()!=null){
                   return;
               }
           }
           if(build==null){
               return;
           }
           build.damage(Time.delta*(slag*1.5f+0.5f));
           Fires.create(world.tile(pos));
        };

        //blah blah
        liquidPropertiesArray = liquidProperties.toArray(GroundLiquidProperties.class);
        interactions = new LiquidInteraction[liquidProperties.size * liquidProperties.size];
        //interactions after etc
        //do particle effects go in update?
        addInteraction(neoplasm.id, water.id, (neo, water, am) -> {
            float n = am[water];
            n = Math.min(n, n * 0.5f + 0.1f) * dt;
            am[neo] += n * 0.5; // nom nom
            am[water] -= n;
        });

        addInteraction(slag.id, water.id, (slag, water, am) -> {
            float n = am[water];
            n = Math.min(n, am[slag] + 0.01f) * dt;
            am[slag] -= n * 0.1;
            am[water] -= n;
        });

        //todo: this is kinda temperorary
        addFloorHeight((Floor)Blocks.shale,0.1f);
        addFloorHeight((Floor)Blocks.darksand,0.5f);
        addFloorHeight((Floor)YoungchaBlocks.stoneHalf,1.3f);
        addFloorHeight((Floor)YoungchaBlocks.stoneFull,1.6f);
        addFloorHeight((Floor)YoungchaBlocks.concrete,2f);
        addFloorHeight((Floor)YoungchaBlocks.concreteBlank,2f);
        addFloorHeight((Floor)YoungchaBlocks.concreteFill,2f);
        addFloorHeight((Floor)YoungchaBlocks.concreteNumber,2f);
        addFloorHeight((Floor)YoungchaBlocks.concreteStripe,2f);
    }

    //testing
    public static IntMap<Float> floorHeights = new IntMap<>();

    public static void addFloorHeight(Floor r, float t){
        floorHeights.put(r.id, t);
        if(Unity.groundFluidControl!=null && Unity.groundFluidControl.loadedStatic){
            Unity.groundFluidControl.pushStaticBlocks(false);
        }
    }

    //local
    float[] previousLiquidAmount;
    float[] liquidAmount;
    int[] previousFluidType;
    int[] fluidType;
    float[] terrainHeight; // another thing for static terrain height
    float[] pressure;

    boolean[] terrainImpassable;
    boolean[] terrainAbsorber;
    boolean[] hasBuilding; // something cache friendly

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
            createGridAnew(world.width(), world.height());
            reloadOffsets();
            pushStaticBlocks(true);
            loadedStatic = true;
            Log.info("Loaded world");
        });


        Events.on(TileChangeEvent.class, event -> {
            if(loadedStatic){
                if(event.tile.build != null){
                    updateBuildTerrain(event.tile.build);
                }else{
                    updateTerrain(event.tile.x, event.tile.y);
                }
            }
        });
        //on tile removed
        Events.on(TilePreChangeEvent.class, event -> {
            if(loadedStatic){
                if(event.tile.build != null){
                    updateBuildTerrain(event.tile.build);
                }else{
                    updateTerrain(event.tile.x, event.tile.y);
                }
            }
        });

        Events.run(Trigger.update, () -> {
            if(state.isGame()){
                if(!state.isPaused()){
                    if(fluidThread == null){
                        fluidThread = new FluidRunnerThread();
                        fluidThread.setPriority(Thread.NORM_PRIORITY - 1);
                        fluidThread.setDaemon(true);
                        fluidThread.start();
                        Log.info("Started Fluid");
                    }
                    fluidThread.updateTime(Time.delta * 0.2f);

                    Groups.unit.each(u -> !u.isFlying(), unit -> {
                        int ind = tileIndexOf(unit.tileX(), unit.tileY());
                        if(ind >= 0 && ind < liquidAmount.length && liquidAmount[ind] > 0){
                            var fluidtype = getFluidType(ind);
                            float scl = MathU.hyperbolicLimit(unit.hitSize * Mathf.clamp(liquidAmount[ind] - (unit instanceof LegsUnit ? unit.hitSize * 0.1f : 0), 0, unit.hitSize * 0.125f) * fluidtype.density * 50f / unit.mass());
                            unit.vel.lerp(fluidVelX(ind) * 0.5f, fluidVelY(ind) * 0.5f, scl);
                            fluidtype.onUnitTouch.get(unit, liquidAmount[ind]);
                        }
                    });
                    activateTouched();
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

    public void createGridAnew(int w, int h){
        this.w = w;
        this.h = h;
        stride = w + 2;

        terrainHeight = new float[getArrayLength()];
        liquidAmount = new float[getArrayLength()];
        previousLiquidAmount = new float[getArrayLength()];
        pressure = new float[getArrayLength()];
        previousFluidType = new int[getArrayLength()];
        fluidType = new int[getArrayLength()];
        terrainImpassable = new boolean[getArrayLength()];
        terrainAbsorber = new boolean[getArrayLength()];
        hasBuilding = new boolean[getArrayLength()];
        vel = new float[getArrayLength() * 4];
        tmpvel = new float[getArrayLength() * 4];
    }

    public float getTransition(){
        return fluidThread.transition;
    }

    public int getTimeStep(){
        return fluidThread.currentTime;
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

    public void removeFluid(int ind, float amount){
        if(amount <= 0 || terrainImpassable[ind]){
            return;
        }

        liquidAmount[ind] -= amount;
        if(liquidAmount[ind]<0){
            liquidAmount[ind]=0;
            fluidType[ind]=0;
        }
    }

    public void addFluidNonPadded(GroundLiquidProperties g, int x, int y, float amount){
        int ind = x + y * stride;
        if(ind < 0 || ind >= liquidAmount.length || amount <= 0 || terrainImpassable[ind]){
            return;
        }

        liquidAmount[ind] += amount;
        fluidType[ind] = g == null ? water.id : g.id;
    }

    public void updateBuildTerrain(Building b){
        if(b.block.size == 1){
            updateTerrain(b.tile.x, b.tile.y);
        }else{
            int offset = (b.block.size - 1) / 2;
            for(int y = b.tile.y - offset; y < b.tile.y - offset + b.block.size; y++){
                for(int x = b.tile.x - offset; x < b.tile.x - offset + b.block.size; x++){
                    updateTerrain(x, y);
                }
            }
        }
    }

    public void updateTerrain(int x, int y){
        int ind = tileIndexOf(x, y);
        var tile = world.tile(x, y);
        //how to save floor data?
        float theight = 1;
        if(tile.floor().liquidDrop != null || tile.floor() == YoungchaBlocks.pit){
            theight = 0;
            terrainImpassable[ind] = true;
            terrainAbsorber[ind] = true;
        }
        if(tile.block() != Blocks.air){
            terrainAbsorber[ind] = false;
        }
        if(floorHeights.containsKey(tile.floorID())){
            theight = floorHeights.get(tile.floorID());
        }
        if(tile.block() instanceof StaticWall){
            theight = 99;
            terrainImpassable[ind] = true;
        }
        if(tile.build != null){
            if(tile.block() instanceof Wall){
                theight += 5;
            }
            if(tile.build instanceof CustomGroundFluidTerrain g){
                theight += g.terrainHeight();
            }else{
                theight += tile.block().solid ? 2 : 0.5;
            }
            hasBuilding[ind] = true;
        }else{
            hasBuilding[ind] = false;
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
                updateInteractions(ind,x,y);
                ind++;
            }
            ind += 2;
        }
        ind = originIndex;
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
        transferTouched();
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

        stream.writeShort(w);
        stream.writeShort(h);
        //todo: Deflate encoding, esp for fluid type, the data is very suited for it
        for(int i = 0; i < liquidAmount.length; i++){
            stream.writeFloat(liquidAmount[i]);
        }
        for(int i = 0; i < fluidType.length; i++){
            stream.writeInt(fluidType[i]);
        }
        //velocity cant be used for run length very well, we can make it very lossy however, but such will only be used in networking packets.
        //splitting the directions also makes it easier for redundancy to show up to be compressed
        for(int i = 0; i < vel.length; i++){
            stream.writeFloat(vel[i]);
        }
        //another way to further compression is to send a traversal path in the beginning.
        //fluids are encoded going from lowest (static) land to highest in a set path designed by the server.
    }

    @Override
    public void read(DataInput stream) throws IOException{
        createGridAnew(stream.readShort(), stream.readShort());
        for(int i = 0; i < liquidAmount.length; i++){
            previousLiquidAmount[i] = liquidAmount[i] = stream.readFloat();
        }
        for(int i = 0; i < fluidType.length; i++){
            previousFluidType[i] = fluidType[i] = stream.readInt();
        }
        for(int i = 0; i < vel.length; i++){
            vel[i] = stream.readFloat();
        }
        pushStaticBlocks(false);
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
    //                prop.onBuildTouch.get(world.tile(x, y).build, liquidAmount[index]);
    IntSeq threadBuildingTouched = new IntSeq();
    IntSeq buildingTouched = new IntSeq();
    final Object touchylock = new Object(); // very touchy
    void transferTouched(){
        synchronized(touchylock){
            buildingTouched.addAll(threadBuildingTouched);
            threadBuildingTouched.clear(); // yeh fuck the clear
        }
    }
    void activateTouched(){
        synchronized(touchylock){
            if(buildingTouched.size==0){
                return;
            }
            int raw;
            for(int i = 0;i<buildingTouched.size;i+=3){
                raw = buildingTouched.items[i];
                liquidProperties.get((raw&(directionPackOffsetMul-1))-1)
                    .onBuildTouch.get(world.build(buildingTouched.items[i+1]), buildingTouched.items[i+2]/256f,buildingTouched.items[i+1],raw>>directionPackOffset); //gl
            }
            buildingTouched.clear();
        }
    }

   static final int packOffsetX = Point2.pack(1, 0);
    static final int directionPackOffset = 16;
    static final int directionPackOffsetMul = 1<<directionPackOffset;

    void updateInteractions(int index, int x, int y){
        if(liquidAmount[index] == 0){
            return;
        }
        int ot = fluidType[index], tt;
        var prop = liquidProperties.get(ot-1);
        int t;
        LiquidInteraction it;
        int pospt = Point2.pack(x,y);
        for(int i = 0; i < 4; i++){
            t = offset(index, i);
            tt = fluidType[t];
            if(ot != tt && tt != 0){
                it = getInteraction(ot, tt);
                if(it != null){
                    it.interact(index, t, liquidAmount);
                }
            }
            if(hasBuilding[t] && prop.onBuildTouch != null){
                threadBuildingTouched.add(prop.id + (i+1)*directionPackOffsetMul);
                threadBuildingTouched.add(pospt+positionOffsetsPacked[i]);
                threadBuildingTouched.add((int)(liquidAmount[index]*256));
            }
        }
        if(hasBuilding[index] && prop.onBuildTouch != null){
            threadBuildingTouched.add(prop.id);
            threadBuildingTouched.add(pospt);
            threadBuildingTouched.add((int)(liquidAmount[index]*256));
        }
        return;

    }

    float updatePressure(int index){
        if(liquidAmount[index] == 0){
            return 0;
        }
        float olevel = liquidAmount[index] + terrainHeight[index];
        int ot = fluidType[index], tt;
        float p = 0;
        int t;
        for(int i = 0; i < 4; i++){
            t = offset(index, i);
            tt = fluidType[t];
            if(ot != tt && tt != 0){
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
        float hdiff = originHeight - liquidAmount[target] - terrainT - pressure[target];
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
    static final int[] positionOffsets = {0,-1, -1,0,  0,1,  1,0};
    static final int[] positionOffsetsPacked = {-1,-packOffsetX,1,packOffsetX};

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
    public float fluidAmount(int index){
        return liquidAmount[index];
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
        int ft = fluidType[tileIndexOf(x, y)]-1;
        return ft==-1?null:liquidProperties.get(ft);
    }

    GroundLiquidProperties getFluidType(int tileindex){
        return liquidProperties.get(fluidType[tileindex] - 1);
    }

    // used for drawin
    GroundLiquidProperties getVisualFluidType(int tileindex){
        return liquidProperties.get(Math.max(fluidType[tileindex], previousFluidType[tileindex]) - 1);
    }

    int getVisualFluidTypeID(int tileindex){
        return Math.max(fluidType[tileindex], previousFluidType[tileindex]);
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
        public Color shallowColor = new Color(1, 1, 1, 0.2f);
        public Color deepColor;
        public Effect evaporateEffect = OtherFx.steamSlow;

        public Cons2<Unit, Float> onUnitTouch = (u, am) -> {
        };
        public Cons4<Building, Float, Integer,Integer> onBuildTouch = null;
        //texture as well probably

        int id = ++idAcc;

        public GroundLiquidProperties(String name, Color deepColor){
            this.name = name;
            this.deepColor = deepColor;
            liquidProperties.add(this);
        }

        public GroundLiquidProperties(Liquid l){
            this(l.name, l.color);
            association = l;
            liquidAssociationMap.put(l.id, this);
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
        volatile boolean doStep = false;
        public void run(){
            Log.info(" ---- Thread started");
            while(!terminate){
                try{
                    while(!doStep){
                        Thread.sleep(16);
                        if(Core.app.isDisposed()){
                            Log.info(" >>>>> Thread terminated due to app"); // we have to busy wait bc theres no hook for app termination
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
                    doStep = false;
                }catch(InterruptedException e){
                    terminate = true;
                    Log.info(" >>>>> Thread terminated");
                    return;
                }catch(Exception e){
                    Log.debug(e);
                    Log.debug(Arrays.asList(e.getStackTrace()).toString());
                    Call.sendChatMessage(e.toString());
                    Log.info(" >>>>> Thread terminated");
                    return;
                }
            }
        }

        private void updateTransition(){
            transition = 1f - Mathf.clamp(currentTime - targetTime);

        }

        public void updateTime(float delta){
            targetTime += delta;
            updateTransition();
            if(targetTime > currentTime){
                doStep = true;
            }
        }


    }



}
