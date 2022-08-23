package unity.world.blocks.distribution;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import unity.graphics.*;
import unity.world.blocks.*;
import unity.world.graph.*;
import unity.world.graph.GraphConnector.*;

import static arc.math.Mathf.pi;
import static mindustry.Vars.*;

public class DriveBelt extends GenericGraphBlock{
    protected final static ObjectSet<Graph> graphs = new ObjectSet<>();
    public float wheelSize = 4f;
    public float maxRange = 5f;
    public TextureRegion[] rotationRegions = new TextureRegion[4];
    public TextureRegion rotator;

    @Override
    public void load(){
        super.load();
        for(int i = 0;i<4;i++){
            rotationRegions[i] = loadTex((1+i)+"");
        }
        rotator = loadTex("rotator");
    }

    public DriveBelt(String name){
        super(name);
        configurable = true;
        config(Integer.class, (DriveBeltBuild build, Integer point) -> {
            Building other = world.build(point);
            if(other instanceof DriveBeltBuild odb){
                if(state.isEditor()){
                    build.connector.recalcNeighbours();
                    odb.connector.recalcNeighbours(); //normally the first update will 'do it', but since there is none in the editor we have to uh improvise.
                }
                boolean contains = build.connector.isConnected(odb);
                if(contains){
                    build.connector.disconnectTo(odb.connector);
                    //disconnect
                }else if(linkValid(build, other) && build.connector.validConnections() < build.connector.maxConnections){
                    build.connector.connectTo(odb.connector);
                }
            }
        });
        config(Point2[].class,(DriveBeltBuild build, Point2[] value) -> {
            build.connector.disconnect();
            //set new
            for(Point2 p : value){
                int newPos = Point2.pack(p.x + build.tileX(), p.y + build.tileY());
                configurations.get(Integer.class).get(build, newPos);
            }
        });
    }


    @Override
    public void init(){
        super.init();
        clipSize = Math.max(clipSize, (maxRange) * tilesize + wheelSize);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Tile tile = Vars.world.tile(x, y);
        if (tile != null){
            Lines.stroke(1.0F);
            Draw.color(Pal.placing);
            Drawf.circles((float)(x * 8) + this.offset, (float)(y * 8) + this.offset, this.maxRange * 8.0F);
        }
    }

    //stealing from power node
    public void getPotentialLinks(Tile tile, Team team, Cons<Building> others){
        Boolf<Building> valid =
            other -> other != null && //not null
            other.tile() != tile &&//not itself
            other.team == team && //not enemy
            other instanceof DriveBeltBuild dbelt && //is belt
            !graphs.contains(dbelt.connector.getGraph()) &&
            overlaps(tile.x * tilesize + offset, tile.y * tilesize + offset, other.tile(), maxRange * tilesize) && //in  range
            dbelt.connector.validConnections()<dbelt.connector.maxConnections; //not saturated

        if(tile.build instanceof DriveBeltBuild cdbb){
            graphs.add(cdbb.connector.getGraph());
        }

        Geometry.circle(tile.x, tile.y, (int)(maxRange + 2), (x, y) -> {
            Building other = world.build(x, y);
            if(valid.get(other) && !tempBuilds.contains(other)){
                tempBuilds.add(other);
            }
        });
        tempBuilds.sort((a, b) -> Float.compare(a.dst2(tile), b.dst2(tile)));

        tempBuilds.each(valid, others);

    }

    protected boolean overlaps(float srcx, float srcy, Tile other, float range){
           return Intersector.overlaps(Tmp.cr1.set(srcx, srcy, range), other.getHitbox(Tmp.r1));
       }

    protected boolean overlaps(Building src, Building other, float range){
       return overlaps(src.x, src.y, other.tile(), range);
   }

    protected boolean overlaps(Tile src, Tile other, float range){
       return overlaps(src.drawx(), src.drawy(), other, range);
   }

