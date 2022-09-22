package unity.parts;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;
import unity.gen.*;
import unity.parts.stat.*;
import unity.parts.stat.AdditiveStat.*;
import unity.ui.*;
import unity.util.*;
import unity.world.blocks.payloads.*;

import javax.swing.text.html.parser.*;

import static unity.graphics.UnityPal.*;

//like Block, this is a singleton
public class ModularPartType implements Displayable{
    public static IntMap<ModularPartType> partMap = new IntMap<>();

    public static final float partSize = 4;

    public static final int TURRET_TYPE = 1;
    public static final int UNIT_TYPE = 2;
    protected int partType = 0;

    private static int idAcc = 0;
    public final int id = idAcc++;

    public String name;
    public String category;
    public int w=1,h=1;

    //behaviour
    public boolean rotates = false;
    public boolean updates = false;

    //graphics
    public boolean drawsTop = false;
    public boolean drawsOverlay = false;
    public TextureRegion icon;
    /**if true will not have paneling**/
    public boolean open = false;
    public static TextureRegion[] panelling;
    /** texture will/may have three variants for the front middle and back **/
    public TextureRegion[] top;
    public TextureRegion[] outline;
    public int variants = 1;
    public VariantOrder variantOrder = VariantOrder.NONE;
    public enum VariantOrder{
        NONE,LEFT_MIDDLE_RIGHT,LEFT_MIDDLE_RIGHT_UP_DOWN;
        public int flipX(int index){
            switch(this){
                case NONE: return index;
                case LEFT_MIDDLE_RIGHT: return index==1?1:2-index;
                case LEFT_MIDDLE_RIGHT_UP_DOWN: return (index==1 || index==3 || index==4) ?index:2-index;
            }
            return index;
        }
    }
    public int drawPriority = 2;


    public boolean hasExtraDecal = false;
    public boolean hasCellDecal = false;
    public TextureRegion[] cell;
    //cost
    public float constructTimeMultiplier = 1; // base time based on item cost
    public ItemStack[] cost = {};
    public int costTotal = 0;
    public ModuleBlock moduleCost = null;
    //module cost..


    //stats
    protected Seq<ModularPartStat> stats = new Seq<>(); // todo: replace with better

    //places it can connect to
    public boolean root = false;
    public boolean visible = true;


    public ModularPart create(int x, int y){
        return new ModularPart(this,x,y);
    }


    public ModularPartType(String name){
        this.name = name;
        partMap.put(id,this);
    }

    public static void loadStatic(){
        panelling = GraphicUtils.getRegions(Core.atlas.find("unity-panel"), 12, 4,16);
    }



    public void load(){
        ///
        String prefix = "unity-part-"+name;
        icon = Core.atlas.find(prefix+"-icon");
        top = getPartSprites(prefix,variants);
        outline = getPartSprites(prefix+"-outline",variants);

        cell= new TextureRegion[]{
            getPartSprite(prefix+"-cell-side"),
            getPartSprite(prefix+"-cell-center")
        };
    }
    public static TextureRegion[] getPartSprites(String name, int am){
        var t = new TextureRegion[am];
        if(am == 1){
            t[0] = getPartSprite(name);
        }else{
            for(int i = 0; i < am; i++){
                t[i] = getPartSprite(name+"i");
            }
        }
        return t;
    }
    public static TextureRegion getPartSprite(String e){
        var f = Core.atlas.find(e);
        if(f == Core.atlas.find("error")){
            f = Core.atlas.find( "unity-part-empty");
        }
        return f;
    }

    public void requirements(String category,ItemStack[] itemcost){
        this.category = category;
        this.cost = itemcost;
        costTotal = 0;
        for(var i:itemcost){
            costTotal+=i.amount;
        }
    }

    public boolean canBeUsedIn(int type){
        return (type & partType) > 0;
    }

    public void setupPanellingIndex(ModularPart part, ModularPart[][] grid){
        if(part.type!=this){Log.err("part with type "+ part.type.name+" is incorrectly using type "+this.name);return;}
        for(int x = 0;x<w;x++){
            for(int y = 0;y<h;y++){
                part.panelingIndexes[x+y*w]=TilingUtils.getTilingIndex(grid,part.x+x,part.y+y,b -> b!=null && !b.type.open);
            }
        }
    }
    public void drawCell(DrawTransform transform, ModularPart part){
        if(hasCellDecal){
            TextureRegion cellsprite = cell[Math.abs(part.cx)<0.01?1:0];
            transform.drawRectScl(cellsprite, part.cx * partSize, part.cy * partSize, part.cx<0?1:-1, 1);
        }
    }
    public void drawTop(DrawTransform transform, ModularPart part){
        if(hasExtraDecal)
            transform.drawRect(top[part.front],part.cx*partSize,part.cy*partSize);
    }
    public void draw(DrawTransform transform, ModularPart part, Entityc c){
        transform.drawRect(panelling[part.panelingIndexes[0]],part.ax*partSize,part.ay*partSize);
    }
    public void drawOutline(DrawTransform transform, ModularPart part, Entityc c){
        if(hasExtraDecal)
            transform.drawRect(outline[part.front], part.cx*partSize,part.cy*partSize);
    }

    //draw editor..

