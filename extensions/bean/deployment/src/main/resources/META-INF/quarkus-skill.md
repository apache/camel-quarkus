<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->

### Referring to beans by name in routes

A bean called by name from a route needs three annotations: `@Named("myBean")` (or `io.smallrye.common.annotation.Identifier`), a CDI scope such as `@ApplicationScoped`, and `@RegisterForReflection` (**required for native mode**):

```java
@ApplicationScoped
@Named("myBean")
@RegisterForReflection
public class MyBean {
    public String hello(String name) {
        return "Hello " + name;
    }
}
```

Use it in a route with the `.bean("myBean", "hello")` EIP (works in every DSL), or as an endpoint: `.to(bean("myBean").method("hello"))` with the Endpoint DSL, `.to("bean:myBean?method=hello")` with string URIs.

### POJO consuming and producing

- `@Consume("direct:in")` goes on the bean **method** that handles the messages: it auto-creates a route delivering each message to that method.
- `@Produce("direct:out")` goes on a **field** (`ProducerTemplate`, `FluentProducerTemplate`, or a custom interface) to send messages to the endpoint.
- The field-only rule applies to `@Produce` and `@EndpointInject` (not supported on setter methods). It does **not** apply to `@Consume`, which annotates the method that handles the messages — that method is what the generated route calls.

### Pitfalls

- A bean method that touches request-scoped state (such as Hibernate Panache entities) throws `ContextNotActiveException` when invoked from a route: annotate the method with `@ActivateRequestContext`.
