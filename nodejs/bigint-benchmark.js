/**
 * BigInt vs Number Performance Benchmark
 * Tests the performance impact of using BigInt for 64-bit integers
 */

const ITERATIONS = 1000000;

console.log('=== BigInt vs Number Performance Benchmark ===\n');
console.log(`Iterations: ${ITERATIONS.toLocaleString()}\n`);

// Test 1: Arithmetic Operations
console.log('Test 1: Arithmetic Operations');
console.log('-'.repeat(50));

let start = performance.now();
let sum = 0;
for (let i = 0; i < ITERATIONS; i++) {
    sum = sum + i;
    sum = sum * 2;
    sum = sum / 2;
}
let numberTime = performance.now() - start;
console.log(`Number:  ${numberTime.toFixed(2)}ms`);

start = performance.now();
let sumBig = 0n;
for (let i = 0n; i < BigInt(ITERATIONS); i++) {
    sumBig = sumBig + i;
    sumBig = sumBig * 2n;
    sumBig = sumBig / 2n;
}
let bigintTime = performance.now() - start;
console.log(`BigInt:  ${bigintTime.toFixed(2)}ms`);
console.log(`Slowdown: ${(bigintTime / numberTime).toFixed(2)}x\n`);

// Test 2: Bitwise Operations (relevant for BitSets)
console.log('Test 2: Bitwise Operations (BitSets)');
console.log('-'.repeat(50));

start = performance.now();
let bits32 = 0;
for (let i = 0; i < ITERATIONS; i++) {
    bits32 = bits32 | (1 << (i % 32));
    bits32 = bits32 & 0xFF;
    const check = (bits32 & 1) !== 0;
}
let bits32Time = performance.now() - start;
console.log(`32-bit (Number):  ${bits32Time.toFixed(2)}ms`);

start = performance.now();
let bits64 = 0n;
for (let i = 0n; i < BigInt(ITERATIONS); i++) {
    bits64 = bits64 | (1n << (i % 64n));
    bits64 = bits64 & 0xFFn;
    const check = (bits64 & 1n) !== 0n;
}
let bits64Time = performance.now() - start;
console.log(`64-bit (BigInt):  ${bits64Time.toFixed(2)}ms`);
console.log(`Slowdown: ${(bits64Time / bits32Time).toFixed(2)}x\n`);

// Test 3: Buffer Read/Write (SBE use case)
console.log('Test 3: Buffer Read/Write (SBE encoding/decoding)');
console.log('-'.repeat(50));

const buffer = Buffer.allocUnsafe(8 * ITERATIONS);

start = performance.now();
for (let i = 0; i < ITERATIONS; i++) {
    buffer.writeInt32LE(i, (i % 100) * 8);
}
for (let i = 0; i < ITERATIONS; i++) {
    const val = buffer.readInt32LE((i % 100) * 8);
}
let int32Time = performance.now() - start;
console.log(`Int32:   ${int32Time.toFixed(2)}ms`);

start = performance.now();
for (let i = 0; i < ITERATIONS; i++) {
    buffer.writeBigInt64LE(BigInt(i), (i % 100) * 8);
}
for (let i = 0; i < ITERATIONS; i++) {
    const val = buffer.readBigInt64LE((i % 100) * 8);
}
let int64Time = performance.now() - start;
console.log(`Int64:   ${int64Time.toFixed(2)}ms`);
console.log(`Slowdown: ${(int64Time / int32Time).toFixed(2)}x\n`);

// Test 4: Real-world SBE scenario - Mixed operations
console.log('Test 4: Real-world SBE Scenario (mixed fields)');
console.log('-'.repeat(50));

class Message32 {
    constructor() {
        this.field1 = 0;
        this.field2 = 0;
        this.field3 = 0;
    }

    encode(buffer, offset) {
        buffer.writeInt32LE(this.field1, offset);
        buffer.writeInt32LE(this.field2, offset + 4);
        buffer.writeInt32LE(this.field3, offset + 8);
        return 12;
    }

    decode(buffer, offset) {
        this.field1 = buffer.readInt32LE(offset);
        this.field2 = buffer.readInt32LE(offset + 4);
        this.field3 = buffer.readInt32LE(offset + 8);
        return 12;
    }
}

class Message64 {
    constructor() {
        this.field1 = 0n;
        this.field2 = 0n;
        this.field3 = 0n;
    }

    encode(buffer, offset) {
        buffer.writeBigInt64LE(this.field1, offset);
        buffer.writeBigInt64LE(this.field2, offset + 8);
        buffer.writeBigInt64LE(this.field3, offset + 16);
        return 24;
    }

    decode(buffer, offset) {
        this.field1 = buffer.readBigInt64LE(offset);
        this.field2 = buffer.readBigInt64LE(offset + 8);
        this.field3 = buffer.readBigInt64LE(offset + 16);
        return 24;
    }
}

const testBuffer = Buffer.allocUnsafe(1024);
const msg32 = new Message32();
const msg64 = new Message64();

const MESSAGES = 100000;

start = performance.now();
for (let i = 0; i < MESSAGES; i++) {
    msg32.field1 = i;
    msg32.field2 = i * 2;
    msg32.field3 = i * 3;
    msg32.encode(testBuffer, 0);
    msg32.decode(testBuffer, 0);
}
let msg32Time = performance.now() - start;
console.log(`Message with 3x Int32:  ${msg32Time.toFixed(2)}ms`);

start = performance.now();
for (let i = 0; i < MESSAGES; i++) {
    msg64.field1 = BigInt(i);
    msg64.field2 = BigInt(i * 2);
    msg64.field3 = BigInt(i * 3);
    msg64.encode(testBuffer, 0);
    msg64.decode(testBuffer, 0);
}
let msg64Time = performance.now() - start;
console.log(`Message with 3x Int64:  ${msg64Time.toFixed(2)}ms`);
console.log(`Slowdown: ${(msg64Time / msg32Time).toFixed(2)}x\n`);

// Summary
console.log('=== Summary ===');
console.log('-'.repeat(50));
console.log('BigInt operations are typically 2-10x slower than Number');
console.log('However, this only affects fields that actually use int64/uint64');
console.log('');
console.log('Recommendation:');
console.log('✓ Use Number (int32/uint32) when values fit in 32 bits');
console.log('✓ Use BigInt (int64/uint64) only when necessary');
console.log('✓ Most financial applications use int64 for timestamps/prices');
console.log('✓ The correctness benefit outweighs the performance cost');
console.log('');
console.log('In a typical SBE message with mixed field types:');
console.log('- If 10% of fields are int64: ~10-20% overall slowdown');
console.log('- If 50% of fields are int64: ~50-100% overall slowdown');
console.log('- Pure int32 messages: no impact');
