# Span Link Implementation Flow - VertxWebsocketSpanLinkTest

This document explains the complete flow of span link creation in the vertx-websocket component, from client request to distributed tracing, using `VertxWebsocketSpanLinkTest` as an example.

---

## Overview

**Goal**: Link WebSocket message spans back to the original HTTP upgrade request span using OpenTelemetry span links.

**Test Scenario**:
- Client establishes WebSocket connection via HTTP upgrade
- Client sends 2 messages
- Timer broadcasts 1 message to all connected clients (sendToAll=true)
- All spans should have FOLLOWS_FROM links to the HTTP upgrade span

---

## Phase 1: Application Startup & Route Initialization

### 1.1 Quarkus Bootstrapping
```
Quarkus Bootstrap
  ↓
CamelBootstrapRecorder.startCamel()
  ↓
CamelContext initialization
  ↓
Route definitions loaded from VertxWebsocketSpanLinkRoutes
```

### 1.2 Consumer Creation (Modified Code)

**File**: `camel-quarkus/extensions/vertx-websocket/runtime/src/main/java/org/apache/camel/quarkus/component/vertx/websocket/VertxWebsocketRecorder.java`

```java
QuarkusVertxWebsocketEndpoint.createConsumer(Processor processor)
```

**When**: During route initialization when Camel creates the consumer for `from("vertx-websocket:///span-link-test")`

**What it does**:
- Overrides default consumer creation
- Returns custom `QuarkusVertxWebsocketConsumer` instead of default `VertxWebsocketConsumer`

**Call Stack**:
```
CamelContext.addRoutes()
  ↓
RouteDefinition.addRoutes()
  ↓
VertxWebsocketEndpoint.createConsumer()  ← OVERRIDDEN
  ↓
new QuarkusVertxWebsocketConsumer(endpoint, processor)
```

---

## Phase 2: HTTP Upgrade Request (WebSocket Handshake)

### 2.1 Client Initiates Connection

**Test Code**:
```java
WebSocketClient client = vertx.createWebSocketClient();
client.connect(8081, "localhost", "/span-link-test", ...)
```

### 2.2 HTTP Request Arrives at Vert.x Server

**File**: `camel/components/camel-vertx/camel-vertx-websocket/src/main/java/org/apache/camel/component/vertx/websocket/VertxWebsocketHost.java`

```
HTTP Upgrade Request arrives
  ↓
Vert.x Router matches route
  ↓
VertxWebsocketHost route handler
```

### 2.3 Capture HTTP Upgrade Span Context (Modified Code)

**Method**: `VertxWebsocketHost.captureHttpUpgradeSpanContext(RoutingContext)`

**When**: IMMEDIATELY when HTTP upgrade request arrives, BEFORE WebSocket upgrade completes

**What it does**:
```java
// Uses reflection to avoid compile-time dependency on OpenTelemetry
Class<?> contextClass = Class.forName("io.opentelemetry.context.Context");
Object otelContext = contextClass.getMethod("current").invoke(null);

Class<?> spanClass = Class.forName("io.opentelemetry.api.trace.Span");
Object currentSpan = spanClass.getMethod("fromContext", contextClass).invoke(null, otelContext);

Object spanContext = spanClass.getMethod("getSpanContext").invoke(currentSpan);

if (spanContext != null && spanContext.isValid()) {
    routingContext.put(HANDSHAKE_SPAN_CONTEXT_KEY, spanContext);
}
```

**Why this timing is critical**:
- The HTTP request span is ACTIVE only during the HTTP upgrade
- Once WebSocket is established, the HTTP span ends
- We must capture the span context NOW or lose it forever

**Call Stack**:
```
Vert.x HTTP Server
  ↓
Router.handle(RoutingContext)
  ↓
route.handler(routingContext -> {
    captureHttpUpgradeSpanContext(routingContext);  ← NEW CODE
    ...
})
```

### 2.4 WebSocket Peer Creation

**File**: `camel/components/camel-vertx/camel-vertx-websocket/src/main/java/org/apache/camel/component/vertx/websocket/VertxWebsocketHost.java`

