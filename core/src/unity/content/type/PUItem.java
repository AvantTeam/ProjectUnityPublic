package unity.content.type;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
import mindustry.type.*;
import unity.util.*;

/**
 * Currently just used to "correct" the region loading.
 * @author GlennFolker
 */
public class PUItem extends Item{
    public PUItem(String name, Color color){
        super(name, color);
    }

    @Override
    public void loadIcon(){
        fullIcon = MiscUtils.reg(this);
        uiIcon = MiscUtils.uiReg(this);

        String targetName = ((AtlasRegion)fullIcon).name;
        if(targetName.endsWith("1")) targetName = targetName.substring(0, targetName.length() - 1);

        if(frames > 0){
            TextureRegion[] regions = new TextureRegion[frames * (transitionFrames + 1)];

            if(transitionFrames <= 0){
                for(int i = 1; i <= frames; i++){
                    regions[i - 1] = Core.atlas.find(targetName + i);
                }
            }else{
                for(int i = 0; i < frames; i++){
                    regions[i * (transitionFrames + 1)] = Core.atlas.find(targetName + (i + 1));
                    for(int j = 1; j <= transitionFrames; j++){
                        int index = i * (transitionFrames + 1) + j;
                        regions[index] = Core.atlas.find(targetName + "-t" + index);
                    }
                }
            }

            fullIcon = new TextureRegion(fullIcon);
            uiIcon = new TextureRegion(uiIcon);

            Events.run(Trigger.update, () -> {
                int frame = (int)(Time.globalTime / frameTime) % regions.length;

                fullIcon.set(regions[frame]);
                uiIcon.set(regions[frame]);
            });
        }
    }

    @Override
    public void createIcons(MultiPacker packer){
        String name = MiscUtils.reg(this).name;
        if(name.endsWith("1")) name = name.substring(0, name.length() - 1);

        if(frames > 0 && transitionFrames > 0){
            PixmapRegion[] pixmaps = new PixmapRegion[frames];

            for(int i = 0; i < frames; i++) pixmaps[i] = Core.atlas.getPixmap(name + (i + 1));
            for(int i = 0; i < frames; i++){
                for(int j = 1; j <= transitionFrames; j++){
                    float f = (float)j / (transitionFrames + 1);
                    int index = i * (transitionFrames + 1) + j;

                    Pixmap res = Pixmaps.blend(pixmaps[i], pixmaps[(i + 1) % frames], f);
                    packer.add(PageType.main, name + "-t" + index, res);
                }
            }
        }
    }
}
