package com.sistema;

import com.auxiliar.ListaDuplamenteLigada;
import com.auxiliar.No;
import com.estruturas.ArvoreAVL;
import com.estruturas.Aresta;
import com.estruturas.Caminho;
import com.estruturas.Grafo;
import com.estruturas.TabelaHash;
import com.estruturas.Trie;
import com.estruturas.Vertice;
import com.modelo.Dispositivo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Nucleo do simulador: gere uma topologia de rede DINAMICA (criada pelo
 * utilizador em tempo real) reutilizando as quatro estruturas de dados do
 * projeto original, sem as alterar:
 *
 *   - {@link Grafo}      -> topologia + algoritmos BFS / DFS / Dijkstra
 *   - {@link TabelaHash} -> consulta O(1) de um dispositivo pelo nome
 *   - {@link ArvoreAVL}  -> lista de dispositivos sempre ordenada (inOrder)
 *   - {@link Trie}       -> autocomplete / sugestoes por prefixo
 *
 * E uma classe simples (POJO), sem qualquer dependencia do Spring, para poder
 * ser testada isoladamente. O Spring liga-a como bean na classe de arranque.
 */
public class TopologiaService {

    private Grafo grafo;
    private TabelaHash hash;
    private ArvoreAVL avl;
    private Trie trie;

    /**
     * Contadores monotonicos por tipo. Nunca diminuem, por isso os
     * identificadores sao UNICOS mesmo depois de remocoes e novas adicoes
     * (apagar o PC1 e o PC2 e criar outro da PC3, nunca reaproveita o PC1).
     */
    private int proxPc;
    private int proxSwitch;
    private int proxRouter;

    public TopologiaService() {
        limpar();
    }

    // ------------------------------------------------------------------
    //  Gestao de estado
    // ------------------------------------------------------------------

    /** Esvazia toda a topologia e reinicia os contadores. */
    public synchronized void limpar() {
        this.grafo = new Grafo();
        this.hash = new TabelaHash();
        this.avl = new ArvoreAVL();
        this.trie = new Trie();
        this.proxPc = 1;
        this.proxSwitch = 1;
        this.proxRouter = 1;
    }

    private String prefixoDe(String tipo) {
        switch (normaliza(tipo)) {
            case "pc":     return "PC";
            case "switch": return "Switch";
            case "router": return "Router";
            default:
                throw new IllegalArgumentException(
                        "Tipo invalido: '" + tipo + "'. Use pc, switch ou router.");
        }
    }

    private String normaliza(String tipo) {
        return (tipo == null) ? "" : tipo.trim().toLowerCase();
    }

    /** Gera o proximo identificador unico para o tipo pedido. */
    private synchronized String gerarId(String tipo) {
        String t = normaliza(tipo);
        String prefixo = prefixoDe(t);
        int numero;
        switch (t) {
            case "pc":     numero = this.proxPc++;     break;
            case "switch": numero = this.proxSwitch++; break;
            default:       numero = this.proxRouter++; break; // router
        }
        return prefixo + numero;
    }

    // ------------------------------------------------------------------
    //  Operacoes sobre dispositivos
    // ------------------------------------------------------------------

    /** Cria um dispositivo do tipo dado, com id automatico, e regista-o nas 4 estruturas. */
    public synchronized Dispositivo adicionarDispositivo(String tipo) {
        return adicionarDispositivo(tipo, null, "", 60, 60);
    }

    public synchronized Dispositivo adicionarDispositivo(String tipo, String idForcado,
                                                         String ip, double x, double y) {
        String id = (idForcado != null && !idForcado.isBlank())
                ? idForcado
                : gerarId(tipo);

        if (this.hash.contem(id)) {
            throw new IllegalArgumentException("Ja existe um dispositivo com o nome '" + id + "'.");
        }

        Dispositivo d = new Dispositivo(id, normaliza(tipo), ip, x, y);
        this.grafo.adicionaVertice(d);
        this.hash.adiciona(d);
        this.avl.inserir(d);
        this.trie.inserir(d.getNome());
        return d;
    }

