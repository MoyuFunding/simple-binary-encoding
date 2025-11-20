/**
 * Generated SBE (Simple Binary Encoding) message codec.
 * WARNING: This code is auto-generated. Do not edit!
 */

/**
 * Message identifiers and length of message root
 */
class MessageHeader {
  static SIZE = 8;

  constructor() {
    this._blockLength = 0;
    this._templateId = 0;
    this._schemaId = 0;
    this._version = 0;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  encode(buffer, offset) {
    buffer.writeUInt16LE(this._blockLength, offset + 0);
    buffer.writeUInt16LE(this._templateId, offset + 2);
    buffer.writeUInt16LE(this._schemaId, offset + 4);
    buffer.writeUInt16LE(this._version, offset + 6);
    return 8;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} offset
   * @returns {number}
   */
  decode(buffer, offset) {
    this._blockLength = buffer.readUInt16LE(offset + 0);
    this._templateId = buffer.readUInt16LE(offset + 2);
    this._schemaId = buffer.readUInt16LE(offset + 4);
    this._version = buffer.readUInt16LE(offset + 6);
    return 8;
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
  get templateId() {
    return this._templateId;
  }

  /** @param {number} value */
  set templateId(value) {
    this._templateId = value;
  }

  /** @returns {number} */
  get schemaId() {
    return this._schemaId;
  }

  /** @param {number} value */
  set schemaId(value) {
    this._schemaId = value;
  }

  /** @returns {number} */
  get version() {
    return this._version;
  }

  /** @param {number} value */
  set version(value) {
    this._version = value;
  }

}

export { MessageHeader };
