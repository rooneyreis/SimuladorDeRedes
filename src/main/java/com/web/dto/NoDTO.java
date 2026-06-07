package com.web.dto;

import com.modelo.Dispositivo;

/** Representacao de um dispositivo (no) para a interface / JSON. */
public class NoDTO {
    private String id;     // nome unico (PC1, Switch1, Router1, ...)
    private String tipo;   // pc | switch | router
    private String ip;
    private double x;
    private double y;

    public NoDTO() { }

    public NoDTO(String id, String tipo, String ip, double x, double y) {
        this.id = id;
        this.tipo = tipo;
        this.ip = ip;
        this.x = x;
        this.y = y;
    }

    public static NoDTO de(Dispositivo d) {
        return new NoDTO(d.getNome(), d.getTipo(), d.getIp(), d.getX(), d.getY());
    }

    public Dispositivo paraDispositivo() {
        return new Dispositivo(id, tipo, ip, x, y);
    }

    public String getId()    { return id; }
    public String getTipo()  { return tipo; }
    public String getIp()    { return ip; }
    public double getX()     { return x; }
    public double getY()     { return y; }

    public void setId(String id)     { this.id = id; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setIp(String ip)     { this.ip = ip; }
    public void setX(double x)       { this.x = x; }
    public void setY(double y)       { this.y = y; }
}
