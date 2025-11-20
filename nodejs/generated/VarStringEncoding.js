/**
 * Generated SBE (Simple Binary Encoding) message codec.
 * WARNING: This code is auto-generated. Do not edit!
 */

class VarStringEncoding {
  static SIZE = 5;

  constructor() {
    this._length = 0;
    this._varData = 0;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  encode(buffer, offset) {
    buffer.writeUInt32LE(this._length, offset + 0);
    buffer.writeUInt8(this._varData, offset + 4);
    return 5;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  decode(buffer, offset) {
    this._length = buffer.readUInt32LE(offset + 0);
    this._varData = buffer.readUInt8(offset + 4);
    return 5;
  }

  /** @returns {number} */
  get length() {
    return this._length;
  }

  /** @param {number} value */
  set length(value) {
    this._length = value;
  }

  /** @returns {number} */
  get varData() {
    return this._varData;
  }

  /** @param {number} value */
  set varData(value) {
    this._varData = value;
  }

}

export { VarStringEncoding };
