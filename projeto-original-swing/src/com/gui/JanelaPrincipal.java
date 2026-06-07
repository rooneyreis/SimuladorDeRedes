package com.gui;

import com.sistema.Sistema;
import com.modelo.Dispositivo;
import com.modelo.Pacote;
import com.estruturas.Caminho;
import com.auxiliar.ListaDuplamenteLigada;
import com.auxiliar.No;

import javax.swing.*;
import java.awt.*;

public class JanelaPrincipal extends JFrame {
    private Sistema sistema;
    private PainelTopologia painelTopologia;

    private JComboBox<String> comboOrigem;
    private JComboBox<String> comboDestino;
    private JComboBox<String> comboAlgoritmo;

    private JTextArea areaRota;
    private DefaultListModel<String> modeloFila;
    private JList<String> listaFila;
    private JLabel labelConectividade;
    private JLabel labelContagem;
    private JTextArea areaLog;

    private static final Color FUNDO     = new Color(0x1E, 0x1E, 0x2E);
    private static final Color PAINEL    = new Color(0x2A, 0x2D, 0x3E);
    private static final Color SUPERF    = new Color(0x23, 0x25, 0x36);
    private static final Color AZUL      = new Color(0x4F, 0x46, 0xE5);
    private static final Color TEXTO     = Color.WHITE;
    private static final Color TEXTO_MUT = new Color(0x9A, 0x9A, 0xB0);

