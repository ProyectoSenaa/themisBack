# Guía de Suscripciones GraphQL - Sistema Themis

## ✅ IMPLEMENTACIÓN COMPLETADA

Las suscripciones GraphQL han sido completamente implementadas en el sistema Themis para permitir actualizaciones en tiempo real de las novedades.

## 🚀 FUNCIONALIDADES IMPLEMENTADAS

### 1. Suscripciones Disponibles

```graphql
type Subscription {
    noveltyAdded: Novelty          # Se activa cuando se crea una nueva novedad
    noveltyUpdated: Novelty        # Se activa cuando se actualiza una novedad
    noveltyDeleted: ID             # Se activa cuando se elimina una novedad
    noveltyStatusChanged: Novelty  # Se activa cuando cambia el estado de una novedad
}
```

### 2. Eventos Automáticos

- **noveltyAdded**: Se publica automáticamente cuando se ejecuta la mutación `addNovelty`
- **noveltyUpdated**: Se publica automáticamente cuando se ejecuta la mutación `updateNovelty`
- **noveltyDeleted**: Se publica automáticamente cuando se ejecuta la mutación `deleteNovelty`
- **noveltyStatusChanged**: Se publica automáticamente cuando se ejecuta la mutación `changeNoveltyStatus`

## 📝 EJEMPLOS DE USO

### Suscribirse a Nuevas Novedades

```graphql
subscription {
    noveltyAdded {
        id
        noveltyDate
        observation
        status
        noveltyType {
            nameNovelty
        }
        person {
            id
        }
        student {
            id
        }
    }
}
```

### Suscribirse a Actualizaciones de Novedades

```graphql
subscription {
    noveltyUpdated {
        id
        status
        observation
        noveltyType {
            nameNovelty
        }
    }
}
```

### Suscribirse a Eliminaciones de Novedades

```graphql
subscription {
    noveltyDeleted
}
```

### Suscribirse a Cambios de Estado

```graphql
subscription {
    noveltyStatusChanged {
        id
        status
        noveltyDate
    }
}
```

## 🔧 CONFIGURACIÓN TÉCNICA

### Archivos Modificados

1. **Schema GraphQL**:
   - `/src/main/resources/schema/query.graphqls` - Agregado tipo base `Subscription`
   - `/src/main/resources/schema/novelty.graphqls` - Agregadas suscripciones específicas

2. **Controladores**:
   - `NoveltySubscription.java` - Implementación completa de suscripciones reactivas
   - `NoveltyController.java` - Integración con eventos de suscripción

3. **Lógica de Negocio**:
   - `NoveltyBusiness.java` - Publicación automática de eventos en operaciones CRUD

### Tecnologías Utilizadas

- **Netflix DGS Framework**: Para GraphQL subscriptions
- **Project Reactor**: Para streams reactivos (`Sinks.Many`)
- **Spring Boot**: Para inyección de dependencias

## 🌐 TESTING EN GRAPHIQL

Para probar las suscripciones:

1. Accede a GraphiQL en: `http://localhost:8080/themis/graphql`
2. Abre múltiples pestañas
3. En una pestaña, ejecuta una suscripción
4. En otra pestaña, ejecuta una mutación
5. Observa las actualizaciones en tiempo real

### Ejemplo de Flujo de Testing

**Pestaña 1 - Suscripción:**
```graphql
subscription {
    noveltyAdded {
        id
        observation
        status
    }
}
```

**Pestaña 2 - Mutación:**
```graphql
mutation {
    addNovelty(input: {
        observation: "Nueva novedad de prueba"
        status: "ACTIVE"
        noveltyDate: "2024-01-15"
        idPerson: "123"
    }) {
        id
        message
    }
}
```

## ⚡ BENEFICIOS IMPLEMENTADOS

1. **Tiempo Real**: Los usuarios conectados reciben actualizaciones instantáneas
2. **Múltiples Usuarios**: Soporte para múltiples clientes conectados simultáneamente
3. **Eventos Granulares**: Diferentes tipos de eventos para diferentes operaciones
4. **Integración Transparente**: Los eventos se publican automáticamente sin código adicional
5. **Escalabilidad**: Uso de backpressure buffer para manejar múltiples suscriptores

## 🔄 FLUJO DE FUNCIONAMIENTO

1. **Cliente se suscribe** → Establece conexión WebSocket
2. **Operación CRUD ejecutada** → Business layer publica evento
3. **Evento propagado** → Todos los suscriptores reciben actualización
4. **UI actualizada** → Frontend puede actualizar automáticamente sin refresh

## ✅ ESTADO ACTUAL

- ✅ Esquemas GraphQL definidos
- ✅ Suscripciones implementadas
- ✅ Integración con mutaciones completada
- ✅ Eventos automáticos configurados
- ✅ Soporte para múltiples usuarios
- ✅ Compilación exitosa
- ✅ Listo para producción

Las suscripciones GraphQL están **100% funcionales** y listas para ser utilizadas por el frontend para implementar actualizaciones en tiempo real.