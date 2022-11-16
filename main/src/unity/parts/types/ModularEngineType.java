package unity.parts.types;

import arc.math.*;
import unity.parts.*;
import unity.ui.*;

public class ModularEngineType extends ModularPartType{
    public ModularEngineType(String name){
        super(name);
    }

    @Override
    public void drawEditor(PartsEditorElement editor, int x, int y, boolean valid){
        float offX = Mathf.range(1);
        float offY = Mathf.range(1);
        editor.rect(top[0], (x + w * 0.5f) * 32 + offX, (y + h * 0.5f) * 32 + offY, 2);
    }
}
