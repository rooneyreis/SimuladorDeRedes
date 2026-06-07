package com.modelo;

public class Pacote {
    private final Dispositivo origem;
    private final Dispositivo destino;

    public Pacote(Dispositivo origem, Dispositivo destino) {
        this.origem = origem;
        this.destino = destino;
    }

    public Dispositivo getOrigem() {
        return this.origem;
    }

    public Dispositivo getDestino() {
        return this.destino;
    }

    @Override
    public String toString() {
        return this.origem.getNome() + " -> " + this.destino.getNome();
    }
}