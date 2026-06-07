package com.estruturas;

import com.auxiliar.ListaDuplamenteLigada;

public class Caminho {

    private final ListaDuplamenteLigada locais; // sequencia de Local, origem -> destino
    private final double distanciaTotal;        // km (POSITIVE_INFINITY se nao houver rota)

    public Caminho(ListaDuplamenteLigada locais, double distanciaTotal) {
        this.locais = locais;
        this.distanciaTotal = distanciaTotal;
    }

    public ListaDuplamenteLigada getLocais() {
        return this.locais;
    }

    public double getDistanciaTotal() {
        return this.distanciaTotal;
    }

    // true se existe uma rota (a lista nao esta vazia).
    public boolean existe() {
        return this.locais.tamanho() > 0;
    }

}
