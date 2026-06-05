#version 150
// 小行星帶 / 柯伊伯帶：射線與黃道面相交 + cellular 散布岩石點

uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  uPlanetDir;   // 恆星方向（帶中心 = 恆星）
uniform float uPlanetDist;  // 玩家到恆星距離
uniform vec3  uDirToStar;   // 恆星光方向（同 uPlanetDir，用於明暗）
uniform float uRingInner;   // 內緣半徑（方塊）
uniform float uRingOuter;   // 外緣半徑
uniform float uRingTilt;    // 厚度（黃道面 Y 散布，借用 tilt slot）
uniform float uAtmoDensity; // 密度閾值（0.9~0.99）
uniform vec3  uPlanetColor; // 岩石基底色
uniform float uAlpha;
uniform float iTime;

in  vec2 texCoord;
out vec4 fragColor;

float hash13(vec3 p){
    p=fract(p*vec3(443.8975,397.2973,491.1871));
    p+=dot(p.zxy,p.yxz+19.19);
    return fract(p.x*p.y*p.z);
}

// cellular 散布：回傳該點的岩石亮度
float scatter(vec3 p, float cellSize, float thr, float sizeFac){
    vec3 g=floor(p/cellSize);
    float h=hash13(g);
    if(h<thr) return 0.0;
    vec3 off=vec3(hash13(g+vec3(1.3,0,0)),hash13(g+vec3(0,2.7,0)),hash13(g+vec3(0,0,5.1)));
    vec3 center=(g+off)*cellSize;
    float d=length(p-center);
    float norm=(h-thr)/(1.0-thr);          // 0..1，越稀疏越大顆
    float size=cellSize*sizeFac*(0.3+0.7*norm);
    return smoothstep(size,0.0,d)*norm;
}

void main(){
    vec4 result=vec4(0.0);

    vec2 ndc=texCoord*2.0-1.0;
    vec4 vd=InvProjMat*vec4(ndc,1.0,1.0);vd.xyz/=vd.w;
    vec3 dir=normalize((InvViewMat*vec4(normalize(vd.xyz),0.0)).xyz);

    vec3 sunPos=uPlanetDir*uPlanetDist;  // 玩家相對的恆星世界座標
    float denom=dir.y;

    if(abs(denom)>1e-5){
        float tp=(sunPos.y)/denom;       // 玩家在原點，交黃道面 Y=sunPos.y
        if(tp>0.0){
            vec3 hit=dir*tp;
            vec2 rel=hit.xz-sunPos.xz;
            float radius=length(rel);

            if(radius>uRingInner&&radius<uRingOuter){
                float bw=uRingOuter-uRingInner;
                // 內外緣密度漸弱
                float edgeFade=smoothstep(uRingInner,uRingInner+bw*0.18,radius)
                              *smoothstep(uRingOuter,uRingOuter-bw*0.18,radius);
                // 徑向不均勻（模擬柯克伍德空隙/密度團塊）
                float clump=0.6+0.4*sin(radius*0.0025+hash13(floor(rel*0.003))*6.28);

                // 微緩公轉漂移（整條帶緩慢轉）
                float ang=iTime*0.0008;
                float cs=cos(ang),sn=sin(ang);
                vec3 hr=vec3(cs*hit.x+sn*hit.z, hit.y, -sn*hit.x+cs*hit.z);

                // 兩層：細塵（密、暗、小）+ 稀疏大岩（亮、大）
                float dust=scatter(hr,55.0, uAtmoDensity,        0.05)*0.45;
                float rock=scatter(hr,180.0, uAtmoDensity+0.025, 0.07)*1.0;

                float bright=(dust+rock)*edgeFade*clump;

                // 距離衰減（遠的岩石更暗）
                bright*=clamp(6000.0/max(tp,1.0),0.15,1.0);

                if(bright>0.002){
                    // 受恆星照亮（朝陽側略亮）
                    float lit=clamp(dot(normalize(hit-sunPos),uDirToStar)*0.3+0.7,0.4,1.0);
                    vec3 col=uPlanetColor*lit;
                    // 大岩偏亮帶點高光
                    col+=vec3(1.0)*rock*0.3;
                    float a=clamp(bright,0.0,1.0)*uAlpha;
                    result=vec4(col*a,a);
                }
            }
        }
    }

    fragColor=result;
}
