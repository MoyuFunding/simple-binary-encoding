/**
 * Generated SBE (Simple Binary Encoding) message codec.
 * WARNING: This code is auto-generated. Do not edit!
 */

import { MessageHeader } from './MessageHeader.js';
import { OptionalExtras } from './OptionalExtras.js';
import { BooleanType } from './BooleanType.js';
import { Model } from './Model.js';
import { Engine } from './Engine.js';

/**
 * Description of a basic Car
 */
class Car {
  static TEMPLATE_ID = 1;
  static SCHEMA_ID = 1;
  static SCHEMA_VERSION = 1;
  static BLOCK_LENGTH = 41;

  constructor() {
    this._serialNumber = 0;
    this._modelYear = 0;
    this._available = 0;
    this._code = 0;
    this._someNumbers = new Array(5).fill(0);
    this._vehicleCode = '';
    this._extras = new OptionalExtras();
    this._engine = new Engine();
    this._fuelFigures = [];
    this._performanceFigures = [];
    this._manufacturer = '';
    this._model = '';
  }

  /**
   * @param {Buffer} buffer
   * @param {number} [offset=0]
   * @returns {number}
   */
  encode(buffer, offset = 0) {
    const header = new MessageHeader();
    header.blockLength = Car.BLOCK_LENGTH;
    header.templateId = Car.TEMPLATE_ID;
    header.schemaId = Car.SCHEMA_ID;
    header.version = Car.SCHEMA_VERSION;
    let pos = offset + header.encode(buffer, offset);

    buffer.writeUInt32LE(this._serialNumber, pos);
    pos += 4;
    buffer.writeUInt16LE(this._modelYear, pos);
    pos += 2;
    buffer.writeUInt8(this._available, pos);
    pos += 1;
    buffer.writeUInt8(this._code, pos);
    pos += 1;
    // Encode array: someNumbers
    for (let i = 0; i < 5; i++) {
      buffer.writeInt32LE(this._someNumbers[i], pos + i * 4);
    }
    pos += 20;
    // Encode char array: vehicleCode
    for (let i = 0; i < 6; i++) {
      buffer.writeUInt8(i < this._vehicleCode.length ? this._vehicleCode.charCodeAt(i) : 0, pos + i);
    }
    pos += 6;
    buffer.writeUInt8(this._extras.value, pos);
    pos += 1;
    pos += this._engine.encode(buffer, pos);
    // Encode group: fuelFigures
    buffer.writeUInt16LE(6, pos);
    pos += 2;
    buffer.writeUInt16LE(this._fuelFigures.length, pos);
    pos += 2;
    for (const entry of this._fuelFigures) {
      pos += entry.encode(buffer, pos);
    }

    // Encode group: performanceFigures
    buffer.writeUInt16LE(1, pos);
    pos += 2;
    buffer.writeUInt16LE(this._performanceFigures.length, pos);
    pos += 2;
    for (const entry of this._performanceFigures) {
      pos += entry.encode(buffer, pos);
    }

    // Encode varData: manufacturer
    const manufacturerLength = Buffer.byteLength(this._manufacturer, 'latin1');
    buffer.writeUInt32LE(manufacturerLength, pos);
    pos += 4;
    buffer.write(this._manufacturer, pos, manufacturerLength, 'latin1');
    pos += manufacturerLength;

    // Encode varData: model
    const modelLength = Buffer.byteLength(this._model, 'latin1');
    buffer.writeUInt32LE(modelLength, pos);
    pos += 4;
    buffer.write(this._model, pos, modelLength, 'latin1');
    pos += modelLength;

    return pos - offset;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} [offset=0]
   * @returns {number}
   */
  decode(buffer, offset = 0) {
    const header = new MessageHeader();
    let pos = offset + header.decode(buffer, offset);

    this._serialNumber = buffer.readUInt32LE(pos);
    pos += 4;
    this._modelYear = buffer.readUInt16LE(pos);
    pos += 2;
    this._available = buffer.readUInt8(pos);
    pos += 1;
    this._code = buffer.readUInt8(pos);
    pos += 1;
    // Decode array: someNumbers
    for (let i = 0; i < 5; i++) {
      this._someNumbers[i] = buffer.readInt32LE(pos + i * 4);
    }
    pos += 20;
    // Decode char array: vehicleCode
    let vehicleCodeChars = '';
    for (let i = 0; i < 6; i++) {
      const charCode = buffer.readUInt8(pos + i);
      if (charCode === 0) break;
      vehicleCodeChars += String.fromCharCode(charCode);
    }
    this._vehicleCode = vehicleCodeChars;
    pos += 6;
    this._extras.value = buffer.readUInt8(pos);
    pos += 1;
    pos += this._engine.decode(buffer, pos);
    // Decode group: fuelFigures
    const fuelFiguresBlockLength = buffer.readUInt16LE(pos);
    pos += 2;
    const fuelFiguresNumInGroup = buffer.readUInt16LE(pos);
    pos += 2;
    this._fuelFigures = [];
    for (let i = 0; i < fuelFiguresNumInGroup; i++) {
      const entry = new FuelFigures();
      pos += entry.decode(buffer, pos);
      this._fuelFigures.push(entry);
    }

    // Decode group: performanceFigures
    const performanceFiguresBlockLength = buffer.readUInt16LE(pos);
    pos += 2;
    const performanceFiguresNumInGroup = buffer.readUInt16LE(pos);
    pos += 2;
    this._performanceFigures = [];
    for (let i = 0; i < performanceFiguresNumInGroup; i++) {
      const entry = new PerformanceFigures();
      pos += entry.decode(buffer, pos);
      this._performanceFigures.push(entry);
    }

    // Decode varData: manufacturer
    let manufacturerLength = buffer.readUInt32LE(pos);
    pos += 4;
    this._manufacturer = buffer.toString('latin1', pos, pos + manufacturerLength);
    pos += manufacturerLength;

    // Decode varData: model
    let modelLength = buffer.readUInt32LE(pos);
    pos += 4;
    this._model = buffer.toString('latin1', pos, pos + modelLength);
    pos += modelLength;

    return pos - offset;
  }

