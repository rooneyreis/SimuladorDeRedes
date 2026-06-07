package com.web;

import com.modelo.Dispositivo;
import com.sistema.TopologiaService;
import com.sistema.TopologiaService.LigacaoSimples;
import com.sistema.TopologiaService.ResultadoCaminho;
import com.sistema.TopologiaService.ResultadoConectividade;
import com.web.dto.ArestaDTO;
import com.web.dto.AtualizaNoReq;
import com.web.dto.NoDTO;
import com.web.dto.NovaArestaReq;
import com.web.dto.TopologiaDTO;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API REST do simulador de rede.
 *
 * Endpoints principais:
 *   GET    /api/topologia                 -> obter nos + arestas
 *   PUT    /api/topologia                 -> substituir toda a topologia
 *   DELETE /api/topologia                 -> limpar
 *   POST   /api/topologia/exemplo         -> carregar exemplo (cidades MZ)
 *   POST   /api/topologia/guardar         -> persistir em ficheiro JSON
 *   POST   /api/topologia/carregar        -> ler do ficheiro JSON
 *
 *   POST   /api/dispositivos?tipo=pc      -> criar dispositivo (id automatico)
 *   PATCH  /api/dispositivos/{id}         -> atualizar IP / posicao
 *   DELETE /api/dispositivos/{id}         -> remover
 *
 *   POST   /api/arestas                   -> criar ligacao (origem,destino,peso)
 *   DELETE /api/arestas?origem=&destino=  -> remover ligacao
 *
 *   GET    /api/algoritmos/bfs?origem=&destino=
 *   GET    /api/algoritmos/dijkstra?origem=&destino=
 *   GET    /api/algoritmos/dfs?origem=
 *   GET    /api/algoritmos/conectividade
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TopologiaController {

    private final TopologiaService servico;
    private final ObjectMapper json = new ObjectMapper();
    private static final File FICHEIRO = new File("topologia.json");

    public TopologiaController(TopologiaService servico) {
        this.servico = servico;
    }

    // ---------------- Topologia ----------------

    @GetMapping("/topologia")
    public TopologiaDTO obterTopologia() {
        return montarTopologia();
    }

    @PutMapping("/topologia")
    public TopologiaDTO substituirTopologia(@RequestBody TopologiaDTO dto) {
        aplicarTopologia(dto);
        return montarTopologia();
    }

    @DeleteMapping("/topologia")
    public TopologiaDTO limpar() {
        servico.limpar();
        return montarTopologia();
    }

    @PostMapping("/topologia/exemplo")
    public TopologiaDTO carregarExemplo() {
        servico.carregarExemplo();
        return montarTopologia();
    }

    @PostMapping("/topologia/guardar")
    public ResponseEntity<Map<String, Object>> guardar() {
        try {
            json.writerWithDefaultPrettyPrinter().writeValue(FICHEIRO, montarTopologia());
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "mensagem", "Topologia guardada em " + FICHEIRO.getAbsolutePath()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "mensagem", "Erro ao guardar: " + e.getMessage()));
        }
    }

    @PostMapping("/topologia/carregar")
    public ResponseEntity<?> carregar() {
        if (!FICHEIRO.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("ok", false, "mensagem", "Nenhuma topologia guardada foi encontrada."));
        }
        try {
            TopologiaDTO dto = json.readValue(FICHEIRO, TopologiaDTO.class);
            aplicarTopologia(dto);
            return ResponseEntity.ok(montarTopologia());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "mensagem", "Erro ao carregar: " + e.getMessage()));
        }
    }

    // ---------------- Dispositivos ----------------

    @PostMapping("/dispositivos")
    public NoDTO criarDispositivo(@RequestParam String tipo) {
        Dispositivo d = servico.adicionarDispositivo(tipo);
        return NoDTO.de(d);
    }

    @PatchMapping("/dispositivos/{id}")
    public ResponseEntity<NoDTO> atualizarDispositivo(@PathVariable String id,
                                                      @RequestBody AtualizaNoReq req) {
        Dispositivo d = servico.atualizarDispositivo(id, req.getIp(), req.getX(), req.getY());
        if (d == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(NoDTO.de(d));
    }

    @DeleteMapping("/dispositivos/{id}")
    public ResponseEntity<Void> removerDispositivo(@PathVariable String id) {
        boolean removido = servico.removerDispositivo(id);
        return removido ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // ---------------- Arestas ----------------

    @PostMapping("/arestas")
    public ResponseEntity<ArestaDTO> criarAresta(@RequestBody NovaArestaReq req) {
        servico.adicionarAresta(req.getOrigem(), req.getDestino(), req.getPeso());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ArestaDTO(req.getOrigem(), req.getDestino(), req.getPeso()));
    }

    @DeleteMapping("/arestas")
    public ResponseEntity<Void> removerAresta(@RequestParam String origem,
                                              @RequestParam String destino) {
        servico.removerAresta(origem, destino);
        return ResponseEntity.noContent().build();
    }

    // ---------------- Algoritmos ----------------

    @GetMapping("/algoritmos/bfs")
    public Map<String, Object> bfs(@RequestParam String origem, @RequestParam String destino) {
        ResultadoCaminho r = servico.bfs(origem, destino);
        Map<String, Object> m = new HashMap<>();
        m.put("algoritmo", "BFS");
        m.put("descricao", "Caminho com menor numero de saltos");
        m.put("existe", r.isExiste());
        m.put("caminho", r.getNos());
        m.put("saltos", (int) r.getValor());
        m.put("mensagem", r.isExiste()
                ? "Caminho com " + (int) r.getValor() + " salto(s)."
                : "Nao existe caminho entre " + origem + " e " + destino + ".");
        return m;
    }

    @GetMapping("/algoritmos/dijkstra")
    public Map<String, Object> dijkstra(@RequestParam String origem, @RequestParam String destino) {
        ResultadoCaminho r = servico.dijkstra(origem, destino);
        Map<String, Object> m = new HashMap<>();
        m.put("algoritmo", "Dijkstra");
        m.put("descricao", "Caminho de menor custo (latencia/distancia)");
        m.put("existe", r.isExiste());
        m.put("caminho", r.getNos());
        m.put("custo", r.getValor());
        m.put("mensagem", r.isExiste()
                ? "Caminho de menor custo = " + r.getValor() + "."
                : "Nao existe caminho entre " + origem + " e " + destino + ".");
        return m;
    }

    @GetMapping("/algoritmos/dfs")
    public Map<String, Object> dfs(@RequestParam String origem) {
        List<String> alcancaveis = servico.dfsAlcancaveis(origem);
        Map<String, Object> m = new HashMap<>();
        m.put("algoritmo", "DFS");
        m.put("descricao", "Dispositivos alcancaveis a partir da origem");
        m.put("origem", origem);
        m.put("alcancaveis", alcancaveis);
        m.put("total", alcancaveis.size());
        return m;
    }

    @GetMapping("/algoritmos/conectividade")
    public Map<String, Object> conectividade() {
        ResultadoConectividade r = servico.testarConectividade();
        Map<String, Object> m = new HashMap<>();
        m.put("algoritmo", "DFS (teste de conectividade)");
        m.put("conectado", r.isConectado());
        m.put("mensagem", r.getMensagem());
        m.put("componentes", r.getComponentes());
        m.put("totalComponentes", r.getComponentes().size());
        return m;
    }

    // ---------------- Tratamento de erros ----------------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> erroPedido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("ok", false, "mensagem", e.getMessage()));
    }

    // ---------------- Helpers ----------------

    private TopologiaDTO montarTopologia() {
        List<NoDTO> nos = servico.obterNos().stream()
                .map(NoDTO::de)
                .collect(Collectors.toList());
        List<ArestaDTO> arestas = servico.obterArestas().stream()
                .map(ArestaDTO::de)
                .collect(Collectors.toList());
        return new TopologiaDTO(nos, arestas);
    }

    private void aplicarTopologia(TopologiaDTO dto) {
        List<Dispositivo> nos = new ArrayList<>();
        if (dto.getNos() != null) {
            for (NoDTO n : dto.getNos()) {
                nos.add(n.paraDispositivo());
            }
        }
        List<LigacaoSimples> arestas = new ArrayList<>();
        if (dto.getArestas() != null) {
            for (ArestaDTO a : dto.getArestas()) {
                arestas.add(a.paraLigacao());
            }
        }
        servico.substituirTopologia(nos, arestas);
    }
}