    public void drawEditorOutline(PartsEditorElement editor, int x,int y, boolean valid){
        Draw.color(valid?Color.white:Color.red);
        editor.rect(outline[0], (x + w * 0.5f) * 32, (y + h * 0.5f) * 32, 2);
    }
    public void drawEditor(PartsEditorElement editor, int x,int y, boolean valid){
        editor.rect(top[0], (x + w * 0.5f) * 32, (y + h * 0.5f) * 32, 2);
    }

    public void drawEditorMinimised(PartsEditorElement editor, float x,float y, boolean valid){
        if(w > 1 || h > 1){
            Draw.color(bgCol);
            editor.rectCorner(x * 32, y * 32, w * 32, h * 32);
            Draw.color(bgColMid);
            Lines.stroke(5 * editor.scl);
            editor.rectLine(x * 32 + 4, y * 32 + 4, w * 32 - 8, h * 32 - 8);
        }
        Draw.color(valid ? Color.white : Color.red);
        float maxsize = Math.min(Math.min(w,h)*32,32f/editor.scl);
        editor.rect(icon, (x + w * 0.5f) * 32, (y + h * 0.5f) * 32, maxsize, maxsize);
    }

    public void drawEditorTop(PartsEditorElement editor, int x,int y, boolean valid){ }
    public void drawEditorSelect(PartsEditorElement editor, int x, int y, boolean placed){
        Draw.color(Color.white,0.5f);
        editor.rectCorner(icon, x * 32, y * 32, w * 32, h * 32);
    }
    public void drawEditorOverlay(PartsEditorElement editor, int x, int y){ }

    public void update(Entityc unit, ModularPart part){

    }

    public static ModularPartType getPartFromId(int id){
        if(partMap.containsKey(id)){
            return partMap.get(id);
        }else{
            Log.info("Part of id "+ id+" not found");
            return partMap.get(0);
        }
    }


    //stats.
    public void appendStats(ModularPartStatMap statmap, ModularPart part, ModularPart[][] grid){
        for(var stat:stats){
            stat.merge(statmap,part);
        }
    }
    public void appendStatsPost(ModularPartStatMap statmap, ModularPart part, ModularPart[][] grid){
        for(var stat:stats){
            stat.mergePost(statmap,part);
        }
    }

    //units
    public void differentialSteer(){
               stats.add(new DifferentialSteerStat());
           }
    public void armor(float amount){
           stats.add(new ArmourStat(amount));
       }
    public void health(float amount){
        stats.add(new HealthStat(amount));
    }
    public void mass(float amount){
        stats.add(new MassStat(amount));
    }
    public void producesPower(float amount, float speed){
        stats.add(new EngineStat(amount,speed));
    }
    public void usesPower(float amount ,float speed, float maxSpeed){
        stats.add(new PowerConsumerStat(amount/speed,maxSpeed));
    }
    public void addsWeaponSlots(float amount){
        stats.add(new WeaponSlotStat(amount));
    }
    public void addsAbilitySlots(float amount){
            stats.add(new AbilitySlotStat(amount));
        }
    public void healthMul(float amount){
        stats.add(new HealthStat(amount));
    }
    public void itemCapacity(float amount){
            stats.add(new ItemCapacityStat(amount));
        }
    //turrets
    //???

    @Override
    public void display(Table table){
        table.table(header->{
            //copied from blocks xd
            header.left();
            header.add(new Image(icon)).size(8 * 4);
            header.labelWrap(() -> Core.bundle.get("part."+name))
            .left().width(190f).padLeft(5);
            header.add().growX();
            header.button("?", Styles.flatBordert, () -> {
                //Unity.ui.partinfo.show(this);
            }).size(8 * 5).padTop(-5).padRight(-5).right().grow().name("blockinfo");
        });
        table.row();
        table.table(req -> {
            req.top().left();
            req.add("[lightgray]" + Stat.buildCost.localized() + ":[] ").left().top();
            for(ItemStack stack : cost){
                req.add(new ItemDisplay(stack.item, stack.amount, false)).padRight(5);
            }
        }).growX().left().margin(3);
    }

    public void displayTooltip(Table tip){
        tip.setBackground(Tex.button);
        tip.table(t -> {
            t.table(header -> {
                header.top().left();
                header.image(icon).size(8 * 4);

                header.label(() -> Core.bundle.get("part." + name))
                .left().padLeft(5);
            }).top().left();
            t.row();
            t.image(Tex.whiteui).color(Pal.darkishGray).center().growX().height(5).padTop(5); // separator
        }).growX().padBottom(5);
        tip.row();
        tip.table(desc -> {
            desc.labelWrap(Core.bundle.get("part." + name + ".description")).minWidth(200).grow();
            desc.row();
            desc.image(Tex.whiteui).color(Pal.darkishGray).center().growX().height(5).padTop(5);
        }).top().left().minWidth(300).padBottom(5);

        tip.row();
        tip.table(statTable -> {
            stats.each(stat->{
                stat.display(statTable);
            });
        }).left();

        tip.row();
        tip.table(req -> {
            req.top().left();
            req.add("[lightgray]" + Stat.buildCost.localized() + ":[] ").left().top();
            req.row();
            req.table(reqlist -> {
                reqlist.top().left();
                for(ItemStack stack : cost){
                    reqlist.add(new ItemDisplay(stack.item, stack.amount, false)).padRight(5);
                }
            }).grow();
        }).growX();
    }
}


