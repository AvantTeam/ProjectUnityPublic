package unity.util;

import java.util.*;

/**
 * Can read or write individual bits. Used in compression algos that use single bit flags.
 */
public class BitReadWriter{
    int pointer=0;
    byte[] raw;
    byte current,next;

    public BitReadWriter(byte[] raw){
        this.raw = raw;
        resetPointer();
    }

    public void resetPointer(){
        pointer = 0;
        current = raw[0];
        next = raw.length>1?raw[1]:-1;
    }

    public int readBit(){
        if(current==-1){
            return current;
        }
        int bit = (current >>> (~pointer & 0x7) ) & 0x1;
        skipBit();
        return bit;
    }

    public int readByte(){
        int o = (pointer & 0x7);
        int b = (((int)current << o) & 0xFF) | ((next & 0xFF) >>> (8-o)); //it makes less sense the more i look at it
        skipByte();
        return b;
    }

    public short readShort(){
        return (short)(readByte() | (readByte()<<8));
    }
    public char readChar(){
        return (char)(readByte());
    }

    void expand(){
        byte[] newRaw = new byte[raw.length*2];
        System.arraycopy(raw,0,newRaw,0,raw.length);
        raw = newRaw;

    }

    void expandToPointer(){
        if(pointer+8 < raw.length*8){
          return;
        }
        while(pointer+8 >= raw.length*8){
            expand();
        }
        int byteOfPointer = byteOf(pointer);
        current = raw[byteOfPointer];
        next = raw[byteOfPointer+1];
    }

    public void trimToPointer(){
        byte[] newRaw = new byte[byteOf(pointer)+1];
        System.arraycopy(raw,0,newRaw,0,Math.min(newRaw.length,raw.length));
        raw = newRaw;
    }

    public byte[] trimAndGetByteArray(){
        byte[] newRaw = new byte[byteOf(pointer)+1];
        System.arraycopy(raw,0,newRaw,0,Math.min(newRaw.length,raw.length));
        return newRaw;
    }

    public void writeBit(int b){
        expandToPointer();
        int mask = 0b10000000 >>> (pointer & 0x7);//a kind of sliding bit window
        current &= ~mask;
        current |= mask*b;
        raw[byteOf(pointer)] = current;
        skipBit();
    }

    public void writeByte(byte b){
       expandToPointer();
       int mask = 0b11111111 >>> (pointer & 0x7);//a kind of sliding bit window
        current &= ~mask;
        current |= (byte)((b & 0xff) >>> (pointer & 0x7)); // java casts bytes to int before >>> so
        raw[byteOf(pointer)] = current;
        next &= mask;
        int invshift = (8-(pointer & 0x7));
        next |= ((b << invshift)& 0xFF);
        raw[byteOf(pointer)+1] = next;

        skipByte();
    }

    //jit will inline this probably


    /**
     * Shifts the pointer by 1
     */
    public void skipBit(){
        int byteOfPointer = byteOf(pointer);
        if(byteOfPointer != byteOf(pointer+1) ){
            current = next;
            next = raw.length > byteOfPointer+2 ? raw[byteOfPointer+2]:-1;
        }
        pointer++;
    }


    /**
     * Shifts the pointer by 8
     */
    public void skipByte(){
        int byteOfPointer = byteOf(pointer);
        current = next;
        next = raw.length > byteOfPointer+2 ? raw[byteOfPointer+2]:-1;
        pointer+=8;
    }

    int byteOf(int point){
        return point>>3;
    }
}
