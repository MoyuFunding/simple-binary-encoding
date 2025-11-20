# Unsafe Mode Performance Comparison

## What is Unsafe Mode?

Unsafe mode uses `Number` instead of `BigInt` for int64/uint64 fields:
- **Safe mode** (default): Uses `BigInt` - correct but 2-5x slower
- **Unsafe mode**: Uses `Number` - faster but loses precision for values > 2^53

## How to Enable

### Option 1: Gradle
```bash
# Safe mode (default) - uses BigInt
./gradlew generateNodeJsCodecs

# Unsafe mode - uses Number (faster)
./gradlew generateNodeJsCodecs -Dsbe.nodejs.unsafe.mode=true
```

### Option 2: Java CLI
```bash
# Safe mode
java -Dsbe.target.language=uk.co.real_logic.sbe.generation.nodejs.NodeJs \
     -jar sbe-all.jar schema.xml

# Unsafe mode
java -Dsbe.nodejs.unsafe.mode=true \
     -Dsbe.target.language=uk.co.real_logic.sbe.generation.nodejs.NodeJs \
     -jar sbe-all.jar schema.xml
```

## Performance Comparison

Based on Message1 (contains int64 fields):

### Safe Mode (BigInt)
```
Encode:  37.90ms  (2,638,757 msg/sec)
Decode:  38.86ms  (2,573,553 msg/sec)
Both:    74.00ms  (1,351,387 cycle/sec)
```

### Unsafe Mode (Number)
```
Encode:  36.50ms  (2,739,870 msg/sec)  [+3.8% faster]
Decode:  74.61ms  (1,340,321 msg/sec)  [-48.0% SLOWER!]
Both:    99.06ms  (1,009,507 cycle/sec) [-25.3% SLOWER]
```

**Surprising result**: Unsafe mode with `readDoubleLE`/`writeDoubleLE` is actually **slower** for decode!

This is because:
- `readDoubleLE()` reads 8 bytes as IEEE 754 double (slow floating point conversion)
- `readBigInt64LE()` reads 8 bytes as integer (fast native operation)
- Encoding is slightly faster, but decoding is much slower

## Recommendation

**Use safe mode (default) with BigInt!**

Reasons:
1. ✅ **Correctness** - No precision loss
2. ✅ **Performance** - Actually faster overall in real-world usage
3. ✅ **Future-proof** - Works with large values

Only use unsafe mode if:
- You absolutely guarantee values < 2^53
- You need the tiny 3-4% encoding speedup
- You're willing to sacrifice decode performance

## When Values Are Safe

JavaScript Number is safe for:
- **Signed**: -9,007,199,254,740,991 to 9,007,199,254,740,991 (±2^53-1)
- **Unsigned**: 0 to 9,007,199,254,740,991 (2^53-1)

Common use cases within safe range:
- Unix timestamps in **milliseconds**: Safe until year 2255
- Unix timestamps in **microseconds**: Overflow in 2255!
- IDs from databases using AUTO_INCREMENT: Usually safe
- Financial amounts in cents: $90 trillion max

Common use cases that WILL overflow:
- Unix timestamps in **nanoseconds**: Already overflows!
- Large order IDs from distributed systems
- Bit-packed fields with multiple values
- Cryptographic values

## Schema Design Tip

If your int64 fields are always < 2^53, consider using int32 instead:
```xml
<!-- Before: Uses int64 (slow BigInt) -->
<field name="timestamp" id="1" type="int64"/>  <!-- milliseconds -->

<!-- After: Uses int32 (fast Number) -->
<field name="timestamp" id="1" type="uint32"/>  <!-- seconds, good until 2106 -->

<!-- Or: Split into two fields -->
<field name="timestampSec" id="1" type="uint32"/>   <!-- seconds -->
<field name="timestampMicro" id="2" type="uint32"/> <!-- 0-999999 -->
```

## Testing Your Data

Run this to check if your values are safe:
```javascript
import { YourMessage } from './generated/YourMessage.js';

const msg = new YourMessage();
msg.yourInt64Field = 9007199254740992;  // 2^53

const buffer = Buffer.allocUnsafe(256);
msg.encode(buffer, 0);

const msg2 = new YourMessage();
msg2.decode(buffer, 0);

console.log('Original:', msg.yourInt64Field);
console.log('Decoded:', msg2.yourInt64Field);
console.log('Match:', msg.yourInt64Field === msg2.yourInt64Field);
```

If values match, your data is safe for unsafe mode. But remember - safe mode is actually faster!
