package com.fredrikux.collordotts.opengl;

import java.util.ArrayList;
import java.util.List;

public class GLDotContainer {

    private final List<GLDot> dotList = new ArrayList<>();

    public synchronized GLDot remove(int index){
        return dotList.remove(index);
    }

    public synchronized GLDot remove(GLDot dot){
        int index = dotList.indexOf(dot);
        return remove(index);
    }

    public synchronized void add(GLDot dot){
        dotList.add(dot);
    }

    public synchronized void clear(){
        dotList.clear();
    }

    public synchronized int size(){
        return dotList.size();
    }

    public synchronized GLDot get(int index){
        return dotList.get(index);
    }

    public synchronized int indexOf(GLDot dot){
        return dotList.indexOf(dot);
    }
}
