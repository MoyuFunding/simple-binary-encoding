/**
 * Generated SBE (Simple Binary Encoding) message codec.
 * WARNING: This code is auto-generated. Do not edit!
 */

class OptionalExtras {
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
  get sunRoof() {
    return (this.value & 1) !== 0;
  }

  /**
   * @param {boolean} value
   */
  set sunRoof(value) {
    if (value) {
      this.value |= 1;
    } else {
      this.value &= ~1;
    }
  }

  /**
   * @returns {boolean}
   */
  get sportsPack() {
    return (this.value & 2) !== 0;
  }

  /**
   * @param {boolean} value
   */
  set sportsPack(value) {
    if (value) {
      this.value |= 2;
    } else {
      this.value &= ~2;
    }
  }

  /**
   * @returns {boolean}
   */
  get cruiseControl() {
    return (this.value & 4) !== 0;
  }

  /**
   * @param {boolean} value
   */
  set cruiseControl(value) {
    if (value) {
      this.value |= 4;
    } else {
      this.value &= ~4;
    }
  }

}

export { OptionalExtras };
