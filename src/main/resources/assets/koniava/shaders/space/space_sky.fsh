#version 430 core

uniform float iTime;
uniform vec2  iResolution;
uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform int   uMoonSky;   // 1 = 月球（無大氣：星星更亮不閃爍、無流星）
uniform float uAtmosphere; // 1 = 全太空（不透明），<1 = 主世界爬升漸變（星空淡入疊在原版天空上）

in  vec2 texCoord;
out vec4 fragColor;

float hash31(vec3 p){
    p=fract(p*vec3(443.8975,397.2973,491.1871));
    p+=dot(p.zxy,p.yxz+19.19);
    return fract(p.x*p.y*p.z);
}

float starGrid(vec3 d,float sc,float thr,float sz){
    vec3  g=floor(d*sc);
    float h=hash31(g);
    if(h<thr)return 0.0;
    vec3 off=vec3(hash31(g+vec3(1,0,0)),hash31(g+vec3(0,1,0)),hash31(g+vec3(0,0,1)));
    off=off*0.6+0.2;
    float ca=dot(normalize(d),normalize(g+off));
    return smoothstep(1.0-sz,1.0,ca)*(h-thr)/(1.0-thr);
}

void main(){
    vec2 ndc=texCoord*2.0-1.0;
    vec4 vd=InvProjMat*vec4(ndc,1.0,1.0);vd.xyz/=vd.w;
    vec3 dir=normalize((InvViewMat*vec4(normalize(vd.xyz),0.0)).xyz);

    // ── 星星（兩層）──────────────────────────────────────────────────────
    float dim=starGrid(dir,     80.0,0.997, 0.0008);
    float big=starGrid(dir.yzx, 45.0,0.9988,0.002);
    float bright=dim*0.7+big*1.4;

    vec3 bigCell=floor(dir.yzx*45.0);
    if(uMoonSky==1){
        // 月球無大氣：星星不閃爍，整體更亮更清澈
        bright *= 1.4;
    } else {
        // 太空：大氣外仍給亮星輕微閃爍
        float twinkle=0.8+0.2*sin(iTime*2.1+hash31(bigCell)*6.28);
        bright=bright-big*1.4+big*1.4*twinkle;
    }

    // ── 恆星色溫（赫羅圖分布）────────────────────────────────────────────
    // M=紅矮星 K=橙星 G=類太陽黃 F=白 A=藍白 B/O=藍
    float temp=hash31(bigCell+vec3(5.5));
    vec3 tint;
    if     (temp<0.12) tint=vec3(1.0, 0.62,0.35); // M 型
    else if(temp<0.28) tint=vec3(1.0, 0.80,0.50); // K 型
    else if(temp<0.52) tint=vec3(1.0, 0.97,0.80); // G 型（類太陽）
    else if(temp<0.72) tint=vec3(0.98,0.98,1.0);  // F 型
    else if(temp<0.88) tint=vec3(0.88,0.92,1.0);  // A 型
    else               tint=vec3(0.72,0.80,1.0);  // B/O 型

    // ── 銀河帶（更明顯）─────────────────────────────────────────────────
    float mwLat=dir.y;
    float mwBase=exp(-mwLat*mwLat/0.05)*0.05;
    // 銀心方向偏移（大致朝向人馬座方向）
    float mwLon=atan(dir.z,dir.x);
    float mwCore=exp(-((mwLon+0.8)*(mwLon+0.8))/0.6);
    float mwBright=mwBase*(0.5+0.5*mwCore);
    // 中心偏暖（老恆星密集），外圍偏藍（年輕熱星）
    vec3 mwCol=mix(vec3(0.14,0.17,0.30),vec3(0.30,0.22,0.16),mwCore)*mwBright;

    // ── 流星（每 9 秒一顆）：月球無大氣無流星，只太空有 ──────────────────
    float ssPeriod=9.0;
    float ssT=mod(iTime,ssPeriod);
    float ssDur=0.7;
    if(uMoonSky==0 && ssT<ssDur){
        float prog=ssT/ssDur;
        float seed=floor(iTime/ssPeriod);
        float az=hash31(vec3(seed,1.3,0.0))*5.28+0.5;
        float el=hash31(vec3(seed,0.0,2.1))*0.45+0.25;

        vec3 ssStart=normalize(vec3(cos(az)*cos(el),sin(el),sin(az)*cos(el)));
        vec3 ssEnd  =normalize(ssStart+vec3(-sin(az),-.18,cos(az))*0.28);

        // 軌跡線段：tip 在 prog 處，tail 在 max(0, prog-0.35) 處
        float head=prog;
        float tail=max(0.0,prog-0.35);
        vec3  lv=ssEnd-ssStart;
        float ll=length(lv); vec3 ld=lv/ll;
        float tp=dot(dir-ssStart,ld)/ll;
        tp=clamp(tp,tail,head);
        vec3 np=ssStart+ld*ll*tp;
        float d=length(dir-normalize(np));
        float fade=(tp-tail)/max(head-tail,0.001); // 0=尾部 1=前端
        float ss=exp(-d*900.0)*(0.2+0.8*fade)*(1.0-prog*0.6);
        if(ss>0.001){
            bright+=ss*5.0;
            tint=mix(tint,vec3(1.0,0.97,0.88),min(ss*2.0,1.0));
        }
    }

    fragColor=vec4(mwCol+tint*bright,uAtmosphere);
}
