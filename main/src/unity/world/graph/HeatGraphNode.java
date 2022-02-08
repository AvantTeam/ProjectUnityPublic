package unity.world.graph;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ui.*;

import static unity.graphics.UnityPal.*;

public class HeatGraphNode extends GraphNode<HeatGraph>{
    public static final float celsiusZero = 273.15f;
    public static float ambientTemp = celsiusZero + 20;
    float tempBuffer = ambientTemp; //write to
    float temp = ambientTemp; //read from
    public float emissiveness = 0.01f;
    public float conductivity = 0.1f;
    public float maxTemp = celsiusZero +1000;

    public boolean heatProducer = false;
    public float targetTemp = 1000; public float prodEfficency = 0.1f;
    public float efficency = 0;

    public HeatGraphNode(GraphBuild build, float emissiveness, float conductivity, float maxTemp){
        super(build);
        this.emissiveness = emissiveness;
        this.conductivity = conductivity;
        this.maxTemp = maxTemp;
    }
    public HeatGraphNode(GraphBuild build, float emissiveness, float conductivity, float maxTemp, float targetTemp ,float prodEfficency){
       super(build);
       this.emissiveness = emissiveness;
       this.conductivity = conductivity;
       this.maxTemp = maxTemp;
       this.targetTemp = targetTemp;
       this.prodEfficency=prodEfficency;
       this.heatProducer = true;
   }

    public HeatGraphNode(GraphBuild build){
        super(build);
    }

    @Override
    public void displayBars(Table table){
        table.row();
        table.add(new Bar(() -> Core.bundle.format("bar.unity-temp", Strings.fixed(temp - celsiusZero, 1)), this::heatColor, () -> Mathf.clamp(Math.abs(temp / 273))));
    }

    @Override
    public void update(){
        //graph handles all heat transmission.
        tempBuffer += (ambientTemp - tempBuffer) * emissiveness * Time.delta / 60f;
        temp = tempBuffer;
        if(heatProducer){
            generateHeat();
        }
    }

    public Color heatColor(){
        Color c = new Color();
        heatColor(c);
        return c;
    }

    public void heatColor(Color input){
        float a = 0;
        if(temp > celsiusZero){
            a = Math.max(0, (temp - 498) * 0.001f);
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
            a = 1.0f - Mathf.clamp(temp / celsiusZero);
            if(a < 0.01){
                input.set(Color.clear);
            }
            input.set(coldcolor.r, coldcolor.g, coldcolor.b, a);
        }
    }

    public void generateHeat(float targetTemp,float eff){
        setTemp(temp + (targetTemp-temp)*eff);
    }
    public void generateHeat(){
        setTemp(temp + (targetTemp-temp)*efficency*prodEfficency);
    }

    public float getTemp(){
        return temp;
    }

    public void setTemp(float temp){
        tempBuffer = temp;
        this.temp = temp;
    }
}