```java
VertxWebsocketPeer peer = new VertxWebsocketPeer(webSocket, path);

// Store the HTTP upgrade span context in the peer
Object spanContext = routingContext.get(HANDSHAKE_SPAN_CONTEXT_KEY);
if (spanContext != null) {
    peer.setHandshakeSpanContext(spanContext);  ← NEW CODE
}

connectedPeers.add(peer);
```

**File**: `camel/components/camel-vertx/camel-vertx-websocket/src/main/java/org/apache/camel/component/vertx/websocket/VertxWebsocketPeer.java`

**Modified Code**:
```java
private Object handshakeSpanContext;  // NEW FIELD

public void setHandshakeSpanContext(Object spanContext) {
    this.handshakeSpanContext = spanContext;
}

public Object getHandshakeSpanContext() {
    return handshakeSpanContext;
}
```

**Result**: The HTTP upgrade span context is now stored in the peer object for later use.

---

## Phase 3: Client Sends WebSocket Messages (Consumer Spans)

### 3.1 Message Arrives

**Test Code**:
```java
client.writeTextMessage("Message 1");
```

### 3.2 Consumer Processes Message

```
WebSocket message arrives at Vert.x
  ↓
VertxWebsocketConsumer.onMessage()
  ↓
Exchange created
  ↓
Processor.process(exchange)
  ↓
OpenTelemetryTracer creates Consumer span
```

### 3.3 Span Decorator Extraction (Modified Code)

**File**: `camel/components/camel-telemetry/src/main/java/org/apache/camel/telemetry/decorators/VertxWebsocketSpanDecorator.java`

**When**: OpenTelemetryTracer needs to extract span context for span creation

**Method**: `VertxWebsocketSpanDecorator.getExtractor(Exchange exchange)`

```java
@Override
public SpanContextPropagationExtractor getExtractor(Exchange exchange) {
    return new VertxWebsocketSpanContextPropagationExtractor(exchange);
}
```

**Constructor of Extractor**:
```java
VertxWebsocketSpanContextPropagationExtractor(Exchange exchange) {
    this.exchange = exchange;
    this.headers = exchange.getIn().getHeaders();
    // For CONSUMER: handshake span context already in headers (set by Consumer)
    // For PRODUCER: proactively collect span context
    collectProducerSpanContext();  ← Called but does nothing for Consumer
}
```

**For Consumer**: The handshake span context was already set in the message headers by the Consumer itself (not shown in modified code, happens in base Consumer).

### 3.4 Create Span Links (Modified Code)

**File**: `camel/components/camel-opentelemetry2/src/main/java/org/apache/camel/opentelemetry2/OpenTelemetryTracer.java`

**Method**: `OpentelemetrySpanLifecycleManager.start(Exchange, SpanKind, ...)`

```java
SpanBuilder builder = tracer.spanBuilder(spanName);

// Extract span link contexts (NEW CODE)
List<SpanContext> linkContexts = extractSpanLinkContexts(extractor);
for (SpanContext linkContext : linkContexts) {
    if (linkContext != null && linkContext.isValid()) {
        builder = builder.addLink(linkContext);  ← Creates span link!
    }
}

// Continue with normal span creation...
Span span = builder.startSpan();
```

**Method**: `extractSpanLinkContexts(SpanContextPropagationExtractor)`

```java
private List<SpanContext> extractSpanLinkContexts(SpanContextPropagationExtractor extractor) {
    List<SpanContext> result = new ArrayList<>();
    if (extractor == null) {
        return result;
    }

    Object value = extractor.get(SPAN_LINK_CONTEXT_PROPERTY);

    if (value instanceof SpanContext) {
        // Direct SpanContext object (from Consumer)
        result.add((SpanContext) value);
    } else if (value instanceof String) {
        // Serialized format "traceId1:spanId1,traceId2:spanId2,..." (from Producer)
        for (String part : str.split(",")) {
            String[] components = part.trim().split(":");
            if (components.length == 2) {
                SpanContext ctx = SpanContext.createFromRemoteParent(
                    components[0], components[1], ...
                );
                result.add(ctx);
            }
        }
    }

    return result;
}
```

