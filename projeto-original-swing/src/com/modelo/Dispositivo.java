package com.modelo;

public class Dispositivo {
    private final String nome;
    private final String tipo;  // "router", "switch", "pc", "servidor"
    private final int x;        // posicao no diagrama
    private final int y;

    public Dispositivo(String nome, String tipo, int x, int y) {
        this.nome = nome;
        this.tipo = tipo;
        this.x = x;
        this.y = y;
    }

    public String getNome() {
        return this.nome;
    }

    public String getTipo() {
        return this.tipo;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    // Dois dispositivos sao o mesmo se tiverem o mesmo nome.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        Dispositivo outro = (Dispositivo) obj;
        return this.nome.equals(outro.nome);
    }

    @Override
    public int hashCode() {
        return this.nome.hashCode();
    }

    @Override
    public String toString() {
        return this.nome;
    }
}