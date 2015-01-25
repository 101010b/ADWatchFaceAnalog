package com.alphadraco.watchface.analog;

/**
 * Created by aladin on 25.01.2015.
 */
public class vector {
    public float x,y,z;
    
    public vector() {
        x=y=z=0.0f;
    }
    
    public vector(float ax,float ay, float az) {
        x=ax;y=ay;z=az;
    }
    
    public vector(vector v) {
        x=v.x;y=v.y;z=v.z;        
    }
    
    public static vector mult(vector v, float f) {
        return new vector(v.x*f,v.y*f,v.z*f);         
    }
    
    public static vector mult(float f,vector v) { return mult(v,f); }
    
    public void mult(float f) {
        x*=f;y*=f;z*=f;
    }
    
    public static vector add(vector v, vector w) {
        return new vector(v.x+w.x,v.y+w.y,v.z+w.z);       
    }
    
    public void add(vector v) {
        x+=v.x;y+=v.y;z+=v.z;        
    }
    
    public static vector sub(vector v, vector w) {
        return new vector(v.x-w.x,v.y-w.y,v.z-w.z);
    }
    
    public void sub(vector v) {
        x-=v.x;y-=v.y;z-=v.z;        
    }
    
    public static float scal(vector a, vector b) {
        return a.x*b.x+a.y*b.y+a.z*b.z;        
    }
    
    public static vector cross(vector b, vector c) {
        return new vector(
                b.y*c.z-b.z*c.y,
                b.z*c.x-b.x*c.z,
                b.x*c.y-b.y*c.x);
    }
    
    public void cross(vector c) {
        float xn=y*c.z-z*c.y;
        float yn=z*c.x-x*c.z;
        float zn=x*c.y-y*c.x;
        x=xn;y=yn;z=zn;       
    }
    
    public float len() {
        return (float)Math.sqrt((double) x*x+y*y+z*z);
    }
    
    public static float len(vector v) {
        return (float)Math.sqrt((double) v.x*v.x+v.y*v.y+v.z*v.z);
    }
    
    public void norm() {
        float l=len();
        if (l > 0f) {
            x/=l;
            y/=l;
            z/=l;
        }            
    }
    
    public static vector norm(vector v) {
        float l=v.len();
        if (l > 0f) return new vector(v.x/l,v.y/l,v.z/l);
        return new vector();
    }

    public static vector linear(vector v, float l) {
        return new vector(v.x*l, v.y*l, v.z*l);
    }

    public static vector linear(vector v1, float l1, vector v2, float l2) {
        return new vector(
                v1.x*l1 + v2.x*l2, 
                v1.y*l1 + v2.y*l2, 
                v1.z*l1 + v2.z*l2);
    }

    public static vector linear(vector v1, float l1, 
                                vector v2, float l2,
                                vector v3, float l3) {
        return new vector(
                v1.x*l1 + v2.x*l2 + v3.x*l3,
                v1.y*l1 + v2.y*l2 + v3.y*l3,
                v1.z*l1 + v2.z*l2 + v3.z*l3);
    }

    public static vector linear(vector v1, float l1,
                                vector v2, float l2,
                                vector v3, float l3,
                                vector v4, float l4) {
        return new vector(
                v1.x*l1 + v2.x*l2 + v3.x*l3 + v4.x*l4,
                v1.y*l1 + v2.y*l2 + v3.y*l3 + v4.y*l4,
                v1.z*l1 + v2.z*l2 + v3.z*l3 + v4.z*l4);
    }


}
