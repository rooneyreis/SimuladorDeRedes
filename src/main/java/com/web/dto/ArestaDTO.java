package com.web.dto;

import com.sistema.TopologiaService.LigacaoSimples;

/** Representacao de uma ligacao (aresta) para a interface / JSON. */
public class ArestaDTO {
    private String origem;
    private String destino;
    private double peso;   // latencia / distancia

    public ArestaDTO() { }

    public ArestaDTO(String origem, String destino, double peso) {
        this.origem = origem;
        this.destino = destino;
        this.peso = peso;
    }

    public static ArestaDTO de(LigacaoSimples l) {
        return new ArestaDTO(l.getOrigem(), l.getDestino(), l.getPeso());
    }

    public LigacaoSimples paraLigacao() {
        return new LigacaoSimples(origem, destino, peso);
    }

    public String getOrigem()  { return origem; }
    public String getDestino() { return destino; }
    public double getPeso()    { return peso; }

    public void setOrigem(String origem)   { this.origem = origem; }
    public void setDestino(String destino) { this.destino = destino; }
    public void setPeso(double peso)        { this.peso = peso; }
}
