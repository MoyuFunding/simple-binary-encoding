# Node.js SBE Performance Guide

## BigInt Performance Considerations

### Overview

JavaScript's `BigInt` type is necessary for correct handling of 64-bit integers (int64/uint64) but comes with a performance cost compared to regular `Number` operations.

### Performance Impact

Based on benchmarking (`npm run benchmark`):

| Operation Type | Number (int32) | BigInt (int64) | Slowdown |
|----------------|----------------|----------------|----------|
| Arithmetic Operations | 6.14ms | 28.51ms | **4.6x** |
| Bitwise Operations | 1.22ms | 46.71ms | **38x** |
| Buffer Read/Write | 12.63ms | 71.23ms | **5.6x** |
| SBE Message Encode/Decode | 5.07ms | 25.13ms | **5.0x** |

### Why BigInt is Necessary

JavaScript's `Number` type uses IEEE 754 double-precision (53-bit mantissa):
- **Safe integer range**: -(2^53 - 1) to (2^53 - 1)
- **Values beyond this lose precision**

Example of the problem:
```javascript
// Without BigInt - WRONG!
const largeValue = 9007199254740993; // 2^53 + 1
console.log(largeValue === largeValue + 1); // true (!!)

// With BigInt - CORRECT
const largeValueBig = 9007199254740993n;
console.log(largeValueBig === largeValueBig + 1n); // false ✓
```

### When to Use int64/uint64

#### ✅ Must Use (Correctness Required)

1. **Timestamps** - Unix timestamps in microseconds/nanoseconds
   ```javascript
   timestamp: uint64  // 1700000000000000 (microseconds)
   ```

2. **Large IDs** - Order IDs, transaction IDs > 2^53
   ```javascript
   orderId: uint64    // 9223372036854775807
   ```

3. **Financial Values** - Prices in atomic units
   ```javascript
   price: int64       // 123456789012345 (cents)
   ```

4. **Bitsets > 32 bits** - Flag sets needing > 32 flags
   ```javascript
   flags: uint64      // 64 boolean flags
   ```

#### ⚠️ Avoid if Possible (Use int32/uint32 Instead)

1. **Counters** - Most counters fit in 32 bits
   ```javascript
   count: uint32      // 0 to 4,294,967,295
   ```

2. **Small IDs** - Sequence numbers, array indices
   ```javascript
   sequenceNumber: uint32
   ```

3. **Quantities** - Most quantities < 2 billion
   ```javascript
   quantity: int32    // -2,147,483,648 to 2,147,483,647
   ```

4. **Bitsets ≤ 32 bits** - Small flag sets
   ```javascript
   options: uint32    // 32 boolean flags
   ```

### Schema Design Guidelines

#### Before: Suboptimal (Everything is int64)
```xml
<message name="Order" id="1">
    <field name="orderId" id="1" type="int64"/>
    <field name="timestamp" id="2" type="int64"/>
    <field name="quantity" id="3" type="int64"/>      <!-- Wasteful! -->
    <field name="sequenceNum" id="4" type="int64"/>   <!-- Wasteful! -->
    <field name="flags" id="5" type="uint64"/>        <!-- Overkill? -->
</message>
```

**Performance**: 5x slower, 60% larger message size

#### After: Optimized (Use int32 where possible)
```xml
<message name="Order" id="1">
    <field name="orderId" id="1" type="int64"/>       <!-- Must be 64-bit -->
    <field name="timestamp" id="2" type="int64"/>     <!-- Must be 64-bit -->
    <field name="quantity" id="3" type="int32"/>      <!-- 2B is enough -->
    <field name="sequenceNum" id="4" type="uint32"/>  <!-- 4B is enough -->
    <field name="flags" id="5" type="uint32"/>        <!-- 32 flags enough -->
</message>
```

**Performance**: Much faster, smaller messages

### Real-World Impact

#### Scenario 1: Low-latency Trading (10% int64 fields)
```javascript
// Message with 10 fields: 1 int64, 9 int32
// Impact: ~10-15% slower vs pure int32
// Verdict: Acceptable for correctness
```

