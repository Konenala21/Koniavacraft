#version 150

uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  uPlanetDir;
uniform float uPlanetDist;
uniform vec3  uDirToStar;
uniform float uAngularRadius;
uniform float uRingInner;
uniform float uRingOuter;
uniform float uRingTilt;
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
    vec4 result = vec4(0.0);

    vec2 ndc=texCoord*2.0-1.0;
    vec4 vd=InvProjMat*vec4(ndc,1.0,1.0);vd.xyz/=vd.w;
    vec3 dir=normalize((InvViewMat*vec4(normalize(vd.xyz),0.0)).xyz);

    float cosA=dot(dir,uPlanetDir);
    float sinA=sqrt(max(1.0-uAngularRadius*uAngularRadius,0.0));
    float R   =uPlanetDist*sinA;

    float sinOuter=sinA*uRingOuter;
    bool inRange=(sinOuter>=1.0)||
                 (cosA>sqrt(max(1.0-sinOuter*sinOuter,0.0))-0.001);

    if(inRange&&R>0.1){
        mat3  L   =buildFrame(uPlanetDir);
        vec3  lD  =normalize(L*dir);
        vec3  lSun=normalize(L*uDirToStar);
        vec3  cam =vec3(0.0,0.0,-uPlanetDist);

        vec3 worldRingNormal=vec3(0.0,cos(uRingTilt),sin(uRingTilt));
        vec3 ringNormal=normalize(L*worldRingNormal);

        float denom=dot(lD,ringNormal);
        if(abs(denom)>0.0005){
            float t=-dot(cam,ringNormal)/denom;
            if(t>=0.0&&t<=1e5){
                vec3  hitPt=cam+lD*t;
                float dist=length(hitPt);
                float innerR=R*uRingInner;
                float outerR=R*uRingOuter;
                bool inRing=(dist>=innerR)&&(dist<=outerR)&&(outerR>innerR);

                if(inRing){
                    vec2 sph=sphHit(cam,lD,R);
                    bool blocked=sph.x>0.0&&sph.x<sph.y&&sph.x<t;
                    if(!blocked){
                        float u=clamp((dist-innerR)/(outerR-innerR),0.0,1.0);
                        vec4 ring=texture(uRingTex,vec2(u,0.5));
                        if(ring.a>0.01){
                            float sunlit=clamp(dot(normalize(hitPt),lSun)*0.5+0.6,0.15,1.0);
                            float a=ring.a*uAlpha;
                            result=vec4(ring.rgb*sunlit*a,a);
                        }
                    }
                }
            }
        }
    }

    fragColor=result;
}
