# Demo-Cassandra-ISD

## Lanzamiento y Verificación del Clúster

Para iniciar nuestro clúster, ejecutamos el siguiente comando:

```bash
# Crear y lanzar los contenedores en modo de segundo plano
sudo docker-compose up -d
```

Para asegurar que el clúster se inicie correctamente con recursos limitados, es recomendable lanzar los nodos secuencialmente:

```bash
# Iniciar solo el nodo semilla primero
sudo docker-compose up -d cassandra-node1

# Esperar a que el nodo 1 esté completamente inicializado (aproximadamente 2-3 minutos)
echo "Esperando a que el nodo semilla se inicialice..."
sleep 180

# Iniciar el segundo nodo
sudo docker-compose up -d cassandra-node2

# Esperar a que el nodo 2 se una al clúster
echo "Esperando a que el nodo 2 se una al clúster..."
sleep 120

# Iniciar el tercer nodo
sudo docker-compose up -d cassandra-node3

# Verificar el estado del clúster
sudo docker exec -it cassandra-node1 nodetool status
```

## Parte 1: Demostración del Manejo de Réplicas en Cassandra

Una de las características principales de Cassandra es su sistema de replicación, que asegura que los datos estén disponibles incluso cuando algunos nodos fallan.

### 1.1 Crear un keyspace con replicación

Primero, conectamos al clúster y creamos un keyspace que demuestre explícitamente la estrategia de replicación:

```bash
# Conectar al clúster usando cqlsh
sudo docker exec -it cassandra-node1 cqlsh
```

```sql
-- Crear un keyspace con factor de replicación 3 (todos los nodos tendrán una copia)
CREATE KEYSPACE replicacion_demo
WITH REPLICATION = {
   'class' : 'SimpleStrategy',
   'replication_factor' : 3
};

-- Usar el keyspace
USE replicacion_demo;

-- Crear una tabla para nuestra demostración
CREATE TABLE sensores (
   id UUID PRIMARY KEY,
   ubicacion TEXT,
   temperatura FLOAT,
   humedad FLOAT,
   tiempo_lectura TIMESTAMP
);

-- Insertar algunos datos
INSERT INTO sensores (id, ubicacion, temperatura, humedad, tiempo_lectura)
VALUES (uuid(), 'Sala Server 1', 22.5, 45.0, toTimestamp(now()));

INSERT INTO sensores (id, ubicacion, temperatura, humedad, tiempo_lectura)
VALUES (uuid(), 'Oficina Principal', 24.2, 38.5, toTimestamp(now()));

INSERT INTO sensores (id, ubicacion, temperatura, humedad, tiempo_lectura)
VALUES (uuid(), 'Laboratorio', 20.8, 52.3, toTimestamp(now()));
```

### 1.2 Verificar la replicación de datos

Para verificar que nuestros datos están realmente replicados en todos los nodos, podemos usar la herramienta nodetool:

```bash
# Ver en qué nodos se encuentran las réplicas
sudo docker exec -it cassandra-node1 nodetool getendpoints replicacion_demo sensores <uuid-de-alguna-fila>

# Consultar los datos para obtener el UUID
sudo docker exec -it cassandra-node1 cqlsh -e "SELECT * FROM replicacion_demo.sensores;"
```

### 1.3 Visualizar la distribución de datos

Para ver cómo Cassandra distribuye los datos entre los nodos:

```bash
# Ver el anillo de tokens y la distribución de datos
sudo docker exec -it cassandra-node1 nodetool ring
```