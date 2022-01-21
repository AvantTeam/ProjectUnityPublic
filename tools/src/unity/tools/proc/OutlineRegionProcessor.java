package unity.tools.proc;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;
import unity.gen.*;
import unity.gen.Regions.*;
import unity.tools.*;
import unity.tools.GenAtlas.*;

import java.lang.invoke.*;
import java.util.concurrent.*;

/**
 * A processor to outline certain regions in {@link Regions}.
 * @author GlennFolker
 */
public class OutlineRegionProcessor implements Processor{
    @Override
    public void process(ExecutorService exec){
        for(var field : Regions.class.getFields()){
            Outline anno = field.getAnnotation(Outline.class);
            if(anno == null) continue;

            String name = field.getName();

            GenRegion rawRegion = Reflect.get(Regions.class, name.replace("OutlineRegion", "Region"));

            submit(exec, "Regions.java", () -> {
                Color color = Color.valueOf(anno.color());
                int rad = anno.radius();

                PixmapRegion region = new PixmapRegion(rawRegion.pixmap());
                Pixmap out = Pixmaps.outline(region, color, rad);

                GenRegion outlineRegion = new GenRegion(rawRegion.name + "-outline", out);
                outlineRegion.relativePath = rawRegion.relativePath;
                outlineRegion.save();

                VarHandle handle = MethodHandles.publicLookup().unreflectVarHandle(field);
                handle.setVolatile(outlineRegion);
            });
        }
    }
}
