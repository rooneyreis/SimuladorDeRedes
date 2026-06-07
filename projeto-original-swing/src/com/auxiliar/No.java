package com.auxiliar;

public class No {
    private No proximo;
    private No anterior;
    private Object elemento;

    public No(Object elemento, No proximo){
        this.proximo = proximo;
        this.elemento = elemento;
    }

    public No(No anterior, Object elemento, No proximo){
        this.anterior = anterior;
        this.elemento = elemento;
        this.proximo = proximo;
    }

    public No(No anterior, Object elemento){
        this.elemento = elemento;
        this.anterior = anterior;
    }

    public No(Object elemento){
        this.elemento = elemento;
    }

    public No getProximo() {
        return proximo;
    }

    public No getAnterior() {
        return anterior;
    }

    public void setProximo(No proximo) {
        this.proximo = proximo;
    }

    public void setAnterior(No anterior) {
        this.anterior = anterior;
    }

    public Object getElemento() {
        return elemento;
    }

}

