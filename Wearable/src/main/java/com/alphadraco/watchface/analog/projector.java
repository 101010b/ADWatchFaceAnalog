package com.alphadraco.watchface.analog;

/**
 * Created by aladin on 25.01.2015.
 */
public class projector {
    
    vector eye, look_at, top;
    float foc, sw, sh;
    float scrw, scrh;
    
    vector front,right,up;
    
    public float outx, outy;
    
    public projector(vector xeye, vector xlook_at, vector xtop, float xfl, float xsw, float xsh,
                     float screenw, float screenh) {
        eye=xeye;
        look_at=xlook_at;
        top=xtop;
        foc=xfl;
        sw = xsw;
        sh=xsh;
        scrw=screenw;
        scrh=screenh;
        
        front=vector.sub(look_at,eye);
        right=vector.cross(front,top);
        up=vector.cross(right,front);
        right.norm();
        up.norm();
        front.norm();
    }
    
    public Boolean project(vector q) {
        vector qq=vector.sub(q,eye);
        float infront=vector.scal(qq,front);
        if (infront < 0) return false;
        float xofs = -1.0f * vector.scal(qq,right)*foc/infront;
        float yofs = -1.0f * vector.scal(qq,up)*foc/infront;
        if ((xofs < -sw/2) || (xofs > sw/2) || (yofs < -sh/2) || (yofs > sh/2)) 
            return false;
        outx=xofs/sw*scrw+scrw/2;
        outy=yofs/sh*scrh+scrh/2;
        return true;
    }
}
