package unity.util;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.storage.*;
import unity.mod.*;
import unity.world.blocks.*;

import static mindustry.Vars.*;

public class BlockMovement{
    //ported from Facdustrio

    //directions
    public static Point2[] dirs = {new Point2(1, 0), new Point2(0, 1), new Point2(-1, 0), new Point2(0, -1)};
    //like starting points for locations based on the direction you want? hard to explain. You wouldnt need it
    // directly anyway.
    public static Point2[][] origins = new Point2[16][];

    public static void init(){
        for(int size = 1; size <= 16; size++){
            int originx = 0;
            int originy = 0;
            originx += Mathf.floor(size / 2f);
            originy += Mathf.floor(size / 2f);
            originy -= (size - 1);
            for(int side = 0; side < 4; side++){
                int ogx = originx;
                int ogy = originy;
                if(side != 0 && size > 1){
                    for(int i = 1; i <= side; i++){
                        ogx += dirs[i].x * (size - 1);
                        ogy += dirs[i].y * (size - 1);
                    }
                }
                if(origins[size - 1] == null){
                    origins[size - 1] = new Point2[4];
                }
                origins[size - 1][side] = new Point2(ogx, ogy);
            }
        }

        Triggers.listen(Trigger.update, BlockMovement::onUpdate);
        Events.on(WorldLoadEvent.class, e -> BlockMovement.onMapLoad());
    }

    public static Point2 getNearbyPosition(Block block, int direction, int index){
        Point2 tangent = dirs[(direction + 1) % 4];
        Point2 o = origins[block.size - 1][direction];
        return new Point2(o.x + tangent.x * index + dirs[direction].x, o.y + tangent.y * index + dirs[direction].y);
    }

    //returns whether a building is allowed to be pushed.
    static boolean pushable(Building build){
        return !(build.block instanceof CoreBlock || build.dead || isBlockMoving(build));
    }

    static boolean isPayloadBlock(Building build){
        return build != null && (build.block instanceof PayloadConveyor || build.block instanceof PayloadBlock);
    }

    //returns whether a block is allowed to be on this tile, disregarding existing pushable buildings and team circles
    static boolean tileAvalibleTo(Tile tile, Block block){
        if(tile == null){
            return false;
        }

        if(tile.build != null){
            return pushable(tile.build);
        }

        if(tile.solid() || !tile.floor().placeableOn || (block.requiresWater && tile.floor().liquidDrop != Liquids.water) || (tile.floor().isDeep() && !block.floating && !block.requiresWater && !block.placeableLiquid)

        ){
            return false;
        }
        return (!block.solid && !block.solidifes) || !Units.anyEntities(tile.x * tilesize + block.offset - block.size * tilesize / 2.0f, tile.y * tilesize + block.offset - block.size * tilesize / 2.0f, block.size * tilesize, block.size * tilesize);
    }

    //returns whether a tile can be pushed in this direction, disregarding buildings.
    static boolean canPush(Building build, int direction){
        if(!pushable(build))
            return false;

        Point2 tangent = dirs[(direction + 1) % 4];
        Point2 o = origins[build.block.size - 1][direction];
        for(int i = 0; i < build.block.size; i++){ // iterate over forward edge.
            Tile t = build.tile.nearby(o.x + tangent.x * i + dirs[direction].x,
                o.y + tangent.y * i + dirs[direction].y);
            if(!tileAvalibleTo(t, build.block)){
                return false;
            }
        }

        Tile next = build.tile.nearby(dirs[direction].x, dirs[direction].y);
        return build.block.canPlaceOn(next, build.team, build.rotation);
    }

    //pushes a single building.
    //if obstructed does not push multiple tiles.
    //returns false if its blocked, otherwise true.
    //used as a subtorutine for the function that actually does push all obstructed tiles.
    /*
        params:
        build - the building to be pushed. DO NOT CALL FROM WITHIN THE BUILDING.
        direction - number from 0-4 same direction as the block rotation to push the building in.
    */