  /** @returns {number} */
  get serialNumber() {
    return this._serialNumber;
  }

  /** @param {number} value */
  set serialNumber(value) {
    this._serialNumber = value;
  }

  /** @returns {number} */
  get modelYear() {
    return this._modelYear;
  }

  /** @param {number} value */
  set modelYear(value) {
    this._modelYear = value;
  }

  /** @returns {number} */
  get available() {
    return this._available;
  }

  /** @param {number} value */
  set available(value) {
    this._available = value;
  }

  /** @returns {number} */
  get code() {
    return this._code;
  }

  /** @param {number} value */
  set code(value) {
    this._code = value;
  }

  /** @returns {number[]} */
  get someNumbers() {
    return this._someNumbers;
  }

  /** @param {number[]} value */
  set someNumbers(value) {
    this._someNumbers = value;
  }

  /** @returns {string} */
  get vehicleCode() {
    return this._vehicleCode;
  }

  /** @param {string} value */
  set vehicleCode(value) {
    this._vehicleCode = value;
  }

  /** @returns {OptionalExtras} */
  get extras() {
    return this._extras;
  }

  /** @returns {Engine} */
  get engine() {
    return this._engine;
  }

  /** @returns {FuelFigures[]} */
  get fuelFigures() {
    return this._fuelFigures;
  }

  /** @param {number} count */
  addFuelFigures(count) {
    for (let i = 0; i < count; i++) {
      this._fuelFigures.push(new FuelFigures());
    }
  }

  /** @returns {PerformanceFigures[]} */
  get performanceFigures() {
    return this._performanceFigures;
  }

