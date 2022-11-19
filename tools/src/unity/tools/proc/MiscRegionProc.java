package unity.tools.proc;

import arc.graphics.*;
import arc.math.*;
import unity.tools.GenAtlas.*;
import unity.tools.*;

import java.util.concurrent.*;

/**
 * Generates miscellaneous regions, e.g. {@code line-shade}.
 * @author GlennFolker
 */
public class MiscRegionProc implements Processor{
    private final Color col1 = new Color(), col2 = new Color(), col3 = new Color();

    @Override
    public void process(ExecutorService exec){
        exec.submit(this::lineShade);
    }

    private void lineShade(){
        int height = 200;
        col1.set(0.85f, 0.7f, 1f, 1f);
        col2.set(col1).shiftHue(180f);

        Pixmap out = new Pixmap(1, height);
        for(int y = 0; y < height; y++){
            float prog = y / (height - 1f);
            float slope = Mathf.slope(prog);

            out.set(0, y, col3.set(col1).lerp(col2, slope).a(Interp.fade.apply(slope)).rgba());
        }

        new GenRegion("line-shade", "effects", out).save();
    }
}
