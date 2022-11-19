attribute vec4 a_position;
attribute vec4 a_normal;
attribute vec4 a_color;

uniform mat4 u_proj;
uniform mat4 u_trans;
uniform mat4 u_nor;
uniform vec3 u_lightdir;
uniform vec3 u_camdir;
uniform vec3 u_campos;
uniform vec4 u_ambientColor;
uniform vec4 u_emissionColor;

varying vec4 v_col;

const vec4 diffuse = vec4(0.01);
void main(){
    vec4 specular = vec4(0.0, 0.0, 0.0, 1.0);

    vec4 trnsPos = u_trans * a_position;
    vec3 trnsNormal = normalize(u_nor * a_normal).xyz;

    vec3 lightReflect = normalize(reflect(trnsNormal, u_lightdir));
    vec3 vertexEye = normalize(u_campos - trnsPos.xyz);

    float specularFactor = dot(vertexEye, lightReflect);
    if(specularFactor > 0.0) specular = vec4(vec3(1.0 * pow(specularFactor, 40.0)), 1.0) * (1.0 - a_color.a);

    vec4 norc = (u_ambientColor + specular) * (diffuse + vec4(vec3(clamp((dot(trnsNormal, -u_lightdir) + 1.0) / 2.0, 0.0, 1.0)), 1.0));

    v_col = u_emissionColor + vec4(a_color.rgb, 1.0) * norc;
    gl_Position = u_proj * trnsPos;
}
