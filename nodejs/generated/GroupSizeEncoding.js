/**
 * Generated SBE (Simple Binary Encoding) message codec.
 * WARNING: This code is auto-generated. Do not edit!
 */

/**
 * Repeating group dimensions
 */
class GroupSizeEncoding {
  static SIZE = 4;

  constructor() {
    this._blockLength = 0;
    this._numInGroup = 0;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  encode(buffer, offset) {
    buffer.writeUInt16LE(this._blockLength, offset + 0);
    buffer.writeUInt16LE(this._numInGroup, offset + 2);
    return 4;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  decode(buffer, offset) {
    this._blockLength = buffer.readUInt16LE(offset + 0);
    this._numInGroup = buffer.readUInt16LE(offset + 2);
    return 4;
  }

  /** @returns {number} */
  get blockLength() {
    return this._blockLength;
  }

  /** @param {number} value */
  set blockLength(value) {
    this._blockLength = value;
  }

  /** @returns {number} */
  get numInGroup() {
    return this._numInGroup;
  }

  /** @param {number} value */
  set numInGroup(value) {
    this._numInGroup = value;
  }

}

export { GroupSizeEncoding };