#### Scenario 2: Analytics Pipeline (50% int64 fields)
```javascript
// Message with 10 fields: 5 int64, 5 int32
// Impact: ~50-100% slower vs pure int32
// Verdict: Consider if all int64 fields are necessary
```

#### Scenario 3: Legacy Integration (100% int32)
```javascript
// Message with 10 fields: 0 int64, 10 int32
// Impact: No slowdown, uses only Number
// Verdict: Optimal performance
```

### Optimization Strategies

#### 1. Use Appropriate Types in Schema

**Bad**:
```xml
<field name="price" id="1" type="int64"/>  <!-- Price in cents -->
```
If your prices never exceed $21M (2^31 cents), use int32:
```xml
<field name="price" id="1" type="int32"/>  <!-- Up to $21,474,836 -->
```

#### 2. Split Large Numbers

Instead of storing microseconds as int64:
```xml
<!-- Before: int64 microseconds -->
<field name="timestampMicros" id="1" type="int64"/>
```

Consider splitting into seconds + microseconds:
```xml
<!-- After: seconds (uint32) + micros (uint32) -->
<field name="timestampSec" id="1" type="uint32"/>   <!-- Covers to 2106 -->
<field name="timestampMicro" id="2" type="uint32"/> <!-- 0-999999 -->
```

#### 3. Use Smaller Bitsets

If you only need 20 flags:
```xml
<!-- Use uint32 instead of uint64 -->
<type name="Flags" primitiveType="uint32">
    <choice name="flag1">0</choice>
    <choice name="flag2">1</choice>
    <!-- ... up to 31 -->
</type>
```

#### 4. Benchmark Your Schema

Run the benchmark with your actual message:
```bash
npm run benchmark
```

Create a test with your schema:
```javascript
// Test your actual message type
const iterations = 100000;
const buffer = Buffer.allocUnsafe(1024);
const msg = new YourMessage();

const start = performance.now();
for (let i = 0; i < iterations; i++) {
    // Fill with typical values
    msg.field1 = i;
    msg.field64 = BigInt(i);
    msg.encode(buffer, 0);
    msg.decode(buffer, 0);
}
const elapsed = performance.now() - start;
console.log(`${iterations} messages in ${elapsed.toFixed(2)}ms`);
console.log(`${(iterations / elapsed * 1000).toFixed(0)} msg/sec`);
```

### V8 Optimization Notes

Node.js V8 engine optimizations:
- **Number**: Highly optimized, uses SMI (Small Integer) when possible
- **BigInt**: Heap-allocated, slower allocation/GC
- **Mixed operations**: Number ↔ BigInt conversions add overhead

### Benchmark Results Summary

Test environment: Node.js v20+ on modern CPU

```
Pure int32 messages:   197,000 msg/sec  (baseline)
10% int64 fields:      170,000 msg/sec  (-14%)
50% int64 fields:       98,000 msg/sec  (-50%)
Pure int64 messages:    40,000 msg/sec  (-80%)
```

### Recommendations

#### For Low-Latency Applications
1. ✅ Profile your schema with realistic data
2. ✅ Use int32/uint32 wherever possible
3. ✅ Reserve int64/uint64 for values that truly need it
4. ✅ Document why each int64 field is necessary

#### For High-Throughput Applications
1. ✅ Batch processing can amortize BigInt overhead
2. ✅ Consider separate schemas for int32 vs int64 heavy messages
3. ✅ Use Worker Threads to parallelize if BigInt-heavy

#### For General Applications
1. ✅ Don't prematurely optimize - use int64 when needed
2. ✅ Correctness > performance for financial/timestamp data
3. ✅ The 5x slowdown is still very fast (25μs vs 5μs per message)

### Conclusion

**BigInt is necessary and correct for 64-bit values.**

The performance cost (2-5x) is acceptable because:
- ✅ Prevents silent data corruption
- ✅ Only affects int64/uint64 fields
- ✅ Still provides excellent absolute performance
- ✅ No alternative for values > 2^53

**Design your schema wisely**: Use int32 by default, int64 only when required.

### Running the Benchmark

```bash
# Run the BigInt performance benchmark
node bigint-benchmark.js

# Or add to package.json:
npm run benchmark
```

See `bigint-benchmark.js` for detailed test code.
