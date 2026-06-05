#version 150

uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  uPlanetDir;
uniform float uPlanetDist;
uniform vec3  uDirToStar;
uniform vec3  uPlanetColor;
uniform vec3  uAtmoColor;
uniform float uAtmoDensity;
uniform float uAtmoHeight;
uniform float uAngularRadius;
uniform float iTime;
uniform float uRotSpeed;
uniform float uCloudSpeed;
uniform float uAlpha;
uniform vec3  uOccluderDir; // 父行星方向（歸一）
uniform float uOccluderCos; // 父行星視角半徑餘弦；1.001=無遮擋       // 淡入係數（0=不可見 1=完全顯示）
uniform sampler2D uSurface;    // 白天/地表貼圖
uniform sampler2D uSurface2;   // 雲層/大氣疊加貼圖
uniform sampler2D uNightTex;   // 夜景貼圖（城市燈光）
uniform int   uHasTexture;
uniform int   uHasTexture2;
uniform int   uHasNight;

in  vec2 texCoord;
out vec4 fragColor;

// ── 噪聲 ──────────────────────────────────────────────────────────────────
float hash(vec3 p){
    p = fract(p * vec3(443.897, 441.423, 437.195));
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}
float vnoise(vec3 p){
    vec3 i=floor(p); vec3 f=fract(p);
    f=f*f*(3.0-2.0*f);
    return mix(
        mix(mix(hash(i),           hash(i+vec3(1,0,0)),f.x),
            mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),
        mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),
            mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z
    )*2.0-1.0;
}
float fbm(vec3 p){
    float v=0.0,a=0.5;
    for(int i=0;i<3;i++){
        v+=a*vnoise(p);
        p=p*2.1+vec3(1.7,9.2,3.4);
        a*=0.5;
    }
    return v;
}

// ── 光學 ──────────────────────────────────────────────────────────────────
vec2 sphHit(vec3 o,vec3 d,float r){
    float b=dot(o,d),c=dot(o,o)-r*r,disc=b*b-c;
    if(disc<0.0)return vec2(1e9,-1e9);
    float s=sqrt(disc);return vec2(-b-s,-b+s);
}
mat3 buildFrame(vec3 z){
    vec3 up=abs(z.y)<0.9?vec3(0,1,0):vec3(1,0,0);
    vec3 x=normalize(cross(up,z));return transpose(mat3(x,cross(z,x),z));
}
float chordThrough(float d_sq,float R_inner,float R_outer){
    float c_outer=sqrt(max(R_outer*R_outer-d_sq,0.0));
    float c_inner=sqrt(max(R_inner*R_inner-d_sq,0.0));
    return max(c_outer-c_inner,0.0)*2.0;
}
vec3 computeAtmo(vec3 cam,vec3 lD,vec3 lSun,float R,bool onSurface){
    float R_atmo=R*(1.0+uAtmoHeight);
    float t=-dot(cam,lD); vec3 ep=cam+lD*t; float d_sq=dot(ep,ep);
    if(d_sq>R_atmo*R_atmo)return vec3(0.0);
    float inner=onSurface?R:0.0;
    float chord    = chordThrough(d_sq,inner,R_atmo);
    float optDepth = chord * uAtmoDensity / R;
    float alpha    = 1.0 - exp(-optDepth);
    float dayFac = clamp(dot(normalize(ep), lSun)*0.7+0.3, 0.0, 1.0);
    float mie = pow(max(dot(lD, lSun), 0.0), 6.0) * 3.0;
    float illumination = max(dayFac, mie);
    float depthFrac = clamp(optDepth / 3.0, 0.0, 1.0);
    vec3 outerColor = uAtmoColor;                        // 外圈：行星定義的大氣色
    vec3 innerColor = uAtmoColor * vec3(1.8, 1.1, 0.6); // 內圈偏橙紅
    vec3 layeredColor = mix(outerColor, innerColor, depthFrac * 0.5);
    return layeredColor * alpha * illumination;
}

