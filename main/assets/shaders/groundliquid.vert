uniform mat4 u_projTrans;
uniform int u_sprites_width;

attribute vec4 a_position;
attribute vec2 a_texCoord0;
attribute vec4 a_color;
attribute vec4 a_mix_color;
attribute float a_fluidType;

varying vec4 v_shallowcolor;
varying vec4 v_deepcolor;
varying vec2 v_texCoords;
varying float v_fluidType;
varying float v_tx;
varying float v_ty;
varying float v_tx2;
varying float v_ty2;
varying float v_ts;

const float epsilon = 0.002;

uniform vec2 u_viewportInverse;

float round(float a){
    return floor(a + 0.5);
}

void main(){
    gl_Position = u_projTrans * a_position;
    v_texCoords = a_texCoord0;
    v_shallowcolor = a_color;
    v_deepcolor = a_mix_color;
    v_fluidType = a_fluidType;

    v_ts = 1.0/float(u_sprites_width);
    v_ty = float(int(a_fluidType*2.0)/u_sprites_width)*v_ts+epsilon;
    v_tx = mod(a_fluidType*2.0,float(u_sprites_width))*v_ts+epsilon;
    v_ty2 = float(floor(round(a_fluidType*2.0+1.0)/float(u_sprites_width)))*v_ts+epsilon;
    v_tx2 = mod(a_fluidType*2.0+1.0,float(u_sprites_width))*v_ts+epsilon;
    v_ts-=epsilon*2.0;

}
