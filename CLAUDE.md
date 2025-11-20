# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Simple Binary Encoding (SBE) is an OSI layer 6 presentation protocol for encoding and decoding binary application messages for low-latency financial applications. This is the reference implementation supporting multiple target languages: Java, C++, C, Golang, C#, and Rust.

The project consists of a code generation tool (`sbe-tool`) that reads XML schema definitions and generates language-specific codecs for efficient binary message encoding/decoding.

## Build Commands

### Java/Gradle Build

**Full clean build:**
```bash
./gradlew
```

**Build without tests:**
```bash
./gradlew build -x test
```

**Run Java tests:**
```bash
./gradlew test
```

**Run specific test:**
```bash
./gradlew :sbe-tool:test --tests "ClassName.methodName"
```

**Run property-based tests:**
```bash
./gradlew :sbe-tool:propertyTest
```

**Run Java examples:**
```bash
./gradlew runJavaExamples
```

**Run Java benchmarks:**
```bash
./gradlew runJavaBenchmarks
```

### C++ Build (CMake)

**Quick build script (full clean, build, test):**
```bash
./cppbuild/cppbuild
```

**Manual CMake build:**
```bash
mkdir -p cppbuild/Debug
cd cppbuild/Debug
cmake ../..
cmake --build . --clean-first
ctest
```

**CMake build options:**
- `--c-warnings-as-errors` - Enable warnings as errors for C
- `--cxx-warnings-as-errors` - Enable warnings as errors for C++
- `-d` or `--debug-build` - Build in debug mode

### Golang Build

**Generate golang codecs:**
```bash
./gradlew generateGolangCodecs
```

**Run golang tests:**
```bash
cd gocode
make
```

### C# Build

**Generate C# codecs:**
```bash
./gradlew generateCSharpCodecs
```

### Rust Build

**Generate rust codecs:**
```bash
./gradlew generateRustCodecs
```

**Run rust tests:**
```bash
./gradlew runRustTests
# Or directly:
cd rust
cargo test
```

### Node.js Build

**Generate Node.js/JavaScript codecs:**
```bash
./gradlew generateNodeJsCodecs
```

The generated JavaScript files will be in `nodejs/generated/`. Each file contains:
- ES6 classes with encode/decode methods
- JSDoc comments for type information
- Native Node.js Buffer operations
- Support for little-endian and big-endian byte orders
- ES6 module exports

## Code Generation

The SBE tool generates codecs from XML schema files. Key system properties for code generation:

- `sbe.output.dir` - Output directory for generated code
- `sbe.target.language` - Target language (Java, cpp, golang, uk.co.real_logic.sbe.generation.csharp.CSharp, Rust, uk.co.real_logic.sbe.generation.nodejs.NodeJs)
- `sbe.target.namespace` - Override namespace/package
- `sbe.validation.xsd` - Path to XSD for schema validation
- `sbe.generate.ir` - Generate intermediate representation
- `sbe.xinclude.aware` - Enable XInclude support

**Example command-line usage:**
```bash
java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED \
  -Dsbe.target.language=Cpp \
  -Dsbe.target.namespace=sbe \
  -Dsbe.output.dir=include/gen \
  -jar sbe-all/build/libs/sbe-all-${VERSION}.jar \
  my-schema.xml
```

## Architecture

### Core Components

1. **sbe-tool** - The main code generation tool
   - `uk.co.real_logic.sbe.xml` - XML schema parsing and validation
   - `uk.co.real_logic.sbe.ir` - Intermediate Representation (IR) layer
   - `uk.co.real_logic.sbe.generation` - Code generators for each target language
   - Main entry point: `uk.co.real_logic.sbe.SbeTool`

2. **sbe-samples** - Example usage and integration samples

3. **sbe-benchmarks** - JMH benchmarks for performance testing

4. **sbe-all** - Shadow jar containing all dependencies (uses Agrona)

### Processing Pipeline

The code generation follows this flow:

```
XML Schema → XmlSchemaParser → MessageSchema → IrGenerator → IR (Intermediate Representation) → Language-Specific Generator → Generated Code
```

**Key insight:** The IR layer decouples schema parsing from code generation. This allows:
- Multiple input formats (XML, binary IR)
- Consistent code generation across languages
- Independent evolution of parsers and generators

### Language-Specific Generators

Each language has its own generator package:
- **Java:** `uk.co.real_logic.sbe.generation.java.JavaGenerator`
- **C++:** `uk.co.real_logic.sbe.generation.cpp.CppGenerator`
- **C:** `uk.co.real_logic.sbe.generation.c.CGenerator`
- **C#:** `uk.co.real_logic.sbe.generation.csharp.CSharp`
- **Golang:** `uk.co.real_logic.sbe.generation.golang.struct.GolangGenerator` (structs) and `uk.co.real_logic.sbe.generation.golang.flyweight.GolangFlyweightGenerator` (flyweights)
- **Rust:** `uk.co.real_logic.sbe.generation.rust.RustGenerator`
- **Node.js:** `uk.co.real_logic.sbe.generation.nodejs.NodeJsGenerator`

