package unity.entities.bullet.misc;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;

public class BoidBulletType extends BasicBulletType{
    public float alignrate = 3f;
    public float cohesion = 1.7f;
    public float sep = 10f;
    private Seq<Bullet> close = new Seq<>();

    public BoidBulletType(float speed, float damage){
        super(speed, damage,"missile");
    }

    @Override
    public void update(Bullet b){

        super.update(b);
        float radius = sep*8;
        var vel = b.vel().cpy();
        Vec2 CoM=new Vec2();
        ///grabs all nearby boids, we sort of need the size of the neighbours beforehand.
        close.clear();
        Groups.bullet.intersect(b.x - radius, b.y - radius, radius * 2f, radius * 2f, (other)->{
            if(other.type instanceof BoidBulletType && other!=b){
                close.add(other);
            }
        });
        if(close.isEmpty()){
           return;
        }
        for(var other:close){
            //velocity alignment
            vel.add((other.vel.x-vel.x)/(close.size*100f/alignrate),(other.vel.y-vel.y)/(close.size*100f/alignrate));

            //seperation
             Vec2 seperation = new Vec2(other.x-b.x,other.y-b.y);
             float dist2 = Mathf.dst2(0, 0, seperation.x,seperation.y);
             seperation.scl(1f/Mathf.sqrt(dist2));
             vel.add(-sep*seperation.x/(dist2),-sep*seperation.y/(dist2));

             //center of mass
             CoM.add(other.x,other.y);
        }
        //cohesion
        CoM= CoM.scl(1f/close.size);
        Vec2 f3 = new Vec2(CoM.x-b.x,CoM.y-b.y);
        f3 = f3.nor();
        vel.add(f3.x/(100f/cohesion),f3.y/(100f/cohesion));

        b.vel.set(vel.x,vel.y);
        b.vel.setLength(speed);


    }
}
