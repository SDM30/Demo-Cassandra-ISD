package com.example;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.time.Instant;
import java.time.Duration;

public class ConsistencyDemo {
    
    private CqlSession session;
    
    public void conectar() {
        System.out.println("Conectando al clúster de Cassandra...");
        session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress("0.0.0.0", 9042))
                .withLocalDatacenter("datacenter1")
                .withConfigLoader(
                    DriverConfigLoader.programmaticBuilder()
                        .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(30))
                        .withDuration(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, Duration.ofSeconds(30))
                        .withDuration(DefaultDriverOption.CONTROL_CONNECTION_TIMEOUT, Duration.ofSeconds(30))
                        .withDuration(DefaultDriverOption.METADATA_SCHEMA_REQUEST_TIMEOUT, Duration.ofSeconds(30))
                        .build())
                .build();
        System.out.println("Conexión establecida correctamente");
    }
    
    public void prepararEsquema() {
        System.out.println("Creando keyspace con factor de replicación 3...");
        session.execute("CREATE KEYSPACE IF NOT EXISTS prueba_consistencia " +
                      "WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor' : 3}");
        
        System.out.println("Usando el keyspace prueba_consistencia...");
        session.execute("USE prueba_consistencia");
        
        System.out.println("Creando tabla datos_prueba si no existe...");
        session.execute("CREATE TABLE IF NOT EXISTS datos_prueba (" +
                      "test_id UUID, " +
                      "valor TEXT, " +
                      "PRIMARY KEY (test_id, valor))");
    }
    
    public void ejecutarPrueba(ConsistencyLevel nivelConsistencia, int operaciones) throws InterruptedException {
        UUID testId = UUID.randomUUID();
        System.out.println("\n--- Prueba " + nivelConsistencia + " ---");
        System.out.println("ID de prueba: " + testId);
        
        // Preparar statements
        SimpleStatement escritura = SimpleStatement.builder(
                "INSERT INTO datos_prueba (test_id, valor) VALUES (?, ?)")
                .addPositionalValue(testId)
                .addPositionalValue(UUID.randomUUID().toString())
                .setConsistencyLevel(nivelConsistencia)
                .build();
        
        SimpleStatement lectura = SimpleStatement.builder(
                "SELECT COUNT(*) AS total FROM datos_prueba WHERE test_id = ?")
                .addPositionalValue(testId)
                .setConsistencyLevel(nivelConsistencia)
                .build();
        
        // Ejecutar escrituras
        Instant inicioEscritura = Instant.now();
        for(int i = 0; i < operaciones; i++) {
            session.execute(escritura);
        }
        Duration tiempoEscritura = Duration.between(inicioEscritura, Instant.now());
        
        // Pequeña pausa para sincronización
        Thread.sleep(2000);
        
        // Ejecutar lectura
        Instant inicioLectura = Instant.now();
        Row resultado = session.execute(lectura).one();
        Duration tiempoLectura = Duration.between(inicioLectura, Instant.now());
        
        // Mostrar resultados
        System.out.println("Escrituras (" + operaciones + "): " + tiempoEscritura.toMillis() + " ms");
        System.out.println("Lectura: " + tiempoLectura.toMillis() + " ms");
        System.out.println("Registros insertados: " + resultado.getLong("total"));
    }
    
    public void limpiar() {
        System.out.println("\nLimpiando tabla de datos_prueba...");
        session.execute("TRUNCATE prueba_consistencia.datos_prueba");
    }
    
    public void cerrar() {
        if(session != null) {
            session.close();
            System.out.println("\nConexión cerrada");
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        ConsistencyDemo demo = new ConsistencyDemo();
        
        try {
            demo.conectar();
            demo.prepararEsquema();
            
            // Ejecutar pruebas en orden descendente de consistencia
            demo.ejecutarPrueba(ConsistencyLevel.ALL, 100);
            demo.limpiar();
            
            demo.ejecutarPrueba(ConsistencyLevel.QUORUM, 100);
            demo.limpiar();
            
            demo.ejecutarPrueba(ConsistencyLevel.ONE, 100);
            
        } catch (Exception e) {
            System.err.println("Error en la demostración: " + e.getMessage());
            e.printStackTrace();
        } finally {
            demo.cerrar();
        }
    }
}