**Result**: Consumer span created with FOLLOWS_FROM link to HTTP upgrade span.

---

## Phase 4: Timer Sends Broadcast Message (Producer Span)

### 4.1 Timer Triggers

**Route Definition**:
```java
from("timer://websocket-timer?delay=5000&repeatCount=1")
    .setBody(constant("Hello World"))
    .to("vertx-websocket:///span-link-test?sendToAll=true");  ← Producer
```

### 4.2 Producer Exchange Created

```
Timer fires
  ↓
Exchange created with body "Hello World"
  ↓
ProducerTemplate.send() to vertx-websocket endpoint
  ↓
VertxWebsocketProducer.process(exchange)
```

### 4.3 Collect Producer Span Contexts (Modified Code)

**File**: `camel/components/camel-telemetry/src/main/java/org/apache/camel/telemetry/decorators/VertxWebsocketSpanDecorator.java`

**When**: In the extractor constructor, BEFORE OpenTelemetryTracer creates the span

**Method**: `collectProducerSpanContext()`

```java
private void collectProducerSpanContext() {
    if (headers.containsKey(HANDSHAKE_SPAN_CONTEXT_KEY)) {
        // Already set by Consumer, nothing to do
        return;
    }

    // This is a Producer - collect span contexts from target peers using reflection
    try {
        // Get the endpoint from exchange context
        Object endpoint = exchange.getContext().hasEndpoint(
            exchange.getProperty(Exchange.TO_ENDPOINT, String.class)
        );

        // Get all peers
        Method findPeersMethod = endpoint.getClass().getMethod("findPeerObjectsForHostPort");
        List<Object> allPeers = (List<Object>) findPeersMethod.invoke(endpoint);

        if (allPeers == null || allPeers.isEmpty()) {
            return;
        }

        // Determine which peers will actually receive the message
        Message message = exchange.getMessage();

        // Read sendToAll from message header or endpoint configuration
        Boolean sendToAll = message.getHeader(SEND_TO_ALL, Boolean.class);
        if (sendToAll == null) {
            // Fallback to endpoint configuration
            Object config = endpoint.getClass().getMethod("getConfiguration").invoke(endpoint);
            sendToAll = (Boolean) config.getClass().getMethod("isSendToAll").invoke(config);
        }

        List<Object> targetPeers;
        if (Boolean.TRUE.equals(sendToAll)) {
            // Send to all peers
            targetPeers = allPeers;  ← In our test: 1 peer
        } else {
            // Send only to specific connection(s)
            String connectionKey = message.getHeader(CONNECTION_KEY, String.class);
            if (connectionKey == null || connectionKey.isEmpty()) {
                return;  // External client, no span links
            }
            // Filter peers by connection key
            targetPeers = filterPeersByConnectionKey(allPeers, connectionKey);
        }

        // Collect span contexts only from target peers
        List<String> serializedContexts = new ArrayList<>();
        for (Object peer : targetPeers) {
            Method getSpanContextMethod = peer.getClass().getMethod("getHandshakeSpanContext");
            Object spanContext = getSpanContextMethod.invoke(peer);  ← Calls VertxWebsocketPeer.getHandshakeSpanContext()

            if (spanContext != null) {
                Class<?> spanContextClass = Class.forName("io.opentelemetry.api.trace.SpanContext");
                Boolean isValid = (Boolean) spanContextClass.getMethod("isValid").invoke(spanContext);

                if (Boolean.TRUE.equals(isValid)) {
                    String traceId = (String) spanContextClass.getMethod("getTraceId").invoke(spanContext);
                    String spanId = (String) spanContextClass.getMethod("getSpanId").invoke(spanContext);
                    serializedContexts.add(traceId + ":" + spanId);
                }
            }
        }

        // Store all span contexts as comma-separated string
        if (!serializedContexts.isEmpty()) {
            headers.put(HANDSHAKE_SPAN_CONTEXT_KEY, String.join(",", serializedContexts));
        }
    } catch (Exception e) {
        // Silently ignore - this is best-effort for span links
    }
}
```

