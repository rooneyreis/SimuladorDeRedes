package com.web.dto;

import java.util.List;

/** Topologia completa: nos + arestas. Usado no GET e no PUT de /api/topologia. */
public class TopologiaDTO {
    private List<NoDTO> nos;
    private List<ArestaDTO> arestas;

    public TopologiaDTO() { }

    public TopologiaDTO(List<NoDTO> nos, List<ArestaDTO> arestas) {
        this.nos = nos;
        this.arestas = arestas;
    }

    public List<NoDTO> getNos()        { return nos; }
    public List<ArestaDTO> getArestas() { return arestas; }

    public void setNos(List<NoDTO> nos)             { this.nos = nos; }
    public void setArestas(List<ArestaDTO> arestas) { this.arestas = arestas; }
}
