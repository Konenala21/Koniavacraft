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
uniform sampler2D uNormalTex;  // 地形法線圖（切線空間，起伏光照）
uniform int       uHasNormal;
uniform sampler2D uSpecTex;    // 海洋 specular 遮罩（海亮陸暗，陽光鏡面反光）
uniform int       uHasSpec;
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
    // Mie 前向散射：背光薄環。係數降低 + 收緊指數，只在邊緣形成細亮環不爆白
    float mie = pow(max(dot(lD, lSun), 0.0), 8.0) * 1.6;
    float illumination = clamp(max(dayFac, mie), 0.0, 1.2);
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
        vec2 uv = vec2(atan(rn.z, rn.x) / 6.28318 + 0.5, acos(clamp(rn.y, -1.0, 1.0)) / 3.14159);
        vec3 texSample = texture(uSurface, uv).rgb;
        // 亮度 > 0.001 = 有有效貼圖（null texture 回傳純黑 = 0）
        if (dot(texSample, vec3(1.0)) > 0.001) {
            surfCol = texSample;
            // 雲層疊加（獨立旋轉 UV）
            vec2 uvCloud = vec2(atan(rnC.z, rnC.x) / 6.28318 + 0.5, acos(clamp(rnC.y, -1.0, 1.0)) / 3.14159);
            vec3 cloudSample = texture(uSurface2, uvCloud).rgb;
            if (dot(cloudSample, vec3(1.0)) > 0.001) {
                float cloudCover = clamp(dot(cloudSample, vec3(0.33)), 0.0, 1.0);
                // 立體感:雲貼圖亮度當高度場，鄰域差分擾動法線，受 world 太陽方向(uDirToStar)照。
                // 用「相對平面球法線多朝光多少」當調制(不是絕對光照)→ 不影響整體日夜，只給雲蓬鬆體積。
                float h0 = cloudCover;
                float hE = clamp(dot(texture(uSurface2, uvCloud + vec2(0.004, 0.0)).rgb, vec3(0.33)), 0.0, 1.0);
                float hN = clamp(dot(texture(uSurface2, uvCloud + vec2(0.0, 0.004)).rgb, vec3(0.33)), 0.0, 1.0);
                vec3  cE = normalize(cross(vec3(0.0, 1.0, 0.0), rnC) + vec3(1e-5));
                vec3  cN = cross(rnC, cE);
                vec3  cNormal = normalize(rnC - ((hE - h0) * cE + (hN - h0) * cN) * 6.0);
                float bump = dot(cNormal, uDirToStar) - dot(rnC, uDirToStar); // 朝光的雲坡 +，背光 -
                vec3  litCloud = cloudSample * clamp(1.0 + bump * 2.2, 0.35, 1.8);
                surfCol = mix(surfCol, litCloud, cloudCover * 0.85);
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

        // ── 地形法線圖：擾動地表法線做起伏光照（山脈/海岸線依太陽角度有明暗陰影）──
        // 切線框由 n 建(東=cross(up,n)、北=cross(n,東))，法線圖 R/G 是東/北坡度。自轉慢，與 UV(rn)的微小偏差看不出。
        vec3 nLit = n;
        if (uHasNormal == 1) {
            vec3 nmTex = texture(uNormalTex, uv).rgb;
            if (dot(nmTex, vec3(1.0)) > 0.001) {
                vec3 east  = normalize(cross(vec3(0.0, 1.0, 0.0), n) + vec3(1e-5));
                vec3 north = cross(n, east);
                vec3 nm = nmTex * 2.0 - 1.0;
                nLit = normalize(n + (nm.x * east + nm.y * north) * 0.55); // 0.55 = 起伏強度
            }
        }
        // ── 光照：Gotanda simplified Oren-Nayar（無 acos/tan）──
        float NdotL = max(dot(nLit, lSun), 0.0);
        float nDotV = max(dot(nLit, -lD), 0.0);
        float rough = 0.85;
        float r2    = rough * rough;
        float A     = 1.0 - 0.5 * r2 / (r2 + 0.33);
        float B     = 0.45 * r2 / (r2 + 0.09);
        float LdotV = dot(lSun, -lD);
        float s     = LdotV - NdotL * nDotV;
        float t     = s <= 0.0 ? 1.0 : max(NdotL, nDotV + 0.001);
        float sh    = NdotL * (A + B * s / t) * 0.85 + 0.18;

        // 邊緣暗化
        float limb = pow(nDotV, 0.35);
        sh *= limb * 0.70 + 0.30;

        // 邊緣大氣暈：前向散射增強，朝太陽那側邊緣形成細亮環（背光更真實）
        float fwdScatter = pow(max(dot(lD, lSun), 0.0), 4.0);
        float edgeAtmo = pow(1.0 - nDotV, 2.4) * uAtmoDensity * (0.28 + 0.55 * fwdScatter);
        edgeAtmo = clamp(edgeAtmo, 0.0, 0.85);

        float chord    = hit.y - hit.x;
        float softEdge = smoothstep(0.0, R * 0.04, chord);

        vec3  col = surfCol * sh + uAtmoColor * edgeAtmo;

        // ── 海洋鏡面反光：specular 圖標出海洋(R 亮)，陽光在海面反射朝鏡頭時形成亮點(Blinn-Phong)──
        if (uHasSpec == 1 && NdotL > 0.0) {
            float specMask = texture(uSpecTex, uv).r;
            if (specMask > 0.01) {
                vec3  halfV = normalize(lSun - lD);                 // -lD = 朝鏡頭；半向量
                float glint = pow(max(dot(nLit, halfV), 0.0), 64.0);
                col += vec3(1.0, 0.95, 0.82) * glint * specMask * NdotL * 1.1; // 暖白陽光反光，限受光面
            }
        }

        // ── 夜景城市燈光：自發光，純加法疊在暗面（不受光照壓暗）──────
        vec3 nightSample = texture(uNightTex, uv).rgb;
        if (dot(nightSample, vec3(1.0)) > 0.001) {
            float nightVis = 1.0 - smoothstep(-0.20, 0.05, dot(n, lSun)); // 1=暗面
            col += nightSample * 3.5 * nightVis;
        }

        softEdge *= uAlpha;
        fragColor = vec4(col * softEdge, softEdge);

    } else {
        vec3 atmo = computeAtmo(cam, lD, lSun, R, false);
        if(length(atmo) < 0.001){fragColor=vec4(0.0);return;}
        fragColor = vec4(atmo * uAlpha, 0.0);
    }
}
