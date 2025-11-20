/**
 * Generated SBE (Simple Binary Encoding) message codec.
 * WARNING: This code is auto-generated. Do not edit!
 */

import { MessageHeader } from './MessageHeader.js';
import { ENUM } from './ENUM.js';
import { SET } from './SET.js';

class Message1 {
  static TEMPLATE_ID = 1;
  static SCHEMA_ID = 3;
  static SCHEMA_VERSION = 0;
  static BLOCK_LENGTH = 41;

  constructor() {
    this._header = new MessageHeader();
    this._eDTField = '';
    this._eNUMField = 0;
    this._sETField = new SET();
    this._int64Field = 0n;
  }

  /**
   * @param {Buffer} buffer
   * @param {number} [offset=0]
   * @returns {number}
   */
  encode(buffer, offset = 0) {
    const header = new MessageHeader();
    header.blockLength = Message1.BLOCK_LENGTH;
    header.templateId = Message1.TEMPLATE_ID;
    header.schemaId = Message1.SCHEMA_ID;
    header.version = Message1.SCHEMA_VERSION;
    let pos = offset + header.encode(buffer, offset);

    pos += this._header.encode(buffer, pos);
    // Encode char array: eDTField
    for (let i = 0; i < 20; i++) {
      buffer.writeUInt8(i < this._eDTField.length ? this._eDTField.charCodeAt(i) : 0, pos + i);
    }
    pos += 20;
    buffer.writeUInt8(this._eNUMField, pos);
    pos += 1;
    buffer.writeUInt32LE(this._sETField.value, pos);
    pos += 4;
    buffer.writeBigInt64LE(this._int64Field, pos);
    pos += 8;
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

    pos += this._header.decode(buffer, pos);
    // Decode char array: eDTField
    let eDTFieldChars = '';
    for (let i = 0; i < 20; i++) {
      const charCode = buffer.readUInt8(pos + i);
      if (charCode === 0) break;
      eDTFieldChars += String.fromCharCode(charCode);
    }
    this._eDTField = eDTFieldChars;
    pos += 20;
    this._eNUMField = buffer.readUInt8(pos);
    pos += 1;
    this._sETField.value = buffer.readUInt32LE(pos);
    pos += 4;
    this._int64Field = buffer.readBigInt64LE(pos);
    pos += 8;
    return pos - offset;
  }

  /** @returns {MessageHeader} */
  get header() {
    return this._header;
  }

  /** @returns {string} */
  get eDTField() {
    return this._eDTField;
  }

  /** @param {string} value */
  set eDTField(value) {
    this._eDTField = value;
  }

  /** @returns {number} */
  get eNUMField() {
    return this._eNUMField;
  }

  /** @param {number} value */
  set eNUMField(value) {
    this._eNUMField = value;
  }

  /** @returns {SET} */
  get sETField() {
    return this._sETField;
  }

  /** @returns {bigint} */
  get int64Field() {
    return this._int64Field;
  }

  /** @param {bigint} value */
  set int64Field(value) {
    this._int64Field = value;
  }

}

export { Message1 };
