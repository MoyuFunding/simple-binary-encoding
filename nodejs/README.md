# SBE Node.js Codecs

This directory contains generated JavaScript (ES6) codecs for Simple Binary Encoding (SBE).

> 📖 **中文文档**: See [使用指南.md](./使用指南.md) for comprehensive Chinese documentation | [Quick Start](./QUICKSTART.md)

## Overview

The Node.js generator creates efficient binary message codecs for Node.js applications. Generated code uses:
- ES6 class syntax
- JSDoc comments for type information
- Native Node.js `Buffer` for optimal performance
- ES6 modules (import/export)

## Generation

Generate JavaScript codecs from your SBE schema:

```bash
./gradlew generateNodeJsCodecs
```

Or generate from a specific schema:

```bash
java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED \
  -Dsbe.output.dir=nodejs/generated \
  -Dsbe.target.language=uk.co.real_logic.sbe.generation.nodejs.NodeJs \
  -jar sbe-all/build/libs/sbe-all-${VERSION}.jar \
  your-schema.xml
```

## Usage Example

```javascript
import { Car } from './generated/Car.js';
import { MessageHeader } from './generated/MessageHeader.js';

// Encoding
const car = new Car();
car.serialNumber = 12345;
car.modelYear = 2024;
car.vehicleCode = 'A'.charCodeAt(0);

const buffer = Buffer.allocUnsafe(128);
const encodedSize = car.encode(buffer, 0);
console.log(`Encoded ${encodedSize} bytes`);

// Decoding
const decodedCar = new Car();
const decodedSize = decodedCar.decode(buffer, 0);
console.log(`Serial Number: ${decodedCar.serialNumber}`);
console.log(`Model Year: ${decodedCar.modelYear}`);
```

## Features

- **Type Documentation**: JSDoc comments for IDE intellisense and type checking
- **Performance**: Native Node.js Buffer operations for efficient binary I/O
- **Byte Order**: Automatic handling of little-endian and big-endian
- **Enums**: Object-based enums for constants
- **Bitsets**: Boolean flags with bitwise operations via getters/setters
- **Composites**: Nested type support
- **Messages**: Complete message encoding/decoding with headers
- **ES6 Modules**: Modern import/export syntax

## Generated Code Structure

Each generated file contains:

### Message Classes
- Static constants: `TEMPLATE_ID`, `SCHEMA_ID`, `SCHEMA_VERSION`, `BLOCK_LENGTH`
- `encode(buffer, offset = 0)` - Encodes message to buffer, returns bytes written
- `decode(buffer, offset = 0)` - Decodes message from buffer, returns bytes read
- Getters and setters for all fields with JSDoc annotations

### Enum Types
```javascript
const Model = {
  MODEL_A: 0,
  MODEL_B: 1,
  MODEL_C: 2
};
export { Model };
```

### Bitset Types
```javascript
class OptionalExtras {
  /** @returns {boolean} */
  get sunRoof() { ... }
  /** @param {boolean} value */
  set sunRoof(value) { ... }
  // ... other flags
}
export { OptionalExtras };
```

### Composite Types
- `encode(buffer, offset)` - Returns number of bytes written
- `decode(buffer, offset)` - Returns number of bytes read
- Getters/setters for component fields with JSDoc

## Integration

### With Node.js (ES Modules)

To use ES6 modules in Node.js, add to your `package.json`:

```json
{
  "type": "module"
}
```

Then import normally:
```javascript
import { Car } from './generated/Car.js';
```

### With CommonJS

Convert to CommonJS if needed:
```bash
# Using a transpiler like Babel
npx babel generated --out-dir dist
```

Or use dynamic import:
```javascript
const { Car } = await import('./generated/Car.js');
```

## Performance Notes

- Use `Buffer.allocUnsafe()` for encoding buffers (faster, but uninitialized)
- Reuse message instances when encoding multiple messages
- Buffer operations are zero-copy where possible
- BigInt used for 64-bit integers (Int64, UInt64)

## Type Mappings

| SBE Type | TypeScript Type | Buffer Method      |
|----------|------------------|-------------------|
| char     | number           | readUInt8/writeUInt8 |
| int8     | number           | readInt8/writeInt8   |
| int16    | number           | readInt16LE/writeInt16LE |
| int32    | number           | readInt32LE/writeInt32LE |
| int64    | bigint           | readBigInt64LE/writeBigInt64LE |
| uint8    | number           | readUInt8/writeUInt8 |
| uint16   | number           | readUInt16LE/writeUInt16LE |
| uint32   | number           | readUInt32LE/writeUInt32LE |
| uint64   | bigint           | readBigUInt64LE/writeBigUInt64LE |
| float    | number           | readFloatLE/writeFloatLE |
| double   | number           | readDoubleLE/writeDoubleLE |

## License

Generated code is subject to the same Apache 2.0 license as SBE.
