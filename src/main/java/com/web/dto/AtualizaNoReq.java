package com.web.dto;

/** Corpo do PATCH para atualizar IP e/ou posicao de um dispositivo. */
public class AtualizaNoReq {
    private String ip;   // pode vir null (nao alterar)
    private Double x;    // pode vir null (nao alterar)
    private Double y;

    public String getIp() { return ip; }
    public Double getX()  { return x; }
    public Double getY()  { return y; }

    public void setIp(String ip) { this.ip = ip; }
    public void setX(Double x)   { this.x = x; }
    public void setY(Double y)   { this.y = y; }
}