    /** Remove um dispositivo (e todas as suas ligacoes) das 4 estruturas. */
    public synchronized boolean removerDispositivo(String nome) {
        Dispositivo d = this.hash.consulta(nome);
        if (d == null) {
            return false;
        }
        this.grafo.removerVertice(d);
        this.hash.remove(nome);
        this.avl.remover(nome);
        this.trie.remover(nome);
        return true;
    }

    /**
     * Atualiza IP e/ou posicao de um dispositivo. Como todas as estruturas
     * partilham a MESMA instancia de Dispositivo (identificada pelo nome),
     * basta mutar o objeto canonico devolvido pela TabelaHash.
     */
    public synchronized Dispositivo atualizarDispositivo(String nome, String ip, Double x, Double y) {
        Dispositivo d = this.hash.consulta(nome);
        if (d == null) {
            return null;
        }
        if (ip != null) {
            d.setIp(ip);
        }
        if (x != null) {
            d.setX(x);
        }
        if (y != null) {
            d.setY(y);
        }
        return d;
    }

    // ------------------------------------------------------------------
    //  Operacoes sobre ligacoes (arestas)
    // ------------------------------------------------------------------

    /** Liga dois dispositivos com o peso (latencia/distancia) indicado. */
    public synchronized void adicionarAresta(String origem, String destino, double peso) {
        if (peso < 0) {
            throw new IllegalArgumentException("O peso da ligacao nao pode ser negativo.");
        }
        Dispositivo a = this.hash.consulta(origem);
        Dispositivo b = this.hash.consulta(destino);
        if (a == null || b == null) {
            throw new IllegalArgumentException("Origem ou destino inexistente.");
        }
        if (a.equals(b)) {
            throw new IllegalArgumentException("Nao e possivel ligar um dispositivo a si proprio.");
        }
        this.grafo.adicionaAresta(a, b, peso);
    }

    public synchronized void removerAresta(String origem, String destino) {
        Dispositivo a = this.hash.consulta(origem);
        Dispositivo b = this.hash.consulta(destino);
        if (a == null || b == null) {
            return;
        }
        this.grafo.removerAresta(a, b);
    }

    // ------------------------------------------------------------------
    //  Consultas auxiliares (AVL / Trie / Hash)
    // ------------------------------------------------------------------

    public synchronized boolean existe(String nome) {
        return this.avl.existe(nome);          // ArvoreAVL
    }

    public synchronized List<String> sugestoes(String prefixo) {
        return paraListaStrings(this.trie.sugestoes(prefixo)); // Trie
    }

    public synchronized List<Dispositivo> dispositivosOrdenados() {
        return paraListaDispositivos(this.avl.inOrder());      // ArvoreAVL inOrder
    }

    // ------------------------------------------------------------------
    //  Algoritmos
    // ------------------------------------------------------------------

    /** BFS: caminho com menor numero de saltos. */
    public synchronized ResultadoCaminho bfs(String origem, String destino) {
        Dispositivo a = this.hash.consulta(origem);
        Dispositivo b = this.hash.consulta(destino);
        if (a == null || b == null) {
            return ResultadoCaminho.semRota();
        }
        ListaDuplamenteLigada caminho = this.grafo.caminhoMenosTrocos(a, b);
        List<String> nomes = paraListaStrings(nomesDispositivos(caminho));
        if (nomes.isEmpty()) {
            return ResultadoCaminho.semRota();
        }
        int saltos = 0;
        for (String nome : nomes) {
            Dispositivo dsp = this.hash.consulta(nome);
            if (dsp != null && "router".equalsIgnoreCase(dsp.getTipo())) {
                saltos++;
            }
        }
        return new ResultadoCaminho(true, nomes, saltos, "saltos");
    }

