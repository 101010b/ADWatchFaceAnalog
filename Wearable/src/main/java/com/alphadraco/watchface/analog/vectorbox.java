package com.alphadraco.watchface.analog;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by aladin on 25.01.2015.
 */
public class vectorbox {
    
    vector va,vb,vc,vd,ve,vf,vg,vh;
    
    public vectorbox() {
        create(new vector(0,0,0),
                new vector(1,0,0),
                new vector(0,1,0),
                1,1,1);
    }
    
    public vectorbox(vector center, vector vfront, vector vtop, float len, float width, float height) {
        create(center,vfront,vtop,len,width,height);
    }
    
    public vectorbox(float x, float y, float z) {
        create(new vector(0,0,0),
                new vector(1,0,0),
                new vector(0,1,0),
                x,y,z);
    }
    
    private void create(vector center, vector vfront, vector vtop, float len, float width, float height) {
        vector M = vector.norm(vfront);
        vector N = vector.norm(vector.cross(M, vtop));
        vector O = vector.norm(vector.cross(M,N));
        
        M.mult(len/2);
        N.mult(width/2);
        O.mult(height/2);

        va=vector.linear(center,1.0f, M,-1.0f, N,-1.0f, O,-1.0f);
        vb=vector.linear(center,1.0f, M,-1.0f, N, 1.0f, O,-1.0f);
        vc=vector.linear(center,1.0f, M, 1.0f, N, 1.0f, O,-1.0f);
        vd=vector.linear(center,1.0f, M, 1.0f, N,-1.0f, O,-1.0f);

        ve=vector.linear(center,1.0f, M,-1.0f, N,-1.0f, O, 1.0f);
        vf=vector.linear(center,1.0f, M,-1.0f, N, 1.0f, O, 1.0f);
        vg=vector.linear(center,1.0f, M, 1.0f, N, 1.0f, O, 1.0f);
        vh=vector.linear(center,1.0f, M, 1.0f, N,-1.0f, O, 1.0f);
            
    }
    
    private void plotline(Canvas cv, Paint p, projector prj, vector a, vector b) {
        
        if (!prj.project(a)) return;
        float ax,ay;
        ax=prj.outx;
        ay=prj.outy;
        if (!prj.project(b)) return;
        cv.drawLine(ax,ay,prj.outx,prj.outy,p);        
    }
    
    public void plot(Canvas cv, Paint p, projector prj) {
        plotline(cv,p,prj,va,vb);
        plotline(cv,p,prj,vb,vc);
        plotline(cv,p,prj,vc,vd);
        plotline(cv,p,prj,vd,va);
        
        plotline(cv,p,prj,ve,vf);
        plotline(cv,p,prj,vf,vg);
        plotline(cv,p,prj,vg,vh);
        plotline(cv,p,prj,vh,ve);

        plotline(cv,p,prj,va,ve);
        plotline(cv,p,prj,vb,vf);
        plotline(cv,p,prj,vc,vg);
        plotline(cv,p,prj,vd,vh);
    }
    
}
