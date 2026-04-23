<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
# Apache Commons XML Factory

Apache Commons XML Factory provides secure-by-default JAXP factory creation,
abstracting over implementation-specific XXE hardening differences between the
stock JDK and external JAXP implementations.


## Why

Any Java library that parses XML has to harden JAXP before handing a factory to user code, and every library ends up
copy-pasting the same hardening snippet. The snippet is fragile: the attributes and features needed to harden a factory
are not standardised, each JAXP implementation exposes a slightly different set, and setting an unknown one throws an
exception that callers routinely swallow. Writing this block correctly for every implementation is real work, and
duplicating it across projects means every project owns the maintenance burden on its own.

Defaults are also uneven. The stock JDK SAX and DOM parsers already prevent external entity resolution through
`FEATURE_SECURE_PROCESSING`, and JAXP 1.5 conformant implementations ship reasonable defaults for most attacks. Others,
such as standalone Xerces, Woodstox, or Saxon's TrAX, need further configuration before they reach the same baseline. A
library author has no control over which implementation is on the classpath at runtime, so the effective security
posture of their code depends on a deployment decision made elsewhere.

This library provides that baseline. Each `XmlFactories` call returns a fresh factory hardened by a provider-specific
SPI, so the returned object behaves the same way security-wise regardless of which JAXP implementation resolved.
Security becomes a property of the call, not of the classpath, and there is one place to update when a new hardening
setting becomes available or a default changes.

## Usage

Add the library to your build:

```xml
<dependency>
  <groupId>org.apache.commons</groupId>
  <artifactId>commons-xml-factory</artifactId>
  <version>${commons.release.version}</version>
</dependency>
```

Every method on `XmlFactories` returns a fresh, hardened factory. Pick the one that matches the API you already use; no
other configuration is required. On hardened factories any attempt to resolve an external resource (DTD, entity, schema,
stylesheet) is blocked, and DOCTYPE input is rejected wherever the underlying implementation allows it.

### Supported implementations

Out of the box the library recognises the stock JDK JAXP implementations, Apache Xerces 2.x, Woodstox, and Saxon-HE. If
a factory resolves to an implementation not covered by any bundled or registered provider, every `XmlFactories` method
throws `UnsupportedXmlImplementationException`.

Support for additional implementations can be plugged in by publishing an
`org.apache.commons.xml.factory.spi.XmlProvider` through the standard Java `ServiceLoader` mechanism. Bundled providers
always take precedence, so a third-party provider cannot hijack hardening for a factory class this library already
supports.

**DOM parsing** via `DocumentBuilderFactory`:

```java
import org.w3c.dom.Document;
import org.apache.commons.xml.factory.XmlFactories;

Document doc = XmlFactories.newDocumentBuilderFactory().newDocumentBuilder().parse(inputStream);
```

**SAX parsing** via `SAXParserFactory`:

```java
import org.apache.commons.xml.factory.XmlFactories;

XmlFactories.newSAXParserFactory().newSAXParser().parse(inputStream, myDefaultHandler);
```

**Streaming (StAX) parsing** via `XMLInputFactory`:

```java
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.xml.factory.XmlFactories;

XMLStreamReader reader = XmlFactories.newXMLInputFactory().createXMLStreamReader(inputStream);
```

**XSLT transforms** via `TransformerFactory`:

```java
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.xml.factory.XmlFactories;

XmlFactories.newTransformerFactory()
        .newTransformer(new StreamSource(stylesheet))
        .transform(new StreamSource(inputStream), new StreamResult(outputStream));
```

**XPath queries** via `XPathFactory`:

```java
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.NodeList;
import org.apache.commons.xml.factory.XmlFactories;

NodeList hits = (NodeList) XmlFactories.newXPathFactory()
        .newXPath()
        .evaluate("//item", doc, XPathConstants.NODESET);
```

**W3C XML Schema validation** via `SchemaFactory`:

```java
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.xml.factory.XmlFactories;

XmlFactories.newSchemaFactory()
        .newSchema(new StreamSource(xsdStream))
        .newValidator()
        .validate(new StreamSource(inputStream));
```

### Stylesheets and schemas

The hardening applies to documents parsed through the returned factory. Stylesheets given to
`TransformerFactory.newTransformer(Source)` and schemas given to `SchemaFactory.newSchema(Source)` are read by a parser
the implementation picks internally, and that parser may not be hardened (Saxon's TrAX is one such case, see Building
below). Treat stylesheets and schemas as trusted input, or pre-parse them through a hardened `XmlFactories` parser and
pass the result as a `DOMSource` or `SAXSource`.

### Caching and thread-safety

There is no caching or pooling inside `XmlFactories`; callers on a hot path are responsible for their own caching. The
returned factories inherit the thread-safety properties of the underlying JAXP implementation, which in practice means
they are not thread-safe. Create a new factory per thread or synchronise externally.

