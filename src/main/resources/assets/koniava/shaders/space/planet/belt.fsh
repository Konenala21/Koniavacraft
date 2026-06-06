#version 150
// 小行星帶 / 柯伊伯帶：射線與黃道面相交 + 連續 FBM 密度場（體積式，遠近都可見）
// 參考業界做法：渲染累積密度而非離散岩石（個別岩石遠處 sub-pixel 必看不到）

uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  uPlanetDir;   // 恆星方向（帶中心 = 恆星）
uniform float uPlanetDist;  // 玩家到恆星距離
uniform vec3  uDirToStar;
uniform float uRingInner;
uniform float uRingOuter;
uniform float uRingTilt;    // 厚度（借用 tilt slot）
uniform float uAtmoDensity; // 密度（保留）
uniform vec3  uPlanetColor;
uniform float uAlpha;
uniform float iTime;

in  vec2 texCoord;
out vec4 fragColor;

float hash13(vec3 p){
    p=fract(p*vec3(443.8975,397.2973,491.1871));
    p+=dot(p.zxy,p.yxz+19.19);
    return fract(p.x*p.y*p.z);
}
float vnoise(vec3 p){
    vec3 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
    return mix(mix(mix(hash13(i),hash13(i+vec3(1,0,0)),f.x),
                   mix(hash13(i+vec3(0,1,0)),hash13(i+vec3(1,1,0)),f.x),f.y),
               mix(mix(hash13(i+vec3(0,0,1)),hash13(i+vec3(1,0,1)),f.x),
                   mix(hash13(i+vec3(0,1,1)),hash13(i+vec3(1,1,1)),f.x),f.y),f.z);
}
float fbm(vec3 p){
    float v=0.0,a=0.5;
    for(int i=0;i<4;i++){ v+=a*vnoise(p); p=p*2.1+vec3(1.7,9.2,3.4); a*=0.5; }
    return v;
}

void main(){
    vec4 result=vec4(0.0);

    vec2 ndc=texCoord*2.0-1.0;
    vec4 vd=InvProjMat*vec4(ndc,1.0,1.0);vd.xyz/=vd.w;
    vec3 dir=normalize((InvViewMat*vec4(normalize(vd.xyz),0.0)).xyz);

    vec3 sunPos=uPlanetDir*uPlanetDist;
    float denom=dir.y;

    if(abs(denom)>1e-5){
        float tp=sunPos.y/denom;          // 交黃道面 Y=sunPos.y
        if(tp>0.0){
            vec3 hit=dir*tp;
            vec2 rel=hit.xz-sunPos.xz;
            float radius=length(rel);

            if(radius>uRingInner&&radius<uRingOuter){
                float bw=uRingOuter-uRingInner;
                // 內外緣柔和漸弱
                float edgeFade=smoothstep(uRingInner,uRingInner+bw*0.20,radius)
                              *smoothstep(uRingOuter,uRingOuter-bw*0.20,radius);

                // 緩慢公轉
                float ang=iTime*0.0008;
                float cs=cos(ang),sn=sin(ang);
                vec3 hr=vec3(cs*hit.x+sn*hit.z, hit.y, -sn*hit.x+cs*hit.z);

                // ── 連續密度場（多尺度 FBM）：稀疏不均，不是完美亮線 ──
                float d1=fbm(hr*0.004);                 // 大團塊
                float d2=fbm(hr*0.016+vec3(5.1,1.3,2.7)); // 中紋理
                float d3=fbm(hr*0.05 +vec3(9.0));         // 細顆粒感
                float density=d1*0.55+d2*0.30+d3*0.15;
                density=clamp(density,0.0,1.0);
                // 大尺度空缺遮罩：整段整段沒有小行星（真實帶很不均勻）
                float mask=fbm(hr*0.0016+vec3(20.0,3.0,11.0));
                mask=smoothstep(0.42,0.66,mask);
                density*=mask;
                // 高對比：拉出明顯空隙與密集團（不是均勻一條線）
                density=pow(density,2.6);

                // 幾乎無基礎霧霾，靠密度本身（稀疏感）
                float bright=density*1.5*edgeFade;

                // 朝陽側略亮
                float lit=clamp(dot(normalize(hit-sunPos),uDirToStar)*0.25+0.75,0.4,1.0);
                vec3 col=uPlanetColor*lit;
                // 高密度團塊偏亮帶白點
                col+=vec3(1.0)*pow(density,3.0)*0.5;

                // 純加法（alpha=0）：只加光不蓋星空
                result=vec4(col*bright*uAlpha, 0.0);
            }
        }
    }

    fragColor=result;
}
