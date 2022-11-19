package unity.content;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.graphics.g3d.*;
import mindustry.type.*;
import unity.assets.list.*;
import unity.graphics.g3d.*;
import unity.mod.*;
import unity.util.*;
import unity.world.planets.*;

import static unity.graphics.MonolithPal.*;
import static unity.mod.FactionRegistry.register;

/**
 * Defines all {@linkplain Faction#monolith monolith} planets.
 * @author GlennFolker
 */
public final class MonolithPlanets{
    public static Planet megalith;

    private MonolithPlanets(){
        throw new AssertionError();
    }

    public static void load(){
        megalith = register(Faction.monolith, new Planet("megalith", Planets.sun, 1f, 3){{
            Color[] colors = {monolithMid, monolithLight, monolithLighter};
            float[] speeds = new float[72];
            for(int i = 0; i < speeds.length; i++){
                Tmp.v1.trns((float)i / speeds.length * 360f, 1f);
                speeds[i] = 1f + Simplex.noise2d(id - 1, 4d, 0.4d, 2.1d, Tmp.v1.x, Tmp.v1.y);
            }

            Func<Integer, PlanetMesh> ring = num -> {
                int i = id + num * 4;
                return new PlanetMesh(this, PUMeshBuilder.createToroid(1.7f + num * 0.07f, 0.05f, 0.05f, 100, 10, prog -> {
                    Tmp.v1.trns(prog * 360f, 1f);
                    return Tmp.c1.set(monolithDark).lerp(colors, Mathf.map(Mathf.clamp(Simplex.noise2d(i, 4d, 0.67d, 1143.2d, Tmp.v1.x, Tmp.v1.y)), 0.15f, 0.85f, 0f, 1f)).toFloatBits();
                }), PUShaders.planet){
                    float
                    time1 = Mathf.randomSeed(i + 1, 360f / 0.01f),
                    time2 = Mathf.randomSeed(i + 2, 360f / 0.002f),
                    time3 = Mathf.randomSeed(i + 3, 360f / 0.004f);

                    long lastUpdated = -1;

                    @Override
                    public void preRender(PlanetParams params){
                        PUShaders.planet.lightDir.set(planet.position).sub(planet.solarSystem.position).nor();
                        PUShaders.planet.ambientColor.set(planet.solarSystem.lightColor);
                        PUShaders.planet.emissionColor.set(monolithDarker).lerp(Color.black, 0.2f);
                    }

                    @Override
                    public void render(PlanetParams params, Mat3D projection, Mat3D transform){
                        long id = Core.graphics.getFrameId();
                        if(id != lastUpdated){
                            lastUpdated = id;
                            float
                            t1 = (Time.globalTime / 6f) % 72f,
                            t2 = (Time.globalTime / 8f + 24f) % 72f,
                            t3 = (Time.globalTime / 10f + 48f) % 72f,

                            p1 = t1 % 1f,
                            p2 = t2 % 1f,
                            p3 = t3 % 1f;

                            int
                            i1 = (int)t1, i1n = (i1 + 1) % 72,
                            i2 = (int)t2, i2n = (i2 + 1) % 72,
                            i3 = (int)t3, i3n = (i3 + 1) % 72;

                            time1 += Mathf.lerp(speeds[i1], speeds[i1n], p1) * Time.delta * 0.01f;
                            time2 += Mathf.lerp(speeds[i2], speeds[i2n], p2) * Time.delta * 0.002f;
                            time3 += Mathf.lerp(speeds[i3], speeds[i3n], p3) * Time.delta * 0.004f;
                        }

                        MathUtils.m31.set(transform).mul(MathUtils.m32.idt().rotate(MathUtils.q1
                        .set(Vec3.Z, time1)
                        .mul(MathUtils.q2.set(Vec3.X, time2))
                        .mul(MathUtils.q2.set(Vec3.Y, time3))
                        .nor()
                        ));

                        preRender(params);
                        shader.bind();
                        shader.setUniformMatrix4("u_proj", projection.val);
                        shader.setUniformMatrix4("u_trans", MathUtils.m31.val);
                        shader.setUniformMatrix4("u_nor", MathUtils.m31.toNormalMatrix().val);

                        shader.apply();
                        mesh.render(shader, Gl.triangles);
                    }
                };
            };

            MegalithPlanetGenerator gen = new MegalithPlanetGenerator();
            generator = gen;

            meshLoader = () -> new MultiMesh(
            new PlanetMesh(this, PUMeshBuilder.createHexGrid(gen, 6, false, radius, 0.2f), PUShaders.planet){
                @Override
                public void preRender(PlanetParams params){
                    PUShaders.planet.lightDir.set(planet.position).sub(planet.solarSystem.position).nor();
                    PUShaders.planet.ambientColor.set(planet.solarSystem.lightColor);
                    PUShaders.planet.emissionColor.set(0f, 0f, 0f, 0f);
                }

                @Override
                public void render(PlanetParams params, Mat3D projection, Mat3D transform){
                    preRender(params);
                    shader.bind();
                    shader.setUniformMatrix4("u_proj", projection.val);
                    shader.setUniformMatrix4("u_trans", transform.val);
                    shader.setUniformMatrix4("u_nor", MathUtils.m31.set(transform).toNormalMatrix().val);
                    shader.apply();

                    mesh.render(shader, Gl.triangles);
                }
            },

            ring.get(0),
            ring.get(1),
            ring.get(2)
            );
        }});
    }
}