    public boolean overlaps(@Nullable Tile src, @Nullable Tile other){
       if(src == null || other == null) return true;
       return Intersector.overlaps(Tmp.cr1.set(src.worldx() + offset, src.worldy() + offset, maxRange * tilesize), Tmp.r1.setSize(size * tilesize).setCenter(other.worldx() + offset, other.worldy() + offset));
    }

    public boolean linkValid(Building tile, Building link){
           return linkValid(tile, link, true);
       }

    public boolean linkValid(Building tile, Building link, boolean checkMaxNodes){
       if(tile == link || !(link instanceof DriveBeltBuild dbb) || tile.team != link.team || !(tile instanceof  DriveBeltBuild dbb2)) return false;

       if(overlaps(tile, link, maxRange * tilesize) || (link.block instanceof DriveBelt node && overlaps(link, tile, node.maxRange * tilesize))){
           if(checkMaxNodes && link.block instanceof DriveBelt node){
               return dbb.connector.validConnections() < dbb.connector.maxConnections || dbb.connector.isConnected(dbb2);
           }
           return true;
       }
       return false;
    }


    public void drawBelt(Team team, float x1, float y1, float x2, float y2, float r, float size1, float size2){
        final float d = Mathf.dst(x2-x1,y2-y1);
        final float f = Mathf.sqrt(d*d - Mathf.sqr(size2-size1));
        final float a = size1>size2? Mathf.atan2(size1-size2,f) : (size1<size2? pi-Mathf.atan2(size2-size1,f):Mathf.halfPi);
        final float a2 = pi-a;
        final float na = Mathf.atan2(x2-x1,y2-y1);
        Tmp.v1.set(x2-x1,y2-y1).scl(1f/d); // normal
        Tmp.v2.set(Tmp.v1).rotateRad(a).scl(size1).add(x1,y1); //tangent 1
        Tmp.v3.set(Tmp.v1).rotateRad(-a).scl(size1).add(x1,y1);  //tangent 2

        Tmp.v4.set(Tmp.v1).rotateRad(a-pi).scl(-size2).add(x2,y2);//tangent 3
        Tmp.v5.set(Tmp.v1).rotateRad(pi-a).scl(-size2).add(x2,y2);//tangent 4

        Lines.stroke(3f,team.color.cpy().lerp(Pal.gray,0.5f));
        Lines.line(Tmp.v2.x,Tmp.v2.y,Tmp.v4.x,Tmp.v4.y);
        Lines.line(Tmp.v3.x,Tmp.v3.y,Tmp.v5.x,Tmp.v5.y);
        UnityDrawf.arc(x1,y1,size1,na-a,na+a-(2*pi));
        UnityDrawf.arc(x2,y2,size2,na-a2+pi,na+a2+pi-(2*pi));

        Lines.stroke(1f,team.color.cpy().lerp(Pal.gray,0.2f));
        Lines.line(Tmp.v2.x,Tmp.v2.y,Tmp.v4.x,Tmp.v4.y);
        Lines.line(Tmp.v3.x,Tmp.v3.y,Tmp.v5.x,Tmp.v5.y);
        UnityDrawf.arc(x1,y1,size1,na-a,na+a-(2*pi));
        UnityDrawf.arc(x2,y2,size2,na-a2+pi,na+a2+pi-(2*pi));

        final float l1 = Math.abs(2*a-(2*pi))*size1;
        final float l2 = Math.abs(2*a2-(2*pi))*size2;
        final float total = l1+l2+f*2;
        Cons<Float> getPt = len->{
            len = Mathf.mod(len,total);
            if(len<l1){
                float ang = Mathf.lerp(na-a,na+a-(2*pi),len/l1);
                Tmp.v6.set(Mathf.cos(ang)*size1+x1,Mathf.sin(ang)*size1+y1);
            }else if(len<l1+f){
                Tmp.v6.set(Tmp.v2).lerp(Tmp.v4,Mathf.curve(len-l1,0,f));
            }else if(len<l1+f+l2){
                float ang = Mathf.lerp(na-a2+pi,na+a2+pi-(2*pi),Mathf.curve(len-l1-f,0,l2));
                Tmp.v6.set(Mathf.cos(ang)*size2+x2,Mathf.sin(ang)*size2+y2);
            }else{
                Tmp.v6.set(Tmp.v5).lerp(Tmp.v3,Mathf.curve(len-(l1+f+l2),0,f));
            }
        };
        //Tmp.v6.set()
        r = r*size1*(pi/180f);
        Lines.stroke(1f,team.color);
        Vec2 tmp7 = new Vec2();
        for(int i = 0;i<10;i++){
            float len = i*0.1f*total;
            getPt.get(len-r);
            tmp7.set(Tmp.v6);
            getPt.get(len-(r+2));
            Lines.line(Tmp.v6.x,Tmp.v6.y,tmp7.x,tmp7.y,false);
        }

        Draw.color();
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("connections", entity -> {
            if(entity instanceof DriveBeltBuild deb){
                return new Bar(
                () -> Core.bundle.format("bar.powerlines", deb.connector.validConnections(), deb.connector.maxConnections),
                () -> Pal.items,
                () -> (float)deb.connector.validConnections()/ deb.connector.maxConnections
                );
            }
            return null;
        });
    }

