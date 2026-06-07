package com.auxiliar;

public class ListaDuplamenteLigada {
    private No primeiro;
    private No ultimo;
    private int tamanho;

    public ListaDuplamenteLigada() {
        this.primeiro = null;
        this.ultimo = null;
        this.tamanho = 0;
    }

    public void adicionaInicio(Object element) {
        No novoNo = new No(element, this.primeiro);

        if (tamanho == 0) {
            this.primeiro = this.ultimo = novoNo;
        } else {
            this.primeiro.setAnterior(novoNo);
            this.primeiro = novoNo;
        }
        this.tamanho++;
    }

    public void adicionaFim(Object element) {
        No novoNo = new No(this.ultimo, element);

        if (this.tamanho == 0) {
            this.ultimo = this.primeiro = novoNo;
        } else {
            this.ultimo.setProximo(novoNo);
            this.ultimo = novoNo;
        }
        this.tamanho++;
    }

    public void adicionaPosicao(int posicao, Object element) {
        if (posicao < 0 || posicao > this.tamanho) {
            throw new IndexOutOfBoundsException("Posicao Invalida");
        }

        if (posicao == 0) {
            adicionaInicio(element);
            return;
        } else if (posicao == this.tamanho) {
            adicionaFim(element);
            return;
        } else {
            No atualNo = this.primeiro;

            for (int i = 0; i < posicao; i++) {
                atualNo = atualNo.getProximo();
            }

            No novoNo = new No(atualNo.getAnterior(), element, atualNo);
            atualNo.getAnterior().setProximo(novoNo);
            atualNo.setAnterior(novoNo);
        }
        this.tamanho++;
    }

    public void removeInicio() {
        if (this.tamanho == 0) {
            throw new IllegalArgumentException("Lista Vazia");
        }

        No removeNo = this.primeiro;
        this.primeiro = this.primeiro.getProximo();
        removeNo.setProximo(null);

        if (this.primeiro != null) {
            this.primeiro.setAnterior(null);
        } else {
            this.ultimo = null;
        }
        this.tamanho--;
    }

    public void removeFim() {
        if (this.tamanho == 0) {
            throw new IllegalArgumentException("Lista Vazia");
        }

        No removeNo = this.ultimo;
        this.ultimo = this.ultimo.getAnterior();
        removeNo.setAnterior(null);

        if (this.ultimo != null) {
            this.ultimo.setProximo(null);
        } else {
            this.primeiro = null;
        }
        this.tamanho--;
    }

    public void removePosicao(int posicao) {
        if (posicao < 0 || posicao >= this.tamanho) {
            throw new IndexOutOfBoundsException("Posicao invalida");
        }

        if (posicao == 0) {
            removeInicio();
            return;
        }

        if (posicao == this.tamanho - 1) {
            removeFim();
            return;
        }

        No atualNo = this.primeiro;

        for (int i = 0; i < posicao; i++) {
            atualNo = atualNo.getProximo();
        }

        atualNo.getAnterior().setProximo(atualNo.getProximo());
        atualNo.getProximo().setAnterior(atualNo.getAnterior());

        atualNo.setAnterior(null);
        atualNo.setProximo(null);

        this.tamanho--;
    }

    public Object pega(int posicao) {
        if (this.tamanho == 0) {
            throw new IllegalArgumentException("Lista vazia");
        }

        if (posicao < 0 || posicao >= this.tamanho) {
            throw new IndexOutOfBoundsException("Posicao invalida: " + posicao);
        }

        No atualNo = this.primeiro;
        for (int i = 0; i < posicao; i++) {
            atualNo = atualNo.getProximo();
        }
        return atualNo.getElemento();
    }

    public int tamanho() {
        return this.tamanho;
    }

    // Devolve o primeiro no da lista para percurso O(n) com getProximo().
    // Andar a partir daqui evita o O(n^2) de chamar pega(i) num ciclo.
    public No getPrimeiro() {
        return this.primeiro;
    }

    public boolean contem(Object elemento) {
        No atualNo = this.primeiro;

        int i = 0;
        while (i < this.tamanho) {
            if (elemento == null) {
                if (atualNo.getElemento() == null) {
                    return true;
                }
            } else if (atualNo.getElemento().equals(elemento)) {
                return true;
            }
            atualNo = atualNo.getProximo();
            i++;
        }
        return false;
    }
}