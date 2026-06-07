# Simulador de Rede — Versão Web

Conversão do projeto Java Swing **"Simulador de Rede"** (AED II — ISUTC) numa
aplicação **web**, mantendo as mesmas estruturas de dados implementadas
manualmente (Grafo, ArvoreAVL, Trie, TabelaHash, Fila, Pilha, ListaDuplamenteLigada).

A interface lembra uma versão simplificada do **Cisco Packet Tracer**: o
utilizador desenha a topologia arrastando dispositivos, liga-os com pesos
(latência/distância) e corre os algoritmos sobre o grafo resultante.

---

## 1. Arquitetura

```
SimuladorRedeWeb/
├── pom.xml                         → Spring Boot 2.7.18 (só spring-boot-starter-web)
├── src/main/java/com/
│   ├── SimuladorApplication.java   → arranque Spring Boot (@SpringBootApplication)
│   ├── auxiliar/                   → No, ListaDuplamenteLigada, Fila, Pilha, NoAVL, NoTrie
│   │                                 (REUTILIZADAS sem qualquer alteração)
│   ├── estruturas/                 → Vertice, Aresta, Caminho, ArvoreAVL, Trie, TabelaHash
│   │                                 (REUTILIZADAS sem alteração)
│   │   └── Grafo.java               → REUTILIZADO; apenas ACRESCENTADA remoção de
│   │                                 vértices/arestas (a lógica original intacta)
│   ├── modelo/Dispositivo.java     → ADAPTADO (campo IP + posição mutável)
│   ├── sistema/TopologiaService.java → NOVO: gere a topologia dinâmica
│   └── web/                         → NOVO: camada REST
│       ├── TopologiaController.java
│       └── dto/                     → objetos de transporte JSON
└── src/main/resources/
    ├── application.properties
    └── static/                     → frontend (HTML5 + CSS3 + JS puro)
        ├── index.html
        ├── style.css
        └── app.js
```

A **lógica de negócio** (`TopologiaService`) é um POJO puro, **sem dependências
do Spring**, o que permite testá-la isoladamente (ver `TesteLogica.java`).

---

## 2. Como executar

### Pré-requisitos
- **Java 11 ou superior** (testado com JDK 17)
- **Maven 3.6+**

### Arrancar a aplicação
Na raiz do projeto (`SimuladorRedeWeb/`):

```bash
mvn spring-boot:run
```

Depois abrir no navegador:

```
http://localhost:8080
```

Em alternativa, gerar o JAR e correr:

```bash
mvn clean package
java -jar target/simulador-rede-web-1.0.0.jar
```

---

## 3. Funcionalidades

### Criação de topologia
- Botões para adicionar **PC**, **Switch** e **Router**. Cada dispositivo recebe
  um **identificador único automático** (`PC1`, `Switch1`, `Router1`, …).
- Os identificadores **continuam únicos mesmo após remoções** — os contadores são
  monotónicos (se remover o `PC2`, o próximo PC será `PC3`, nunca reaproveita o 2).
- Dispositivos **arrastáveis** na área de desenho (SVG). A posição é gravada no
  servidor ao soltar.
- Campo de **IP** configurável por dispositivo no painel lateral.
- **Modo de ligação**: clicar num dispositivo, depois noutro, e indicar o **peso**
  da aresta (latência/distância) num diálogo. O peso é usado pelo Dijkstra.
- Clicar numa ligação remove-a; selecionar um dispositivo permite editá-lo/removê-lo.

### Algoritmos (no contexto de redes)
| Algoritmo | Uso em redes | Endpoint |
|-----------|--------------|----------|
| **BFS**  | menor número de **saltos** (hops) entre dois dispositivos | `GET /api/algoritmos/bfs` |
| **Dijkstra** | menor **latência/custo** total (usa os pesos) | `GET /api/algoritmos/dijkstra` |
| **DFS**  | **alcançabilidade** — que dispositivos são atingíveis a partir de um | `GET /api/algoritmos/dfs` |
| **Conectividade** | verifica (via DFS) se a rede é **totalmente conexa** | `GET /api/algoritmos/conectividade` |

Quando a rede tem componentes separados, o sistema apresenta exatamente:

