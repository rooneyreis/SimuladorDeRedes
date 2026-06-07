package com.gui;

import com.sistema.Sistema;
import com.estruturas.Grafo;
import com.estruturas.Vertice;
import com.estruturas.Aresta;
import com.modelo.Dispositivo;
import com.auxiliar.ListaDuplamenteLigada;
import com.auxiliar.No;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.Dimension;
import java.awt.FontMetrics;

public class PainelTopologia extends JPanel {
    private Sistema sistema;
    private ListaDuplamenteLigada rota;        // dispositivos da rota a animar (pode ser null)
    private ListaDuplamenteLigada destacados;  // nos alcancaveis a destacar (DFS, pode ser null)

    // geometria da rota (para a animacao)
    private int[] rx, ry;
    private double[] segCum;
    private double total;
    private double progresso;
    private javax.swing.Timer timer;

    private static final Color COR_FUNDO    = new Color(0x23, 0x25, 0x36);
    private static final Color COR_WAN      = new Color(0xD8, 0x5A, 0x30);
    private static final Color COR_LAN      = new Color(0x4A, 0x4D, 0x5E);
    private static final Color COR_ROTA     = new Color(0x4A, 0xDE, 0x80);
    private static final Color COR_PACOTE   = new Color(0xFA, 0xC7, 0x75);
    private static final Color COR_TEXTO    = Color.WHITE;
    private static final Color COR_LAT      = new Color(0x9A, 0xA0, 0xB4);
    private static final Color COR_ROUTER   = new Color(0x7F, 0xB0, 0xEE);
    private static final Color COR_SWITCH   = new Color(0x4F, 0xC5, 0x9A);
    private static final Color COR_PC       = new Color(0xB4, 0xB2, 0xA9);
    private static final Color COR_SERVIDOR = new Color(0xCE, 0xCB, 0xF6);

    public PainelTopologia(Sistema sistema) {
        this.sistema = sistema;
        setBackground(COR_FUNDO);
        setPreferredSize(new Dimension(880, 620));
    }

    // Mostra uma rota e anima um pacote a percorre-la.
    public void animarRota(ListaDuplamenteLigada rota) {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
        this.destacados = null;
        this.rota = rota;
        construirGeometria();
        this.progresso = 0;
        if (rota == null || rota.tamanho() < 2) {
            repaint();
            return;
        }
        timer = new javax.swing.Timer(25, e -> {
            progresso += 0.02;
            if (progresso >= 1.0) {
                progresso = 1.0;
                timer.stop();
            }
            repaint();
        });
        timer.start();
    }

    // Destaca um conjunto de nos (resultado do DFS de alcance).
    public void destacarNos(ListaDuplamenteLigada nos) {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
        this.rota = null;
        this.destacados = nos;
        repaint();
    }

