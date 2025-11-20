/**
 * Generated SBE (Simple Binary Encoding) message codec.
 * WARNING: This code is auto-generated. Do not edit!
 */

class OuterWithOffsets {
  static SIZE = 17;

  constructor() {
    this._zeroth = 0;
    this._first = 0n;
    this._second = 0n;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  encode(buffer, offset) {
    buffer.writeUInt8(this._zeroth, offset + 0);
    buffer.writeBigInt64LE(this._first, offset + 1);
    buffer.writeBigInt64LE(this._second, offset + 9);
    return 17;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  decode(buffer, offset) {
    this._zeroth = buffer.readUInt8(offset + 0);
    this._first = buffer.readBigInt64LE(offset + 1);
    this._second = buffer.readBigInt64LE(offset + 9);
    return 17;
  }

  /** @returns {number} */
  get zeroth() {
    return this._zeroth;
  }

  /** @param {number} value */
  set zeroth(value) {
    this._zeroth = value;
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

export { OuterWithOffsets };