> **A rede não está totalmente conectada. Existem componentes desconexos.**

A topologia de **exemplo** (cidades de Moçambique como routers) está desenhada
para que **BFS e Dijkstra deem caminhos diferentes** entre o `PC1` e o `PC3`:
existe uma ligação direta `Router1`–`Router4` com poucos saltos mas custo alto
(50), e um trajeto alternativo `Router1`–`Router2`–`Router3`–`Router4` com mais
saltos mas custo baixo (5+5+5). Assim o **BFS** escolhe a ligação direta (menos
saltos) e o **Dijkstra** escolhe o trajeto mais longo mas mais barato (menor custo).

---

## 4. API REST

Base: `http://localhost:8080/api`

| Método | Caminho | Descrição |
|--------|---------|-----------|
| `GET`    | `/topologia` | obter nós e arestas |
| `PUT`    | `/topologia` | substituir a topologia completa |
| `DELETE` | `/topologia` | limpar tudo |
| `POST`   | `/topologia/exemplo` | carregar topologia de exemplo (cidades de Moçambique) |
| `POST`   | `/topologia/guardar` | persistir em `topologia.json` |
| `POST`   | `/topologia/carregar` | recarregar de `topologia.json` |
| `POST`   | `/dispositivos?tipo=PC\|SWITCH\|ROUTER` | adicionar dispositivo (ID automático) |
| `PATCH`  | `/dispositivos/{id}` | atualizar IP/posição |
| `DELETE` | `/dispositivos/{id}` | remover dispositivo |
| `POST`   | `/arestas` | criar ligação `{origem, destino, peso}` |
| `DELETE` | `/arestas?origem=&destino=` | remover ligação |
| `GET`    | `/algoritmos/bfs?origem=&destino=` | menor nº de saltos |
| `GET`    | `/algoritmos/dijkstra?origem=&destino=` | menor custo |
| `GET`    | `/algoritmos/dfs?origem=` | alcançáveis |
| `GET`    | `/algoritmos/conectividade` | teste de conectividade |

A **persistência em JSON é opcional** (botões "Guardar"/"Carregar"); por defeito a
topologia vive em memória.

---

## 5. O que foi reutilizado vs. adaptado

Cumprindo o requisito de **não descartar** o código base e **reaproveitar as
estruturas quase sem alterações**:

- **Sem qualquer alteração:** `No`, `ListaDuplamenteLigada`, `Fila`, `Pilha`,
  `NoAVL`, `NoTrie`, `Vertice`, `Aresta`, `Caminho`, `ArvoreAVL`, `Trie`,
  `TabelaHash`.
- **`Grafo.java`** — toda a lógica original mantida verbatim (BFS em
  `caminhoMenosTrocos`, DFS em `dfs`/`alcancaveisDe`, Dijkstra O(V²) sem
  `PriorityQueue` em `caminhoMaisCurto`, `estaTotalmenteLigado`). Foram **apenas
  acrescentados** os métodos `removerAresta(...)` e `removerVertice(...)`,
  necessários porque a versão Swing não previa remoções e a topologia web é
  totalmente editável.
- **`Dispositivo.java`** — acrescentado o campo **`ip`** e tornadas mutáveis as
  coordenadas `x`/`y` e o `ip` (com *setters*), para suportar edição e arrasto na
  web. `nome` e `tipo` continuam `final`; `equals`/`hashCode` continuam baseados
  no nome.

O projeto Swing original encontra-se preservado na pasta
**`projeto-original-swing/`**, sem modificações.

---

## 6. Teste da lógica

O ficheiro `teste/TesteLogica.java` é um *harness* independente do Spring que
valida a camada reutilizada. A partir da raiz do projeto:

```bash
javac -d out $(find src/main/java/com/auxiliar src/main/java/com/estruturas src/main/java/com/modelo src/main/java/com/sistema -name '*.java') teste/TesteLogica.java
java -cp out TesteLogica
```

Cobre: IDs únicos automáticos e monotónicos, ligações/pesos, BFS, Dijkstra, DFS,
deteção de componentes desconexos, atualização de IP/posição, ordenação AVL,
autocomplete da Trie e o exemplo de Moçambique. Resultado esperado: **28 passou, 0 falhou**.
