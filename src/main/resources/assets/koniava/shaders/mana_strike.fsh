#version 150

#define STEPS    90
#define HIT_DIST 0.08
#define MAX_D    280.0
#define PI       3.14159265

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  CameraPosition;
uniform vec3  BlockPosition;
uniform float iTime;
uniform float iAlpha;

in  vec2 texCoord;
out vec4 fragColor;

const vec3 C_VOID    = vec3(0.02, 0.04, 0.20);
const vec3 C_CORE    = vec3(0.14, 0.48, 1.00);
const vec3 C_BRIGHT  = vec3(0.60, 0.86, 1.00);
const vec3 C_CRYSTAL = vec3(0.52, 0.22, 1.00);
const vec3 C_PILLAR  = vec3(0.75, 0.90, 1.00);
const vec3 C_WAVE    = vec3(0.88, 0.95, 1.00);

// ── Timing ────────────────────────────────────────────────────────────────────
const float T_CHARGE     = 2.0;
const float T_BURST_END  = 10.0;
const float T_EXPL_START = 7.5;    // radial burst start (near sphere collapse)
const float T_EXPL_END   = 10.5;
const float T_SHOCK_OFF  = 1.2;    // shockwave starts T_BURST_END - this
const float T_ORB_START  = 14.0;   // 6 suns appear
const float T_ORB_ACCEL  = 2.0;    // slow→fast rotation phase
const float T_ORB_RISE   = 1.5;    // rise to 15-block height
const float T_CONVERGE   = 0.6;    // inward convergence
const float T_TOTAL      = 24.0;

// ── Helpers ───────────────────────────────────────────────────────────────────

vec3 worldPos(vec3 uvd) {
    vec3 ndc = uvd * 2.0 - 1.0;
    vec4 hp  = InvProjMat * vec4(ndc, 1.0);
    vec3 vp  = hp.xyz / hp.w;
    return (InvViewMat * vec4(vp, 1.0)).xyz + CameraPosition;
}

float sdCrystalSphere(vec3 p, float R) {
    float r    = length(p);
    float shell= abs(r - R);
    float theta= atan(p.z, p.x);
    float phi  = acos(clamp(p.y / (r + 0.001), -1.0, 1.0));
    float facet= 0.28 * abs(sin(7.0 * theta)) * abs(sin(5.0 * phi));
    return shell - facet * smoothstep(1.0, 6.0, R);
}

float marchSphere(vec3 ro, vec3 rd, float R, float maxDist) {
    float b    = dot(ro, rd);
    float disc = b*b - (dot(ro,ro) - (R+0.5)*(R+0.5));
    if (disc < 0.0) return MAX_D;
    float sqD  = sqrt(disc);
    float tN   = max(0.0, -b - sqD - 0.5);
    float tF   = min(maxDist, -b + sqD + 0.5);
    if (tN >= tF) return MAX_D;
    float t = tN;
    for (int i = 0; i < STEPS; i++) {
        float d = sdCrystalSphere(ro + rd * t, R);
        if (d < HIT_DIST) return t;
        if (t >= tF)      return MAX_D;
        t += max(d * 0.55, 0.02);
    }
    return MAX_D;
}

// 6 suns — slightly different shades of blue per orb
vec3 sunColor(int i) {
    float f = float(i) / 5.0;
    return mix(vec3(0.08, 0.30, 1.00), vec3(0.55, 0.90, 1.00), f);
}

// Analytical ray-sphere intersection — O(1), no loop
// Returns distance to hit, or -1.0 if miss
float raySphere(vec3 ro, vec3 rd, vec3 center, float radius) {
    vec3  oc   = ro - center;
    float b    = dot(oc, rd);
    float c    = dot(oc, oc) - radius * radius;
    float disc = b * b - c;
    if (disc < 0.0) return -1.0;
    float t = -b - sqrt(disc);
    return (t > 0.0) ? t : -1.0;
}

