/**
 * Generated SBE (Simple Binary Encoding) message codec.
 * WARNING: This code is auto-generated. Do not edit!
 */

class Inner {
  static SIZE = 16;

  constructor() {
    this._first = 0n;
    this._second = 0n;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  encode(buffer, offset) {
    buffer.writeBigInt64LE(this._first, offset + 0);
    buffer.writeBigInt64LE(this._second, offset + 8);
    return 16;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  decode(buffer, offset) {
    this._first = buffer.readBigInt64LE(offset + 0);
    this._second = buffer.readBigInt64LE(offset + 8);
    return 16;
  }

  /** @returns {bigint} */
  get first() {
    return this._first;
  }

  /** @param {bigint} value */
  set first(value) {
    this._first = value;
  }

  /** @returns {bigint} */
  get second() {
    return this._second;
  }

  /** @param {bigint} value */
  set second(value) {
    this._second = value;
  }

}

export { Inner };
