#define HIGHP

//shades of lava
#define S4 vec3(100.0, 0.0, 0.0) / 100.0
#define S3 vec3(99.0, 19.0, 5.0) / 100.0
#define S2 vec3(100.0, 93.0, 49.0) / 100.0
#define S1 vec3(100.0, 45.0, 25.0) / 100.0
#define NSCALE 850.0

uniform sampler2D u_texture;
uniform sampler2D u_noise;

uniform vec2 u_campos;
uniform vec2 u_resolution;
uniform float u_time;

varying vec2 v_texCoords;

void main(){
    vec2 c = v_texCoords.xy;
    vec2 coords = vec2(c.x * u_resolution.x + u_campos.x, c.y * u_resolution.y + u_campos.y);

    float btime = u_time / 10000.0;
    float wave = (sin(coords.x * 0.1 + coords.y * 0.06) - 0.1 * sin(0.05 * coords.x) - 0.15 * sin(0.03 * coords.y)) / 50.0;
    float noise = wave + (texture2D(u_noise, (coords) / NSCALE + vec2(btime) * vec2(-0.2, 0.8)).r + texture2D(u_noise, (coords) / NSCALE + vec2(btime * 1.1) * vec2(0.8, -1.0)).r) / 2.0;
    vec4 tex = texture2D(u_texture, c);
    vec4 color = vec4(0.0, 0.0, 0.0, tex.a);

    //noise = clamp(noise, 0.0, 1.0) - 0.6 + color.a * 0.5;
    float elevation = (0.5 - abs(0.5 - noise)) + wave;
    if(elevation > 0.495){
        color.rgb = S2;
    }else if (elevation > 0.47){
        color.rgb = S1; //orange
    }else if (elevation > 0.45){
        color.rgb = S3; //red
    }else{
        float nt = 0.85 * (1.0 - abs(0.5 - noise));
        color.rgb = S4 * nt * nt * nt * nt;
        color.rgb += tex.rgb * 0.3;
        //color.a = clamp(color.a * 5.0, 0.0, 1.0);
    }

    gl_FragColor = color;
}