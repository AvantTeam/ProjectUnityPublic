package unity.parts.graphics;

import mindustry.gen.*;
import unity.ui.*;
import unity.util.*;

public abstract class ModularPartDoodad{
    public abstract void draw(DrawTransform d, float x, float y, Entityc e);
    public abstract void draw(PartsEditorElement editor, float x, float y, float rotation);
}
