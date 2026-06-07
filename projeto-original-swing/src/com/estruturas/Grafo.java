package com.estruturas;

import com.auxiliar.Fila;
import com.auxiliar.ListaDuplamenteLigada;
import com.auxiliar.No;
import com.auxiliar.Pilha;
import com.modelo.Dispositivo;

public class Grafo {
    private ListaDuplamenteLigada vertices; // lista de Vertice

    public Grafo() {
        this.vertices = new ListaDuplamenteLigada();
    }

    // Encontra o Vertice que embrulha um dado Dispositivo, ou null.
    private Vertice procuraVertice(Dispositivo dispositivo) {
        No atual = this.vertices.getPrimeiro();
        while (atual != null) {
            Vertice v = (Vertice) atual.getElemento();
            if (v.getDispositivo().equals(dispositivo)) {
                return v;
            }
            atual = atual.getProximo();
        }
        return null;
    }

    public void adicionaVertice(Dispositivo dispositivo) {
        if (procuraVertice(dispositivo) != null) {
            return; // ja existe este vertice
        }
        this.vertices.adicionaFim(new Vertice(dispositivo));
    }

    public void adicionaAresta(Dispositivo origem, Dispositivo destino, double peso) {
        Vertice vOrigem = procuraVertice(origem);
        Vertice vDestino = procuraVertice(destino);

        if (vOrigem == null || vDestino == null) {
            throw new IllegalArgumentException(
                    "Ambos os dispositivos tem de existir no grafo antes de os ligar.");
        }

        ligar(vOrigem, vDestino, peso);   // origem -> destino
        ligar(vDestino, vOrigem, peso);   // destino -> origem (nao-dirigido)
    }

    // Adiciona uma aresta dirigida origem->destino, evitando duplicados.
    private void ligar(Vertice origem, Vertice destino, double peso) {
        if (existeAresta(origem, destino)) {
            return;
        }
        origem.getAdjacentes().adicionaFim(new Aresta(destino, peso));
    }

    // Verifica se 'origem' ja tem uma aresta para o vertice 'destino'.
    private boolean existeAresta(Vertice origem, Vertice destino) {
        No atual = origem.getAdjacentes().getPrimeiro();
        while (atual != null) {
            Aresta a = (Aresta) atual.getElemento();
            if (a.getDestino() == destino) {   // mesma instancia de vertice
                return true;
            }
            atual = atual.getProximo();
        }
        return false;
    }

    // Reinicia as marcas de todos os vertices antes de cada algoritmo.
    private void limparMarcas() {
        No atual = this.vertices.getPrimeiro();
        while (atual != null) {
            Vertice v = (Vertice) atual.getElemento();
            v.setVisitado(false);
            v.setDistancia(Double.POSITIVE_INFINITY);
            v.setAnterior(null);
            atual = atual.getProximo();
        }
    }

    //=======BFS======
    public ListaDuplamenteLigada caminhoMenosTrocos(Dispositivo origemDispositivo, Dispositivo destinoDispositivo) {
        Vertice origem = procuraVertice(origemDispositivo);
        Vertice destino = procuraVertice(destinoDispositivo);
        if (origem == null || destino == null) {
            return new ListaDuplamenteLigada(); // origem ou destino nao existem
        }

        limparMarcas();

        Fila fila = new Fila();
        origem.setVisitado(true);
        fila.enqueue(origem);

        while (!fila.isEmpty()) {
            Vertice atual = (Vertice) fila.peekAndDequeue();

            if (atual == destino) {
                break; // ja chegamos
            }

            No no = atual.getAdjacentes().getPrimeiro();
            while (no != null) {
                Aresta a = (Aresta) no.getElemento();
                Vertice vizinho = a.getDestino();
                if (!vizinho.isVisitado()) {
                    vizinho.setVisitado(true);
                    vizinho.setAnterior(atual);   // rasto para reconstruir
                    fila.enqueue(vizinho);
                }
                no = no.getProximo();
            }
        }

        if (!destino.isVisitado()) {
            return new ListaDuplamenteLigada(); // nao ha rota
        }
        return reconstroiCaminho(destino);
    }

    // Reconstroi o caminho origem->destino seguindo o rasto 'anterior'.
    private ListaDuplamenteLigada reconstroiCaminho(Vertice destino) {
        ListaDuplamenteLigada caminho = new ListaDuplamenteLigada();
        Vertice atual = destino;
        while (atual != null) {
            caminho.adicionaInicio(atual.getDispositivo());
            atual = atual.getAnterior();
        }
        return caminho;
    }

