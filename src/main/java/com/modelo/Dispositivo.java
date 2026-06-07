package com.modelo;

/**
 * Representa um dispositivo de rede (PC, Switch ou Router).
 *
 * Em relacao a versao Swing original foram feitas duas pequenas adaptacoes,
 * exigidas pela interface web:
 *   1) campo {@code ip} para configuracao basica de endereco;
 *   2) posicao (x, y) passou a ser mutavel, porque o utilizador arrasta os
 *      dispositivos na area de desenho.
 *
 * A IDENTIDADE continua a ser o {@code nome} (final): dois dispositivos sao
 * iguais se tiverem o mesmo nome. Isto garante que a TabelaHash, a ArvoreAVL e
 * o Grafo continuam a funcionar exatamente como antes, porque todos guardam a
 * MESMA instancia de Dispositivo, identificada pelo nome.
 */
public class Dispositivo {
    private final String nome;   // identificador unico (PC1, Switch1, Router1, ...)
    private final String tipo;   // "router", "switch", "pc"
    private String ip;           // endereco IP configuravel (pode ser null/"")
    private double x;            // posicao no canvas (arrastavel)
    private double y;

    public Dispositivo(String nome, String tipo, double x, double y) {
        this(nome, tipo, "", x, y);
    }

    public Dispositivo(String nome, String tipo, String ip, double x, double y) {
        this.nome = nome;
        this.tipo = tipo;
        this.ip = (ip == null) ? "" : ip;
        this.x = x;
        this.y = y;
    }

    public String getNome() {
        return this.nome;
    }

    public String getTipo() {
        return this.tipo;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = (ip == null) ? "" : ip;
    }

    public double getX() {
        return this.x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return this.y;
    }

    public void setY(double y) {
        this.y = y;
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
