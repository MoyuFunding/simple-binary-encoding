/**
 * Generated SBE (Simple Binary Encoding) message codec.
 * WARNING: This code is auto-generated. Do not edit!
 */

import { MessageHeader } from './MessageHeader.js';
import { Outer } from './Outer.js';

class Msg {
  static TEMPLATE_ID = 1;
  static SCHEMA_ID = 3;
  static SCHEMA_VERSION = 0;
  static BLOCK_LENGTH = 22;

  constructor() {
    this._structure = new Outer();
  }

  /**
   * @param {Buffer} buffer
   * @param {number} [offset=0]
   * @returns {number}
   */
  encode(buffer, offset = 0) {
    const header = new MessageHeader();
    header.blockLength = Msg.BLOCK_LENGTH;
    header.templateId = Msg.TEMPLATE_ID;
    header.schemaId = Msg.SCHEMA_ID;
    header.version = Msg.SCHEMA_VERSION;
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

  /** @returns {Outer} */
  get structure() {
    return this._structure;
  }

}

export { Msg };