Golang supports two generation modes:
- **Structs** (default): Generate Go structs with encode/decode methods
- **Flyweights**: Generate flyweight-style codecs (set `sbe.go.generate.generate.flyweights=true`)

Node.js generator features:
- Generates JavaScript (.js) files with ES6 syntax
- JSDoc annotations for type documentation and IDE support
- Uses native Node.js Buffer for efficient binary operations
- Supports both little-endian and big-endian byte orders
- Generated classes include encode() and decode() methods
- Enums (as objects), bitsets (with getters/setters), and composite types fully supported
- ES6 module exports for modern JavaScript

### Intermediate Representation (IR)

The IR is a binary format that represents the parsed schema. Key classes:
- `Ir` - Container for the entire IR
- `Token` - Represents a field, type, or structural element
- `IrEncoder` - Encodes IR to binary format
- `IrDecoder` - Decodes binary IR

The IR itself is defined via SBE schema at `sbe-tool/src/main/resources/sbe-ir.xml` and has generated codecs in:
- Java: `sbe-tool/src/main/java/uk/co/real_logic/sbe/ir/generated/`
- C++: `sbe-tool/src/main/cpp/`
- Golang: `sbe-tool/src/main/golang/`

**Important:** IR codecs must be kept in sync. The build verifies this via `verifyJavaIrCodecsInSync` task. If modifying the IR schema, run `generateJavaIrCodecs` (and corresponding tasks for other languages) and commit changes.

## Important Constraints

### Java Version
- Source compatibility: Java 17
- Build uses toolchain specified by `BUILD_JAVA_VERSION` environment variable or current Java version
- Tests require JVM arg: `--add-opens java.base/jdk.internal.misc=ALL-UNNAMED`

### Dependencies
- **Agrona** (org.agrona) - Used for buffer implementations in Java
- JUnit 5 for testing
- JMH for benchmarks
- Checkstyle for code quality

### Code Style
- Checkstyle enforced on build
- All warnings treated as errors in Java compilation (`-Xlint:all -Werror`)
- Follow existing conventions for readability

### Module Structure
- `sbe-tool` - Core library (published to Maven Central)
- `sbe-all` - Executable jar with dependencies
- `sbe-samples` - Not typically modified
- `sbe-benchmarks` - JMH benchmarks

### Testing
- Generate test codecs before compiling: `generateTestCodecs`, `generateTestDtos`
- Property-based testing with jqwik in `:sbe-tool:propertyTest`
- C++ tests use Google Test (fetched via CMake)
- Test resources in `sbe-tool/src/test/resources/` contain schema examples

## Common Workflows

### Adding a New Feature to Code Generation

1. Modify the XML parser or IR generator if schema changes needed
2. Update the relevant language generator(s)
3. Add test schema in `sbe-tool/src/test/resources/`
4. Write unit tests for the new functionality
5. Regenerate IR codecs if IR schema changed: `./gradlew generateIrCodecs`
6. Run tests: `./gradlew test` and language-specific tests

### Modifying IR Schema

1. Edit `sbe-tool/src/main/resources/sbe-ir.xml`
2. Generate new IR codecs:
   ```bash
   ./gradlew generateJavaIrCodecs
   ./gradlew generateCppIrCodecs
   ./gradlew generateGolangIrCodecs
   ```
3. Commit all generated files
4. Build will fail if IR codecs are out of sync

### Running Individual Language Tests

Each language can be tested independently after generating codecs:

```bash
# C++
./cppbuild/cppbuild

# Golang
./gradlew generateGolangCodecs
cd gocode && make

# Rust
./gradlew generateRustCodecs
cd rust && cargo test

# C#
./gradlew generateCSharpCodecs
# (requires .NET SDK)

# Node.js
./gradlew generateNodeJsCodecs
# Generated files in nodejs/generated/
# Use with Node.js (ES modules, requires Node >= 14)
```

## Schema Design

Schemas are XML files following the FIX SBE standard. Key schema elements:

- `<messageSchema>` - Root element with package/namespace info
- `<types>` - Type definitions (primitives, composites, enums, sets)
- `<message>` - Message definitions with fields and groups
- `<field>` - Primitive field in a message
- `<group>` - Repeating group of fields
- `<data>` - Variable-length data field

Schema validation XSD: `sbe-tool/src/main/resources/fpl/sbe.xsd`

Example schemas in `sbe-samples/src/main/resources/` demonstrate common patterns.

## File Locations

- Gradle build artifacts: `*/build/`
- CMake build artifacts: `cppbuild/Release/` or `cppbuild/Debug/`
- Generated codecs (for testing): `sbe-tool/build/generated-src/`
- Version: `version.txt`
- Maven group ID: `uk.co.real-logic`
- Artifact names: `sbe-tool`, `sbe-all`, `sbe-samples`, `sbe-benchmarks`
