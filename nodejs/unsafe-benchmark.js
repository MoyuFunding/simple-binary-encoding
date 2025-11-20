/**
 * Unsafe Mode Performance Comparison
 * Compares safe mode (BigInt) vs unsafe mode (Number) performance
 *
 * To test:
 * 1. Generate with safe mode: ./gradlew generateNodeJsTestCodecs
 * 2. Run benchmark: node unsafe-benchmark.js
 * 3. Generate with unsafe mode: ./gradlew generateNodeJsTestCodecs -Dsbe.nodejs.unsafe.mode=true
 * 4. Run benchmark again: node unsafe-benchmark.js
 */

import { Car } from './generated/Car.js';
import { Model } from './generated/Model.js';
import { BooleanType } from './generated/BooleanType.js';

const ITERATIONS = 100000;

console.log('=== SBE Node.js Safe vs Unsafe Mode Benchmark ===\n');

// Create test message
const car = new Car();
car.serialNumber = 12345;
car.modelYear = 2024;
car.available = BooleanType.T;
car.code = Model.A;
car.someNumbers = [100, 200, 300, 400, 500];
car.vehicleCode = 'ABC123';
car.extras.sunRoof = true;
car.extras.cruiseControl = true;
car.engine.capacity = 2000;
car.engine.numCylinders = 4;
car.engine.manufacturerCode = 'ENG';
car.manufacturer = 'Toyota';
car.model = 'Camry';

// Add some groups
car.addFuelFigures(2);
car.fuelFigures[0].speed = 30;
car.fuelFigures[0].mpg = 35.5;
car.fuelFigures[1].speed = 60;
car.fuelFigures[1].mpg = 42.0;

const buffer = Buffer.allocUnsafe(512);

// Warmup
for (let i = 0; i < 1000; i++) {
    car.encode(buffer, 0);
    car.decode(buffer, 0);
}

// Encode benchmark
console.log(`Running ${ITERATIONS.toLocaleString()} encode operations...`);
const encodeStart = performance.now();
for (let i = 0; i < ITERATIONS; i++) {
    car.encode(buffer, 0);
}
const encodeTime = performance.now() - encodeStart;

// Decode benchmark
console.log(`Running ${ITERATIONS.toLocaleString()} decode operations...`);
const decodeStart = performance.now();
for (let i = 0; i < ITERATIONS; i++) {
    car.decode(buffer, 0);
}
const decodeTime = performance.now() - decodeStart;

// Combined benchmark
console.log(`Running ${ITERATIONS.toLocaleString()} encode+decode cycles...`);
const combinedStart = performance.now();
for (let i = 0; i < ITERATIONS; i++) {
    car.encode(buffer, 0);
    car.decode(buffer, 0);
}
const combinedTime = performance.now() - combinedStart;

// Results
console.log('\n=== Results ===');
console.log(`Encode:  ${encodeTime.toFixed(2)}ms  (${(ITERATIONS / encodeTime * 1000).toFixed(0)} msg/sec)`);
console.log(`Decode:  ${decodeTime.toFixed(2)}ms  (${(ITERATIONS / decodeTime * 1000).toFixed(0)} msg/sec)`);
console.log(`Both:    ${combinedTime.toFixed(2)}ms  (${(ITERATIONS / combinedTime * 1000).toFixed(0)} cycle/sec)`);

console.log('\n=== Mode Detection ===');
// Detect mode by checking serialNumber type
const testCar = new Car();
testCar.serialNumber = 123;
const testBuffer = Buffer.allocUnsafe(256);
testCar.encode(testBuffer, 0);
const testCar2 = new Car();
testCar2.decode(testBuffer, 0);

console.log(`Detected mode: ${typeof testCar2.serialNumber === 'bigint' ? 'SAFE (BigInt)' : 'UNSAFE (Number)'}`);

if (typeof testCar2.serialNumber === 'bigint') {
    console.log('\n⚠️  Using SAFE mode (BigInt for 64-bit integers)');
    console.log('To enable UNSAFE mode:');
    console.log('  ./gradlew generateNodeJsTestCodecs -Dsbe.nodejs.unsafe.mode=true');
} else {
    console.log('\n⚡ Using UNSAFE mode (Number for 64-bit integers)');
    console.log('WARNING: Values > 2^53 will lose precision!');
    console.log('Safe range: ±9,007,199,254,740,991');
}