    /** Dijkstra: caminho de menor custo (latencia/distancia acumulada). */
    public synchronized ResultadoCaminho dijkstra(String origem, String destino) {
        Dispositivo a = this.hash.consulta(origem);
        Dispositivo b = this.hash.consulta(destino);
        if (a == null || b == null) {
            return ResultadoCaminho.semRota();
        }
        Caminho c = this.grafo.caminhoMaisCurto(a, b);
        if (!c.existe()) {
            return ResultadoCaminho.semRota();
        }
        List<String> nomes = paraListaStrings(nomesDispositivos(c.getLocais()));
        return new ResultadoCaminho(true, nomes, c.getDistanciaTotal(), "custo");
    }

    /** DFS: dispositivos alcancaveis a partir da origem. */
    public synchronized List<String> dfsAlcancaveis(String origem) {
        Dispositivo a = this.hash.consulta(origem);
        if (a == null) {
            return new ArrayList<>();
        }
        return paraListaStrings(nomesDispositivos(this.grafo.alcancaveisDe(a)));
    }

    /**
     * Teste de conectividade via DFS. Devolve se a rede esta totalmente
     * conectada e a lista de componentes desconexos (cada um com os seus nos).
     */
    public synchronized ResultadoConectividade testarConectividade() {
        boolean conectado = this.grafo.estaTotalmenteLigado();
        List<List<String>> componentes = componentesConexos();

        String mensagem;
        if (componentes.isEmpty()) {
            mensagem = "A rede esta vazia. Adicione dispositivos para comecar.";
        } else if (conectado) {
            mensagem = "A rede esta totalmente conectada. Todos os dispositivos se alcancam.";
        } else {
            mensagem = "A rede nao esta totalmente conectada. Existem componentes desconexos.";
        }
        return new ResultadoConectividade(conectado, mensagem, componentes);
    }

    /** Descobre os componentes conexos correndo DFS a partir de cada no ainda nao visitado. */
    private List<List<String>> componentesConexos() {
        List<List<String>> componentes = new ArrayList<>();
        Set<String> jaVistos = new HashSet<>();

        No atual = this.grafo.getVertices().getPrimeiro();
        while (atual != null) {
            Vertice v = (Vertice) atual.getElemento();
            String nome = v.getDispositivo().getNome();
            if (!jaVistos.contains(nome)) {
                List<String> comp = paraListaStrings(nomesDispositivos(
                        this.grafo.alcancaveisDe(v.getDispositivo())));
                jaVistos.addAll(comp);
                componentes.add(comp);
            }
            atual = atual.getProximo();
        }
        return componentes;
    }

    // ------------------------------------------------------------------
    //  Serializacao da topologia (para a interface / JSON)
    // ------------------------------------------------------------------

    /** Devolve todos os dispositivos (ordenados pela AVL). */
    public synchronized List<Dispositivo> obterNos() {
        return dispositivosOrdenados();
    }

    /**
     * Devolve as arestas nao-dirigidas, sem duplicados. Como o grafo guarda
     * cada ligacao nos dois sentidos, emitimos apenas o par (a, b) com a < b.
     */
    public synchronized List<LigacaoSimples> obterArestas() {
        List<LigacaoSimples> arestas = new ArrayList<>();
        Set<String> emitidas = new LinkedHashSet<>();

        No no = this.grafo.getVertices().getPrimeiro();
        while (no != null) {
            Vertice v = (Vertice) no.getElemento();
            String origem = v.getDispositivo().getNome();

            No noAdj = v.getAdjacentes().getPrimeiro();
            while (noAdj != null) {
                Aresta a = (Aresta) noAdj.getElemento();
                String destino = a.getDestino().getDispositivo().getNome();

                String chave = (origem.compareTo(destino) < 0)
                        ? origem + "||" + destino
                        : destino + "||" + origem;

                if (!emitidas.contains(chave)) {
                    emitidas.add(chave);
                    arestas.add(new LigacaoSimples(origem, destino, a.getPeso()));
                }
                noAdj = noAdj.getProximo();
            }
            no = no.getProximo();
        }
        return arestas;
    }

