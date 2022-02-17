package unity.parts;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import unity.content.*;
import unity.parts.PanelDoodadType.*;

public class UnitDoodadGenerator{
    public static void initDoodads(int rngseed, Seq<PanelDoodad> doodads,ModularConstruct construct){
        Rand rand = new Rand();
        rand.setSeed(rngseed);
        /// :I welp i tried
        if(construct != null){
            if(construct.parts.length == 0){
                return;
            }
            boolean[][] filled = new boolean[construct.parts.length][construct.parts[0].length];


            int w = construct.parts.length;
            int h = construct.parts[0].length;
            int miny = 999, maxy = 0;
            for(int i = 0; i < w; i++){
                for(int j = 0; j < h; j++){
                    filled[i][j] = construct.parts[i][j] != null && !construct.parts[i][j].type.open;
                    if(filled[i][j]){
                        miny = Math.min(j, miny);
                        maxy = Math.max(j, maxy);
                    }
                }
            }

            float[][] lightness = new float[w][h];
            int tiles = 0;
            for(int i = 0; i < w; i++){
                for(int j = 0; j < h; j++){
                    lightness[i][j] = Mathf.clamp((0.5f - (1f - Mathf.map(j, miny, maxy, 0, 1))) * 2 + 1, 0, 1);
                    tiles += filled[i][j] ? 1 : 0;
                }
            }
            if(tiles == 0){
                return;
            }
            Seq<Point2> seeds = new Seq();
            int[][] seedspace = new int[w][h];
            int[][] seedspacebuf = new int[w][h];
            for(int i = 0; i < Math.max(Mathf.floor(Mathf.sqrt(tiles) / 2), 1); i++){
                int cnx = rand.random(0, Mathf.floor(w)-1);
                int cny = rand.random(0, h - 1);
                while(!filled[cnx][cny]){
                    cnx = rand.random(0, Mathf.floor(w)-1);
                    cny = rand.random(0, h - 1);
                }
                seeds.add(new Point2(cnx, cny));
                seedspace[cnx][cny] = seeds.size;
                if(filled[w - cnx - 1][cny]){
                    seedspace[w - cnx - 1][cny] = seeds.size;
                }
            }
            boolean hasEmpty = true;
            while(hasEmpty){
                hasEmpty = false;
                for(int i = 0; i < w; i++){
                    for(int j = 0; j < h; j++){
                        if(seedspace[i][j] != 0){
                            int seed = seedspace[i][j];
                            seedspacebuf[i][j] = seed;
                            if(i > 0 && seedspace[i - 1][j] == 0 && seedspacebuf[i - 1][j] < seed){
                                seedspacebuf[i - 1][j] = seed;
                            }
                            if(i < w - 1 && seedspace[i + 1][j] == 0 && seedspacebuf[i + 1][j] < seed){
                                seedspacebuf[i + 1][j] = seed;
                            }
                            if(j > 0 && seedspace[i][j - 1] == 0 && seedspacebuf[i][j - 1] < seed){
                                seedspacebuf[i][j - 1] = seed;
                            }
                            if(j < h - 1 && seedspace[i][j + 1] == 0 && seedspacebuf[i][j + 1] < seed){
                                seedspacebuf[i][j + 1] = seed;
                            }
                        }else{
                            hasEmpty = true;
                        }
                    }
                }
                for(int i = 0; i < w; i++){
                    for(int j = 0; j < h; j++){
                        seedspace[i][j] = seedspacebuf[i][j];
                    }
                }
            }
            for(int i = 0; i < Math.round(w / 2f) - 1; i++){
                for(int j = 0; j < h; j++){
                    float val = Mathf.map((i * 34.343f + j * 844.638f) % 1f, -0.1f, 0.1f);
                    lightness[i][j] += val;
                    lightness[w - i - 1][j] += val;
                }
            }
            for(int i = 0; i < w; i++){
                for(int j = 0; j < h; j++){

                    if(j > 0 && seedspace[i][j] != seedspace[i][j - 1]){
                        lightness[i][j] -= 0.5;
                    }
                    if(i == Math.round(w / 2f) - 1){
                        continue;
                    }
                    lightness[i][j] = Mathf.clamp(lightness[i][j], 0, 1);
                }
            }

            ///finally apply doodads
            boolean[][] placed = new boolean[construct.parts.length][construct.parts[0].length];
            float ox = -w * 0.5f;
            float oy = -h * 0.5f;
            int middlex = Math.round(w / 2f) - 1;
            Seq<PanelDoodadType> draw = new Seq<>();
            PanelDoodadType mirrored = null;
            for(int i = 0; i < Math.round(w / 2f); i++){
                for(int j = 0; j < h; j++){
                    mirrored = null;

                    if(filled[i][j] && !placed[i][j]){
                        draw.clear();
                        for(var pal: UnityParts.unitDoodads){
                            if(pal.w==1 && pal.h==1){
                                draw.add(pal.get(1-lightness[i][j]));
                            }else{
                                var type = pal.get(1-lightness[i][j]);
                                boolean allowed = false;
                                if((pal.w%2==0 || pal.sides) && i + pal.w-1 < middlex){
                                    allowed = true;
                                }
                                if(pal.center && i == middlex-(pal.w/2)){
                                    allowed = true;
                                }
                                if(allowed && type.canFit(construct.parts, i, j)){
                                    draw.add(type);
                                }
                            }
                        }
                        PanelDoodadType doodad = draw.random(rand);
                        mirrored = doodad;

                        addDoodad(doodads,placed, get(doodad, i + ox, j + oy), i, j);
                    }
                    if(filled[w - i - 1][j] && !placed[w - i - 1][j]){
                        if(mirrored != null){
                            addDoodad(doodads,placed, get(mirrored, w - i - mirrored.w + ox, j + oy), w - i - mirrored.w, j);
                            continue;
                        }
                        draw.clear();
                        for(var pal:UnityParts.unitDoodads){
                            if(pal.w==1 && pal.h==1){
                                draw.add(pal.get(1-lightness[w - i -1][j]));
                            }else{
                                var type = pal.get(1-lightness[w - i -1][j]);
                                boolean allowed = false;
                                if((pal.w%2==0 || pal.sides) && w - i -1 > middlex){
                                    allowed = true;
                                }
                                if(allowed && type.canFit(construct.parts, i, j)){
                                    draw.add(type);
                                }
                            }
                        }
                        PanelDoodadType doodad = draw.random(rand);
                        addDoodad(doodads,placed, get(doodad, w - i - doodad.w + ox, j + oy), w - i - doodad.w, j);
                    }
                }
            }
        }
    }

    public static void addDoodad(Seq<PanelDoodad> doodadlist,boolean[][] placed, PanelDoodad p, int x, int y){
        doodadlist.add(p);
        for(int i = 0; i < p.type.w; i++){
            for(int j = 0; j < p.type.h; j++){
                placed[x + i][y + j] = true;
            }
        }
    }

    public static PanelDoodad get(PanelDoodadType type, float x, float y){
        return type.create((type.w * 0.5f + x) * ModularPartType.partSize, (type.h * 0.5f + y) * ModularPartType.partSize, x > 0);
    }

}
