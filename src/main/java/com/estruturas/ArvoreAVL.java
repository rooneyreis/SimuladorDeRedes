package com.estruturas;

import com.auxiliar.ListaDuplamenteLigada;
import com.auxiliar.NoAVL;
import com.modelo.Dispositivo;

public class ArvoreAVL {
    private NoAVL raiz;

    public ArvoreAVL() {
        this.raiz = null;
    }

    // Altura guardada no no (0 se o no nao existe).
    private int altura(NoAVL no) {
        if (no == null) {
            return 0;
        }
        return no.getAltura();
    }

    // Recalcula a altura de um no a partir das alturas dos filhos.
    private void atualizaAltura(NoAVL no) {
        int he = altura(no.getEsquerda());
        int hd = altura(no.getDireita());
        no.setAltura(1 + Math.max(he, hd));
    }

    // Fator de balanceamento = altura(esquerda) - altura(direita).
    private int fatorBalanceamento(NoAVL no) {
        if (no == null) {
            return 0;
        }
        return altura(no.getEsquerda()) - altura(no.getDireita());
    }

    private NoAVL rotacaoDireita(NoAVL y) {
        NoAVL x = y.getEsquerda();
        NoAVL T2 = x.getDireita();

        // roda
        x.setDireita(y);
        y.setEsquerda(T2);

        // atualiza alturas: primeiro o y (que desceu), depois o x (nova raiz)
        atualizaAltura(y);
        atualizaAltura(x);

        return x; // x e a nova raiz desta subarvore
    }

    private NoAVL rotacaoEsquerda(NoAVL x) {
        NoAVL y = x.getDireita();
        NoAVL T2 = y.getEsquerda();

        y.setEsquerda(x);
        x.setDireita(T2);

        atualizaAltura(x);
        atualizaAltura(y);

        return y;
    }

    // Reequilibra o no se necessario e devolve a (possivelmente nova) raiz da subarvore.
    private NoAVL balancear(NoAVL no) {
        if (no == null) {
            return null;
        }

        atualizaAltura(no);
        int fb = fatorBalanceamento(no);

        // pesado a esquerda
        if (fb > 1) {
            if (fatorBalanceamento(no.getEsquerda()) < 0) {
                // Esquerda-Direita: roda o filho esquerdo a esquerda (vira Esquerda-Esquerda)
                no.setEsquerda(rotacaoEsquerda(no.getEsquerda()));
            }
            // Esquerda-Esquerda
            return rotacaoDireita(no);
        }

        // pesado a direita
        if (fb < -1) {
            if (fatorBalanceamento(no.getDireita()) > 0) {
                // Direita-Esquerda: roda o filho direito a direita (vira Direita-Direita)
                no.setDireita(rotacaoDireita(no.getDireita()));
            }
            // Direita-Direita
            return rotacaoEsquerda(no);
        }

        // ja equilibrado (-1, 0 ou +1)
        return no;
    }

    public void inserir(Dispositivo dispositivo) {
        this.raiz = inserir(this.raiz, dispositivo);
    }

    private NoAVL inserir(NoAVL no, Dispositivo dispositivo) {
        // 1. insercao BST normal, ordenada por nome
        if (no == null) {
            return new NoAVL(dispositivo);
        }

        int cmp = dispositivo.getNome().compareToIgnoreCase(no.getLocal().getNome());
        if (cmp < 0) {
            no.setEsquerda(inserir(no.getEsquerda(), dispositivo));
        } else if (cmp > 0) {
            no.setDireita(inserir(no.getDireita(), dispositivo));
        } else {
            return no; // nome ja existe: nao duplica
        }

        // 2. reequilibra na subida
        return balancear(no);
    }

    public ListaDuplamenteLigada inOrder() {
        ListaDuplamenteLigada resultado = new ListaDuplamenteLigada();
        inOrder(this.raiz, resultado);
        return resultado;
    }
    private void inOrder(NoAVL no, ListaDuplamenteLigada resultado) {
        if (no == null) {
            return;
        }
        inOrder(no.getEsquerda(), resultado);
        resultado.adicionaFim(no.getLocal());
        inOrder(no.getDireita(), resultado);
    }

    public Dispositivo pesquisar(String nome) {
        NoAVL no = pesquisarRec(this.raiz, nome);
        return (no == null) ? null : no.getLocal();
    }
    private NoAVL pesquisarRec(NoAVL no, String nome) {
        if (no == null) {
            return null; // nao encontrado
        }
        int cmp = nome.compareToIgnoreCase(no.getLocal().getNome());
        if (cmp < 0) {
            return pesquisarRec(no.getEsquerda(), nome);
        } else if (cmp > 0) {
            return pesquisarRec(no.getDireita(), nome);
        }
        return no; // encontrado
    }

    public boolean existe(String nome) {
        return pesquisar(nome) != null;
    }

    // Menor no de uma subarvore (o mais a esquerda) = sucessor in-order.
    private NoAVL menorNo(NoAVL no) {
        NoAVL atual = no;
        while (atual.getEsquerda() != null) {
            atual = atual.getEsquerda();
        }
        return atual;
    }

    public void remover(String nome) {
        this.raiz = removerRec(this.raiz, nome);
    }

    private NoAVL removerRec(NoAVL no, String nome) {
        if (no == null) {
            return null; // o nome nao existe na arvore
        }

        int cmp = nome.compareToIgnoreCase(no.getLocal().getNome());
        if (cmp < 0) {
            no.setEsquerda(removerRec(no.getEsquerda(), nome));
        } else if (cmp > 0) {
            no.setDireita(removerRec(no.getDireita(), nome));
        } else {
            // no encontrado
            if (no.getEsquerda() == null) {
                return no.getDireita();   // casos 1 e 2: devolve o filho direito (ou null)
            } else if (no.getDireita() == null) {
                return no.getEsquerda();  // caso 2: so tem filho esquerdo
            }
            // caso 3: dois filhos -> sucessor in-order
            NoAVL sucessor = menorNo(no.getDireita());
            no.setLocal(sucessor.getLocal()); // copia o dado do sucessor para aqui
            no.setDireita(removerRec(no.getDireita(), sucessor.getLocal().getNome()));
        }

        // reequilibra na subida (igual a insercao)
        return balancear(no);
    }
}