**Key Points**:
- Uses **reflection** to avoid compile-time dependencies
- Determines target peers based on `sendToAll` and `connectionKey`
- Retrieves handshake span context from each target peer
- Serializes contexts as "traceId:spanId,..." format
- Stores in exchange headers for OpenTelemetryTracer to consume

### 4.4 Create Producer Span with Links

**File**: `camel/components/camel-opentelemetry2/src/main/java/org/apache/camel/opentelemetry2/OpenTelemetryTracer.java`

Same as Phase 3.4, but now:
- `extractor.get(SPAN_LINK_CONTEXT_PROPERTY)` returns a **String** (serialized format)
- `extractSpanLinkContexts()` parses the string and creates SpanContext objects
- Span links are added to the Producer span

**Result**: Producer span created with FOLLOWS_FROM link(s) to HTTP upgrade span(s).

---

## Complete Call Timeline for SpanLinkTest

### Initialization (Once)
```
1. Quarkus starts
2. CamelContext initializes
3. Routes loaded
4. For each Consumer route:
   QuarkusVertxWebsocketEndpoint.createConsumer()  ← MODIFIED
     ↓
   new QuarkusVertxWebsocketConsumer(...)  ← NEW CLASS
```

### HTTP Upgrade (Per Connection)
```
5. Client connects
6. HTTP upgrade request arrives
7. VertxWebsocketHost.captureHttpUpgradeSpanContext()  ← MODIFIED
     Uses reflection to extract current OpenTelemetry span context
     Stores in RoutingContext
8. WebSocket upgrade completes
9. new VertxWebsocketPeer(...)
10. peer.setHandshakeSpanContext(spanContext)  ← MODIFIED
      Stores HTTP upgrade span context in peer object
```

### Consumer Message (Per Message Received)
```
11. WebSocket message arrives
12. VertxWebsocketConsumer.onMessage()
13. OpenTelemetryTracer.start()
14.   VertxWebsocketSpanDecorator.getExtractor()  ← MODIFIED
15.     new VertxWebsocketSpanContextPropagationExtractor()  ← NEW CLASS
16.       collectProducerSpanContext() (no-op for Consumer)
17.   OpenTelemetryTracer.extractSpanLinkContexts()  ← MODIFIED
18.     extractor.get(HANDSHAKE_SPAN_CONTEXT_KEY)
19.     Returns SpanContext object
20.   SpanBuilder.addLink(spanContext)  ← MODIFIED
21. Consumer span created with link to HTTP upgrade span
```

### Producer Message (Per Message Sent)
```
22. Timer fires (or Producer triggered)
23. Exchange created
24. OpenTelemetryTracer.start()
25.   VertxWebsocketSpanDecorator.getExtractor()  ← MODIFIED
26.     new VertxWebsocketSpanContextPropagationExtractor()  ← NEW CLASS
27.       collectProducerSpanContext()  ← MODIFIED
28.         Uses reflection to get endpoint
29.         Calls endpoint.findPeerObjectsForHostPort() (reflection)
30.         Determines sendToAll or specific connectionKey
31.         For each target peer:
32.           peer.getHandshakeSpanContext() (reflection)  ← Calls MODIFIED method
33.         Serializes span contexts as "traceId:spanId,..."
34.         Stores in exchange headers
35.   OpenTelemetryTracer.extractSpanLinkContexts()  ← MODIFIED
36.     extractor.get(HANDSHAKE_SPAN_CONTEXT_KEY)
37.     Returns String (serialized format)
38.     Parses and creates SpanContext objects
39.   For each SpanContext:
40.     SpanBuilder.addLink(spanContext)  ← MODIFIED
41. Producer span created with link(s) to HTTP upgrade span(s)
42. VertxWebsocketProducer sends message
```

---

## Modified Files Summary

### Camel Repository

