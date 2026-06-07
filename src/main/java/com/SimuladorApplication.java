package com;

import com.sistema.TopologiaService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Ponto de arranque do Simulador de Rede (versao web).
 *
 * Depois de arrancar, abrir no browser:  http://localhost:8080
 */
@SpringBootApplication
public class SimuladorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimuladorApplication.class, args);
    }

    /**
     * Regista o TopologiaService como bean unico (singleton) partilhado por
     * todos os pedidos. Como o servico nao tem dependencias do Spring, e
     * criado aqui manualmente.
     */
    @Bean
    public TopologiaService topologiaService() {
        return new TopologiaService();
    }
}
