#define HIGHP

uniform sampler2D u_texture;
uniform sampler2D u_texture2;
uniform sampler2D u_noise;

uniform vec2 u_campos;
uniform vec2 u_resolution;
uniform float u_time;

varying vec2 v_texCoords;


const float samplelen = 20.0;
const float epsilonp1 = 1.01;

uniform vec4 u_toplayer;
uniform float tvariants;
uniform vec4 u_bottomlayer;
uniform float bvariants;
uniform vec4 u_truss;


float rand(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}

float noise(vec2 p){
    vec2 ip = floor(p);
    vec2 u = fract(p);
    u = u*u*(3.0-2.0*u);

    float res = mix(
    mix(rand(ip), rand(ip+vec2(1.0, 0.0)), u.x),
    mix(rand(ip+vec2(0.0, 1.0)), rand(ip+vec2(1.0, 1.0)), u.x), u.y);
    return res*res;
}

vec4 textureRegion(vec4 region, vec2 param, float variants, float variant){
    vec2 rsize = vec2((region.z-region.x)/variants, region.w-region.y);
    return texture2D(u_texture2, region.xy + rsize*param + vec2(rsize.x*variant, 0.0));
}

vec4 textureRegion(vec4 region, vec2 param){
    return texture2D(u_texture2, mix(region.xy, region.zw, param));
}


float sqaureRay(vec2 rorg, vec2 invrdir){
    float t1 = (-rorg.x)*invrdir.x;
    float t2 = (1.0 - rorg.x)*invrdir.x;
    float t3 = (-rorg.y)*invrdir.y;
    float t4 = (1.0 - rorg.y)*invrdir.y;
    return min(max(t1, t2), max(t3, t4));
}



//rdir should be normalised
float tileMarch(vec2 tpos, vec2 rdir, float maxlen, vec2 tile, vec2 tilestep){
    vec2 irdir = vec2(1.0)/rdir;
    ivec2 rt = ivec2(0, 0);
    //first step shouldnt have anything
    float len = 0.0;
    for (float i = 0.0;i<maxlen*2.0+2.0;i++){
        float st = sqaureRay(tpos, irdir);
        tpos += st*epsilonp1*rdir;
        vec2 l = fract(tpos);
        rt += ivec2(floor(tpos));
        tile += floor(tpos)*tilestep;
        tpos = l;
        len += st;
        if (texture2D(u_texture, tile).a<0.9 || len>maxlen){
            break;
        }
    }

    return len;
}
float tileMarchCoord(vec2 rdir, float maxlen, vec2 coord, vec2 v){
    vec2 tile = mod(coord, 8.0);
    return tileMarch(tile/vec2(8.0), rdir, maxlen, (coord - tile - u_campos)*v, vec2(8.0)*v);
}


float fade(vec2 bcoords, vec2 v){
    vec2 nc = (bcoords- u_campos)*v;
    float fade =  max(abs(nc.x-0.5), abs(nc.y-0.5))*2.0;
    return 1.0 - (fade*fade*fade);
}
float fade2(vec2 bcoords, vec2 v){
    vec2 nc = (bcoords- u_campos)*v;
    float ratio = v.x/v.y;
    nc -= vec2(0.5);
    nc.x/=ratio;
    float fade =  length(nc)*2.0;
    return 1.0 - (fade*fade*fade);
}


void main() {

    vec4 tex = texture2D(u_texture, v_texCoords);
    if (tex.a<1.0){
        discard;
    }
    float btime = u_time / 1000.0;
    vec2 c = v_texCoords;
    vec2 v = vec2(1.0/u_resolution.x, 1.0/u_resolution.y);
    vec2 coords = vec2(c.x * u_resolution.x + u_campos.x, c.y * u_resolution.y + u_campos.y);
    vec2 tile =  mod(coords+vec2(4.0), 8.0)/vec2(8.0);
    vec2 dir = (c-vec2(0.5))*vec2(1.0, u_resolution.y/u_resolution.x);
    float length = length(dir);
    dir/=length;
    float slen = samplelen*length;

    coords+=vec2(4.0);
    vec2 tiletexv = ((coords-mod(coords, 8.0)) - u_campos)*v;
    float z = tileMarch(tile, dir, slen, tiletexv, vec2(8.0)*v);
    if (z>slen){
        z = slen;
    }
    z*=8.0;
    vec2 bcoords=coords+dir*z;
    float az = z/=length;
    tile = mod(bcoords, 8.0);
    vec2 wallcoords = vec2(bcoords.x+bcoords.y, az)/8.0;
    vec2 repeat = fract(wallcoords);
    vec3 col = vec3(0.0);
    if (az<=8.0){ //textures
        col = textureRegion(u_toplayer, repeat).rgb;
    } else {
        col = textureRegion(u_bottomlayer, repeat, bvariants, floor(bvariants*noise(wallcoords-repeat))).rgb;
    }
    col*=(1.0- length*z/(slen*8.0));

    gl_FragColor = vec4(col*fade(bcoords, v), 1.0);

    if (az>=samplelen*7.95){ //glowy
        vec2 tpos = coords+dir*samplelen*16.0*length;
        vec2 offset = vec2(sin(btime+tpos.x*0.01), cos(btime+tpos.y*0.01));
        vec2 offset2 = vec2(texture2D(u_noise, offset).r,texture2D(u_noise, offset+vec2(0.67,0.13)).r)-vec2(0.5);
        float truss = texture2D(u_texture, (tpos- u_campos + offset2*16.0)*v).a;
        gl_FragColor.rgb = vec3(0.4, 0.2, 0.1) * fade2(tpos, v) * truss;
    }
    if (az>=60.0){ //truss bottom
        tileMarch(tile, dir, slen, tiletexv, vec2(8.0)*v);
        vec2 tpos = coords+dir*60.0*length;
        vec4 truss = textureRegion(u_truss, fract(tpos/24.0)).rgba;
        gl_FragColor.rgb = mix(gl_FragColor.rgb, truss.rgb * fade(tpos, v) * 0.5, truss.a);
    }
    if (az>=52.0){ //truss top
        vec2 tpos = coords+dir*52.0*length;
        vec4 truss = textureRegion(u_truss, fract(tpos/24.0)).rgba;
        float shadlen = 30.0/8.0;
        float sz = tileMarchCoord(vec2(0.707,0.707),shadlen,tpos,v);
        if(sz<shadlen-1.0){
            truss.rgb *= 0.5;
        }
        gl_FragColor.rgb = mix(gl_FragColor.rgb, truss.rgb * fade(tpos, v), truss.a);
    }

}
