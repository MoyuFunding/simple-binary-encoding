# Node.js SBE Documentation Index

Welcome to the SBE Node.js code generator documentation! This directory contains comprehensive guides for using the Node.js/JavaScript code generator.

## 📚 Documentation Guide

### For Users (Start Here!)

1. **[QUICKSTART.md](./QUICKSTART.md)** - 5分钟快速开始 / 5-Minute Quick Start
   - Fastest way to get started
   - Basic usage examples
   - Common data types reference

2. **[使用指南.md](./使用指南.md)** - 完整中文使用指南 (Comprehensive Chinese Guide)
   - 详细的安装和代码生成说明 (38KB, 1,573 lines)
   - 所有数据类型详解 with examples
   - 高级特性：重复组、嵌套组
   - 性能优化技巧
   - 完整示例：文件、TCP、HTTP、消息路由
   - 常见问题解答

3. **[README.md](./README.md)** - English Technical Documentation
   - Overview of Node.js generator
   - API reference
   - Type mappings (SBE → JavaScript)
   - Integration guide (ES modules, CommonJS)
   - Performance notes

### For Developers

4. **[IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md)** - Implementation Status
   - List of completed features
   - Bug fixes (64-bit BigInt, VarData optimization)
   - Verification and test results
   - Code quality improvements

5. **[IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md)** - Developer Implementation Guide
   - Generator architecture
   - Code generation patterns
   - How to modify the generator

6. **[STATUS.md](./STATUS.md)** - Historical Status Document
   - Original implementation tracking
   - Feature completion status

### Code Examples

7. **[test-example.js](./test-example.js)** - Comprehensive Test Example
   - Demonstrates all features
   - Tests encode/decode cycle
   - Run with: `npm test`

### Configuration

8. **[package.json](./package.json)** - NPM Package Configuration
   - ES module setup (`"type": "module"`)
   - Node.js version requirement (>= 14.0.0)
   - Test script

## 🚀 Quick Navigation by Task

### I want to...

- **Start using SBE immediately** → [QUICKSTART.md](./QUICKSTART.md)
- **Learn how to compile and generate code** → [使用指南.md](./使用指南.md#安装与代码生成)
- **See working code examples** → [test-example.js](./test-example.js)
- **Understand type mappings** → [README.md](./README.md#type-mappings)
- **Optimize performance** → [使用指南.md](./使用指南.md#性能优化)
- **Debug generation issues** → [使用指南.md](./使用指南.md#常见生成问题)
- **Contribute to the generator** → [IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md)
- **Check what's been implemented** → [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md)

## 📖 Recommended Reading Order

### For New Users:
1. [QUICKSTART.md](./QUICKSTART.md) - Get running in 5 minutes
2. [使用指南.md](./使用指南.md) - Deep dive (Chinese) OR [README.md](./README.md) (English)
3. [test-example.js](./test-example.js) - Study the working example

### For Developers:
1. [README.md](./README.md) - Understand the generated code
2. [IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md) - Learn generator internals
3. [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md) - See what's been implemented

## ✅ Key Features Implemented

- ✅ **64-bit BigInt Support** - Correct handling of int64/uint64 in enums and bitsets
- ✅ **Optimized VarData Encoding** - Zero-allocation string encoding (10%+ faster)
- ✅ **ES6 Modules** - Modern JavaScript with import/export
- ✅ **JSDoc Type Annotations** - Full IDE support and type checking
- ✅ **Repeating Groups** - Including nested groups
- ✅ **All SBE Types** - Primitives, enums, bitsets, composites, constants, varData
- ✅ **Byte Order Support** - Little-endian and big-endian

## 🧪 Testing

Run the comprehensive test:

```bash
npm test
```

This runs [test-example.js](./test-example.js) which tests all features including:
- Basic fields (primitives)
- Arrays and char arrays
- BitSets with correct bit masks
- Composites (nested types)
- Repeating groups
- Nested groups
- Variable-length data (strings, binary)

## 📁 Directory Structure

```
nodejs/
├── INDEX.md                     # This file - documentation guide
├── QUICKSTART.md                # 5-minute quick start
├── 使用指南.md                   # Complete Chinese guide (38KB)
├── README.md                    # English technical docs
├── IMPLEMENTATION_STATUS.md     # What's been completed
├── IMPLEMENTATION_GUIDE.md      # Developer guide
├── STATUS.md                    # Historical tracking
├── test-example.js              # Working test example
├── package.json                 # NPM configuration
└── generated/                   # Generated codec files
    ├── Car.js
    ├── Model.js
    ├── OptionalExtras.js
    ├── Engine.js
    ├── MessageHeader.js
    └── ... (other generated files)
```

## 💡 Need Help?

1. Check the FAQ in [使用指南.md](./使用指南.md#常见问题)
2. Look at [test-example.js](./test-example.js) for working code
3. Review type mappings in [README.md](./README.md#type-mappings)
4. File an issue at: https://github.com/real-logic/simple-binary-encoding/issues

## 📝 License

All generated code and documentation are subject to the Apache 2.0 license.

---

**Last Updated**: 2025-11-20
**Generator Version**: SBE 1.36.2+
**Node.js Requirement**: >= 14.0.0
