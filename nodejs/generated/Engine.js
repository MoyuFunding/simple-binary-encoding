/**
 * Generated SBE (Simple Binary Encoding) message codec.
 * WARNING: This code is auto-generated. Do not edit!
 */

class Engine {
  static SIZE = 6;

  constructor() {
    this._capacity = 0;
    this._numCylinders = 0;
    this._manufacturerCode = '';
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  encode(buffer, offset) {
    buffer.writeUInt16LE(this._capacity, offset + 0);
    buffer.writeUInt8(this._numCylinders, offset + 2);
    for (let i = 0; i < 3; i++) {
      buffer.writeUInt8(i < this._manufacturerCode.length ? this._manufacturerCode.charCodeAt(i) : 0, offset + 3 + i);
    }
    return 6;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  decode(buffer, offset) {
    this._capacity = buffer.readUInt16LE(offset + 0);
    this._numCylinders = buffer.readUInt8(offset + 2);
    let manufacturerCodeChars = '';
    for (let i = 0; i < 3; i++) {
      const charCode = buffer.readUInt8(offset + 3 + i);
      if (charCode === 0) break;
      manufacturerCodeChars += String.fromCharCode(charCode);
    }
    this._manufacturerCode = manufacturerCodeChars;
    return 6;
  }

  /** @returns {number} */
  get capacity() {
    return this._capacity;
  }

  /** @param {number} value */
  set capacity(value) {
    this._capacity = value;
  }

  /** @returns {number} */
  get numCylinders() {
    return this._numCylinders;
  }

  /** @param {number} value */
  set numCylinders(value) {
    this._numCylinders = value;
  }

  /** @returns {number} */
  static get maxRpm() {
    return 9000;
  }

  /** @returns {string} */
  get manufacturerCode() {
    return this._manufacturerCode;
  }

  /** @param {string} value */
  set manufacturerCode(value) {
    this._manufacturerCode = value;
  }

  /** @returns {string} */
  static get fuel() {
    return 'Petrol';
  }

}

export { Engine };
