/**
 * Generated SBE (Simple Binary Encoding) message codec.
 * WARNING: This code is auto-generated. Do not edit!
 */

/**
 * set as uint32
 */
class SET {
  /**
   * @param {number} [value=0]
   */
  constructor(value = 0) {
    this.value = value;
  }

  /**
   * @returns {number}
   */
  getValue() {
    return this.value;
  }

  /**
   * @returns {boolean}
   */
  get bit0() {
    return (this.value & 1) !== 0;
  }

  /**
   * @param {boolean} value
   */
  set bit0(value) {
    if (value) {
      this.value |= 1;
    } else {
      this.value &= ~1;
    }
  }

  /**
   * @returns {boolean}
   */
  get bit16() {
    return (this.value & 65536) !== 0;
  }

  /**
   * @param {boolean} value
   */
  set bit16(value) {
    if (value) {
      this.value |= 65536;
    } else {
      this.value &= ~65536;
    }
  }

  /**
   * @returns {boolean}
   */
  get bit26() {
    return (this.value & 67108864) !== 0;
  }

  /**
   * @param {boolean} value
   */
  set bit26(value) {
    if (value) {
      this.value |= 67108864;
    } else {
      this.value &= ~67108864;
    }
  }

}

export { SET };
