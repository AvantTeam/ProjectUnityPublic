package unity.io;

import arc.util.io.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.net.*;
import unity.gen.entities.*;

import static mindustry.Vars.net;
import static mindustry.net.Net.registerPacket;

/** Defines all shared modded network synchronization packet handlers. Do as little refactorings as possible! */
public final class PUPackets{
    private PUPackets(){
        throw new AssertionError();
    }

    /** Registers the defined packets to {@link Net}. */
    public static void register(){
        registerPacket(MonolithSoulChangePacket::new);
    }

    /**
     * Handles the synchronization for {@link MonolithSoul}'s {@linkplain MonolithSoul#form() forming}, {@linkplain MonolithSoul#join()
     * joining}, and {@linkplain MonolithSoul#crack() cracking}.
     * @author GlennFolker
     */
    public static class MonolithSoulChangePacket extends Packet{
        public MonolithSoul soul;
        public Change to;

        public MonolithSoulChangePacket(){}

        public MonolithSoulChangePacket(MonolithSoul soul, Change to){
            this.soul = soul;
            this.to = to;
        }

        @Override
        public void write(Writes write){
            TypeIO.writeUnit(write, soul);

            write.b(to.ordinal());
            if(to == Change.join) PUTypeIO.writeHealthc(write, soul.joinTarget);
        }

        @Override
        public void read(Reads read){
            soul = (MonolithSoul)TypeIO.readUnit(read);
            if((to = Change.all[read.b()]) == Change.join){
                Healthc target = PUTypeIO.readHealthc(read);
                if(soul != null) soul.joinTarget = target;
            }
        }

        @Override
        public void handled(){
            if(soul == null) return;
            switch(to){
                case crack -> soul.crack();
                case join -> soul.join();
                case form -> soul.form();
            }
        }

        @Override
        public void handleServer(NetConnection con){
            net.sendExcept(con, new MonolithSoulChangePacket(soul, to), false);
        }

        public static void send(MonolithSoul soul, Change to){
            net.send(new MonolithSoulChangePacket(soul, to), false);
        }

        public enum Change{
            crack,
            join,
            form;

            public static final Change[] all = values();
        }
    }
}
