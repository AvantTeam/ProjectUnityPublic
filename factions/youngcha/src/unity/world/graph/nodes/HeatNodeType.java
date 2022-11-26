package unity.world.graph.nodes;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.meta.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.meta.*;

import static unity.graphics.YoungchaPal.*;

public class HeatNodeType extends GraphNodeType<HeatGraph> implements HeatNodeTypeI<HeatGraph>{
    public static final float celsiusZero = 273.15f;
    public static float ambientTemp = celsiusZero + 20;

    public float emissiveness = 0.01f;
    public float conductivity = 0.1f;
    public float heatCapacity = 1f;
    public float maxTemp = celsiusZero + 1000f;
    public float targetTemp = 1000f;
    public float prodEfficency = 0.1f;

    @Override
    public void setStats(Stats stats){
        addLevelStat(stats, PUStat.emissivity, emissiveness * 60f, new float[]{0.5f, 1f, 3f, 10f, 20f});
        addStat(stats, PUStat.heatCapacity, heatCapacity);
        addStat(stats, PUStat.heatConductivity, conductivity);
        stats.add(PUStat.maxTemperature, maxTemp - celsiusZero, PUStatUnit.celcius);
    }

    @Override
    public <E extends Building & GraphBuild> HeatNode create(E build){
        return new HeatNode(build, emissiveness, conductivity, heatCapacity, maxTemp, targetTemp, prodEfficency);
    }

    public static void heatColor(float t,Color input){
        float a = 0;
        if(t > celsiusZero){
            a = Math.max(0, (t - 498) * 0.001f);
            if(a < 0.01){
                input.set(Color.clear);
                return;
            }
            input.set(heatColor.r, heatColor.g, heatColor.b, a);
            if(a > 1){
                input.add(0, 0, 0.01f * a);
                input.mul(a);
            }
        }else{
            a = 1.0f - Mathf.clamp(t / celsiusZero);
            if(a < 0.01){
                input.set(Color.clear);
            }
            input.set(coldColor.r, coldColor.g, coldColor.b, a);
        }
    }

    public static class HeatNode extends GraphNode<HeatGraph> implements HeatNodeI<HeatGraph>{
        public float flux = 0;
        public float heatEnergy = 1f;
        public float energyBuffer = 0; //write to
        public float emissiveness;
        public float conductivity;
        public float heatCapacity;
        public float maxTemp;

        public boolean heatProducer = false;
        public float targetTemp;
        public float prodEfficency;
        public float minGenerate = -9999999999999999f;
        public float lastEnergyInput = 0;
        public float efficency = 0;

        public HeatNode(GraphBuild build, float emissiveness, float conductivity, float heatCapacity, float maxTemp, float targetTemp , float prodEfficency){
            super(build);
            this.emissiveness = emissiveness;
            this.conductivity = conductivity;
            this.maxTemp = maxTemp;
            this.targetTemp = targetTemp;
            this.prodEfficency = prodEfficency;
            this.heatProducer = true;
            this.heatCapacity = heatCapacity;
            energyBuffer = this.heatEnergy = heatCapacity * ambientTemp;
        }

        @Override
        public void displayBars(Table table){
            table.row();
            table.add(new Bar(
                () -> Core.bundle.format("bar.unity-temp",
                Strings.fixed(getTemp() - celsiusZero, 1)),
                () -> (getTemp() < maxTemp ? heatColor() : (Time.time % 30 > 15 ? Color.scarlet : Color.black)),
                () -> Mathf.clamp(Math.abs(getTemp() / maxTemp))
            ));
        }

        @Override
        public void update(){
            // Xelo: graph handles all heat transmission.
            heatEnergy += (ambientTemp - getTemp()) * emissiveness * Time.delta / 60f;
            if(heatProducer) generateHeat();

            if(getTemp() > maxTemp){
                Puddles.deposit(build().tile, Liquids.slag, 9);
                build().damage(((getTemp()-maxTemp)/maxTemp)*Time.delta*10f);
            }
        }

        @Override
        public Color heatColor(){
            Color c = new Color();
            heatColor(c);
            return c;
        }

        @Override
        public void heatColor(Color input){
            HeatNodeType.heatColor(getTemp(), input);
        }

        @Override
        public void read(Reads read){
            this.energyBuffer = this.heatEnergy = read.f();
        }

        @Override
        public void write(Writes write){
            write.f(this.heatEnergy );
        }

        @Override
        public float generateHeat(float targetTemp, float eff){
            float gen = (targetTemp-getTemp())*eff;
            heatEnergy += gen;
            return gen;
        }

        @Override
        public void generateHeat(){
            lastEnergyInput = Math.max(minGenerate, (targetTemp - getTemp()) * efficency * prodEfficency);
            heatEnergy += lastEnergyInput * Time.delta;
        }

        @Override
        public float getTemp(){
            return heatEnergy / heatCapacity;
        }

        @Override
        public void setTemp(float temp){
            heatEnergy = temp * heatCapacity;
        }

        @Override
        public void affectUnit(Unit unit, float intensityScl){
            float temp = getTemp();
            if(temp > celsiusZero + Math.max(400 - intensityScl * 100f, 150)){
                float intensity = Mathf.clamp(Mathf.map(temp, celsiusZero + 400, celsiusZero + 2000f, 0f, 1f));
                unit.apply(StatusEffects.burning, (intensity * 40f + 7f) * intensityScl);
                if(unit.isImmune(StatusEffects.burning)) intensity *= 0.2;
                if(unit.isImmune(StatusEffects.melting)) intensity *= 0.2;

                unit.damage(intensity * 50f * intensityScl);
            }else if(temp < celsiusZero-Math.max(100-intensityScl*50f,30)){
                float intensity = Mathf.clamp(Mathf.map(temp, celsiusZero - 100, 0, 0f, 1f));
                unit.apply(StatusEffects.freezing, (intensity * 40f + 7f) * intensityScl);
                if(unit.isImmune(StatusEffects.freezing)) intensity*=0.2;
                if(unit.hasEffect(StatusEffects.wet)){
                    intensity*=2;
                    unit.apply(StatusEffects.slow, (intensity * 20f + 7f) * intensityScl);
                }

                unit.damage(intensity * 50f * intensityScl);
            }
        }

        public void addheatEnergy(float e){
            heatEnergy += e;
        }
    }
}