    public void limpar() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
        this.rota = null;
        this.destacados = null;
        repaint();
    }

    private void construirGeometria() {
        if (rota == null || rota.tamanho() == 0) {
            rx = null; ry = null; total = 0;
            return;
        }
        int n = rota.tamanho();
        rx = new int[n];
        ry = new int[n];
        int i = 0;
        No no = rota.getPrimeiro();
        while (no != null) {
            Dispositivo d = (Dispositivo) no.getElemento();
            rx[i] = d.getX();
            ry[i] = d.getY();
            i++;
            no = no.getProximo();
        }
        segCum = new double[n];
        segCum[0] = 0;
        total = 0;
        for (int k = 1; k < n; k++) {
            double dx = rx[k] - rx[k - 1];
            double dy = ry[k] - ry[k - 1];
            total += Math.sqrt(dx * dx + dy * dy);
            segCum[k] = total;
        }
    }

    private double[] pontoAoLongo(double p) {
        if (rx == null || rx.length < 2 || total == 0) {
            return new double[]{ rx == null ? 0 : rx[0], ry == null ? 0 : ry[0] };
        }
        double dist = p * total;
        for (int k = 1; k < rx.length; k++) {
            if (dist <= segCum[k]) {
                double inicio = segCum[k - 1];
                double comp = segCum[k] - inicio;
                double f = comp == 0 ? 0 : (dist - inicio) / comp;
                double x = rx[k - 1] + (rx[k] - rx[k - 1]) * f;
                double y = ry[k - 1] + (ry[k] - ry[k - 1]) * f;
                return new double[]{ x, y };
            }
        }
        return new double[]{ rx[rx.length - 1], ry[ry.length - 1] };
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Grafo grafo = this.sistema.getGrafo();
        ListaDuplamenteLigada vertices = grafo.getVertices();

        // 1. ligacoes
        No vn = vertices.getPrimeiro();
        while (vn != null) {
            Vertice v = (Vertice) vn.getElemento();
            Dispositivo da = v.getDispositivo();
            No an = v.getAdjacentes().getPrimeiro();
            while (an != null) {
                Aresta a = (Aresta) an.getElemento();
                Dispositivo db = a.getDestino().getDispositivo();
                if (da.getNome().compareTo(db.getNome()) < 0) {
                    boolean wan = da.getTipo().equals("router") && db.getTipo().equals("router");
                    g2.setColor(wan ? COR_WAN : COR_LAN);
                    g2.setStroke(new BasicStroke(wan ? 2.5f : 1.5f));
                    g2.drawLine(da.getX(), da.getY(), db.getX(), db.getY());
                    int mx = (da.getX() + db.getX()) / 2;
                    int my = (da.getY() + db.getY()) / 2;
                    g2.setColor(COR_LAT);
                    g2.drawString(formatar(a.getPeso()), mx + 3, my - 3);
                }
                an = an.getProximo();
            }
            vn = vn.getProximo();
        }

        // 2. nos alcancaveis (DFS) - aneis verdes
        if (this.destacados != null) {
            g2.setColor(COR_ROTA);
            g2.setStroke(new BasicStroke(2.5f));
            No dn = this.destacados.getPrimeiro();
            while (dn != null) {
                Dispositivo d = (Dispositivo) dn.getElemento();
                int r = raioPorTipo(d.getTipo()) + 4;
                g2.drawOval(d.getX() - r, d.getY() - r, r * 2, r * 2);
                dn = dn.getProximo();
            }
        }

        // 3. rota (linha verde)
        if (this.rota != null && this.rota.tamanho() > 1) {
            g2.setColor(COR_ROTA);
            g2.setStroke(new BasicStroke(4f));
            No rn = this.rota.getPrimeiro();
            Dispositivo prev = (Dispositivo) rn.getElemento();
            rn = rn.getProximo();
            while (rn != null) {
                Dispositivo cur = (Dispositivo) rn.getElemento();
                g2.drawLine(prev.getX(), prev.getY(), cur.getX(), cur.getY());
                prev = cur;
                rn = rn.getProximo();
            }
        }

        // 4. nos (dispositivos)
        vn = vertices.getPrimeiro();
        while (vn != null) {
            Vertice v = (Vertice) vn.getElemento();
            Dispositivo d = v.getDispositivo();
            int r = raioPorTipo(d.getTipo());
            g2.setColor(corPorTipo(d.getTipo()));
            g2.fillOval(d.getX() - r, d.getY() - r, r * 2, r * 2);
            g2.setColor(COR_TEXTO);
            FontMetrics fm = g2.getFontMetrics();
            int larg = fm.stringWidth(d.getNome());
            g2.drawString(d.getNome(), d.getX() - larg / 2, d.getY() + r + 14);
            vn = vn.getProximo();
        }

        // 5. pacote em movimento (por cima de tudo)
        if (this.rota != null && this.rota.tamanho() > 1) {
            double[] pt = pontoAoLongo(progresso);
            int px = (int) pt[0];
            int py = (int) pt[1];
            g2.setColor(COR_PACOTE);
            g2.fillOval(px - 7, py - 7, 14, 14);
            g2.setColor(COR_TEXTO);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(px - 7, py - 7, 14, 14);
        }
    }

    private String formatar(double peso) {
        if (peso == Math.floor(peso)) {
            return String.valueOf((int) peso);
        }
        return String.valueOf(peso);
    }

    private int raioPorTipo(String tipo) {
        if (tipo.equals("router")) return 15;
        if (tipo.equals("switch")) return 11;
        return 6;
    }

    private Color corPorTipo(String tipo) {
        if (tipo.equals("router")) return COR_ROUTER;
        if (tipo.equals("switch")) return COR_SWITCH;
        if (tipo.equals("servidor")) return COR_SERVIDOR;
        return COR_PC;
    }
}