package com.auxiliar;

import java.util.HashMap;

public class NoTrie {
    private HashMap<Character, NoTrie> filhos;
    private boolean fimDePalavra;
    private String nomeOriginal; // so preenchido no no terminal

    public NoTrie() {
        this.filhos = new HashMap<Character, NoTrie>();
        this.fimDePalavra = false;
        this.nomeOriginal = null;
    }

    public HashMap<Character, NoTrie> getFilhos() {
        return this.filhos;
    }

    public boolean isFimDePalavra() {
        return this.fimDePalavra;
    }

    public void setFimDePalavra(boolean fimDePalavra) {
        this.fimDePalavra = fimDePalavra;
    }

    public String getNomeOriginal() {
        return this.nomeOriginal;
    }

    public void setNomeOriginal(String nomeOriginal) {
        this.nomeOriginal = nomeOriginal;
    }
}
