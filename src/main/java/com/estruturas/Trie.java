package com.estruturas;

import com.auxiliar.ListaDuplamenteLigada;
import com.auxiliar.NoTrie;

public class Trie {
    private NoTrie raiz;

    public Trie() {
        this.raiz = new NoTrie();
    }

    public void inserir(String nome) {
        NoTrie atual = this.raiz;
        String chave = nome.toLowerCase(); // percorremos em minusculas

        for (int i = 0; i < chave.length(); i++) {
            char c = chave.charAt(i);
            NoTrie filho = atual.getFilhos().get(c);
            if (filho == null) {            // ainda nao existe ramo para esta letra
                filho = new NoTrie();
                atual.getFilhos().put(c, filho);
            }
            atual = filho;                  // desce
        }

        atual.setFimDePalavra(true);        // aqui termina um nome
        atual.setNomeOriginal(nome);        // guarda "Polana", nao "polana"
    }

    public ListaDuplamenteLigada sugestoes(String prefixo) {
        ListaDuplamenteLigada resultado = new ListaDuplamenteLigada();
        NoTrie atual = this.raiz;
        String chave = prefixo.toLowerCase();

        // 1. desce ate ao fim do prefixo
        for (int i = 0; i < chave.length(); i++) {
            char c = chave.charAt(i);
            NoTrie filho = atual.getFilhos().get(c);
            if (filho == null) {
                return resultado; // nenhum nome comeca por este prefixo: lista vazia
            }
            atual = filho;
        }

        // 2. a partir daqui, recolhe todos os nomes da subarvore
        recolhe(atual, resultado);
        return resultado;
    }

    // Percorre a subarvore e junta os nomes completos (fimDePalavra).
    private void recolhe(NoTrie no, ListaDuplamenteLigada resultado) {
        if (no.isFimDePalavra()) {
            resultado.adicionaFim(no.getNomeOriginal());
        }
        for (NoTrie filho : no.getFilhos().values()) {
            recolhe(filho, resultado);
        }
    }

    public void remover(String nome) {
        removerRec(this.raiz, nome.toLowerCase(), 0);
    }

    // Devolve true se 'no' pode ser podado pelo pai.
    private boolean removerRec(NoTrie no, String chave, int indice) {
        if (indice == chave.length()) {
            if (!no.isFimDePalavra()) {
                return false; // o nome nem estava na Trie
            }
            no.setFimDePalavra(false);
            no.setNomeOriginal(null);
            return no.getFilhos().isEmpty(); // poda-se se nao tiver filhos
        }

        char c = chave.charAt(indice);
        NoTrie filho = no.getFilhos().get(c);
        if (filho == null) {
            return false; // o nome nao existe
        }

        boolean podarFilho = removerRec(filho, chave, indice + 1);
        if (podarFilho) {
            no.getFilhos().remove(c);
        }

        return !no.isFimDePalavra() && no.getFilhos().isEmpty();
    }
}