#define HIGHP true

#define baseTex u_texture
#define noiseTex u_texture1

varying highp vec2 v_texCoords;

uniform highp sampler2D u_texture;
uniform highp sampler2D u_texture1;

uniform highp vec2 u_campos;
uniform highp vec2 u_resolution;
uniform highp float u_time;

const float noiseScl = 96.0;
const float pi = 3.1415926;
const float pi2 = pi * 2.0;

const float mscl = 30.0;
const float mth = 6.0;

vec2 gradient(vec2 base, float z){
    float rand = texture2D(noiseTex, base / 64.0).r;

    float angle = pi2 * rand + 4.0 * z * rand;
    return vec2(cos(angle), sin(angle));
}

float noise(vec3 pos){
    vec2 base = floor(pos.xy);
    vec2 frac = pos.xy - base;

    vec2 blend = frac * frac * (3.0 - 2.0 * frac);
    return mix(
        mix(
            dot(gradient(base + vec2(0.0, 0.0), pos.z), frac - vec2(0.0, 0.0)),
            dot(gradient(base + vec2(1.0, 0.0), pos.z), frac - vec2(1.0, 0.0)),
            blend.x
        ),
        mix(
            dot(gradient(base + vec2(0.0, 1.0), pos.z), frac - vec2(0.0, 1.0)),
            dot(gradient(base + vec2(1.0, 1.0), pos.z), frac - vec2(1.0, 1.0)),
            blend.x
        ),
    blend.y) / 0.7;
}

vec4 octaveNoise(vec2 coords){
    vec2 uv = coords / noiseScl;

    float val = 0.0;
    float sum = 0.0;
    float mul = 1.0;

    for(int i = 0; i < 6; i++) {
        vec3 noisePos = vec3(uv, 0.2 * u_time / 90.0 / mul);
        val += mul * clamp(noise(noisePos), 0.0, 1.0);
        sum += mul;
        mul *= 0.5;

        uv *= 2.0;
    }

    val = pow((sin(pi2 * val / sum) + 1.0) / 2.0, 4.0);
    return vec4(val * 0.2, val * 0.3, val * 0.4, 1.0);
}

void main(){
    vec2 coords = v_texCoords * u_resolution + u_campos;
    vec2 ratio = vec2(1.0, u_resolution.x / u_resolution.y);

    vec4 base = texture2D(baseTex, v_texCoords + vec2(sin(u_time / 45.0 + coords.y / 1.2) / u_resolution.x / 4.0, 0.0));
    vec4 noise = octaveNoise(coords);

    vec4 color = (base * vec4(0.85, 0.9, 0.95, 1.0) + noise);
    float tester = mod(
        (coords.x + coords.y * 1.1 + sin(u_time / 32.0 + coords.x / 5.0 - coords.y / 100.0) * 2.0) +
        sin(u_time / 80.0 + coords.y / 3.0) * 1.0 +
        sin(u_time / 40.0 - coords.y / 2.0) * 2.0 +
        sin(u_time / 28.0 + coords.y / 1.0) * 0.5 +
        sin(coords.x / 3.0 + coords.y / 2.0) +
        sin(u_time / 80.0 + coords.x / 4.0) * 1.0,
    mscl);

    if(tester < mth) color *= 1.1;
    gl_FragColor = vec4(color.rgb, base.a);
}
