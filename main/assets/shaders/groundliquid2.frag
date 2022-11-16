#define HIGHP

uniform sampler2D u_texture;
uniform sampler2D u_sprites;
uniform float u_time;
uniform float u_trans;
uniform vec2 u_resolution;
uniform vec2 u_chunksize;
uniform vec2 u_offset;
uniform int u_sprites_width;

varying vec4 v_shallowcolor;
varying vec4 v_deepcolor;
varying vec2 v_texCoords;
varying float v_fluidType;
varying float v_tx;
varying float v_ty;
varying float v_tx2;
varying float v_ty2;
varying float v_ts;

const float epsilon = 0.001;



vec4 getTex(vec2 offset){
    offset = mod(offset,vec2(3.0))/3.0;
    return texture2D(u_sprites,vec2(v_tx+offset.x*v_ts,v_ty+offset.y*v_ts));
}

float getNoise(vec2 offset){
    offset = mod(offset,vec2(2.0))/3.0;
    return texture2D(u_sprites,vec2(v_tx2+offset.x*(v_ts-epsilon),v_ty2+(offset.y+0.3333)*(v_ts-epsilon))).r;
}

float getLayeredNoise(vec2 offset){
    return (getNoise(offset*0.08) + 0.5* getNoise(offset*0.333) + 0.25* getNoise(offset))/1.75;
}

float blendFun(float x){
    return 1.0-2.0*abs(fract(x)-0.5);
}

vec4 getBlendedTex(vec2 offset,vec2 dir, float t){
    t = fract(t);
    return blendFun(t)*getTex(offset+dir*t) + blendFun(t+0.5)*getTex(offset+dir*fract(t+0.5));
}

float getBlendedNoise(vec2 offset,vec2 dir, float t){
    t = fract(t);
    return blendFun(t)*getLayeredNoise(offset+dir*t) + blendFun(t+0.5)*getLayeredNoise(offset+dir*fract(t+0.5));
}

vec4 getWaveTex(float t){
    return texture2D(u_sprites,vec2(v_tx2+clamp(t,epsilon,1.0-epsilon)*v_ts,v_ty2+0.1666*v_ts));
}

float contrast(float t,float am){
    return (t-0.5)*am+0.5;
}

vec4 alphaComposite(vec4 under,vec4 over){
    float a0 = over.a + under.a * (1.0-over.a);
    return vec4((over.rgb*over.a + under.rgb*under.a * (1.0-over.a))/a0,a0);
}

void main(){
    vec4 color = texture2D(u_texture, v_texCoords.xy);
    float depth = mix(color.r,color.a,u_trans);
    if(depth<0.1){
        discard;
    }
    vec2 tilepos = mod(v_texCoords.xy*u_resolution.xy,u_chunksize.xy)+u_offset.xy;
    vec2 vel = (color.gb-vec2(0.5))*10.0;
    float vdis = length(vel);
    gl_FragColor = getBlendedTex(tilepos ,-vel, u_time * 0.007);
    gl_FragColor.rgba *= mix(v_shallowcolor,v_deepcolor,(depth-0.1)*1.5).rgba;
    float wavedepth = clamp((depth-0.1)*10.0,0.0,1.0);
    bool isWave = depth+sin(u_time*0.07+wavedepth*wavedepth*9.0)*0.4*(1.0-wavedepth) < 0.2; // border ripples

    float noiser = contrast(getBlendedNoise(tilepos*0.5 ,-vel*0.5, u_time * 0.007+1.0),1.5)-0.3 + vdis*0.15; // the rapids foam
    if(isWave){
        gl_FragColor = mix(gl_FragColor,vec4(1.0),0.5);
    }
    vec4 wtex = getWaveTex(noiser);
    gl_FragColor = alphaComposite(gl_FragColor,wtex);


}

