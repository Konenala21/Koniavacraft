#version 150

uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  uPlanetDir;
uniform float uPlanetDist;
uniform vec3  uDirToStar;
uniform vec3  uPlanetColor;   // 恆星色溫
uniform vec3  uAtmoColor;     // 日冕色
uniform float uAtmoDensity;   // 日冕強度
uniform float uAtmoHeight;    // 日冕延伸比例
uniform float uAngularRadius;
uniform float iTime;
uniform sampler2D uSurface;
uniform int   uHasTexture;

in  vec2 texCoord;
out vec4 fragColor;

float hash(vec3 p){
    p=fract(p*vec3(443.897,441.423,437.195));
    p+=dot(p,p.yzx+19.19);
    return fract((p.x+p.y)*p.z);
}
float vnoise(vec3 p){
    vec3 i=floor(p);vec3 f=fract(p);f=f*f*(3.0-2.0*f);
    return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),
                   mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),
               mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),
                   mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z)*2.0-1.0;
}

vec2 sphHit(vec3 o,vec3 d,float r){
    float b=dot(o,d),c=dot(o,o)-r*r,disc=b*b-c;
    if(disc<0.0)return vec2(1e9,-1e9);
    float s=sqrt(disc);return vec2(-b-s,-b+s);
}
mat3 buildFrame(vec3 z){
    vec3 up=abs(z.y)<0.9?vec3(0,1,0):vec3(1,0,0);
    vec3 x=normalize(cross(up,z));return transpose(mat3(x,cross(z,x),z));
}

void main(){
    vec2 ndc=texCoord*2.0-1.0;
    vec4 vd=InvProjMat*vec4(ndc,1.0,1.0);vd.xyz/=vd.w;
    vec3 dir=normalize((InvViewMat*vec4(normalize(vd.xyz),0.0)).xyz);

    float cosA=dot(dir,uPlanetDir);
    float sinA=sqrt(max(1.0-uAngularRadius*uAngularRadius,0.0));
    // 日冕延伸到 3x 半徑
    float coronaFac = 3.5;
    float sinCorona = min(sinA * coronaFac, 0.999);
    float cosCorona = sqrt(1.0 - sinCorona*sinCorona);
    if(cosA < cosCorona - 0.002){fragColor=vec4(0.0);return;}

    mat3  L  =buildFrame(uPlanetDir);
    vec3  lD =normalize(L*dir);
    vec3  cam=vec3(0.0,0.0,-uPlanetDist);
    float R  =uPlanetDist*sinA;
    if(R<0.1){fragColor=vec4(0.0);return;}

    vec2 hit=sphHit(cam,lD,R);
    bool onSurface=hit.x>0.0&&hit.x<hit.y;

    if(onSurface){
        vec3 p=cam+lD*hit.x;
        vec3 n=normalize(p);
        vec3 wUp2=abs(uPlanetDir.y)<0.9?vec3(0,1,0):vec3(1,0,0);
        vec3 xW2=normalize(cross(wUp2,uPlanetDir));
        vec3 yW2=cross(uPlanetDir,xW2);
        vec3 wn=n.x*xW2+n.y*yW2+n.z*uPlanetDir;
        float angle=iTime*0.004;
        float cs=cos(angle),sn=sin(angle);
        vec3 rn=vec3(cs*wn.x+sn*wn.z,wn.y,-sn*wn.x+cs*wn.z);
        float nDotV=max(dot(n,-lD),0.0);
        float limb=0.7+0.3*nDotV;

        vec3 col;
        vec2 sunUv=vec2(atan(rn.z,rn.x)/6.28318+0.5, acos(clamp(rn.y,-1.0,1.0))/3.14159);
        vec3 sunTex=texture(uSurface,sunUv).rgb;
        if(dot(sunTex,vec3(1.0))>0.001){
            col=sunTex*limb*1.3;
            col=mix(col,vec3(1.5,1.4,1.1),pow(nDotV,3.0)*0.5);
        } else {
            float plasma=vnoise(rn*3.0)*0.5+vnoise(rn*7.0+vec3(1.3,2.7,0.9))*0.25;
            col=uPlanetColor*(1.0+plasma*0.2)*limb;
            col=mix(col,vec3(1.4,1.3,1.1),pow(nDotV,3.0)*0.4);
        }

        float chord=hit.y-hit.x;
        float edge=smoothstep(0.0,R*0.02,chord);
        fragColor=vec4(col*edge,edge);

    } else {
        // 日冕：距離衰減
        float sinRay=sqrt(max(1.0-cosA*cosA,0.0));
        float normDist=sinRay/sinA;   // 1.0=表面，coronaFac=日冕外緣
        if(normDist<1.0){fragColor=vec4(0.0);return;}

        float falloff=exp(-(normDist-1.0)*4.0/coronaFac)*uAtmoDensity;
        falloff=clamp(falloff,0.0,1.0);
        if(falloff<0.001){fragColor=vec4(0.0);return;}

        // 日冕顏色偏橙紅
        vec3 coronaCol=mix(uAtmoColor,uPlanetColor,0.3)*falloff;
        fragColor=vec4(coronaCol,0.0);  // alpha=0：純加法混合
    }
}