void main(){
    vec2 ndc=texCoord*2.0-1.0;
    vec4 vd=InvProjMat*vec4(ndc,1.0,1.0);vd.xyz/=vd.w;
    vec3 dir=normalize((InvViewMat*vec4(normalize(vd.xyz),0.0)).xyz);

    // 父行星遮擋：視線穿過父行星圓盤時直接丟棄（消除衛星跳躍）
    if(uOccluderCos < 1.0 && dot(dir, uOccluderDir) > uOccluderCos){
        fragColor=vec4(0.0); return;
    }

    float cosA=dot(dir,uPlanetDir);
    float sinA   =sqrt(max(1.0-uAngularRadius*uAngularRadius,0.0));
    float sinAtmo=sinA*(1.0+uAtmoHeight);
    // 只有大氣層角度在 90° 以內才做早退；超過（玩家在行星內）任何方向都可見
    if(sinAtmo<1.0){
        float cosAtmo=sqrt(1.0-sinAtmo*sinAtmo);
        if(cosA<cosAtmo-0.001){fragColor=vec4(0.0);return;}
    }

    mat3  L   =buildFrame(uPlanetDir);
    vec3  lD  =normalize(L*dir);
    vec3  lSun=normalize(L*uDirToStar);
    vec3  cam =vec3(0.0,0.0,-uPlanetDist);
    float R   =uPlanetDist*sinA;
    if(R<0.1){fragColor=vec4(0.0);return;}

    // local→world：明確展開（避免 mat3 constructor 在某些驅動有問題）
    vec3 pDir = uPlanetDir;
    vec3 wUp  = abs(pDir.y)<0.9?vec3(0,1,0):vec3(1,0,0);
    vec3 xW   = normalize(cross(wUp, pDir));
    vec3 yW   = cross(pDir, xW);

    vec2  hit      =sphHit(cam,lD,R);
    bool  onSurface=hit.x>0.0&&hit.x<hit.y;

    if(onSurface){
        vec3  p =cam+lD*hit.x;
        vec3  n =normalize(p);

        // world-space 法線：明確展開 local→world（UV 固定在星球表面）
        vec3 wn = n.x * xW + n.y * yW + n.z * pDir;

        // 自轉：繞 world Y 軸旋轉
        float angle  = iTime * uRotSpeed;
        float cs=cos(angle), sn=sin(angle);
        vec3 rn = vec3(cs*wn.x+sn*wn.z, wn.y, -sn*wn.x+cs*wn.z);
        float angleC = iTime * uCloudSpeed;
        float csC=cos(angleC), snC=sin(angleC);
        vec3 rnC = vec3(csC*wn.x+snC*wn.z, wn.y, -snC*wn.x+csC*wn.z);

        vec3 surfCol;
        float dayFac = 1.0;
        vec2 uv = vec2(atan(rn.z, rn.x) / 6.28318 + 0.5, acos(clamp(rn.y, -1.0, 1.0)) / 3.14159);
        // STBImage 強制 RGBA：JPG alpha=1.0，null貼圖(ID=0) alpha=0 → 用 alpha 判斷而非亮度
        vec4 texFull = texture(uSurface, uv);
        if (texFull.a > 0.5) {
            surfCol = texFull.rgb;
            // 夜景混合
            vec4 nightFull = texture(uNightTex, uv);
            if (nightFull.a > 0.5) {
                dayFac = smoothstep(-0.15, 0.15, dot(n, lSun));
                surfCol = mix(nightFull.rgb * 2.5, surfCol, dayFac);
            }
            // 雲層疊加（獨立旋轉 UV）
            vec2 uvCloud = vec2(atan(rnC.z, rnC.x) / 6.28318 + 0.5, acos(clamp(rnC.y, -1.0, 1.0)) / 3.14159);
            vec4 cloudFull = texture(uSurface2, uvCloud);
            if (cloudFull.a > 0.5) {
                float cloudCover = clamp(dot(cloudFull.rgb, vec3(0.33)), 0.0, 1.0);
                surfCol = mix(surfCol, cloudFull.rgb, cloudCover * 0.85);
            }
        } else {
            float coarse = fbm(rn * 2.2);
            float fine   = fbm(rn * 5.5 + vec3(5.1,1.3,2.7));
            float band   = sin(rn.y * 7.0 + coarse * 3.0) * 0.5 + 0.5;
            float detail = coarse * 0.65 + fine * 0.25 + (band - 0.5) * 0.40;
            vec3  dark   = uPlanetColor * 0.55;
            vec3  light  = uPlanetColor * 1.45;
            surfCol = mix(dark, light, clamp(detail * 0.5 + 0.5, 0.0, 1.0));
        }

        // ── 光照：Gotanda simplified Oren-Nayar（無 acos/tan）──
        float NdotL = max(dot(n, lSun), 0.0);
        float nDotV = max(dot(n, -lD), 0.0);
        float rough = 0.85;
        float r2    = rough * rough;
        float A     = 1.0 - 0.5 * r2 / (r2 + 0.33);
        float B     = 0.45 * r2 / (r2 + 0.09);
        float LdotV = dot(lSun, -lD);
        float s     = LdotV - NdotL * nDotV;
        float t     = s <= 0.0 ? 1.0 : max(NdotL, nDotV + 0.001);
        // 夜景側：城市燈光不受陽光影響（dayFac=0 時 sh→1.0）
        float sh    = mix(1.0, NdotL * (A + B * s / t) * 0.85 + 0.18, dayFac);

        // 邊緣暗化
        float limb = pow(nDotV, 0.35);
        sh *= limb * 0.70 + 0.30;

        // 邊緣大氣暈：只用 nDotV（不依賴太陽方向，避免中心亮峰）
        float edgeAtmo = pow(1.0 - nDotV, 2.5) * uAtmoDensity * 0.35;
        edgeAtmo = clamp(edgeAtmo, 0.0, 0.9);

        float chord    = hit.y - hit.x;
        float softEdge = smoothstep(0.0, R * 0.04, chord);

        vec3  col = surfCol * sh + uAtmoColor * edgeAtmo;
        softEdge *= uAlpha;
        fragColor = vec4(col * softEdge, softEdge);

    } else {
        vec3 atmo = computeAtmo(cam, lD, lSun, R, false);
        if(length(atmo) < 0.001){fragColor=vec4(0.0);return;}
        fragColor = vec4(atmo * uAlpha, 0.0);
    }
}
