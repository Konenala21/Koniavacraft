#version 150

uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  uPlanetDir;
uniform float uPlanetDist;
uniform float uAngularRadius;
uniform vec3  uDirToStar;
uniform vec3  uColorLight;
uniform vec3  uColorDark;
uniform vec3  uHeatColor;
uniform float uHeatAmount;
uniform float uAlpha;
uniform vec3  uOccluderDir;
uniform float uOccluderCos;
uniform sampler2D uSurface;
uniform int   uHasTexture;

in  vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5);}
float noise(vec2 p){
    vec2 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
    return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),
               mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);
}
float fbm(vec2 p){return noise(p)*0.6+noise(p*2.1)*0.4;}

vec2 sphHit(vec3 o,vec3 d,float r){
    float b=dot(o,d),c=dot(o,o)-r*r,disc=b*b-c;
    if(disc<0.0)return vec2(1e9,-1e9);
    float s=sqrt(disc);return vec2(-b-s,-b+s);
}
mat3 buildFrame(vec3 z){
    vec3 up=abs(z.y)<0.9?vec3(0,1,0):vec3(1,0,0);
    vec3 x=normalize(cross(up,z));return transpose(mat3(x,cross(z,x),z));
}

const float PI=3.14159265;

void main(){
    vec2 ndc=texCoord*2.0-1.0;
    vec4 vd=InvProjMat*vec4(ndc,1.0,1.0);vd.xyz/=vd.w;
    vec3 dir=normalize((InvViewMat*vec4(normalize(vd.xyz),0.0)).xyz);

    if(uOccluderCos<1.0&&dot(dir,uOccluderDir)>uOccluderCos){fragColor=vec4(0.0);return;}
    float cosA=dot(dir,uPlanetDir);
    if(cosA<uAngularRadius-0.005){fragColor=vec4(0.0);return;}

    mat3  L   =buildFrame(uPlanetDir);
    vec3  lDir=normalize(L*dir);
    vec3  lSun=normalize(L*uDirToStar);
    vec3  cam =vec3(0.0,0.0,-uPlanetDist);

    float sinA=sqrt(max(1.0-uAngularRadius*uAngularRadius,0.0));
    float R   =uPlanetDist*sinA;
    if(R<0.1){fragColor=vec4(0.0);return;}

    vec2 hit=sphHit(cam,lDir,R);
    if(hit.x>0.0&&hit.x<hit.y){
        vec3  p =cam+lDir*hit.x;
        vec3  n =normalize(p);
        float li=max(dot(n,lSun),0.0)*0.70+0.30;

        vec3 wUp3=abs(uPlanetDir.y)<0.9?vec3(0,1,0):vec3(1,0,0);
        vec3 xW3=normalize(cross(wUp3,uPlanetDir));
        vec3 yW3=cross(uPlanetDir,xW3);
        vec3 wn3=n.x*xW3+n.y*yW3+n.z*uPlanetDir;

        vec2 uv3=vec2(atan(wn3.z,wn3.x)/(2.0*PI)+0.5, acos(clamp(wn3.y,-1.0,1.0))/PI);
        vec4 tex3=texture(uSurface,uv3);
        vec3 col;
        if(tex3.a>0.5){
            col=tex3.rgb*li;
        } else {
            vec2 uv=vec2(atan(n.x,n.z)/(2.0*PI)+0.5,
                         clamp(acos(clamp(n.y,-1.0,1.0))/PI,0.0,1.0));
            float c=fbm(uv*4.0);
            col=li*mix(uColorLight,uColorDark,smoothstep(0.3,0.7,c));
            col+=c*uHeatAmount*uHeatColor;
        }
        col=pow(max(col,0.0),vec3(1.0/2.2));
        float alpha=smoothstep(uAngularRadius-0.0003,uAngularRadius,cosA)*uAlpha;
        fragColor=vec4(col*alpha,alpha);
    } else {
        fragColor=vec4(0.0);
    }
}
