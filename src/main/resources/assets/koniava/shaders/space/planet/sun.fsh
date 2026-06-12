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
// 多八度 FBM（persistence 0.7，仿 bpodgursky 程序恆星）
float fbm(vec3 p){
    float v=0.0,a=0.5;
    for(int i=0;i<4;i++){ v+=a*vnoise(p); p=p*2.0+vec3(1.7,9.2,3.4); a*=0.7; }
    return v;
}
// 溫度 → 光球層顏色（冷=深橙黑子，熱=近白亮斑）
vec3 tempColor(float t){
    vec3 c0=vec3(0.35,0.10,0.02); // 黑子本影（最冷）
    vec3 c1=vec3(0.85,0.35,0.08); // 暗胞間隙
    vec3 c2=vec3(1.00,0.70,0.30); // 正常光球 5772K
    vec3 c3=vec3(1.00,0.92,0.65); // 熱胞中心
    vec3 c4=vec3(1.00,0.98,0.90); // 亮斑（最熱）
    if(t<0.25) return mix(c0,c1,t/0.25);
    if(t<0.50) return mix(c1,c2,(t-0.25)/0.25);
    if(t<0.75) return mix(c2,c3,(t-0.50)/0.25);
    return mix(c3,c4,(t-0.75)/0.25);
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

        // ── 混合：sun.jpg 真實貼圖 + 程序沸騰層 ────────────────────────
        vec3  sp = rn;
        float tt = iTime * 0.03; // 沸騰速度

        // 程序三層噪聲（bpodgursky）
        float gran = (fbm(sp*10.0 + vec3(0.0,0.0,tt)) + 1.0) * 0.5;
        float fine = (fbm(sp*22.0 + vec3(tt*1.5,0.0,0.0)) + 1.0) * 0.5;
        float granExpr = mix(gran, fine, 0.35);
        float ssN = fbm(sp*2.4 + vec3(tt*0.15)) * 1.4 - 0.7;
        float latFac = 1.0 - abs(sp.y)*0.6;
        float ss = max(0.0, ssN) * latFac;
        float bsN = fbm(sp*1.4 + vec3(5.1,1.3,tt*0.1)) * 1.3 - 0.8;
        float brightSpot = max(0.0, bsN);
        float total = clamp(granExpr - ss*1.4 + brightSpot*0.5, 0.0, 1.0);

        vec2 sunUv=vec2(atan(rn.z,rn.x)/6.28318+0.5, acos(clamp(rn.y,-1.0,1.0))/3.14159);
        vec3 sunTex=texture(uSurface,sunUv).rgb;

        vec3 col;
        if(dot(sunTex,vec3(1.0))>0.001){
            // 有貼圖：真實照片當底色，程序層做動態調變
            col = sunTex * 1.3;
            // 沸騰亮度起伏（程序 total 圍繞 0.5 調變貼圖明暗）
            col *= 0.78 + total*0.5;
            // 程序黑子疊上去（暗化）
            col *= (1.0 - ss*0.55);
            // 溫度色偏：熱的地方往白，冷的往橙，輕微混入
            col = mix(col, tempColor(total)*1.3, 0.30);
        } else {
            // 無貼圖：純程序
            col = tempColor(total) * mix(vec3(1.0), uPlanetColor*1.3, 0.25);
        }
        col *= limb;
        // 核心過曝白熱（讓它像恆星不像球）
        col = mix(col, vec3(1.7,1.6,1.3), pow(nDotV,3.5)*0.5);

        float chord=hit.y-hit.x;
        float edge=smoothstep(0.0,R*0.02,chord);
        fragColor=vec4(col*edge,edge);

    } else {
        // 日冕：距離衰減
        float sinRay=sqrt(max(1.0-cosA*cosA,0.0));
        float normDist=sinRay/sinA;   // 1.0=表面，coronaFac=日冕外緣
        if(normDist<1.0){fragColor=vec4(0.0);return;}

        float falloff=exp(-(normDist-1.0)*4.0/coronaFac)*uAtmoDensity;
        // 外緣平滑淡出,不要硬切。遠處用 normDist 柔化外圈;近處(日冕撐到接近 90° 上限)用 sinRay 柔化,
        // 否則近看時 cap(0.999)那條硬邊會像 2D 貼片橫過螢幕。
        falloff *= smoothstep(coronaFac, coronaFac*0.6, normDist) * smoothstep(0.999, 0.96, sinRay);
        falloff=clamp(falloff,0.0,1.0);

        // 日冕顏色偏橙紅
        vec3 coronaCol=mix(uAtmoColor,uPlanetColor,0.3)*falloff;

        // ── 日珥噴發（Solar Prominences）─────────────────────────────────
        // 角度用 WORLD space 計算（不跟玩家視角/位置轉，修坑1）
        float tt=iTime*0.08;
        vec3  toSunW=uPlanetDir*uPlanetDist;        // world: 玩家→太陽中心
        float tW=dot(toSunW,dir);
        vec3  closestW=dir*tW;
        vec3  fcW=normalize(closestW-toSunW);       // world: 太陽中心→光線最近點
        float promAngle=atan(fcW.z,fcW.x)+iTime*0.01; // + 太陽自轉
        float promZone=normDist-1.0;                 // 0=表面, 往外增加

        vec3 promCol=vec3(0.0);
        // 近表面亮、往外變淡的整體衰減（真實日珥特徵）
        float heightFade=exp(-promZone*2.2);

        // ── 寧靜日珥拱圈（Quiescent Loops）：穩定存在，只緩慢呼吸飄動 ──
        // 真實太陽：寧靜日珥可持續數天到數週，幾乎不動
        for(int i=0;i<4;i++){
            float fi=float(i);
            float A1=fi*1.7+sin(fi*4.3)*1.5+iTime*0.002*(0.4+fi*0.2); // 極緩慢漂移
            float arcSpan=0.30+fi*0.10;
            // 呼吸：高度只 ±12% 微幅起伏，不是噴發
            float breathe=0.94+0.06*sin(iTime*0.04+fi*2.0);
            float peakH=(0.04+fi*0.025)*breathe; // 真實寧靜日珥高度 ~0.04~0.14 太陽半徑,是貼著邊緣的小環(原本 0.22+ 高了 5~10 倍)

            float dA=promAngle-A1;
            dA=mod(dA+3.14159,6.28318)-3.14159;
            float s=dA/arcSpan;
            if(s<0.0||s>1.0) continue;

            // 不規則拱形：用噪聲擾動高度，弧線不再是完美拋物線（飄動電漿）
            float warp=(fbm(vec3(s*4.0, fi*3.0, iTime*0.03))-0.5)*0.35  // 大尺度起伏
                      +(fbm(vec3(s*11.0, fi*5.0, iTime*0.08))-0.5)*0.15; // 細節抖動
            float archH=peakH*(sin(3.14159*s)*(1.0+warp));
            float dH=promZone-archH;
            // 管壁厚度也隨機，不均勻
            float thick=(0.03+0.015*sin(3.14159*s))*(0.7+0.6*fbm(vec3(s*6.0,fi,iTime*0.05)));
            float wisp=0.55+0.45*fbm(vec3(s*9.0, promZone*7.0-iTime*0.06, fi)); // 絲狀紊流
            float tube=exp(-(dH*dH)/(thick*thick))*wisp;
            vec3 pc=mix(vec3(1.0,0.16,0.16), vec3(0.70,0.04,0.08), clamp(promZone/peakH,0.0,1.0)); // 純 H-alpha 紅,別讓 G/B 太高否則疊在橘日冕上偏橘
            promCol+=pc*tube;
        }

        // ── 爆發日珥（Eruptive）：稀有離散事件，按遊戲時間縮放 ────────
        // iTime 已含 timeScale，調快遊戲時噴發也跟著變頻
        for(int j=0;j<3;j++){
            float fj=float(j);
            // 每 ~50 iTime 單位一個週期窗口（timeScale=1 約每 50 秒一窗）
            float period=50.0+fj*17.0;
            float cyc=iTime/period+fj*0.41;
            float cycId=floor(cyc);
            float cycPhase=fract(cyc);
            // 此週期是否真的噴發（hash 決定，~35% 機率）
            float roll=fract(sin(cycId*12.9898+fj*78.233)*43758.5453);
            if(roll<0.65) continue; // 多數週期無事發生
            // 噴發只在週期前 25% 發生：快速上升 → 緩慢消散
            if(cycPhase>0.25) continue;
            float ep=cycPhase/0.25;             // 0..1 噴發進程
            float env=sin(3.14159*ep);          // 上升下降包絡
            // 噴發角度（此週期隨機）
            float jetAngle=fract(sin(cycId*3.7+fj*5.1)*1000.0)*6.28318;
            float dA=promAngle-jetAngle;
            dA=mod(dA+3.14159,6.28318)-3.14159;
            float jetH=(0.40+fj*0.10)*env;      // 縮短,別噴太遠
            float hN=clamp(promZone/max(jetH,0.001),0.0,1.0);
            float w=0.035*(1.0+hN*1.5);         // 往上展開成羽流
            float fil=0.5+0.5*fbm(vec3(dA*8.0, promZone*8.0-iTime*0.3, fj));
            float jet=exp(-(dA*dA)/(w*w))*pow(1.0-hN,1.1)*step(promZone,jetH)*fil;
            promCol+=mix(vec3(1.0,0.20,0.16), vec3(0.70,0.04,0.08), hN)*jet*1.0; // 純紅
        }

        // 提亮一點讓紅壓過後面的橘日冕(否則加法疊上去會偏橘看不出紅)。小環不會掃螢幕,只在貼到太陽臉上才淡出
        coronaCol+=promCol*heightFade*2.3*smoothstep(1.0,0.72,sinA);

        if(dot(coronaCol,vec3(1.0))<0.001){fragColor=vec4(0.0);return;}
        fragColor=vec4(coronaCol,0.0);  // alpha=0：純加法混合
    }
}