    //=======DFS======
    // DFS iterativo (Pilha): devolve os dispositivos alcancaveis a partir de 'inicio'.
    private ListaDuplamenteLigada dfs(Vertice inicio) {
        limparMarcas();
        ListaDuplamenteLigada resultado = new ListaDuplamenteLigada();
        Pilha pilha = new Pilha();
        pilha.push(inicio);

        while (!pilha.isEmpty()) {
            Vertice atual = (Vertice) pilha.peekAndPop();
            if (atual.isVisitado()) {
                continue; // ja processado; ignora repetidos
            }
            atual.setVisitado(true);
            resultado.adicionaFim(atual.getDispositivo());

            No no = atual.getAdjacentes().getPrimeiro();
            while (no != null) {
                Aresta a = (Aresta) no.getElemento();
                Vertice vizinho = a.getDestino();
                if (!vizinho.isVisitado()) {
                    pilha.push(vizinho);
                }
                no = no.getProximo();
            }
        }
        return resultado;
    }

    // Conta vertices alcancaveis a partir de 'inicio' (usa o mesmo DFS).
    public int contaAlcancaveis(Vertice inicio) {
        return dfs(inicio).tamanho();
    }

    // Interface: dispositivos alcancaveis a partir de um dispositivo (DFS).
    public ListaDuplamenteLigada alcancaveisDe(Dispositivo origem) {
        Vertice inicio = procuraVertice(origem);
        if (inicio == null) {
            return new ListaDuplamenteLigada();
        }
        return dfs(inicio);
    }

    // Grafo conexo: todos os vertices alcancaveis a partir de um qualquer.
    public boolean estaTotalmenteLigado() {
        if (this.vertices.tamanho() == 0) {
            return true; // grafo vazio: trivialmente ligado
        }
        Vertice inicio = (Vertice) this.vertices.pega(0);
        int alcancaveis = contaAlcancaveis(inicio);
        return alcancaveis == this.vertices.tamanho();
    }

    // Procura linear: vertice nao-visitado com menor distancia (ou null se nao houver).
    private Vertice verticeMaisProximoNaoVisitado() {
        Vertice melhor = null;
        No atual = this.vertices.getPrimeiro();
        while (atual != null) {
            Vertice v = (Vertice) atual.getElemento();
            if (!v.isVisitado() && v.getDistancia() < Double.POSITIVE_INFINITY) {
                if (melhor == null || v.getDistancia() < melhor.getDistancia()) {
                    melhor = v;
                }
            }
            atual = atual.getProximo();
        }
        return melhor;
    }

    //===========Dijkstra===========
    public Caminho caminhoMaisCurto(Dispositivo origemDispositivo, Dispositivo destinoDispositivo) {
        Vertice origem = procuraVertice(origemDispositivo);
        Vertice destino = procuraVertice(destinoDispositivo);
        if (origem == null || destino == null) {
            return new Caminho(new ListaDuplamenteLigada(), Double.POSITIVE_INFINITY);
        }

        limparMarcas();
        origem.setDistancia(0);

        while (true) {
            Vertice atual = verticeMaisProximoNaoVisitado();
            if (atual == null) {
                break; // nao ha mais vertices alcancaveis
            }
            atual.setVisitado(true);

            if (atual == destino) {
                break; // ja fixamos a menor distancia ao destino
            }

            // relaxar os vizinhos
            No no = atual.getAdjacentes().getPrimeiro();
            while (no != null) {
                Aresta a = (Aresta) no.getElemento();
                Vertice vizinho = a.getDestino();
                double novaDistancia = atual.getDistancia() + a.getPeso();
                if (novaDistancia < vizinho.getDistancia()) {
                    vizinho.setDistancia(novaDistancia);
                    vizinho.setAnterior(atual);
                }
                no = no.getProximo();
            }
        }

        if (destino.getDistancia() == Double.POSITIVE_INFINITY) {
            return new Caminho(new ListaDuplamenteLigada(), Double.POSITIVE_INFINITY); // sem rota
        }
        return new Caminho(reconstroiCaminho(destino), destino.getDistancia());
    }

    public ListaDuplamenteLigada getVertices() {
        return this.vertices;
    }
}