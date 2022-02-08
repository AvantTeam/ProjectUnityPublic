package unity.util;

import arc.func.*;

public class TilingUtils{
    static int[][] tilechkdirs = {
    	{-1, 1},{0, 1},{1, 1},
    	{-1, 0},/*{X}*/{1, 0},
    	{-1,-1},{0,-1},{1,-1},
    };

    static int[] tileMap = {//not sure how to format this.
    	39,39,27,27,39,39,27,27,38,38,17,26,38,38,17,26,36,
    	36,16,16,36,36,24,24,37,37,41,21,37,37,43,25,39,
    	39,27,27,39,39,27,27,38,38,17,26,38,38,17,26,36,
    	36,16,16,36,36,24,24,37,37,41,21,37,37,43,25,3,
    	3,15,15,3,3,15,15,5,5,29,31,5,5,29,31,4,
    	4,40,40,4,4,20,20,28,28,10,11,28,28,23,32,3,
    	3,15,15,3,3,15,15,2,2,9,14,2,2,9,14,4,
    	4,40,40,4,4,20,20,30,30,47,44,30,30,22,6,39,
    	39,27,27,39,39,27,27,38,38,17,26,38,38,17,26,36,
    	36,16,16,36,36,24,24,37,37,41,21,37,37,43,25,39,
    	39,27,27,39,39,27,27,38,38,17,26,38,38,17,26,36,
    	36,16,16,36,36,24,24,37,37,41,21,37,37,43,25,3,
    	3,15,15,3,3,15,15,5,5,29,31,5,5,29,31,0,
    	0,42,42,0,0,12,12,8,8,35,34,8,8,33,7,3,
    	3,15,15,3,3,15,15,2,2,9,14,2,2,9,14,0,
    	0,42,42,0,0,12,12,1,1,45,18,1,1,19,13
    };

    public static <T> int getMaskIndex(T[][] map, int x,int y, Boolf<T> canConnect){
        int index = 0, ax=0,ay=0; T t = null;
        for(int i = 0;i<tilechkdirs.length;i++){
            ax = tilechkdirs[i][0]+x;
            ay = tilechkdirs[i][1]+y;
            t = null;
            if(ax>=0 && ay>=0 && ax<map.length && ay<map[0].length){
                t= map[ax][ay];
            }
            index += canConnect.get(t)?(1<<i):0;
        }
        return index;
    }

    public static <T> int getTilingIndex(T[][] map, int x,int y, Boolf<T> canConnect){
        return tileMap[getMaskIndex(map,x,y,canConnect)];
    }
}