  /** @param {number} count */
  addPerformanceFigures(count) {
    for (let i = 0; i < count; i++) {
      this._performanceFigures.push(new PerformanceFigures());
    }
  }

  /** @returns {string} */
  get manufacturer() {
    return this._manufacturer;
  }

  /** @param {string} value */
  set manufacturer(value) {
    this._manufacturer = value;
  }

  /** @returns {string} */
  get model() {
    return this._model;
  }

  /** @param {string} value */
  set model(value) {
    this._model = value;
  }

}

class FuelFigures {
  constructor() {
    this._speed = 0;
    this._mpg = 0;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  encode(buffer, offset) {
    let pos = offset;

    buffer.writeUInt16LE(this._speed, pos);
    pos += 2;
    buffer.writeFloatLE(this._mpg, pos);
    pos += 4;
    return pos - offset;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  decode(buffer, offset) {
    let pos = offset;

    this._speed = buffer.readUInt16LE(pos);
    pos += 2;
    this._mpg = buffer.readFloatLE(pos);
    pos += 4;
    return pos - offset;
  }

  /** @returns {number} */
  get speed() {
    return this._speed;
  }

  /** @param {number} value */
  set speed(value) {
    this._speed = value;
  }

  /** @returns {number} */
  get mpg() {
    return this._mpg;
  }

  /** @param {number} value */
  set mpg(value) {
    this._mpg = value;
  }

}

class PerformanceFigures {
  constructor() {
    this._octaneRating = 0;
    this._acceleration = [];
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  encode(buffer, offset) {
    let pos = offset;

    buffer.writeUInt8(this._octaneRating, pos);
    pos += 1;
    buffer.writeUInt16LE(6, pos);
    pos += 2;
    buffer.writeUInt16LE(this._acceleration.length, pos);
    pos += 2;
    for (const entry of this._acceleration) {
      pos += entry.encode(buffer, pos);
    }
    return pos - offset;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  decode(buffer, offset) {
    let pos = offset;

    this._octaneRating = buffer.readUInt8(pos);
    pos += 1;
    const accelerationBlockLength = buffer.readUInt16LE(pos);
    pos += 2;
    const accelerationNumInGroup = buffer.readUInt16LE(pos);
    pos += 2;
    this._acceleration = [];
    for (let i = 0; i < accelerationNumInGroup; i++) {
      const entry = new PerformanceFiguresAcceleration();
      pos += entry.decode(buffer, pos);
      this._acceleration.push(entry);
    }
    return pos - offset;
  }

  /** @returns {number} */
  get octaneRating() {
    return this._octaneRating;
  }

  /** @param {number} value */
  set octaneRating(value) {
    this._octaneRating = value;
  }

  /** @returns {PerformanceFiguresAcceleration[]} */
  get acceleration() {
    return this._acceleration;
  }

  /** @param {number} count */
  addPerformanceFiguresAcceleration(count) {
    for (let i = 0; i < count; i++) {
      this._acceleration.push(new PerformanceFiguresAcceleration());
    }
  }

}

class PerformanceFiguresAcceleration {
  constructor() {
    this._mph = 0;
    this._seconds = 0;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  encode(buffer, offset) {
    let pos = offset;

    buffer.writeUInt16LE(this._mph, pos);
    pos += 2;
    buffer.writeFloatLE(this._seconds, pos);
    pos += 4;
    return pos - offset;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  decode(buffer, offset) {
    let pos = offset;

    this._mph = buffer.readUInt16LE(pos);
    pos += 2;
    this._seconds = buffer.readFloatLE(pos);
    pos += 4;
    return pos - offset;
  }

  /** @returns {number} */
  get mph() {
    return this._mph;
  }

  /** @param {number} value */
  set mph(value) {
    this._mph = value;
  }

  /** @returns {number} */
  get seconds() {
    return this._seconds;
  }

  /** @param {number} value */
  set seconds(value) {
    this._seconds = value;
  }

}

export { Car };
