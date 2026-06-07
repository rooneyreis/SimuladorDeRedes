package com.auxiliar;

public class Fila {
    private No primeiro;
    private No ultimo;
    private int tamanho;

    public Fila() {}


    public void enqueue(Object elemento) {
        No novo = new No(elemento);
        if (isEmpty()) {
            primeiro = novo;
            ultimo = novo;
        } else {
            ultimo.setProximo(novo);
            ultimo = novo;
        }
        tamanho++;
    }


    public void dequeue() {
        if (isEmpty()) {
            throw new java.util.NoSuchElementException("Fila vazia");
        }
        primeiro = primeiro.getProximo();
        if (primeiro == null) {
            ultimo = null;
        }
        tamanho--;
    }


    public Object peek() {
        if (isEmpty()) {
            throw new java.util.NoSuchElementException("Fila vazia");
        }
        return primeiro.getElemento();
    }


    public Object peekAndDequeue() {
        Object elemento = peek();
        dequeue();
        return elemento;
    }


    public boolean isEmpty() {
        return tamanho == 0;
    }


    public int size() {
        return tamanho;
    }
}

