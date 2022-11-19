package unity.annotations.processors.util;

import arc.struct.*;
import arc.util.io.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.util.*;
import mindustry.io.*;
import unity.annotations.Annotations.*;
import unity.annotations.processors.*;

import javax.lang.model.element.*;
import javax.lang.model.element.Modifier;

import static unity.annotations.processors.BaseProcessor.*;
import static javax.lang.model.element.Modifier.*;
import static javax.lang.model.type.TypeKind.*;

/**
 * @author Anuke
 * @author GlennFolker
 */
public class TypeIOResolver{
    public static ClassSerializer resolve(BaseProcessor proc){
        ClassSerializer out = new ClassSerializer(new ObjectMap<>(), new ObjectMap<>(), new ObjectMap<>(), new ObjectMap<>());

        Seq<ClassSymbol> handlers = Seq.with(proc.<ClassSymbol>with(TypeIOHandler.class)).add(proc.conv(TypeIO.class));
        for(ClassSymbol handler : handlers){
            for(Element e : handler.getEnclosedElements()){
                if(!(e instanceof MethodSymbol)) continue;
                MethodSymbol m = (MethodSymbol)e;

                if(is(m, PUBLIC, STATIC)){
                    List<VarSymbol> params = m.params;
                    int size = params.size();

                    if(size == 0) continue;
                    String sig = fName(handler) + "." + name(m), ret = fixName(m.getReturnType().toString());

                    boolean isVoid = m.getReturnType().getKind() == VOID;
                    Type f = params.get(0).type;
                    ClassSymbol w = proc.conv(Writes.class), r = proc.conv(Reads.class);

                    if(size == 2 && proc.same(f, w)){
                        (sig.endsWith("Net") ? out.netWriters : out.writers).put(fixName(params.get(1).type.toString()), sig);
                    }else if(size == 1 && proc.same(f, r) && !isVoid){
                        out.readers.put(ret, sig);
                    }else if(size == 2 && proc.same(f, r) && !isVoid && proc.same(m.getReturnType(), params.get(1).type)){
                        out.mutatorReaders.put(ret, sig);
                    }
                }
            }
        }

        return out;
    }

    public static class ClassSerializer{
        public final ObjectMap<String, String> writers, readers, mutatorReaders, netWriters;

        public ClassSerializer(ObjectMap<String, String> writers, ObjectMap<String, String> readers, ObjectMap<String, String> mutatorReaders, ObjectMap<String, String> netWriters){
            this.writers = writers;
            this.readers = readers;
            this.mutatorReaders = mutatorReaders;
            this.netWriters = netWriters;
        }

        public String getNetWriter(String type, String fallback){
            return netWriters.get(type, writers.get(type, fallback));
        }
    }
}
