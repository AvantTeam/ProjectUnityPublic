package unity.world.blocks.exp;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.world.meta.*;
import unity.graphics.*;
import unity.ui.*;

public abstract class EField<T> {
    public static final float graphWidth = 330f, graphHeight = 160f;
    public @Nullable
    Stat stat;
    public boolean hasTable = true;
    public boolean formatAll = true;
    public EField(Stat stat){
        this.stat = stat;
    }

    public abstract T fromLevel(int l);
    public abstract void setLevel(int l);
    public void buildTable(Table table, int end){}

    public EField<T> formatAll(boolean f){
        this.formatAll = f;
        return this;
    }

    @Override
    public String toString(){
        return "[#84ff00]NULL[]";
    }

    //f(x) = scale * x + start
    public static class ELinear extends EField<Float> {
        public Floatc set;
        public float start, scale;
        public Func<Float, String> format;

        public ELinear(Floatc set, float start, float scale, Stat stat, Func<Float, String> format){
            super(stat);
            this.start = start;
            this.scale = scale;
            this.set = set;
            this.format = format;
        }

        public ELinear(Floatc set, float start, float scale, Stat stat){
            this(set, start, scale, stat, f -> Strings.autoFixed(f, 1));
        }

        @Override
        public Float fromLevel(int l){
            return start + l * scale;
        }

        @Override
        public void setLevel(int l){
            set.get(fromLevel(l));
        }

        @Override
        public String toString(){
            //return Strings.autoFixed(start, 1) + " + " + "[#84ff00]" + Strings.autoFixed(scale, 1) + " per level[]";
            return Core.bundle.format("field.linear", format.get(start), formatAll ? format.get(scale) : Strings.autoFixed(scale, 2));
        }

        @Override
        public void buildTable(Table table, int end){
            table.left();
            Graph g = new Graph(this::fromLevel, end, UnityPal.exp);
            table.add(g).size(graphWidth, graphHeight).left();
            table.row();
            table.label(() -> g.lastMouseOver ? (Core.bundle.format("ui.graph.label", g.lastMouseStep, formatAll ? format.get(g.mouseValue()) : Strings.autoFixed(g.mouseValue(), 2))) : Core.bundle.get("ui.graph.hover"));
        }
    }

    public static class ELinearCap extends ELinear {
        public int cap; //after this level, the stats do not rise

        public ELinearCap(Floatc set, float start, float scale, int cap, Stat stat, Func<Float, String> format){
            super(set, start, scale, stat, format);
            this.cap = cap;
        }

        public ELinearCap(Floatc set, float start, float scale, int cap, Stat stat){
            this(set, start, scale, cap, stat, f -> Strings.autoFixed(f, 1));
        }

        @Override
        public Float fromLevel(int l){
            return start + Math.min(l, cap) * scale;
        }

        @Override
        public void setLevel(int l){
            set.get(fromLevel(l));
        }

        @Override
        public String toString(){
            return Core.bundle.format("field.linearcap", format.get(start), formatAll ? format.get(scale) : Strings.autoFixed(scale, 2), cap);
        }
    }

    //f(x) = start * scale ^ x
    public static class EExpo extends EField<Float> {
        public Floatc set;
        public float start, scale;
        public Func<Float, String> format;

        public EExpo(Floatc set, float start, float scale, Stat stat, Func<Float, String> format){
            super(stat);
            this.start = start;
            this.scale = scale;
            this.set = set;
            this.format = format;
        }

        public EExpo(Floatc set, float start, float scale, Stat stat){
            this(set, start, scale, stat, f -> Strings.autoFixed(f, 1));
        }

        @Override
        public Float fromLevel(int l){
            return start * Mathf.pow(scale, l);
        }

        @Override
        public void setLevel(int l){
            set.get(fromLevel(l));
        }

        @Override
        public String toString(){
            return Core.bundle.format("field.exponent", format.get(start), scale);
        }

