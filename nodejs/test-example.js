/**
 * Test example demonstrating Node.js SBE codec usage
 * This tests all major features including BigInt, BitSet, VarData, and Groups
 */

import { Car } from './generated/Car.js';
import { Model } from './generated/Model.js';
import { BooleanType } from './generated/BooleanType.js';

console.log('=== SBE Node.js Codec Test ===\n');

// Create and populate a Car message
const car = new Car();

// Basic fields
car.serialNumber = 12345;
car.modelYear = 2024;
car.available = BooleanType.T;
car.code = Model.A;

// Array field
car.someNumbers = [100, 200, 300, 400, 500];

// Char array (string)
car.vehicleCode = 'ABC123';

// BitSet
car.extras.sunRoof = true;
car.extras.sportsPack = false;
car.extras.cruiseControl = true;

// Composite
car.engine.capacity = 2000;
car.engine.numCylinders = 4;
car.engine.manufacturerCode = 'ENG';

// Group: fuelFigures (repeating group)
car.addFuelFigures(2);
car.fuelFigures[0].speed = 30;
car.fuelFigures[0].mpg = 35.5;
car.fuelFigures[1].speed = 60;
car.fuelFigures[1].mpg = 42.0;

// Nested group: performanceFigures
car.addPerformanceFigures(1);
car.performanceFigures[0].octaneRating = 95;
car.performanceFigures[0].addPerformanceFiguresAcceleration(2);
car.performanceFigures[0].acceleration[0].mph = 30;
car.performanceFigures[0].acceleration[0].seconds = 4.5;
car.performanceFigures[0].acceleration[1].mph = 60;
car.performanceFigures[0].acceleration[1].seconds = 7.8;

// VarData (variable length strings)
car.manufacturer = 'Toyota';
car.model = 'Camry';

// Encode the message
const buffer = Buffer.allocUnsafe(512);
let encodedSize;
try {
    encodedSize = car.encode(buffer, 0);
    console.log(`✓ Encoded ${encodedSize} bytes\n`);
} catch (error) {
    console.error('✗ Encoding failed:', error.message);
    process.exit(1);
}

// Decode the message
const car2 = new Car();
let decodedSize;
try {
    decodedSize = car2.decode(buffer, 0);
    console.log(`✓ Decoded ${decodedSize} bytes\n`);
} catch (error) {
    console.error('✗ Decoding failed:', error.message);
    process.exit(1);
}

// Verify decoded values
console.log('=== Decoded Values ===');
console.log(`Serial Number: ${car2.serialNumber}`);
console.log(`Model Year: ${car2.modelYear}`);
console.log(`Available: ${car2.available === BooleanType.T ? 'Yes' : 'No'}`);
console.log(`Model Code: ${car2.code} (${car2.code === Model.A ? 'Model A' : 'Unknown'})`);
console.log(`Some Numbers: [${car2.someNumbers.join(', ')}]`);
console.log(`Vehicle Code: ${car2.vehicleCode}`);

console.log('\nExtras (BitSet):');
console.log(`  Sun Roof: ${car2.extras.sunRoof}`);
console.log(`  Sports Pack: ${car2.extras.sportsPack}`);
console.log(`  Cruise Control: ${car2.extras.cruiseControl}`);

console.log('\nEngine (Composite):');
console.log(`  Capacity: ${car2.engine.capacity} cc`);
console.log(`  Cylinders: ${car2.engine.numCylinders}`);
console.log(`  Manufacturer Code: ${car2.engine.manufacturerCode}`);

console.log('\nFuel Figures (Group):');
car2.fuelFigures.forEach((fig, i) => {
    console.log(`  [${i}] Speed: ${fig.speed} mph, MPG: ${fig.mpg}`);
});

console.log('\nPerformance Figures (Nested Group):');
car2.performanceFigures.forEach((pf, i) => {
    console.log(`  [${i}] Octane Rating: ${pf.octaneRating}`);
    pf.acceleration.forEach((acc, j) => {
        console.log(`    [${j}] ${acc.mph} mph in ${acc.seconds} seconds`);
    });
});

console.log('\nVarData (Variable Length Strings):');
console.log(`  Manufacturer: ${car2.manufacturer}`);
console.log(`  Model: ${car2.model}`);

// Verification
const allMatch =
    car2.serialNumber === car.serialNumber &&
    car2.modelYear === car.modelYear &&
    car2.vehicleCode === car.vehicleCode &&
    car2.manufacturer === car.manufacturer &&
    car2.model === car.model &&
    car2.fuelFigures.length === car.fuelFigures.length &&
    car2.performanceFigures.length === car.performanceFigures.length;

console.log('\n=== Result ===');
if (allMatch) {
    console.log('✓ All tests passed! Encode/decode cycle successful.');
    process.exit(0);
} else {
    console.log('✗ Test failed! Data mismatch after decode.');
    process.exit(1);
}
