package unity.ui;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import mindustry.graphics.*;

public class Graph extends Element {
    private final float drawPad = 0.02f;
    public Color background = Color.black, backlines = Pal.gray, graphline, selection = Pal.accent, selectline = Pal.accentBack;
    public int steps;
    public boolean dots = true;
    public Floatf<Integer> fun;

    public int lastMouseStep = 0;
    public boolean lastMouseOver = false;
    private float minf, maxf;

    public Graph(Floatf<Integer> fun, int steps, Color graphline){
        this.fun = fun;
        this.steps = steps;
        this.graphline = graphline;
        lastMouseOver = false;
        dots = steps <= 20;
        init();

        setSize(getPrefWidth(), getPrefHeight());

        addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                select(x, y); //for mobile users
                return true;
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                select(x, y);
                return false;
            }
        });
    }

    public Graph(Floatf<Integer> fun, int steps){
        this(fun, steps, Color.white);
    }

    public void init(){
        minf = fun.get(0);
        maxf = fun.get(steps);
        if(minf > maxf){
            float c = minf;
            minf = maxf;
            maxf = c;
        }
    }

    public void select(float cx, float cy){
        float w = getWidth();
        float x = this.x + drawPad * w;
        w -= w * drawPad * 2;

        if(cx < x) cx = x;
        else if(cx > x + w) cx = x + w;
        lastMouseOver = true;
        lastMouseStep = Mathf.clamp(Mathf.roundPositive((cx - x) * steps / w), 0, steps);
    }

    public float mouseValue(){
        return fun.get(lastMouseStep);
    }

    @Override
    public void draw(){
        validate();
        float width = getWidth();
        float height = getHeight();
        float x = this.x + width * drawPad;
        width -= width * drawPad * 2;

        Draw.color(background, parentAlpha);
        Fill.rect(x, y, width, height);

        float stepw = width / steps;
        float sy = y + 0.06f * height; float ey = y + height * 0.94f;
        float selx = x + stepw * lastMouseStep; float sely = Mathf.map(fun.get(lastMouseStep), minf, maxf, sy, ey);

        float stroke = 3.5f;
        //Lines.stroke(0.8f * stroke);
        Draw.color(backlines, parentAlpha);
        //Lines.line(x, y, x + width, y);
        //Lines.line(x, y + height, x + width, y + height);
        for(int i = 0; i< steps; i+=2){
            //Lines.line(x + stepw * i, y, x + stepw * i, y + height);
            Fill.rect(x + stepw * i + stepw / 2f, y + height / 2f, stepw, height);
        }
        Lines.stroke(stroke);

        if(lastMouseOver){
            Draw.color(selectline, parentAlpha * 0.7f);
            Lines.line(selx, y, selx, sely);
            Lines.line(x, sely, selx, sely);
        }

        Draw.color(graphline, parentAlpha);
        Lines.beginLine();
        for(int i = 0; i<= steps; i++){
            float yy = Mathf.map(fun.get(i), minf, maxf, sy, ey);
            Lines.linePoint(x + stepw * i, yy);
            if(dots) Fill.square(x + stepw * i, yy, 1.4f * stroke, 45f);
        }
        Lines.endLine(false);

        if(lastMouseOver){
            Draw.color(selection, parentAlpha);
            Fill.square(selx, sely, 1.5f * stroke, 45f);
        }
    }
}
