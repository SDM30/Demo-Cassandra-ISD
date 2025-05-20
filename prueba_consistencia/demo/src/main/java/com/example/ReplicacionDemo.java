package com.example;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.time.Instant;

public class ReplicacionDemo {
    
    private CqlSession session;
    
    public void conectar() {
        System.out.println("Conectando al clúster de Cassandra...");
        session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress("0.0.0.0", 9042))
                .withLocalDatacenter("datacenter1")
                .build();
        System.out.println("Conexión establecida correctamente");
    }
    
    public void crearEsquema() {
        System.out.println("Creando keyspace con factor de replicación 3...");
        session.execute("CREATE KEYSPACE IF NOT EXISTS replicacion_demo " +
                      "WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor' : 3}");
        
        System.out.println("Usando el keyspace replicacion_demo...");
        session.execute("USE replicacion_demo");
        
        System.out.println("Eliminando tabla de sensores si existe...");
        session.execute("DROP TABLE IF EXISTS sensores");
        
        System.out.println("Creando tabla de sensores...");
        session.execute("CREATE TABLE sensores (" +
                      "id UUID PRIMARY KEY, " +
                      "ubicacion TEXT, " +
                      "temperatura FLOAT, " +
                      "humedad FLOAT, " +
                      "tiempo_lectura TIMESTAMP)");
    }
    
    public void insertarDatos() {
        System.out.println("\n--- Insertando datos en la tabla sensores ---");
        
        // Preparar statement para la inserción
        PreparedStatement prepared = session.prepare(
                "INSERT INTO sensores (id, ubicacion, temperatura, humedad, tiempo_lectura) " +
                "VALUES (?, ?, ?, ?, ?)");
        
        // Insertar datos para Sala Server 1
        BoundStatement stmt1 = prepared.bind()
                .setUuid(0, UUID.randomUUID())
                .setString(1, "Sala Server 1")
                .setFloat(2, 22.5f)
                .setFloat(3, 45.0f)
                .setInstant(4, Instant.now());
        
        // Insertar datos para Oficina Principal
        BoundStatement stmt2 = prepared.bind()
                .setUuid(0, UUID.randomUUID())
                .setString(1, "Oficina Principal")
                .setFloat(2, 24.2f)
                .setFloat(3, 38.5f)
                .setInstant(4, Instant.now());
        
        // Insertar datos para Laboratorio
        BoundStatement stmt3 = prepared.bind()
                .setUuid(0, UUID.randomUUID())
                .setString(1, "Laboratorio")
                .setFloat(2, 20.8f)
                .setFloat(3, 52.3f)
                .setInstant(4, Instant.now());
        
        // Ejecutar las inserciones con diferentes niveles de consistencia
        session.execute(stmt1.setConsistencyLevel(ConsistencyLevel.ONE));
        System.out.println("Datos insertados para Sala Server 1 con nivel de consistencia ONE");
        
        session.execute(stmt2.setConsistencyLevel(ConsistencyLevel.QUORUM));
        System.out.println("Datos insertados para Oficina Principal con nivel de consistencia QUORUM");
        
        session.execute(stmt3.setConsistencyLevel(ConsistencyLevel.ALL));
        System.out.println("Datos insertados para Laboratorio con nivel de consistencia ALL");
    }
    
    public void consultarDatos() {
        System.out.println("\n--- Consultando datos de sensores ---");
        
        // Consultar con diferentes niveles de consistencia
        SimpleStatement stmt1 = SimpleStatement.builder("SELECT * FROM sensores")
                .setConsistencyLevel(ConsistencyLevel.ONE)
                .build();
        
        ResultSet resultados = session.execute(stmt1);
        System.out.println("\nDatos recuperados con nivel de consistencia ONE:");
        for (Row row : resultados) {
            System.out.printf("ID: %s, Ubicación: %s, Temperatura: %.1f°C, Humedad: %.1f%%, Tiempo: %s\n",
                    row.getUuid("id"),
                    row.getString("ubicacion"),
                    row.getFloat("temperatura"),
                    row.getFloat("humedad"),
                    row.getInstant("tiempo_lectura"));
        }
        
        // Contar registros
        SimpleStatement stmt2 = SimpleStatement.builder("SELECT COUNT(*) AS total FROM sensores")
                .setConsistencyLevel(ConsistencyLevel.QUORUM)
                .build();
        
        Row resultado = session.execute(stmt2).one();
        System.out.println("\nTotal de registros (QUORUM): " + resultado.getLong("total"));
    }
    
    public void cerrar() {
        if(session != null) {
            session.close();
            System.out.println("\nConexión cerrada");
        }
    }
    
    public static void main(String[] args) {
        ReplicacionDemo demo = new ReplicacionDemo();
        
        try {
            // Conectar al clúster
            demo.conectar();
            
            // Crear keyspace y tablas
            demo.crearEsquema();
            
            // Insertar datos con diferentes niveles de consistencia
            demo.insertarDatos();
            
            // Consultar los datos insertados
            demo.consultarDatos();
            
        } catch (Exception e) {
            System.err.println("Error en la demostración: " + e.getMessage());
            e.printStackTrace();
        } finally {
            demo.cerrar();
        }
    }
}