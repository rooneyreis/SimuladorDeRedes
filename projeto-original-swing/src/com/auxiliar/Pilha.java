package com.auxiliar;

import java.util.NoSuchElementException;

public class Pilha {
    private No topo;
    private int tamanho;

    public Pilha() {
    }

    public int getTamanho() {
        return this.tamanho;
    }


    public void push(Object elemento) {
        No novo = new No(elemento);

        if (isEmpty()) {
            this.topo = novo;
        } else {
            novo.setProximo(this.topo);
            this.topo = novo;
        }
        this.tamanho++;
    }


    public void pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("Pilha vazia");
        }
        this.topo = this.topo.getProximo();
        this.tamanho--;
    }

    public Object peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Pilha vazia");
        }
        return this.topo.getElemento();
    }

    public Object peekAndPop() {
        Object elemento = peek();
        pop();
        return elemento;
    }

    public boolean isEmpty() {
        return this.tamanho == 0;
    }

    public int size() {
        return this.tamanho;
    }

}