| File | When Called | What It Does |
|------|-------------|--------------|
| `VertxWebsocketHost.java` | HTTP upgrade request arrives | Captures OpenTelemetry span context using reflection |
| `VertxWebsocketPeer.java` | Peer created after upgrade | Stores handshake span context |
| `VertxWebsocketSpanDecorator.java` | Before span creation (Consumer & Producer) | Collects span contexts from target peers (Producer only) |
| `OpenTelemetryTracer.java` | Span creation | Extracts span contexts and creates span links |

### Camel-Quarkus Repository

| File | When Called | What It Does |
|------|-------------|--------------|
| `VertxWebsocketRecorder.java` | Route initialization | Creates custom QuarkusVertxWebsocketConsumer |
| `QuarkusVertxWebsocketConsumer.java` | Message consumption | Custom consumer (future extension point) |

---

## Key Design Decisions

### 1. Why Reflection?
- **Problem**: Compile-time dependencies create tight coupling
- **Solution**: Use reflection to access OpenTelemetry APIs
- **Benefit**: `camel-vertx-websocket` has ZERO dependency on OpenTelemetry
- **Tradeoff**: Slightly more complex code, but better modularity

### 2. Why Store in Peer?
- **Problem**: HTTP span ends after upgrade, WebSocket messages arrive much later
- **Solution**: Capture span context during upgrade, store in long-lived peer object
- **Benefit**: Span context available for entire WebSocket connection lifetime

### 3. Why Custom Extractor?
- **Problem**: Need to inject span context into message headers for OpenTelemetryTracer
- **Solution**: Custom SpanContextPropagationExtractor that proactively collects contexts
- **Benefit**: OpenTelemetryTracer remains component-agnostic

### 4. Why Serialization Format?
- **Problem**: Need to support multiple span links (sendToAll to N connections)
- **Solution**: Serialize as "traceId1:spanId1,traceId2:spanId2,..."
- **Benefit**: Single header value can represent multiple span links

---

## Testing in Jaeger

After running `VertxWebsocketSpanLinkTest`, you should see in Jaeger:

```
HTTP upgrade span (from test client)
  ↑ FOLLOWS_FROM
Consumer span (Message 1)

HTTP upgrade span (from test client)
  ↑ FOLLOWS_FROM
Consumer span (Message 2)

HTTP upgrade span (from test client)
  ↑ FOLLOWS_FROM
Producer span (Timer broadcast)
```

All WebSocket spans trace back to the original HTTP request!

---

## Appendix: Full Test Scenario

### Initial State
- Jaeger running on localhost:16686
- Camel application with OpenTelemetry enabled
- Route: `from("vertx-websocket:///span-link-test")`
- Route: `from("timer://...").to("vertx-websocket:///span-link-test?sendToAll=true")`

### Test Execution Timeline
```
T=0ms    : Test starts, client connects to ws://localhost:8081/span-link-test
           → HTTP upgrade span created by OpenTelemetry HTTP instrumentation
           → VertxWebsocketHost.captureHttpUpgradeSpanContext() captures it
           → Stored in VertxWebsocketPeer

T=100ms  : Client sends "Message 1"
           → Consumer span created
           → extractSpanLinkContexts() retrieves handshake span context
           → Span link created: Consumer span FOLLOWS_FROM HTTP upgrade span

T=200ms  : Client sends "Message 2"
           → Consumer span created
           → Span link created: Consumer span FOLLOWS_FROM HTTP upgrade span

T=5000ms : Timer fires, broadcasts "Hello World" with sendToAll=true
           → collectProducerSpanContext() finds 1 peer
           → Retrieves handshake span context from peer
           → Producer span created
           → Span link created: Producer span FOLLOWS_FROM HTTP upgrade span

T=5100ms : Client receives "Hello World"
T=5200ms : Test assertions pass
T=5300ms : Application shuts down
```

### Expected Jaeger Output
- **4 spans total**:
  - 1 HTTP upgrade span
  - 2 Consumer spans (Message 1, Message 2)
  - 1 Producer span (Timer broadcast)
- **3 span links** (all pointing to HTTP upgrade span)
- All links use `FOLLOWS_FROM` reference type
