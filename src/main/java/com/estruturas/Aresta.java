package com.estruturas;

public class Aresta {
    private final Vertice destino;
    private final double peso; // distancia em km

    public Aresta(Vertice destino, double peso) {
        this.destino = destino;
        this.peso = peso;
    }

    public Vertice getDestino() {
        return this.destino;
    }

    public double getPeso() {
        return this.peso;
    }

    @Override
    public String toString() {
        return "-> " + this.destino.getDispositivo().getNome() + " (" + this.peso + " km)";
    }
}