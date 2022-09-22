package unity.parts;

public class ModularPart{
    public ModularPartType type;
    int x,y;
    int rotation;
    //position of lowest left tile
    public float ax,ay;
    //middle
    public float cx,cy;
    public int[] panelingIndexes;
    //which lighting variation to draw
    public int front = 0;
    //transient properties index
    public int prop_index = -1;

    //editor only fields
    boolean valid = false;



    public ModularPart(ModularPartType type, int x, int y){
        this.type = type;
        this.x = x;
        this.y = y;
        panelingIndexes = new int[type.w * type.h];
    }

    public void setX(int x){
        this.x = x;
    }

    public void setY(int y){
        this.y = y;
    }
    public void setPos(int x, int y){
        this.x = x;
        this.y = y;
    }

    public float getAx(){
        return ax;
    }

    public float getAy(){
        return ay;
    }

    public int getX(){
        return x;
    }

    public int getY(){
        return y;
    }

    public boolean isHere(int x_,int y_){
        return x==x_ &&y==y_;
    }

    public float getCx(){
        return cx;
    }

    public float getCy(){
        return cy;
    }
}
