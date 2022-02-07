package unity.util;

import arc.struct.*;

import java.util.*;

public class ValueList extends ValueMap implements Iterable<ValueMap>{
    Seq<ValueMap> list = new Seq<>();

    public ValueMap getMap(int index){
        return list.get(index);
    }
    public void add(ValueMap f){
        list.add(f);
    }

    public void addFloat(float f){
        list.add(new ValueMap(f));
    }
    public float getFloat(int t){
        return list.get(t).floatval;
    }
    public void addInt(int f){
        list.add(new ValueMap(f));
    }
    public float getInt(int t){
        return list.get(t).intval;
    }
    public void add(Object f){
        list.add(new ValueMap(f));
    }


    public <T> T get(int t){
        return (T)list.get(t).val;
    }
    public void set(int index,ValueMap f){
        list.set(index,f);
    }

    @Override
    public Iterator<ValueMap> iterator(){
        return list.iterator();
    }

    public int length(){
        return list.size;
    }
}