    public class DriveBeltBuild extends GenericGraphBuild{
        DistanceGraphConnector<TorqueGraph> connector;
        @Override
        public void onInit(){
            var conn = torqueNode().getConnectorOfType(DistanceGraphConnector.class);
            if(conn == null){
                throw new IllegalStateException(name+" is missing an instance of "+ DistanceGraphConnector.class.getName());
            }
            connector = conn;
        }

        @Override
        public void drawSelect(){
            super.drawSelect();

            Lines.stroke(1f);

            Draw.color(Pal.accent);
            Drawf.circles(x, y, maxRange * tilesize);

            for(var pt:connector.connection){
                if(pt!=null && !pt.equals(0,0)){
                    Building b = world.build(pt.x+tile.x,pt.y+tile.y);
                    if(b!=null){
                        Drawf.square(b.x, b.y, b.block.size * tilesize / 2f + 1f, Pal.place);
                    }else{
                        Drawf.square((pt.x+tile.x)*8, (pt.y+tile.y)*8, tilesize / 2f + 1f, Pal.place);
                    }
                }
            }

            Draw.reset();
        }

        @Override
        public Point2[] config(){
            Point2[] out = new Point2[connector.maxConnections];
            for(int i = 0; i < out.length; i++){
                out[i] = connector.connection[i]==null?new Point2():connector.connection[i].cpy();
            }
            return out;
        }


        @Override
        public boolean onConfigureBuildTapped(Building other){

            if(linkValid(this, other)){
                configure(other.pos());
                return false;
            }

            if(this == other){
                if(connector.validConnections()==0){
                    int[] total = {0};
                    getPotentialLinks(tile, team, link -> {
                        if(total[0]++ < connector.maxConnections){
                            configure(link.pos());
                        }
                    });
                }else{
                    int v = connector.validConnections();
                    for(int i =0;i<v;i++){
                        configure(connector.first().cpy().add(tile.x,tile.y).pack());
                    }
                }
                deselect();
                return false;
            }

            return true;
        }

        @Override
        public void draw(){
            float r = connector.getGraph().rotation*(4f/wheelSize);
            Draw.rect(rotationRegions[rotation],x,y,0);
            Draw.z(Layer.blockOver);
            Drawf.shadow(rotator,x-1,y-1,r);
            Drawf.spinSprite(rotator,x,y,r);
            Draw.z(Layer.power);
            for(var pt: connector.connections){
                if(pt!=null &&  pt.other(connector).getNode().build() instanceof DriveBeltBuild db){
                    if(((DriveBelt)db.block()).maxRange< maxRange || db.id<id){
                        drawBelt(team, x, y, db.x, db.y, r, wheelSize, ((DriveBelt)db.block()).wheelSize);
                    }
                }
            }
            drawTeamTop();
        }

        public DistanceGraphConnector<TorqueGraph> getConnector(){
            return connector;
        }
    }
}
