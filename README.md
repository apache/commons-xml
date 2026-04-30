<!--
  ~ SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
  ~ SPDX-License-Identifier: Apache-2.0
  -->

# Copernik XML Factory

Secure-by-default JAXP factory creation for Java.
A single method call returns a hardened JAXP factory that can be used to **safely** parse XML files.

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

This library provides that baseline. Each `XmlFactories` call returns a fresh factory hardened by an
implementation-specific recipe, so the returned object behaves the same way security-wise regardless of which JAXP
implementation resolved. Security becomes a property of the call, not of the classpath, and there is one place to
update when a new hardening setting becomes available or a default changes.

## Usage

Add the library to your build:

```xml
<dependency>
  <groupId>eu.copernik</groupId>
  <artifactId>copernik-xml-factory</artifactId>
  <version>0.1.0</version>
</dependency>
```

Every method on `XmlFactories` returns a fresh, hardened factory. Pick the one that matches the API you already use; no
other configuration is required. On hardened factories any attempt to resolve an external resource (DTD, entity, schema,
stylesheet) is blocked, and DOCTYPE input is rejected wherever the underlying implementation allows it.

### Supported implementations

The library recognises:

- the stock JDK JAXP implementations,
- Android,
- Saxon-HE,
- Apache Xalan,
- Apache Xerces,
- Woodstox.

If a factory resolves to an implementation not covered by any bundled hardening recipe, every `XmlFactories` method throws
`IllegalStateException` with a message naming the unsupported class. Adding support for a new JAXP implementation
requires a code change to this library.

**DOM parsing** via `DocumentBuilderFactory`:

```java
import org.w3c.dom.Document;
import eu.copernik.xml.factory.XmlFactories;

Document doc = XmlFactories.newDocumentBuilderFactory().newDocumentBuilder().parse(inputStream);
```

**SAX parsing** via `SAXParserFactory`:

```java
import eu.copernik.xml.factory.XmlFactories;

XmlFactories.newSAXParserFactory().newSAXParser().parse(inputStream, myDefaultHandler);
```

**Streaming (StAX) parsing** via `XMLInputFactory`:

```java
import javax.xml.stream.XMLStreamReader;
import eu.copernik.xml.factory.XmlFactories;

XMLStreamReader reader = XmlFactories.newXMLInputFactory().createXMLStreamReader(inputStream);
```

**XSLT transforms** via `TransformerFactory`:

```java
import javax.xml.transform.stream.StreamSource;
import eu.copernik.xml.factory.XmlFactories;
XmlFactories.newTransformerFactory()
        .newTransformer(new StreamSource(stylesheet))
        .transform(new StreamSource(inputStream), new StreamResult(outputStream));
```

**XPath queries** via `XPathFactory`:

```java
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.NodeList;
import eu.copernik.xml.factory.XmlFactories;

NodeList hits = (NodeList) XmlFactories.newXPathFactory()
        .newXPath()
        .evaluate("//item", doc, XPathConstants.NODESET);
```

**W3C XML Schema validation** via `SchemaFactory`:

```java
import javax.xml.transform.stream.StreamSource;
import eu.copernik.xml.factory.XmlFactories;

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

### Android compatibility

The library is compiled to Java 8 bytecode and runs on any Android version that supports a Java 8 runtime (API 19 and
above). What ships with the platform and what the application has to add varies by JAXP API:

- **DOM** (`DocumentBuilderFactory`) and **SAX** (`SAXParserFactory`) ship in `android.jar` since API 1. DOM is backed by
  KXmlParser (a kxml2 pull parser); SAX is a wrapper around the system `libexpat`. The hardened factories returned by
  `XmlFactories` route through these built-in implementations.
- **TrAX** (`TransformerFactory`) and **XPath** (`XPathFactory`) ship as Apache Xalan since Android 1.0. The hardened
  factories receive the same JDK-style entity-expansion limits and deny-all resolver that the standalone Xalan recipe
  applies.
- **W3C XML Schema** (`SchemaFactory`) is **declared** in `android.jar` (the `javax.xml.validation.*` API is present)
  but the platform ships **no implementation**.
- **StAX** (`XMLInputFactory`) is **not** part of `android.jar` at any API level.

The SAX path's native billion-laughs amplification protection lives in `libexpat` 2.4 (March 2022), which AOSP first
shipped in **Android 13 (API 33)**. On API 33 and above the platform SAX parser blocks billion-laughs payloads natively;
on older Android releases this specific defence could be unavailable, and a hostile internal-entity payload could
amplify without bound. If your minimum-supported Android level is below 33 and you parse SAX input that you do not
control, sanitise upstream of `XmlFactories`, or pre-parse with Apache Xerces (which carries its own entity-expansion
limit) once you have added it to the classpath for schema support.

### Caching and thread-safety

There is no caching or pooling inside `XmlFactories`; callers on a hot path are responsible for their own caching. The
returned factories inherit the thread-safety properties of the underlying JAXP implementation, which in practice means
they are not thread-safe. Create a new factory per thread or synchronise externally.

## Building

Building requires a Java JDK and [Apache Maven](https://maven.apache.org/).
The required Java version is found in the `pom.xml` as the `maven.compiler.source` property.

From a command shell, run `mvn` without arguments to invoke the default Maven goal to run all tests and checks

## Licensing

Licensed under the Apache License, Version 2.0. See `LICENSE`.