    public JanelaPrincipal(Sistema sistema) {
        this.sistema = sistema;

        setTitle("Simulador de Rede - Grafos & Routing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(FUNDO);
        setLayout(new BorderLayout(8, 8));

        add(criarPainelControlos(), BorderLayout.WEST);
        add(criarPainelCentral(), BorderLayout.CENTER);
        add(criarPainelInfo(), BorderLayout.EAST);
        add(criarPainelLog(), BorderLayout.SOUTH);

        preencherCombos();
        atualizarInfo();
        log("topologia carregada: " + contarNos() + " nos");
        log("conectividade: " + (sistema.redeLigada() ? "OK (rede ligada)" : "rede NAO ligada"));
    }

    private JPanel criarPainelControlos() {
        JPanel p = new JPanel();
        p.setBackground(PAINEL);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        p.setPreferredSize(new Dimension(200, 0));

        comboOrigem = combo();
        comboDestino = combo();
        comboAlgoritmo = new JComboBox<>(new String[]{"Dijkstra", "BFS", "DFS"});
        estilizarCombo(comboAlgoritmo);

        p.add(rotulo("Origem (PC)"));
        p.add(comboOrigem);
        p.add(Box.createVerticalStrut(10));
        p.add(rotulo("Destino (PC)"));
        p.add(comboDestino);
        p.add(Box.createVerticalStrut(10));
        p.add(rotulo("Algoritmo"));
        p.add(comboAlgoritmo);
        p.add(Box.createVerticalStrut(16));

        JButton bCalcular  = botao("Calcular rota");
        JButton bInserir   = botao("Inserir pacote");
        JButton bRemover   = botao("Remover pacote");
        JButton bProcessar = botao("Processar pacotes");

        bCalcular.addActionListener(e -> calcularRota());
        bInserir.addActionListener(e -> inserirPacote());
        bRemover.addActionListener(e -> removerPacote());
        bProcessar.addActionListener(e -> processarPacote());

        p.add(bCalcular);
        p.add(Box.createVerticalStrut(8));
        p.add(bInserir);
        p.add(Box.createVerticalStrut(8));
        p.add(bRemover);
        p.add(Box.createVerticalStrut(8));
        p.add(bProcessar);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JComponent criarPainelCentral() {
        painelTopologia = new PainelTopologia(sistema);
        JScrollPane scroll = new JScrollPane(painelTopologia);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(SUPERF);
        return scroll;
    }

    private JPanel criarPainelInfo() {
        JPanel p = new JPanel();
        p.setBackground(PAINEL);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        p.setPreferredSize(new Dimension(230, 0));

        p.add(rotulo("Resultado"));
        areaRota = new JTextArea(3, 16);
        areaRota.setEditable(false);
        areaRota.setLineWrap(true);
        areaRota.setWrapStyleWord(true);
        areaRota.setBackground(SUPERF);
        areaRota.setForeground(TEXTO);
        areaRota.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        areaRota.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        areaRota.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(areaRota);
        p.add(Box.createVerticalStrut(14));

        p.add(rotulo("Fila de pacotes"));
        modeloFila = new DefaultListModel<>();
        listaFila = new JList<>(modeloFila);
        listaFila.setBackground(SUPERF);
        listaFila.setForeground(TEXTO);
        JScrollPane scrollFila = new JScrollPane(listaFila);
        scrollFila.setBorder(null);
        scrollFila.setPreferredSize(new Dimension(200, 130));
        scrollFila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        scrollFila.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(scrollFila);
        p.add(Box.createVerticalStrut(14));

        labelConectividade = rotulo("Conectividade: --");
        labelContagem = rotulo("Nos: --   Pacotes: --");
        p.add(labelConectividade);
        p.add(Box.createVerticalStrut(4));
        p.add(labelContagem);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JComponent criarPainelLog() {
        areaLog = new JTextArea(6, 0);
        areaLog.setEditable(false);
        areaLog.setBackground(new Color(0x1A, 0x1A, 0x28));
        areaLog.setForeground(new Color(0x8C, 0x8C, 0xA0));
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaLog.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JScrollPane scroll = new JScrollPane(areaLog);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(0, 130));
        return scroll;
    }

    // ---------- accoes ----------

    private void calcularRota() {
        String o = (String) comboOrigem.getSelectedItem();
        String d = (String) comboDestino.getSelectedItem();
        String alg = (String) comboAlgoritmo.getSelectedItem();
        if (o == null || d == null) return;
        executar(o, d, alg);
    }

    private void executar(String o, String d, String alg) {
        if (alg.equals("Dijkstra")) {
            Caminho c = sistema.rotaMaisCurta(o, d);
            if (c == null || !c.existe()) {
                mostrarSemRota(o, d);
                return;
            }
            ListaDuplamenteLigada lista = c.getLocais();
            painelTopologia.animarRota(lista);
            String texto = caminhoTexto(lista);
            areaRota.setText(texto + "\n" + formatar(c.getDistanciaTotal()) + " ms");
            log("Dijkstra " + o + " -> " + d + ": " + texto + " (" + formatar(c.getDistanciaTotal()) + " ms)");
        } else if (alg.equals("BFS")) {
            ListaDuplamenteLigada lista = sistema.rotaMenosSaltos(o, d);
            if (lista == null || lista.tamanho() == 0) {
                mostrarSemRota(o, d);
                return;
            }
            painelTopologia.animarRota(lista);
            String texto = caminhoTexto(lista);
            int saltos = lista.tamanho() - 1;
            areaRota.setText(texto + "\n" + saltos + " saltos");
            log("BFS " + o + " -> " + d + ": " + texto + " (" + saltos + " saltos)");
        } else { // DFS - alcance / conectividade
            ListaDuplamenteLigada lista = sistema.alcancaveis(o);
            painelTopologia.destacarNos(lista);
            int n = lista.tamanho();
            boolean tudo = sistema.redeLigada();
            areaRota.setText("DFS a partir de " + o + ":\n" + n + " dispositivos alcancaveis"
                    + "\n" + (tudo ? "rede ligada" : "ha nos isolados"));
            log("DFS " + o + ": " + n + " alcancaveis (" + (tudo ? "rede ligada" : "ha nos isolados") + ")");
        }
    }

    private void mostrarSemRota(String o, String d) {
        painelTopologia.limpar();
        areaRota.setText("Sem rota de " + o + " para " + d);
        log("sem rota: " + o + " -> " + d);
    }

    private void inserirPacote() {
        String o = (String) comboOrigem.getSelectedItem();
        String d = (String) comboDestino.getSelectedItem();
        if (o == null || d == null) return;
        sistema.inserirPacote(o, d);
        modeloFila.addElement(o + " -> " + d);
        atualizarInfo();
        log("pacote inserido: " + o + " -> " + d);
    }

    private void removerPacote() {
        Pacote p = sistema.removerPacote();
        if (p == null) {
            log("fila vazia: nada para remover");
            return;
        }
        if (!modeloFila.isEmpty()) {
            modeloFila.remove(0);
        }
        atualizarInfo();
        log("pacote removido: " + p.toString());
    }

    private void processarPacote() {
        Pacote p = sistema.proximoPacote();
        if (p == null) {
            log("fila vazia: nada para processar");
            return;
        }
        String o = p.getOrigem().getNome();
        String d = p.getDestino().getNome();
        String alg = (String) comboAlgoritmo.getSelectedItem();
        if (alg.equals("DFS")) {
            alg = "Dijkstra"; // um pacote precisa de uma rota; o DFS nao encaminha
        }
        executar(o, d, alg);
        sistema.removerPacote();
        if (!modeloFila.isEmpty()) {
            modeloFila.remove(0);
        }
        atualizarInfo();
        log("pacote processado: " + o + " -> " + d);
    }

    // ---------- auxiliares ----------

    private void preencherCombos() {
        ListaDuplamenteLigada nos = sistema.dispositivosOrdenados();
        No atual = nos.getPrimeiro();
        while (atual != null) {
            Dispositivo dsp = (Dispositivo) atual.getElemento();
            if (dsp.getTipo().equals("pc")) {   // so PCs como origem/destino
                comboOrigem.addItem(dsp.getNome());
                comboDestino.addItem(dsp.getNome());
            }
            atual = atual.getProximo();
        }
    }

    private void atualizarInfo() {
        labelConectividade.setText("Conectividade: " + (sistema.redeLigada() ? "OK" : "NAO ligada"));
        labelContagem.setText("Nos: " + contarNos() + "   Pacotes: " + sistema.pacotesPendentes());
    }

    private int contarNos() {
        return sistema.dispositivosOrdenados().tamanho();
    }

    private String caminhoTexto(ListaDuplamenteLigada lista) {
        StringBuilder sb = new StringBuilder();
        No atual = lista.getPrimeiro();
        while (atual != null) {
            Dispositivo dsp = (Dispositivo) atual.getElemento();
            sb.append(dsp.getNome());
            if (atual.getProximo() != null) {
                sb.append(" > ");
            }
            atual = atual.getProximo();
        }
        return sb.toString();
    }

    private String formatar(double v) {
        if (v == Math.floor(v)) return String.valueOf((int) v);
        return String.valueOf(v);
    }

    private void log(String msg) {
        areaLog.append("> " + msg + "\n");
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }

    // ---------- estilos ----------

    private JLabel rotulo(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(TEXTO_MUT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JComboBox<String> combo() {
        JComboBox<String> c = new JComboBox<>();
        estilizarCombo(c);
        return c;
    }

    private void estilizarCombo(JComboBox<String> c) {
        c.setBackground(new Color(0x1E, 0x1E, 0x2E));
        c.setForeground(TEXTO);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JButton botao(String txt) {
        JButton b = new JButton(txt);
        b.setBackground(AZUL);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }
}