    /**
     * Substitui toda a topologia pelos nos/arestas dados (usado no PUT e no
     * carregamento de JSON). Reajusta os contadores acima do maior numero
     * existente, para continuar a garantir ids unicos.
     */
    public synchronized void substituirTopologia(List<Dispositivo> nos, List<LigacaoSimples> arestas) {
        limpar();
        if (nos != null) {
            for (Dispositivo d : nos) {
                adicionarDispositivo(d.getTipo(), d.getNome(), d.getIp(), d.getX(), d.getY());
                ajustarContador(d.getNome());
            }
        }
        if (arestas != null) {
            for (LigacaoSimples l : arestas) {
                if (existe(l.getOrigem()) && existe(l.getDestino())) {
                    this.grafo.adicionaAresta(
                            this.hash.consulta(l.getOrigem()),
                            this.hash.consulta(l.getDestino()),
                            l.getPeso());
                }
            }
        }
    }

    private void ajustarContador(String nome) {
        String[] prefixos = {"PC", "Switch", "Router"};
        for (String p : prefixos) {
            if (nome.startsWith(p)) {
                String resto = nome.substring(p.length());
                try {
                    int n = Integer.parseInt(resto);
                    switch (p) {
                        case "PC":     if (n >= this.proxPc)     this.proxPc = n + 1;     break;
                        case "Switch": if (n >= this.proxSwitch) this.proxSwitch = n + 1; break;
                        case "Router": if (n >= this.proxRouter) this.proxRouter = n + 1; break;
                    }
                } catch (NumberFormatException ignorado) {
                    // nome nao segue o padrao prefixo+numero; nao mexe nos contadores
                }
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    //  Topologia de exemplo (cidades de Mocambique) - opcional, util p/ demo
    // ------------------------------------------------------------------

    public synchronized void carregarExemplo() {
        limpar();
        // ------------------------------------------------------------------
        // Exemplo desenhado para que BFS e Dijkstra deem caminhos DIFERENTES.
        //
        //   PC1, PC2 --[Switch1]-- Router1(Maputo)
        //
        //   Router1 --- ligação DIRETA cara (50) --- Router4(Beira)   << poucos saltos
        //   Router1 -- Router2 -- Router3 -- Router4 (5+5+5 = 15)      << mais saltos, barato
        //
        //   Router4(Beira) --[Switch2]-- PC3      e  Router4 -- Router5(Tete)
        //
        // PC1 -> PC3:
        //   BFS      escolhe a rota DIRETA Router1-Router4  (menos saltos, mas custo 54)
        //   Dijkstra escolhe a rota Router1-Router2-Router3-Router4 (mais saltos, custo 19)
        // ------------------------------------------------------------------
        criar("router", "10.0.1.1", 300, 180);   // Router1 - MAPUTO
        criar("router", "10.0.2.1", 400, 360);   // Router2 - XAI-XAI
        criar("router", "10.0.3.1", 560, 360);   // Router3 - INHAMBANE
        criar("router", "10.0.4.1", 660, 180);   // Router4 - BEIRA
        criar("router", "10.0.5.1", 760, 320);   // Router5 - TETE
        criar("switch", "",         180, 180);    // Switch1 (LAN Maputo)
        criar("switch", "",         790, 120);    // Switch2 (LAN Beira)
        criar("pc",     "10.0.1.10", 70, 110);    // PC1
        criar("pc",     "10.0.1.11", 70, 250);    // PC2
        criar("pc",     "10.0.4.10", 840, 240);   // PC3

        // Backbone WAN (latência em ms)
        this.grafo.adicionaAresta(disp("Router1"), disp("Router4"), 50); // direta, cara
        this.grafo.adicionaAresta(disp("Router1"), disp("Router2"), 5);
        this.grafo.adicionaAresta(disp("Router2"), disp("Router3"), 5);
        this.grafo.adicionaAresta(disp("Router3"), disp("Router4"), 5);
        this.grafo.adicionaAresta(disp("Router4"), disp("Router5"), 8);
        // LAN (latência baixa)
        this.grafo.adicionaAresta(disp("Router1"), disp("Switch1"), 0);
        this.grafo.adicionaAresta(disp("Switch1"), disp("PC1"), 0);
        this.grafo.adicionaAresta(disp("Switch1"), disp("PC2"), 0);
        this.grafo.adicionaAresta(disp("Router4"), disp("Switch2"), 0);
        this.grafo.adicionaAresta(disp("Switch2"), disp("PC3"), 0);
    }

    private void criar(String tipo, String ip, double x, double y) {
        adicionarDispositivo(tipo, null, ip, x, y);
    }

    private Dispositivo disp(String nome) {
        return this.hash.consulta(nome);
    }

    // ------------------------------------------------------------------
    //  Helpers de conversao (ListaDuplamenteLigada -> List do Java)
    // ------------------------------------------------------------------

    private ListaDuplamenteLigada nomesDispositivos(ListaDuplamenteLigada dispositivos) {
        // Recebe lista de Dispositivo e devolve lista de String (nomes).
        ListaDuplamenteLigada nomes = new ListaDuplamenteLigada();
        No atual = dispositivos.getPrimeiro();
        while (atual != null) {
            Dispositivo d = (Dispositivo) atual.getElemento();
            nomes.adicionaFim(d.getNome());
            atual = atual.getProximo();
        }
        return nomes;
    }

    private List<String> paraListaStrings(ListaDuplamenteLigada lista) {
        List<String> resultado = new ArrayList<>();
        No atual = lista.getPrimeiro();
        while (atual != null) {
            resultado.add((String) atual.getElemento());
            atual = atual.getProximo();
        }
        return resultado;
    }

    private List<Dispositivo> paraListaDispositivos(ListaDuplamenteLigada lista) {
        List<Dispositivo> resultado = new ArrayList<>();
        No atual = lista.getPrimeiro();
        while (atual != null) {
            resultado.add((Dispositivo) atual.getElemento());
            atual = atual.getProximo();
        }
        return resultado;
    }

    // ------------------------------------------------------------------
    //  Tipos de resultado simples (sem dependencias externas)
    // ------------------------------------------------------------------

    /** Resultado de BFS/Dijkstra. */
    public static class ResultadoCaminho {
        private boolean existe;
        private List<String> nos;
        private double valor;     // saltos (BFS) ou custo (Dijkstra)
        private String unidade;   // "saltos" ou "custo"

        public ResultadoCaminho(boolean existe, List<String> nos, double valor, String unidade) {
            this.existe = existe;
            this.nos = nos;
            this.valor = valor;
            this.unidade = unidade;
        }

        public static ResultadoCaminho semRota() {
            return new ResultadoCaminho(false, new ArrayList<>(), 0, "");
        }

        public boolean isExiste()       { return existe; }
        public List<String> getNos()    { return nos; }
        public double getValor()        { return valor; }
        public String getUnidade()      { return unidade; }
    }

    /** Resultado do teste de conectividade. */
    public static class ResultadoConectividade {
        private boolean conectado;
        private String mensagem;
        private List<List<String>> componentes;

        public ResultadoConectividade(boolean conectado, String mensagem, List<List<String>> componentes) {
            this.conectado = conectado;
            this.mensagem = mensagem;
            this.componentes = componentes;
        }

        public boolean isConectado()                { return conectado; }
        public String getMensagem()                 { return mensagem; }
        public List<List<String>> getComponentes()  { return componentes; }
    }

    /** Ligacao simples (origem, destino, peso) para serializacao. */
    public static class LigacaoSimples {
        private String origem;
        private String destino;
        private double peso;

        public LigacaoSimples() { }

        public LigacaoSimples(String origem, String destino, double peso) {
            this.origem = origem;
            this.destino = destino;
            this.peso = peso;
        }

        public String getOrigem()  { return origem; }
        public String getDestino() { return destino; }
        public double getPeso()    { return peso; }

        public void setOrigem(String origem)   { this.origem = origem; }
        public void setDestino(String destino) { this.destino = destino; }
        public void setPeso(double peso)        { this.peso = peso; }
    }
}
