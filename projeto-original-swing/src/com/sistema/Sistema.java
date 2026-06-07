package com.sistema;

import com.modelo.Dispositivo;
import com.modelo.Pacote;
import com.estruturas.Grafo;
import com.estruturas.TabelaHash;
import com.estruturas.ArvoreAVL;
import com.estruturas.Trie;
import com.estruturas.Caminho;
import com.auxiliar.Fila;
import com.auxiliar.ListaDuplamenteLigada;

public class Sistema {
    private Grafo grafo;
    private TabelaHash hash;
    private ArvoreAVL avl;
    private Trie autocomplete;
    private Fila filaPacotes;

    public Sistema() {
        this.grafo = new Grafo();
        this.hash = new TabelaHash();
        this.avl = new ArvoreAVL();
        this.autocomplete = new Trie();
        this.filaPacotes = new Fila();
        carregarTopologia();
    }

    // ---------- arranque / dados ----------

    private void carregarTopologia() {
        Dispositivo[] disp = {
                new Dispositivo("MAPUTO",    "router", 305, 210),
                new Dispositivo("INHAMBANE", "router", 505,  85),
                new Dispositivo("BEIRA",     "router", 490, 210),
                new Dispositivo("CHIMOIO",   "router", 535, 310),
                new Dispositivo("NAMPULA",   "router", 440, 410),
                new Dispositivo("TETE",      "router", 525, 510),
                new Dispositivo("SW-MAP", "switch", 155, 205),
                new Dispositivo("SW-INH", "switch", 710,  80),
                new Dispositivo("SW-BEI", "switch", 715, 205),
                new Dispositivo("SW-CHI", "switch", 640, 320),
                new Dispositivo("SW-NAM", "switch", 715, 420),
                new Dispositivo("SW-TET", "switch", 610, 515),
                new Dispositivo("PC-M1", "pc",  70, 160),
                new Dispositivo("PC-M2", "pc",  70, 250),
                new Dispositivo("PC-I1", "pc", 820,  30),
                new Dispositivo("PC-I2", "pc", 820, 130),
                new Dispositivo("PC-B1", "pc", 820, 185),
                new Dispositivo("PC-B2", "pc", 820, 255),
                new Dispositivo("PC-C1", "pc", 720, 285),
                new Dispositivo("PC-C2", "pc", 720, 355),
                new Dispositivo("PC-N1", "pc", 805, 350),
                new Dispositivo("PC-N2", "pc", 805, 435),
                new Dispositivo("PC-T1", "pc", 705, 485),
                new Dispositivo("PC-T2", "pc", 705, 565)
        };
        for (Dispositivo d : disp) {
            registarDispositivo(d);
        }

        // backbone WAN entre cidades (latencia em ms)
        ligar("MAPUTO", "INHAMBANE", 8);
        ligar("MAPUTO", "BEIRA", 12);
        ligar("MAPUTO", "NAMPULA", 30);
        ligar("INHAMBANE", "BEIRA", 10);   // ligacao acrescentada
        ligar("BEIRA", "CHIMOIO", 5);
        ligar("NAMPULA", "TETE", 7);
        ligar("CHIMOIO", "TETE", 9);

        // LAN: router -> switch
        ligar("MAPUTO", "SW-MAP", 1);
        ligar("INHAMBANE", "SW-INH", 1);
        ligar("BEIRA", "SW-BEI", 1);
        ligar("CHIMOIO", "SW-CHI", 1);
        ligar("NAMPULA", "SW-NAM", 1);
        ligar("TETE", "SW-TET", 1);

        // LAN: switch -> PCs
        ligar("SW-MAP", "PC-M1", 1);
        ligar("SW-MAP", "PC-M2", 1);
        ligar("SW-INH", "PC-I1", 1);
        ligar("SW-INH", "PC-I2", 1);
        ligar("SW-BEI", "PC-B1", 1);
        ligar("SW-BEI", "PC-B2", 1);
        ligar("SW-CHI", "PC-C1", 1);
        ligar("SW-CHI", "PC-C2", 1);
        ligar("SW-NAM", "PC-N1", 1);
        ligar("SW-NAM", "PC-N2", 1);
        ligar("SW-TET", "PC-T1", 1);
        ligar("SW-TET", "PC-T2", 1);

        if (!this.grafo.estaTotalmenteLigado()) {
            System.out.println("AVISO: a rede nao esta totalmente ligada.");
        }
    }

    private void registarDispositivo(Dispositivo d) {
        this.grafo.adicionaVertice(d);
        this.hash.adiciona(d);
        this.avl.inserir(d);
        this.autocomplete.inserir(d.getNome());
    }

    private void ligar(String a, String b, double latencia) {
        this.grafo.adicionaAresta(this.hash.consulta(a), this.hash.consulta(b), latencia);
    }

    // ---------- consultas para a interface ----------

    public ListaDuplamenteLigada sugerir(String prefixo) {
        return this.autocomplete.sugestoes(prefixo);
    }

    public Dispositivo dispositivo(String nome) {
        return this.hash.consulta(nome);
    }

    public boolean existe(String nome) {
        return this.avl.existe(nome);
    }

    public ListaDuplamenteLigada dispositivosOrdenados() {
        return this.avl.inOrder();
    }

    public boolean redeLigada() {
        return this.grafo.estaTotalmenteLigado();
    }

    public Grafo getGrafo() {
        return this.grafo;
    }

    // ---------- rotas ----------

    public Caminho rotaMaisCurta(String origemNome, String destinoNome) {
        return this.grafo.caminhoMaisCurto(
                this.hash.consulta(origemNome),
                this.hash.consulta(destinoNome));
    }

    public ListaDuplamenteLigada rotaMenosSaltos(String origemNome, String destinoNome) {
        return this.grafo.caminhoMenosTrocos(
                this.hash.consulta(origemNome),
                this.hash.consulta(destinoNome));
    }

    // DFS: dispositivos alcancaveis a partir da origem (demonstra conectividade)
    public ListaDuplamenteLigada alcancaveis(String origemNome) {
        return this.grafo.alcancaveisDe(this.hash.consulta(origemNome));
    }

    // ---------- pacotes (Fila) ----------

    public void inserirPacote(String origemNome, String destinoNome) {
        Dispositivo origem = this.hash.consulta(origemNome);
        Dispositivo destino = this.hash.consulta(destinoNome);
        if (origem == null || destino == null) {
            return;
        }
        this.filaPacotes.enqueue(new Pacote(origem, destino));
    }

    public Pacote removerPacote() {
        if (this.filaPacotes.isEmpty()) {
            return null;
        }
        return (Pacote) this.filaPacotes.peekAndDequeue();
    }

    public Pacote proximoPacote() {
        if (this.filaPacotes.isEmpty()) {
            return null;
        }
        return (Pacote) this.filaPacotes.peek();
    }

    public int pacotesPendentes() {
        return this.filaPacotes.size();
    }
}