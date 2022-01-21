package unity;

import arc.graphics.*;
import arc.math.geom.*;
import unity.annotations.Annotations.*;
import unity.annotations.Annotations.StructField.*;

@SuppressWarnings("all")
final class StructDefs{
    // SColor - Packs r, g, b, and a values into int from left-most bits to right, each taking 8 bits
    @StructWrap(value = {
        @StructField(name = "r", packer = FloatPacker.rgba8888),
        @StructField(name = "g", packer = FloatPacker.rgba8888),
        @StructField(name = "b", packer = FloatPacker.rgba8888),
        @StructField(name = "a", packer = FloatPacker.rgba8888)
    }, left = true)
    Color colorStruct = new Color();

    // SVec2 - Packs x and y values into int from right-most bits to left, each taking 32 bits
    @StructWrap({
        @StructField(name = "x"),
        @StructField(name = "y")
    })
    Vec2 vec2Struct = new Vec2();

    // Float2 - Stores 2 floats in a long integer
    @Struct
    class Float2Struct{
        float x;
        float y;
    }

    // Bool3 - Stores 3 booleans in a byte
    @Struct
    class Bool3Struct{
        boolean x;
        boolean y;
        boolean z;
    }
}
