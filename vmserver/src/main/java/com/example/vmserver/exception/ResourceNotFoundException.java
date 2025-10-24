package com.example.vmserver.exception;

public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException (String s){
        super(s);
    }
}
