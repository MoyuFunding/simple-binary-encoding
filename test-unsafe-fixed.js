// Test fixed unsafe mode
import { MessageHeader } from './nodejs/okx-test-unsafe-fixed/MessageHeader.js';
import { BooksL2TbtChannelEvent } from './nodejs/okx-test-unsafe-fixed/BooksL2TbtChannelEvent.js';

const base64Data = 'KgDpAwEAAAADAAAAAAAAAAAsTI4TRAYA8zBMjhNEBgB34PQmDwAAAHHg9CYPAAAA//gUAAMANBgNAAAAAAC5VykDAAAAAAIAAABMGA0AAAAAAMJDgwEAAAAAAQAAACUaDQAAAAAAAAAAAAAAAAAAAAAAFAAAAAAAAAAAAAAAAAA=';
const buffer = Buffer.from(base64Data, 'base64');

console.log('=== Testing FIXED Unsafe Mode ===\n');

try {
    const message = new BooksL2TbtChannelEvent();
    const bytesDecoded = message.decode(buffer, 0);

    console.log('✓ Decoded successfully!');
    console.log('');
    console.log('instIdCode:', message.instIdCode, '(expected: 3)');
    console.log('tsUs:', message.tsUs);
    console.log('outTime:', message.outTime);
    console.log('seqId:', message.seqId);
    console.log('prevSeqId:', message.prevSeqId);
    console.log('pxExponent:', message.pxExponent);
    console.log('szExponent:', message.szExponent);
    console.log('');

    console.log('Asks (' + message.asks.length + ' entries):');
    message.asks.forEach((ask, i) => {
        const price = ask.pxMantissa * Math.pow(10, message.pxExponent);
        const size = ask.szMantissa * Math.pow(10, message.szExponent);
        console.log(`  [${i}] pxMantissa: ${ask.pxMantissa}, Price: ${price.toFixed(2)}, Size: ${size.toFixed(8)}, Orders: ${ask.ordCount}`);
    });
    console.log('');

    console.log('Bids (' + message.bids.length + ' entries):');
    message.bids.forEach((bid, i) => {
        const price = bid.pxMantissa * Math.pow(10, message.pxExponent);
        const size = bid.szMantissa * Math.pow(10, message.szExponent);
        console.log(`  [${i}] pxMantissa: ${bid.pxMantissa}, Price: ${price.toFixed(2)}, Size: ${size.toFixed(8)}, Orders: ${bid.ordCount}`);
    });
    console.log('');

    console.log('Total bytes decoded:', bytesDecoded);

    // Verification
    console.log('');
    console.log('=== Verification ===');
    const checks = [
        { name: 'instIdCode', actual: message.instIdCode, expected: 3 },
        { name: 'seqId', actual: message.seqId, expected: 65078091895 },
        { name: 'asks.length', actual: message.asks.length, expected: 3 },
        { name: 'asks[0].pxMantissa', actual: message.asks[0].pxMantissa, expected: 858164 },
        { name: 'asks[0].szMantissa', actual: message.asks[0].szMantissa, expected: 53041081 },
        { name: 'asks[0].ordCount', actual: message.asks[0].ordCount, expected: 2 },
    ];

    let allPassed = true;
    checks.forEach(check => {
        const passed = check.actual === check.expected;
        if (passed) {
            console.log(`✓ ${check.name} = ${check.actual}`);
        } else {
            console.log(`✗ ${check.name}: got ${check.actual}, expected ${check.expected}`);
            allPassed = false;
        }
    });

    // Round-trip test
    console.log('');
    console.log('=== Round-trip Test ===');
    const encodeBuffer = Buffer.alloc(256);
    const encodedSize = message.encode(encodeBuffer, 0);
    console.log('Encoded size:', encodedSize);

    const decoded2 = new BooksL2TbtChannelEvent();
    decoded2.decode(encodeBuffer, 0);

    console.log('Re-decoded instIdCode:', decoded2.instIdCode);
    console.log('Re-decoded seqId:', decoded2.seqId);
    console.log('Re-decoded asks[0].pxMantissa:', decoded2.asks[0].pxMantissa);

    if (decoded2.instIdCode === message.instIdCode &&
        decoded2.seqId === message.seqId &&
        decoded2.asks[0].pxMantissa === message.asks[0].pxMantissa) {
        console.log('✓ Round-trip successful!');
    } else {
        console.log('✗ Round-trip failed!');
        allPassed = false;
    }

    // Test with large numbers (near precision limit)
    console.log('');
    console.log('=== Precision Test ===');
    const testMsg = new BooksL2TbtChannelEvent();
    testMsg.instIdCode = Number.MAX_SAFE_INTEGER; // 2^53 - 1
    testMsg.seqId = 123456789012345; // Within safe range
    testMsg.pxExponent = -2;
    testMsg.szExponent = -8;

    const testBuffer = Buffer.alloc(256);
    testMsg.encode(testBuffer, 0);

    const decodedTest = new BooksL2TbtChannelEvent();
    decodedTest.decode(testBuffer, 0);

    console.log('MAX_SAFE_INTEGER:', Number.MAX_SAFE_INTEGER);
    console.log('Encoded:', testMsg.instIdCode);
    console.log('Decoded:', decodedTest.instIdCode);
    console.log('Match:', testMsg.instIdCode === decodedTest.instIdCode ? '✓' : '✗');

    console.log('');
    if (allPassed) {
        console.log('🎉 All tests PASSED! Unsafe mode is now working correctly!');
    } else {
        console.log('❌ Some tests failed');
    }

} catch (error) {
    console.error('ERROR:');
    console.error('  Message:', error.message);
    console.error('  Stack:', error.stack);
}