    /*algorithm:
        scan forward tiles for blockage
        return false if a block exists in forward tiles or tile isnt allowed forward space
        remove building
        readd building.
    */
    public static boolean pushSingle(Building build, int direction){
        direction = direction % 4;
        //dont move the core. >:(  BAD BAD BAD BAD
        if(build.block instanceof CoreBlock){
            return false;
        }
        short bx = build.tile.x;
        short by = build.tile.y;
        build.tile.remove();
        //scan forward tiles for blockage
        if(!Build.validPlace(build.block, build.team, bx + dirs[direction].x, by + dirs[direction].y, build.rotation,
            false)){
            world.tile(bx, by).setBlock(build.block, build.team, build.rotation, () -> build);
            return false;
        }

        world.tile(bx + dirs[direction].x, by + dirs[direction].y).setBlock(build.block, build.team,
            build.rotation, () -> build);
        return true;
    }

    //projection of the block's leading edge along a direction.
    static int project(Building build, int direction){
        return (origins[build.block.size - 1][direction].x + build.tile.x) * dirs[direction].x + (origins[build.block.size - 1][direction].y + build.tile.y) * dirs[direction].y;
    }

    ///gets all buildings connected to each other in the push direction sorted
    //if group cannot be pushed because its too large or an unpushable block exists it returns null.
    /*
        params:
        root - the building to be scanned from
        direction - number from 0-4 same direction as the block rotation to push the building in.
        max - max number of blocks to scan
        bool - boolf consumer as a custom selection criteria.
    */
    //usage:
    //this.global.facdustrio.functions.getAllContacted(Vars.world.tile(197,212).build,0,99,null)
    //this.global.facdustrio.functions.getAllContacted(Vars.world.tile(197,212).build,0,99,null).each(b=>{print(b
    // .block.name)})
    //this.global.facdustrio.functions.getAllContacted(Vars.world.tile(197,212).build,0,99,null).each(b=>{print(b
    // .x/8+","+b.y/8)})
    public static Seq<Building> getAllContacted(Building root, int direction, int max, Boolf<Building> bool){
        PQueue<Building> queue = new PQueue<>(10, (a, b) ->
            // require ordering to be projection of the block's leading edge along  push direction.
            Math.round(project(a, direction) - project(b, direction))
        );

        queue.add(root);
        Seq<Building> contacts = null;
        while(!queue.empty() && (contacts == null || contacts.size <= max)){
            Building next = queue.poll();
            if(contacts == null){
                contacts = Seq.with(next);
            }else{
                contacts.add(next);
            }

            Point2 tangent = dirs[(direction + 1) % 4];
            Point2 o = origins[next.block.size - 1][direction];

            outer:
            for(int i = 0; i < next.block.size; i++){ // iterate over forward edge.
                Tile t = next.tile.nearby(o.x + tangent.x * i + dirs[direction].x,
                    o.y + tangent.y * i + dirs[direction].y);
                Building b = t.build;

                if(b == null || Structs.indexOf(queue.queue, b) >= 0 || contacts.contains(b)) continue;

                // if a single block cannot be pushed then the entire group cannot be pushed from the root.
                if(!pushable(b) || (bool != null && !bool.get(b))) return null;

                queue.add(b);

                if(next instanceof ConnectedBlock){
                    for(int dir = 0; dir < 4; dir++){
                        if(dir == direction) continue outer;
                    }
                }
            }
        }

        if(contacts != null && contacts.size <= max){
            return contacts;
        }else{
            return null;
        }
    }

