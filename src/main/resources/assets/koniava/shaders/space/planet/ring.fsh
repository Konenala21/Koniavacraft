#version 150

uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  uPlanetDir;
uniform float uPlanetDist;
uniform vec3  uDirToStar;
uniform float uAngularRadius;
uniform float uRingInner;   // 內緣半徑 / 星球半徑
uniform float uRingOuter;   // 外緣半徑 / 星球半徑
uniform float uRingTilt;    // 自轉軸傾斜（弧度）
uniform float uAlpha;
uniform sampler2D uRingTex;

in  vec2 texCoord;
out vec4 fragColor;

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
    float R   =uPlanetDist*sinA;
    if(R<0.1){fragColor=vec4(0.0);return;}

    // 早退：超出光環外緣範圍（只有 sinOuter < 1 時才能用角度剔除）
    float sinOuter=sinA*uRingOuter;
    if(sinOuter<1.0){
        float cosOuter=sqrt(1.0-sinOuter*sinOuter);
        if(cosA<cosOuter-0.001){fragColor=vec4(0.0);return;}
    }
    // 若 sinOuter >= 1.0（玩家在環系統內），環在任何方向都可見，不剔除

    mat3  L   =buildFrame(uPlanetDir);
    vec3  lD  =normalize(L*dir);
    vec3  lSun=normalize(L*uDirToStar);
    vec3  cam =vec3(0.0,0.0,-uPlanetDist);

    // 光環平面法線：世界空間固定，再轉到 local frame
    // 若直接在 local frame 定義，則隨 uPlanetDir（玩家方向）旋轉 = 跟著玩家
    vec3 worldRingNormal=vec3(0.0, cos(uRingTilt), sin(uRingTilt));
    vec3 ringNormal=normalize(L*worldRingNormal);

    // 射線 - 平面交叉
    float denom=dot(lD,ringNormal);
    if(abs(denom)<0.0005){fragColor=vec4(0.0);return;}  // 更嚴格閾值防 NaN
    float t=-dot(cam,ringNormal)/denom;
    if(t<0.0||t>1e5){fragColor=vec4(0.0);return;}       // 限制 t 範圍防 inf

    vec3 hitPt=cam+lD*t;
    float dist=length(hitPt);
    float innerR=R*uRingInner;
    float outerR=R*uRingOuter;
    if(dist<innerR||dist>outerR||outerR<=innerR){fragColor=vec4(0.0);return;}

    // 採樣前確保 u 在安全範圍（NaN u 座標會崩 AMD 驅動）
    float u=clamp((dist-innerR)/(outerR-innerR), 0.0, 1.0);

    // 若射線先打到星球表面則丟棄（光環在星球後方）
    vec2 sph=sphHit(cam,lD,R);
    if(sph.x>0.0&&sph.x<sph.y&&sph.x<t){fragColor=vec4(0.0);return;}

    // 採樣光環貼圖（U = 從內緣到外緣，已 clamp 防 NaN）
    vec4  ring=texture(uRingTex,vec2(u,0.5));
    if(ring.a<0.01){fragColor=vec4(0.0);return;}

    // 光照
    float sunlit=clamp(dot(normalize(hitPt),lSun)*0.5+0.6, 0.15, 1.0);
    float a=ring.a*uAlpha;
    fragColor=vec4(ring.rgb*sunlit*a, a);
}
