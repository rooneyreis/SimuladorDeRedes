package com.web.dto;

/** Corpo do POST para criar uma ligacao com peso. */
public class NovaArestaReq {
    private String origem;
    private String destino;
    private double peso;

    public String getOrigem()  { return origem; }
    public String getDestino() { return destino; }
    public double getPeso()    { return peso; }

    public void setOrigem(String origem)   { this.origem = origem; }
    public void setDestino(String destino) { this.destino = destino; }
    public void setPeso(double peso)        { this.peso = peso; }
}