        @Override
        public void buildTable(Table table, int end){
            table.left();
            Graph g = new Graph(this::fromLevel, end, UnityPal.exp);
            table.add(g).size(graphWidth, graphHeight).left();
            table.row();
            table.label(() -> g.lastMouseOver ? (Core.bundle.format("ui.graph.label", g.lastMouseStep, formatAll ? format.get(g.mouseValue()) : Strings.autoFixed(g.mouseValue(), 2))) : Core.bundle.get("ui.graph.hover"));
        }
    }

    public static class EExpoZero extends EExpo{
        public boolean clamp;
        public EExpoZero(Floatc set, float start, float scale, boolean clamp, Stat stat, Func<Float, String> format){
            super(set, start, scale, stat, format);
            this.clamp = clamp;
        }

        public EExpoZero(Floatc set, float start, float scale, Stat stat){
            this(set, start, scale, false, stat, f -> Strings.autoFixed(f, 1));
        }

        @Override
        public Float fromLevel(int l){
            return clamp ? Mathf.clamp(super.fromLevel(l) - start) : super.fromLevel(l) - start;
        }

        @Override
        public String toString(){
            return Core.bundle.format("field.exponentzero", format.get(start), scale);
        }
    }

    //f(x) = a / (x - axis) + end, a = a(start) -> f(0) = start (axis != 0)
    public static class ERational extends EField<Float> {
        public Floatc set;
        public float start, end, axis, a;
        public Func<Float, String> format;

        public ERational(Floatc set, float start, float end, float axis, Stat stat, Func<Float, String> format){
            super(stat);
            this.start = start;
            this.end = end;
            if(axis == 0) throw new ArithmeticException("Vertical asymptote cannot be x = 0");
            this.axis = axis;
            a = (end - start) * axis;
            this.set = set;
            this.format = format;
        }

        public ERational(Floatc set, float start, float end, Stat stat, Func<Float, String> format){
            this(set, start, end, -1, stat, format);
        }

        public ERational(Floatc set, float start, float end, Stat stat){
            this(set, start, end, stat, f -> Strings.autoFixed(f, 1));
        }

        @Override
        public Float fromLevel(int l){
            return a / (l - axis) + end;
        }

        @Override
        public void setLevel(int l){
            set.get(fromLevel(l));
        }

        @Override
        public String toString(){
            return Core.bundle.format("field.rational", format.get(start), formatAll ? format.get(end) : Strings.autoFixed(end, 2));
        }

        @Override
        public void buildTable(Table table, int end){
            table.left();
            Graph g = new Graph(this::fromLevel, end, UnityPal.exp);
            table.add(g).size(graphWidth, graphHeight).left();
            table.row();
            table.label(() -> g.lastMouseOver ? (Core.bundle.format("ui.graph.label", g.lastMouseStep, formatAll ? format.get(g.mouseValue()) : Strings.autoFixed(g.mouseValue(), 2))) : Core.bundle.get("ui.graph.hover"));
        }
    }

    public static class EBool extends EField<Boolean> {
        public Boolc set;
        public boolean start;
        public int thresh;

        public EBool(Boolc set, boolean start, int thresh, Stat stat){
            super(stat);
            this.start = start;
            this.thresh = thresh;
            this.set = set;
            this.hasTable = false;
        }

        @Override
        public Boolean fromLevel(int l){
            return (l >= thresh) != start;
        }

        @Override
        public void setLevel(int l){
            set.get(fromLevel(l));
        }

        @Override
        public String toString(){
            return Core.bundle.format("field.bool", bs(start), bs(!start), thresh);
        }

        public String bs(boolean b){
            return Core.bundle.get(b ? "yes" : "no");
        }
    }

    public static class EList<T> extends EField<T> {
        public Cons<T> set;
        public T[] list;
        public String unit;

        public EList(Cons<T> set, T[] list, Stat stat, String unit){
            super(stat);
            this.set = set;
            this.list = list;
            this.unit = unit;
            this.hasTable = false;
        }

        public EList(Cons<T> set, T[] list, Stat stat){
            this(set, list, stat, "");
        }

        @Override
        public T fromLevel(int l){
            return list[Math.min(list.length - 1, l)];
        }

        @Override
        public void setLevel(int l){
            set.get(fromLevel(l));
        }

        @Override
        public String toString(){
            return Core.bundle.format("field.list", list[0], list[list.length - 1], unit);
        }
    }
}

