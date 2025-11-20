/**
 * Generated SBE (Simple Binary Encoding) message codec.
 * WARNING: This code is auto-generated. Do not edit!
 */

class FuturesPrice {
  static SIZE = 9;

  constructor() {
    this._mantissa = 0n;
    this._exponent = 0;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  encode(buffer, offset) {
    buffer.writeBigInt64LE(this._mantissa, offset + 0);
    buffer.writeInt8(this._exponent, offset + 8);
    return 9;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  decode(buffer, offset) {
    this._mantissa = buffer.readBigInt64LE(offset + 0);
    this._exponent = buffer.readInt8(offset + 8);
    return 9;
  }

  /** @returns {bigint} */
  get mantissa() {
    return this._mantissa;
  }

  /** @param {bigint} value */
  set mantissa(value) {
    this._mantissa = value;
  }

  /** @returns {number} */
  get exponent() {
    return this._exponent;
  }

  /** @param {number} value */
  set exponent(value) {
    this._exponent = value;
  }

}

export { FuturesPrice };