    //pushes a single building and pushes all buildings behind the pushed block., unlike the previous.
    /*
        params:
        build - the building to be pushed from
        direction - number from 0-4 same direction as the block rotation to push the building in.
        maxBlocks - max number of blocks to push
        speed - anything > 0 will be animated push., measured in tiles per second.
    */
    //usage: BlockMovement.pushBlock(Vars.world.tile(203,208).build,0,99,1)
    public static boolean pushBlock(Building build, int direction, int maxBlocks, float speed, Boolf<Building> bool){
        Seq<Building> pushing = getAllContacted(build, direction, maxBlocks, bool);
        if(pushing == null){
            return false;
        }
        //scan in reverse
        for(var i = pushing.size - 1; i >= 0; i--){
            if(!canPush(pushing.get(i), direction)){
                return false;
            }
        }
        for(var i = pushing.size - 1; i >= 0; i--){
            pushSingle(pushing.get(i), direction);
            if(speed > 0){
                addPushedBlock(pushing.get(i), direction, speed);
            }
        }
        return true;
    }

    // similar to the above but it spawns in a block after the push, and takes into account payload accepting blocks
    // returns 2 if successful, 0 if not, and 1 if the forward tile is a payload acceptor and was unsuccesful... so
    // you can spam attempts to push.
    public static int pushOut(Building build, int x, int y, int direction, float speed, int max, Boolf<Building> bool, boolean waitPayload){
        Tile tile = world.tile(x, y);
        if(tile.build == null){
            if(!tileAvalibleTo(tile, build.block)){
                return 0;
            }
            addPushedBlock(build, direction, speed);
            tile.setBlock(build.block, build.team, build.rotation, () -> build);
            return 2;
        }else{
            if(waitPayload && isPayloadBlock(tile.build)){
                BuildPayload bp = new BuildPayload(build);
                bp.set((x - dirs[direction].x) * 8, (y - dirs[direction].y) * 8, 0);
                if(tile.build.acceptPayload(build, bp)){
                    tile.build.handlePayload(build, bp);
                    return 2;
                }
                return 1;
            }else{
                if(pushBlock(tile.build, direction, max, speed, bool)){
                    addPushedBlock(build, direction, speed);
                    tile.setBlock(build.block, build.team, build.rotation, () -> build);
                    return 2;
                }else{
                    return 0;
                }
            }
        }
    }

    static ObjectMap<Building, BlockMovementUpdater> currentlyPushing = new ObjectMap<>();

    //building under animation cannot be pushed.
    public static boolean isBlockMoving(Building build){
        return currentlyPushing.containsKey(build);
    }

    // this adds the animation so the building isn't just 'teleported' to the new location visually speaking.
    //however currently there are visual artifacts if its applied too quickly in succession.
    static class BlockMovementUpdater{
        Building build;
        Point2 dir;
        float delay;
        float timer;
        float ox, oy;

        public BlockMovementUpdater(Building building, Point2 dir, float delay, float timer, float ox, float oy){
            this.build = building;
            this.dir = dir;
            this.delay = delay;
            this.timer = timer;
            this.ox = ox;
            this.oy = oy;
        }

        public void update(){
            if(this.timer == 0){
                this.build.x -= this.dir.x * tilesize;
                this.build.y -= this.dir.y * tilesize;
                this.ox = this.build.x;
                this.oy = this.build.y;
            }
            this.timer += Time.delta;
            float progress = Math.min(1, this.timer / this.delay);

            this.build.x = this.ox + this.dir.x * tilesize * progress;
            this.build.y = this.oy + this.dir.y * tilesize * progress;
        }

        public boolean isDead(){
            return this.timer > this.delay;
        }
    }

    static void addPushedBlock(Building build, int direction, float speed){
        BlockMovementUpdater bmu = new BlockMovementUpdater(build, dirs[direction], 60.0f / speed, 0, 0, 0);
        currentlyPushing.put(build, bmu);
        bmu.update();
    }

    private static final Seq<Building> toRemove = new Seq<>();

    public static void onUpdate(){
        currentlyPushing.each((b, animate) -> {
            animate.update();
            if(animate.isDead()) toRemove.add(b);
        });

        toRemove.each(currentlyPushing::remove);
        toRemove.clear();
    }

    public static void onMapLoad(){
        currentlyPushing.clear();
    }

    public static void onMapUnload(){

    }
}
