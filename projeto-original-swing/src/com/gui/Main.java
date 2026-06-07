package com.gui;

import com.sistema.Sistema;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        Sistema sistema = new Sistema();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JanelaPrincipal janela = new JanelaPrincipal(sistema);
                janela.setVisible(true);
            }
        });
    }
}