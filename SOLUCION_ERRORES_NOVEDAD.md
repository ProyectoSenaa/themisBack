# Solución a Errores 400 al Responder Novedades

## Problemas Identificados y Soluciones

### ✅ 1. Error: "Cannot query field 'data' on type 'Response'"

**Problema:** El tipo `Response` no tenía el campo `data` definido en el schema.

**Solución Aplicada:** Se agregó el campo `data: String` al tipo `Response` en `/src/main/resources/schema/Response.graphqls`

```graphql
type Response {
    id: Long
    code: String
    message: String
    data: String  // ✅ AGREGADO
}
```

### ⚠️ 2. Error: Variable "$committeeId" tipo String! vs Long!

**Problema:** Estás enviando `committeeId` como `String!` pero la mutación `respondNoveltiesFromCommittee` espera `Long!`.

**Solución:** En tu cliente (frontend), debes cambiar el tipo de variable:

**ANTES (Incorrecto):**
```graphql
mutation RespondNovelties($committeeId: String!, $responses: [CommitteeNoveltyResponseInput!]!) {
  respondNoveltiesFromCommittee(committeeId: $committeeId, responses: $responses) {
    code
    message
    data
  }
}
```

**DESPUÉS (Correcto):**
```graphql
mutation RespondNovelties($committeeId: Long!, $responses: [CommitteeNoveltyResponseInput!]!) {
  respondNoveltiesFromCommittee(committeeId: $committeeId, responses: $responses) {
    code
    message
    data
  }
}
```

Y cuando envíes las variables, asegúrate de enviar un número, no un string:
```javascript
// INCORRECTO
{ committeeId: "123", responses: [...] }

// CORRECTO
{ committeeId: 123, responses: [...] }
```

### ⚠️ 3. Error: "Unknown type CommitteeNoveltyResponseInput"

**Problema:** El cliente Apollo tiene el schema cacheado o desactualizado.

**Soluciones:**

1. **Reinicia el servidor backend** para que cargue los schemas actualizados
2. **Refresca el schema en el cliente:**
   - Si usas Apollo Client, limpia el cache del cliente
   - En el navegador, refresca la página con Ctrl+Shift+R (hard refresh)
   - Si usas GraphQL Playground/Apollo Studio, usa el botón "Reload Schema"

## Estructura Correcta de CommitteeNoveltyResponseInput

El tipo está definido correctamente en `committeeEvent.graphqls`:

```graphql
input CommitteeNoveltyResponseInput {
    noveltyId: ID!
    studentId: Long!
    statusId: Long!
    observation: String!
    name: String!
}
```

## Ejemplo de Uso Correcto

```graphql
mutation RespondNovelties($committeeId: Long!, $responses: [CommitteeNoveltyResponseInput!]!) {
  respondNoveltiesFromCommittee(committeeId: $committeeId, responses: $responses) {
    code
    message
    data
  }
}
```

**Variables:**
```json
{
  "committeeId": 123,
  "responses": [
    {
      "noveltyId": "456",
      "studentId": 789,
      "statusId": 2,
      "observation": "Novedad aprobada",
      "name": "Juan Pérez"
    }
  ]
}
```

## Pasos Siguientes

1. ✅ El schema ya fue actualizado y compilado
2. ⚠️ Reinicia tu aplicación Spring Boot para cargar el nuevo schema
3. ⚠️ Actualiza tu cliente (frontend) para usar `Long!` en lugar de `String!` para committeeId
4. ⚠️ Limpia el cache del cliente Apollo si es necesario

## Comando para Reiniciar

```bash
cd /home/fabrica/themisBack/themis_backend
./gradlew bootRun
```

