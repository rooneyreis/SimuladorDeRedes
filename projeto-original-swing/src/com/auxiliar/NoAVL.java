package com.auxiliar;

import com.modelo.Dispositivo;

public class NoAVL {
    private Dispositivo dispositivo;
    private NoAVL esquerda;
    private NoAVL direita;
    private int altura;

    public NoAVL(Dispositivo dispositivo) {
        this.dispositivo = dispositivo;
        this.esquerda = null;
        this.direita = null;
        this.altura = 1; // um no novo e uma folha
    }

    public Dispositivo getLocal() {
        return this.dispositivo;
    }

    public void setLocal(Dispositivo dispositivo) {
        this.dispositivo = dispositivo;
    }

    public NoAVL getEsquerda() {
        return this.esquerda;
    }

    public void setEsquerda(NoAVL esquerda) {
        this.esquerda = esquerda;
    }

    public NoAVL getDireita() {
        return this.direita;
    }

    public void setDireita(NoAVL direita) {
        this.direita = direita;
    }

    public int getAltura() {
        return this.altura;
    }

    public void setAltura(int altura) {
        this.altura = altura;
    }
}