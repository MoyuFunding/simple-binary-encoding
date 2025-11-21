// Test script to decode OKX SBE binary data - CORRECTED
import { MessageHeader } from './nodejs/okx-test/MessageHeader.js';
import { BboTbtChannelEvent } from './nodejs/okx-test/BboTbtChannelEvent.js';
import { BooksL2TbtChannelEvent } from './nodejs/okx-test/BooksL2TbtChannelEvent.js';
import { SnapshotDepthResponseEvent } from './nodejs/okx-test/SnapshotDepthResponseEvent.js';

// Base64 encoded binary data from the user
const base64Data = 'KgDpAwEAAAADAAAAAAAAAAAsTI4TRAYA8zBMjhNEBgB34PQmDwAAAHHg9CYPAAAA//gUAAMANBgNAAAAAAC5VykDAAAAAAIAAABMGA0AAAAAAMJDgwEAAAAAAQAAACUaDQAAAAAAAAAAAAAAAAAAAAAAFAAAAAAAAAAAAAAAAAA=';

// Decode base64 to Buffer
const buffer = Buffer.from(base64Data, 'base64');

console.log('Buffer length:', buffer.length);
console.log('Buffer hex:', buffer.toString('hex'));
console.log('');

console.log('=== APPROACH 1: Peek at header first, then decode full message ===');
// First, peek at the header to determine which message type to decode
const header = new MessageHeader();
let headerSize = header.decode(buffer, 0);

console.log('Message Header:');
console.log('  Block Length:', header.blockLength);
console.log('  Template ID:', header.templateId);
console.log('  Schema ID:', header.schemaId);
console.log('  Version:', header.version);
console.log('  Header size:', headerSize, 'bytes');
console.log('');

// Now decode the full message based on template ID
// IMPORTANT: Start from offset 0, not from headerSize!
// The message decode() method will re-read the header
try {
    const templateId = header.templateId;
    let message;
    let offset;

    if (templateId === 1000) {
        console.log('Decoding BboTbtChannelEvent (ID: 1000)');
        message = new BboTbtChannelEvent();
        offset = message.decode(buffer, 0);  // Start from 0, not headerSize!
    } else if (templateId === 1001) {
        console.log('Decoding BooksL2TbtChannelEvent (ID: 1001)');
        message = new BooksL2TbtChannelEvent();
        offset = message.decode(buffer, 0);  // Start from 0, not headerSize!

        console.log('\nDecoded message:');
        console.log('  instIdCode:', message.instIdCode);
        console.log('  tsUs:', message.tsUs);
        console.log('  outTime:', message.outTime);
        console.log('  seqId:', message.seqId);
        console.log('  prevSeqId:', message.prevSeqId);
        console.log('  pxExponent:', message.pxExponent);
        console.log('  szExponent:', message.szExponent);

        console.log('\n  Asks (' + message.asks.length + ' entries):');
        message.asks.forEach((ask, i) => {
            // Convert mantissa to decimal using exponent
            const price = Number(ask.pxMantissa) * Math.pow(10, message.pxExponent);
            const size = Number(ask.szMantissa) * Math.pow(10, message.szExponent);
            console.log(`    [${i}] Price: ${price.toFixed(8)}, Size: ${size.toFixed(8)}, Orders: ${ask.ordCount}`);
        });

        console.log('\n  Bids (' + message.bids.length + ' entries):');
        message.bids.forEach((bid, i) => {
            const price = Number(bid.pxMantissa) * Math.pow(10, message.pxExponent);
            const size = Number(bid.szMantissa) * Math.pow(10, message.szExponent);
            console.log(`    [${i}] Price: ${price.toFixed(8)}, Size: ${size.toFixed(8)}, Orders: ${bid.ordCount}`);
        });
    } else if (templateId === 1006) {
        console.log('Decoding SnapshotDepthResponseEvent (ID: 1006)');
        message = new SnapshotDepthResponseEvent();
        offset = message.decode(buffer, 0);  // Start from 0, not headerSize!
    } else {
        console.log('Unknown template ID:', templateId);
    }

    console.log('\nTotal bytes decoded:', offset);
    console.log('Buffer length:', buffer.length);
    console.log('Success!');
} catch (error) {
    console.error('\nERROR during decode:');
    console.error('  Message:', error.message);
    console.error('  Stack:', error.stack);
}

console.log('\n');
console.log('=== APPROACH 2: Direct decode without peeking ===');
console.log('If you know the message type, you can decode directly:');

try {
    const message = new BooksL2TbtChannelEvent();
    const offset = message.decode(buffer, 0);

    console.log('Successfully decoded BooksL2TbtChannelEvent');
    console.log('  seqId:', message.seqId);
    console.log('  Asks count:', message.asks.length);
    console.log('  Bids count:', message.bids.length);
    console.log('  Total bytes:', offset);
} catch (error) {
    console.error('ERROR:', error.message);
}
