import com.sistema.TopologiaService;
import com.sistema.TopologiaService.ResultadoCaminho;
import com.sistema.TopologiaService.ResultadoConectividade;
import com.modelo.Dispositivo;

import java.util.List;

public class TesteLogica {
    static int passou = 0, falhou = 0;

    static void check(String nome, boolean cond) {
        if (cond) { passou++; System.out.println("  OK   " + nome); }
        else      { falhou++; System.out.println("  FALHA " + nome); }
    }

    public static void main(String[] args) {
        TopologiaService s = new TopologiaService();

        System.out.println("== 1. IDs unicos automaticos ==");
        Dispositivo pc1 = s.adicionarDispositivo("pc");
        Dispositivo pc2 = s.adicionarDispositivo("pc");
        Dispositivo sw1 = s.adicionarDispositivo("switch");
        Dispositivo r1  = s.adicionarDispositivo("router");
        check("PC1 gerado", pc1.getNome().equals("PC1"));
        check("PC2 gerado", pc2.getNome().equals("PC2"));
        check("Switch1 gerado", sw1.getNome().equals("Switch1"));
        check("Router1 gerado", r1.getNome().equals("Router1"));

        System.out.println("== 2. Unicidade apos remocao (monotonico) ==");
        s.removerDispositivo("PC2");
        Dispositivo pc3 = s.adicionarDispositivo("pc");
        check("Apos remover PC2, proximo e PC3 (nao reutiliza PC2)", pc3.getNome().equals("PC3"));
        check("PC2 ja nao existe", !s.existe("PC2"));

        System.out.println("== 3. Ligacoes e pesos ==");
        s.adicionarAresta("PC1", "Switch1", 1);
        s.adicionarAresta("Switch1", "Router1", 1);
        s.adicionarAresta("Router1", "PC3", 5);
        check("3 arestas registadas", s.obterArestas().size() == 3);
        check("Sem duplicados nas arestas (nao-dirigido)", s.obterArestas().size() == 3);

        System.out.println("== 4. BFS (menos saltos) PC1 -> PC3 ==");
        ResultadoCaminho bfs = s.bfs("PC1", "PC3");
        check("BFS encontra rota", bfs.isExiste());
        check("BFS caminho PC1->Switch1->Router1->PC3",
                bfs.getNos().equals(List.of("PC1", "Switch1", "Router1", "PC3")));
        check("BFS conta 3 saltos", bfs.getValor() == 3);

        System.out.println("== 5. Dijkstra (menor custo) ==");
        // Adiciona caminho alternativo mais curto em custo: PC1 - Router1 (peso 2)
        s.adicionarAresta("PC1", "Router1", 2);
        ResultadoCaminho dij = s.dijkstra("PC1", "PC3");
        check("Dijkstra encontra rota", dij.isExiste());
        // PC1->Router1 (2) ->PC3 (5) = 7  vs  PC1->Switch1(1)->Router1(1)->PC3(5)=7 -> empate
        check("Dijkstra custo = 7", dij.getValor() == 7.0);

        System.out.println("== 6. DFS alcancaveis ==");
        List<String> alc = s.dfsAlcancaveis("PC1");
        check("DFS alcanca 4 nos a partir de PC1", alc.size() == 4);

        System.out.println("== 7. Conectividade: rede ligada ==");
        ResultadoConectividade c1 = s.testarConectividade();
        check("Rede conectada", c1.isConectado());
        check("Um unico componente", c1.getComponentes().size() == 1);

        System.out.println("== 8. Conectividade: criar no isolado ==");
        Dispositivo pcX = s.adicionarDispositivo("pc"); // PC4, sem ligacoes
        ResultadoConectividade c2 = s.testarConectividade();
        check("Rede NAO conectada", !c2.isConectado());
        check("Mensagem de componentes desconexos",
                c2.getMensagem().contains("nao esta totalmente conectada"));
        check("Dois componentes", c2.getComponentes().size() == 2);

        System.out.println("== 9. Atualizar IP e posicao ==");
        s.atualizarDispositivo("PC1", "10.0.0.99", 123.0, 456.0);
        Dispositivo atualizado = s.obterNos().stream()
                .filter(d -> d.getNome().equals("PC1")).findFirst().orElse(null);
        check("IP atualizado", atualizado != null && atualizado.getIp().equals("10.0.0.99"));
        check("X atualizado", atualizado.getX() == 123.0);
        check("Y atualizado", atualizado.getY() == 456.0);

        System.out.println("== 10. AVL ordena os nos ==");
        List<Dispositivo> ord = s.obterNos();
        boolean ordenado = true;
        for (int i = 1; i < ord.size(); i++) {
            if (ord.get(i - 1).getNome().compareToIgnoreCase(ord.get(i).getNome()) > 0) ordenado = false;
        }
        check("Lista ordenada pela AVL (inOrder)", ordenado);

        System.out.println("== 11. Trie autocomplete ==");
        List<String> sug = s.sugestoes("PC");
        check("Trie sugere todos os PCs por prefixo", sug.size() >= 3);

        System.out.println("== 12. Remover aresta ==");
        int antes = s.obterArestas().size();
        s.removerAresta("PC1", "Router1");
        check("Aresta removida", s.obterArestas().size() == antes - 1);

        System.out.println("== 13. Exemplo Mocambique ==");
        s.carregarExemplo();
        check("Exemplo: 10 dispositivos", s.obterNos().size() == 10);
        check("Exemplo: rede conectada", s.testarConectividade().isConectado());
        ResultadoCaminho rota = s.dijkstra("PC1", "PC3");
        check("Exemplo: rota PC1->PC3 existe", rota.isExiste());

        System.out.println("\n=========================================");
        System.out.println("RESULTADO: " + passou + " passou, " + falhou + " falhou");
        System.out.println("=========================================");
        if (falhou > 0) System.exit(1);
    }
}
