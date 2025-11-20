/**
 * Generated SBE (Simple Binary Encoding) message codec.
 * WARNING: This code is auto-generated. Do not edit!
 */

import { MessageHeader } from './MessageHeader.js';
import { OuterWithOffsets } from './OuterWithOffsets.js';

class Msg2 {
  static TEMPLATE_ID = 2;
  static SCHEMA_ID = 3;
  static SCHEMA_VERSION = 0;
  static BLOCK_LENGTH = 32;

  constructor() {
    this._structure = new OuterWithOffsets();
  }

  /**
   * @param {Buffer} buffer
   * @param {number} [offset=0]
   * @returns {number}
   */
  encode(buffer, offset = 0) {
    const header = new MessageHeader();
    header.blockLength = Msg2.BLOCK_LENGTH;
    header.templateId = Msg2.TEMPLATE_ID;
    header.schemaId = Msg2.SCHEMA_ID;
    header.version = Msg2.SCHEMA_VERSION;
    let pos = offset + header.encode(buffer, offset);

    pos += this._structure.encode(buffer, pos);
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

    pos += this._structure.decode(buffer, pos);
    return pos - offset;
  }

  /** @returns {OuterWithOffsets} */
  get structure() {
    return this._structure;
  }

}

export { Msg2 };