void main() {
    float depth    = texture(DepthSampler,   texCoord).r;
    float lt = clamp(iTime, 0.0, T_TOTAL);

    // ── Chromatic Aberration — peaks at explosion start, fades in 1.5s ────────
    float caAge = lt - T_EXPL_START;
    float ca    = smoothstep(0.0, 0.15, caAge) * smoothstep(1.5, 0.0, caAge) * 0.007;
    vec2  caOff = (texCoord - 0.5) * ca;
    vec3  original = vec3(
        texture(DiffuseSampler, texCoord + caOff).r,
        texture(DiffuseSampler, texCoord       ).g,
        texture(DiffuseSampler, texCoord - caOff).b
    );

    vec3  startP = worldPos(vec3(texCoord, 0.0)) - BlockPosition;
    vec3  dir    = normalize(worldPos(vec3(texCoord, 0.5)) - worldPos(vec3(texCoord, 0.0)));
    float sceneD = (depth >= 0.9999)
                 ? MAX_D
                 : length((worldPos(vec3(texCoord, depth)) - BlockPosition) - startP);
    vec3  endP   = startP + dir * sceneD;

    vec3  addLight  = vec3(0.0);
    vec3  repCol    = original;
    float repW      = 0.0;

    // ── A. Charge-up: ground gathering rings ─────────────────────────────────
    if (lt < T_CHARGE && abs(dir.y) > 0.001) {
        float cp = lt / T_CHARGE;
        float tG = -startP.y / dir.y;
        if (tG > 0.05 && tG < sceneD) {
            float hr   = length((startP + dir * tG).xz);
            float ring = 0.06 / (abs(mod(hr * 0.45 - (T_CHARGE - lt) * 2.2, 2.2) - 1.1) + 0.05);
            ring *= smoothstep(28.0, 1.5, hr) * cp * iAlpha;
            addLight += C_CORE * ring;
        }
    }

    // ── B. Crystal sphere ────────────────────────────────────────────────────
    float burstP  = clamp((lt - T_CHARGE) / (T_BURST_END - T_CHARGE), 0.0, 1.0);
    float sphereR = sin(burstP * PI) * 21.0;
    if (sphereR > 0.3) {
        float t   = marchSphere(startP, dir, sphereR, sceneD);
        bool  hit = (t < MAX_D - 0.1);
        if (hit) {
            vec3  hp    = startP + dir * t;
            float r     = length(hp);
            float theta = atan(hp.z, hp.x);
            float phi   = acos(clamp(hp.y / (r + 0.001), -1.0, 1.0));
            float facet = abs(sin(7.0 * theta)) * abs(sin(5.0 * phi));
            float rim   = 1.0 - smoothstep(0.0, 0.30, sdCrystalSphere(hp, sphereR));
            vec3  sCol  = mix(C_CORE, C_CRYSTAL, facet * 0.6);
            sCol        = mix(sCol, C_BRIGHT, rim * 0.55 + facet * facet * 0.35);
            float sFade = 1.0 - burstP * burstP;
            repCol = sCol * sFade;
            repW   = sFade * iAlpha;
        }
        float outerD = sdCrystalSphere(endP, sphereR);
        if (outerD > 0.0)
            addLight += C_CORE * (0.022 / (outerD*outerD + 0.012)) * (1.0 - burstP * 0.7) * iAlpha;
        if (!hit && length(endP) < sphereR * 0.88) {
            float vF = (1.0 - length(endP) / (sphereR * 0.88)) * (1.0 - burstP);
            repCol = mix(original * 0.30, C_VOID + C_CORE * 0.12, vF * 0.8);
            repW   = max(repW, vF * 0.60 * iAlpha);
        }
        // 8 pillar glows during sphere
        float pLife = smoothstep(T_CHARGE + 0.4, T_CHARGE + 2.0, lt)
                    * (1.0 - smoothstep(T_BURST_END - 2.2, T_BURST_END, lt));
        if (pLife > 0.001) {
            for (int i = 0; i < 8; i++) {
                float ang = float(i) * PI * 0.25;
                float cx  = cos(ang) * 4.0, cz = sin(ang) * 4.0;
                float d2d = length(endP.xz - vec2(cx, cz));
                float vF  = step(0.0, endP.y) * smoothstep(26.0, 0.0, endP.y);
                vec3  pc  = mix(C_CORE, C_PILLAR, smoothstep(16.0, 26.0, endP.y));
                addLight += pc * (0.008 / (d2d*d2d + 0.008)) * vF * pLife * iAlpha;
            }
        }
    }

    // ── C. Nuclear-style explosion burst ─────────────────────────────────────
    float explAge = lt - T_EXPL_START;
    if (explAge > 0.0 && lt < T_EXPL_END) {
        float explDur  = T_EXPL_END - T_EXPL_START;
        float explFade = (1.0 - explAge / explDur) * iAlpha;
        float flashP   = explAge / explDur;

        // Central expanding shockwave sphere flash (k/dist² — ref pattern)
        float blastR = explAge * 15.0;
        float blastD = abs(length(endP) - blastR);
        float blastG = 0.08 / (blastD * blastD + 2.5) * (1.0 - flashP * flashP) * iAlpha;
        addLight += mix(C_BRIGHT, vec3(1.0), 0.6) * blastG;

        // 24 solid spheres flying outward
        float bestFragT   = sceneD;
        vec3  bestFragCol = vec3(0.0);
        for (int i = 0; i < 24; i++) {
            float fi      = float(i);
            float phi_i   = acos(1.0 - 2.0 * (fi + 0.5) / 24.0);
            float theta_i = fi * 2.399963;
            vec3  od = normalize(vec3(sin(phi_i)*cos(theta_i),
                                      cos(phi_i) * 0.4 + 0.15,
                                      sin(phi_i)*sin(theta_i)));
            float speed = 9.0 + fract(fi * 0.618) * 7.0;
            vec3  op    = od * explAge * speed;

            float t = raySphere(startP, dir, op, 0.40);
            if (t > 0.0 && t < bestFragT) {
                bestFragT   = t;
                bestFragCol = mix(C_CORE, C_BRIGHT, fi / 23.0);
            }

            float d2 = dot(endP - op, endP - op) + 0.08;
            addLight += mix(C_CORE, C_BRIGHT, fi / 23.0) * (0.006 / d2) * explFade;
        }
        if (bestFragT < sceneD) {
            repCol = bestFragCol;
            repW   = max(repW, explFade * 0.9);
        }
    }

    // ── D. Ground shockwave rings ─────────────────────────────────────────────
    float shockT = max(lt - (T_BURST_END - T_SHOCK_OFF), 0.0);
    if (shockT > 0.0 && abs(dir.y) > 0.001) {
        float tG = -startP.y / dir.y;
        if (tG > 0.05 && tG < sceneD) {
            float hitR = length((startP + dir * tG).xz);
            for (int w = 0; w < 7; w++) {
                float wf    = float(w);
                float wStart= wf * 0.75 + fract(wf * 0.6180339) * 0.45;
                float wt    = shockT - wStart;
                if (wt <= 0.0) continue;
                float speed = 7.0 + wf * 0.5;
                float maxR  = 4.5 + wf * 4.0;
                float waveR = wt * speed;
                if (waveR > maxR + 1.8) continue;
                float thick = 0.26 + wf * 0.042;
                float edgeF = smoothstep(maxR + thick, maxR * 0.28, waveR);
                float s     = smoothstep(thick, 0.0, abs(hitR - waveR)) * edgeF * iAlpha;
                if (s < 0.003) continue;
                float lead  = smoothstep(-thick * 0.6, thick * 0.32, hitR - waveR);
                addLight   += mix(C_CORE, C_WAVE, lead * 0.80) * s * (0.45 + wf * 0.055);
            }
        }
    }

    // ── E. 6 orbiting suns ───────────────────────────────────────────────────
    float orbAge = lt - T_ORB_START;
    if (orbAge > 0.0) {
        float orbFade = (1.0 - smoothstep(T_ORB_ACCEL + T_ORB_RISE + T_CONVERGE,
                                           T_ORB_ACCEL + T_ORB_RISE + T_CONVERGE + 0.8,
                                           orbAge)) * iAlpha;

        if (orbFade > 0.001) {
            if (orbAge < T_ORB_ACCEL) {
                // ── E1. Birth: 6 ring halos expand outward to orbit radius ─
                // 6 solid spheres expanding outward from center
                float bestBirthT   = sceneD;
                vec3  bestBirthCol = vec3(0.0);
                for (int i = 0; i < 6; i++) {
                    float stag  = float(i) * 0.08;
                    float bp    = clamp((orbAge - stag) / (T_ORB_ACCEL * 0.92), 0.0, 1.0);
                    float ringR = bp * bp * 6.0;
                    float ang   = float(i) * (PI / 3.0);
                    vec3  sPos  = vec3(cos(ang) * ringR, 0.4, sin(ang) * ringR);

                    float t = raySphere(startP, dir, sPos, 0.45);
                    if (t > 0.0 && t < bestBirthT) {
                        bestBirthT   = t;
                        bestBirthCol = sunColor(i);
                    }

                    float d2 = dot(endP - sPos, endP - sPos) + 0.08;
                    addLight += sunColor(i) * (0.005 / d2) * orbFade;
                }
                if (bestBirthT < sceneD) {
                    repCol = bestBirthCol;
                    repW   = max(repW, orbFade);
                }
            } else {
                // ── E2. Fast rotation + rise + convergence ────────────────
                float fastAge   = orbAge - T_ORB_ACCEL;
                float baseAngle = 5.0 * fastAge;

                float riseP  = clamp(fastAge / T_ORB_RISE, 0.0, 1.0);
                float height = mix(0.4, 15.0, riseP);

                float convAge = max(fastAge - T_ORB_RISE, 0.0);
                float convP   = clamp(convAge / T_CONVERGE, 0.0, 1.0);
                float orbR    = mix(6.0, 0.0, convP * convP);

                float convFade = 1.0 - convP * convP;

                // Find closest sun sphere hit (analytical ray-sphere)
                float bestT   = sceneD;
                vec3  bestCol = vec3(0.0);
                for (int i = 0; i < 6; i++) {
                    float ang  = baseAngle + float(i) * (PI / 3.0);
                    vec3  sPos = vec3(cos(ang) * orbR, height, sin(ang) * orbR);

                    // Solid sphere — visible only where ray hits, no sky pollution
                    float t = raySphere(startP, dir, sPos, 0.45);
                    if (t > 0.0 && t < bestT) {
                        bestT   = t;
                        bestCol = sunColor(i);
                    }

                    // k/dist² geometry glow — only when orb is in front of geometry
                    float tOrb = raySphere(startP, dir, sPos, 6.0);
                    float occlude = (tOrb < 0.0 || tOrb < sceneD) ? 1.0 : 0.0;
                    float d2 = dot(endP - sPos, endP - sPos) + 0.08;
                    addLight += sunColor(i) * (0.005 / d2) * orbFade * convFade * occlude;
                }
                // Render closest hit sun as solid pixel
                if (bestT < sceneD) {
                    repCol = bestCol;
                    repW   = max(repW, orbFade * convFade);
                }
            }
        }
    }

    // ── F. Skyward convergence pillar ─────────────────────────────────────────
    float pillarStart = T_ORB_START + T_ORB_ACCEL + T_ORB_RISE + T_CONVERGE; // ≈18.1s
    float pillarAge   = lt - pillarStart;
    if (pillarAge > 0.0) {
        float pFade = smoothstep(0.0, 1.0, pillarAge)
                    * (1.0 - smoothstep(T_TOTAL - 2.5, T_TOTAL, lt))
                    * iAlpha;
        if (pFade > 0.001) {
            // Geometry glow on nearby blocks
            float dAxis = length(endP.xz);
            float ht    = endP.y;
            float yFade = smoothstep(-0.5, 2.5, ht) * smoothstep(85.0, 70.0, ht);
            addLight += C_CORE   * (0.22 / (dAxis*dAxis + 0.55)) * yFade * pFade;
            addLight += C_BRIGHT * (0.07 / (dAxis*dAxis + 0.07)) * yFade * pFade;
            addLight += vec3(1.0) * (0.010/ (dAxis*dAxis + 0.005)) * yFade * pFade;
            // Ray-to-Y-axis closest approach: visible against open sky
            float denom = dot(dir.xz, dir.xz);
            if (denom > 0.0001) {
                float tCA  = -dot(startP.xz, dir.xz) / denom;
                if (tCA > 0.0 && tCA < sceneD) {
                    vec3  ca   = startP + dir * tCA;
                    float htCA = ca.y;
                    float dCA  = length(ca.xz);
                    float yCA  = smoothstep(-0.5, 2.5, htCA) * smoothstep(85.0, 70.0, htCA);
                    addLight += C_CORE   * (0.28 / (dCA*dCA + 0.55)) * yCA * pFade;
                    addLight += C_BRIGHT * (0.10 / (dCA*dCA + 0.07)) * yCA * pFade;
                    addLight += vec3(1.0) * (0.018/ (dCA*dCA + 0.005)) * yCA * pFade;
                }
            }
        }
    }

    // ── Composite + HDR tonemapping (light only, scene stays intact) ──────────
    vec3 baseCol    = mix(original, repCol, clamp(repW, 0.0, 1.0));
    vec3 mappedLight = addLight / (addLight + vec3(0.6)) * 1.4; // tonemap addLight only
    fragColor = vec4(clamp(baseCol + mappedLight, vec3(0.0), vec3(1.0)), 1.0);
}
