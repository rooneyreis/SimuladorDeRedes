package com.estruturas;

import com.auxiliar.ListaDuplamenteLigada;
import com.modelo.Dispositivo;

public class Vertice {
    private final Dispositivo dispositivo;
    private final ListaDuplamenteLigada adjacentes; // lista de Aresta

    // Marcas usadas pelos algoritmos (reiniciadas antes de cada execucao).
    private boolean visitado;
    private double distancia;
    private Vertice anterior;

    public Vertice(Dispositivo dispositivo) {
        this.dispositivo = dispositivo;
        this.adjacentes = new ListaDuplamenteLigada();
        this.visitado = false;
        this.distancia = Double.POSITIVE_INFINITY;
        this.anterior = null;
    }

    public Dispositivo getDispositivo() {
        return this.dispositivo;
    }

    public ListaDuplamenteLigada getAdjacentes() {
        return this.adjacentes;
    }

    public boolean isVisitado() {
        return this.visitado;
    }

    public void setVisitado(boolean visitado) {
        this.visitado = visitado;
    }

    public double getDistancia() {
        return this.distancia;
    }

    public void setDistancia(double distancia) {
        this.distancia = distancia;
    }

    public Vertice getAnterior() {
        return this.anterior;
    }

    public void setAnterior(Vertice anterior) {
        this.anterior = anterior;
    }
}