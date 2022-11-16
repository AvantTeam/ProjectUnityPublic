package unity.world.graph;

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
import unity.world.meta.*;

import static unity.graphics.UnityPal.*;

public class HeatGraphNode extends GraphNode<HeatGraph>{
    public static final float celsiusZero = 273.15f;
    public static float ambientTemp = celsiusZero + 20;
    public float flux = 0;
    public float heatenergy = 1f;
    float energyBuffer = 0; //write to
    public float emissiveness = 0.01f;
    public float conductivity = 0.1f;
    public float heatcapacity = 1f;
    public float maxTemp = celsiusZero +1000;

    public boolean heatProducer = false;
    public float targetTemp = 1000; public float prodEfficency = 0.1f;
    public float minGenerate= -9999999999999999f;
    public float lastEnergyInput = 0;
    public float efficency = 0;

    public HeatGraphNode(GraphBuild build, float emissiveness, float conductivity, float heatcapacity, float maxTemp){
        super(build);
        this.emissiveness = emissiveness;
        this.conductivity = conductivity;
        this.maxTemp = maxTemp;
        this.heatcapacity = heatcapacity;
        energyBuffer = this.heatenergy = heatcapacity * ambientTemp;
    }
    public HeatGraphNode(GraphBuild build, float emissiveness, float conductivity, float heatcapacity, float maxTemp, float targetTemp , float prodEfficency){
       super(build);
       this.emissiveness = emissiveness;
       this.conductivity = conductivity;
       this.maxTemp = maxTemp;
       this.targetTemp = targetTemp;
       this.prodEfficency=prodEfficency;
       this.heatProducer = true;
       this.heatcapacity = heatcapacity;
       energyBuffer = this.heatenergy = heatcapacity * ambientTemp;
   }

    public HeatGraphNode(GraphBuild build){
        super(build);
    }

    @Override
    public void displayBars(Table table){
        table.row();
        table.add(new Bar(
            () -> Core.bundle.format("bar.unity-temp",
            Strings.fixed(getTemp() - celsiusZero, 1)),
            ()->(getTemp()<maxTemp? heatColor(): (Time.time%30>15?Color.scarlet:Color.black)),
            () -> Mathf.clamp(Math.abs(getTemp() / maxTemp))
        ));
    }

    @Override
    public void update(){
        //graph handles all heat transmission.
        heatenergy += (ambientTemp - getTemp()) * emissiveness * Time.delta / 60f;
        if(heatProducer){
            generateHeat();
        }
        if(getTemp()>maxTemp){
            Puddles.deposit(build().tile, Liquids.slag, 9);
            build().damage(((getTemp()-maxTemp)/maxTemp)*Time.delta*10f);
        }
    }

    public Color heatColor(){
        Color c = new Color();
        heatColor(c);
        return c;
    }

    public void heatColor(Color input){
        heatColor(getTemp(),input);
    }
    public static void heatColor(float t,Color input){
        float a = 0;
        if(t > celsiusZero){
            a = Math.max(0, (t - 498) * 0.001f);
            if(a < 0.01){
                input.set(Color.clear);
                return;
            }
            input.set(heatcolor.r, heatcolor.g, heatcolor.b, a);
            if(a > 1){
                input.add(0, 0, 0.01f * a);
                input.mul(a);
            }
        }else{
            a = 1.0f - Mathf.clamp(t / celsiusZero);
            if(a < 0.01){
                input.set(Color.clear);
            }
            input.set(coldcolor.r, coldcolor.g, coldcolor.b, a);
        }
    }

    @Override
    public void read(Reads read){
        this.energyBuffer = this.heatenergy = read.f();
    }

    @Override
    public void write(Writes write){
        write.f(this.heatenergy );
    }

    public float generateHeat(float targetTemp, float eff){
        float gen = (targetTemp-getTemp())*eff;
        heatenergy += gen;
        return gen;
    }
    public void generateHeat(){
        lastEnergyInput = Math.max(minGenerate,(targetTemp-getTemp())*efficency*prodEfficency);
        heatenergy += lastEnergyInput*Time.delta;
    }

    public float getTemp(){
        return heatenergy/ heatcapacity;
    }

    public void affectUnit(Unit unit, float intensityScl){
        float temp = getTemp();
        if(temp > HeatGraphNode.celsiusZero+Math.max(400-intensityScl*100f,150)){
            float intensity = Mathf.clamp(Mathf.map(temp, HeatGraphNode.celsiusZero + 400, HeatGraphNode.celsiusZero + 2000f, 0f, 1f));
            unit.apply(StatusEffects.burning, (intensity * 40f + 7f) * intensityScl);
            if(unit.isImmune(StatusEffects.burning)){
                intensity*=0.2;
            }
            if(unit.isImmune(StatusEffects.melting)){
                intensity*=0.2;
            }
            unit.damage(intensity * 50f * intensityScl);
        }else if(temp < HeatGraphNode.celsiusZero-Math.max(100-intensityScl*50f,30)){
            float intensity = Mathf.clamp(Mathf.map(temp, HeatGraphNode.celsiusZero - 100, 0, 0f, 1f));
            unit.apply(StatusEffects.freezing, (intensity * 40f + 7f) * intensityScl);
            if(unit.isImmune(StatusEffects.freezing)){
                intensity*=0.2;
            }
            if(unit.hasEffect(StatusEffects.wet)){
                intensity*=2;
                unit.apply(StatusEffects.slow, (intensity * 20f + 7f) * intensityScl);
            }
            unit.damage(intensity * 50f * intensityScl);
        }
    }

    public void setTemp(float temp){
        heatenergy = temp*heatcapacity;
    }

    public void addHeatEnergy(float e){
            heatenergy += e;
        }

    @Override
    public void setStats(Stats stats){
        addLevelStat(stats, UnityStat.emissivity, emissiveness * 60f, new float[]{0.5f, 1f, 3f, 10f, 20f});
        addStat(stats, UnityStat.heatCapacity, heatcapacity);
        addStat(stats, UnityStat.heatConductivity, conductivity);
        stats.add(UnityStat.maxTemperature, maxTemp - celsiusZero, UnityStatUnit.celcius);
    }
}
