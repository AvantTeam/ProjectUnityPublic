#define HIGHP

uniform sampler2D u_texture;
uniform sampler2D u_sprites;
uniform float u_time;
uniform vec2 u_resolution;
uniform int u_sprites_width;

varying vec4 v_color;
varying vec2 v_texCoords;

const float epsilon = 0.001;



vec4 getTex(vec2 offset, int type){
    offset = mod(offset,vec2(3.0))/3.0;
    float ts = 1.0/float(u_sprites_width);
    float ty = float(type/u_sprites_width)*ts+epsilon;
    float tx = mod(float(type),float(u_sprites_width))*ts+epsilon;
    ts-=epsilon*2.0;
    return texture2D(u_sprites,vec2(tx+offset.x*ts,ty+offset.y*ts));
}

float getNoise(vec2 offset){
    offset = mod(offset,vec2(3.0))/3.0;
    float ts = 1.0/float(u_sprites_width) - epsilon * 2.0 ;
    return texture2D(u_sprites,vec2(epsilon+offset.x*ts,epsilon+offset.y*ts)).r;
}

float getLayeredNoise(vec2 offset){
    return (getNoise(offset*0.333) + 0.5* getNoise(offset))/1.5;
}

float blendFun(float x){
    return 1.0-2.0*abs(fract(x)-0.5);
}

vec4 getBlendedTex(vec2 offset,vec2 dir, float t, int type){
    t = fract(t);
    return blendFun(t)*getTex(offset+dir*t,type) + blendFun(t+0.5)*getTex(offset+dir*fract(t+0.5),type);
}

float getBlendedNoise(vec2 offset,vec2 dir, float t){
    t = fract(t);
    return blendFun(t)*getLayeredNoise(offset+dir*t) + blendFun(t+0.5)*getLayeredNoise(offset+dir*fract(t+0.5));
}

float contrast(float t,float am){
    return (t-0.5)*am+0.5;
}

void main(){
    vec4 color = texture2D(u_texture, v_texCoords.xy);
    float depth = color.a;
    if(depth<0.1){
        discard;
    }
    vec2 tilepos = v_texCoords.xy*u_resolution.xy;
    float type = texture2D(u_texture, (floor(tilepos)+vec2(0.5))/u_resolution.xy).r;
    vec2 vel = (color.gb-vec2(0.5))*10.0;
    float vdis = length(vel);
    gl_FragColor = getBlendedTex(tilepos ,-vel, u_time * 0.007,int(type*256.0)+1);
    gl_FragColor.a *= clamp(depth*2.0,0.0,1.0);
    gl_FragColor.rgb *= 1.0-(depth-0.1)*0.5;
    float wavedepth = clamp((depth-0.1)*10.0,0.0,1.0);
    bool isWave = depth+sin(u_time*0.07+wavedepth*wavedepth*9.0)*0.4*(1.0-wavedepth) < 0.2; // border ripples

    float noiser = contrast(getBlendedNoise(tilepos*0.5 ,-vel*0.5, u_time * 0.007+1.0),1.5); // the rapids foam
    if(isWave || noiser<vdis*0.1){
        gl_FragColor = mix(gl_FragColor,vec4(1.0),0.5);
    }
}

