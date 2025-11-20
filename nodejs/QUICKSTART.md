# Node.js SBE - 5分钟快速开始

## 第一步：安装环境

确保已安装：
- **Node.js** >= 14.0.0
- **Java** >= 17

## 第二步：生成代码

```bash
# 从 car.xml schema 生成 JavaScript 代码
./gradlew generateNodeJsCodecs \
  -Dsbe.output.dir=nodejs/generated \
  -Dsbe.target.language=uk.co.real_logic.sbe.generation.nodejs.NodeJs
```

## 第三步：编写代码

创建 `test.js`:

```javascript
import { Car } from './generated/Car.js';
import { Model } from './generated/Model.js';

// 创建并填充消息
const car = new Car();
car.serialNumber = 12345;
car.modelYear = 2024;
car.code = Model.A;
car.vehicleCode = 'ABC123';
car.manufacturer = 'Toyota';
car.model = 'Camry';

// 编码
const buffer = Buffer.allocUnsafe(512);
const size = car.encode(buffer, 0);
console.log(`✓ 编码了 ${size} 字节`);

// 解码
const car2 = new Car();
car2.decode(buffer, 0);
console.log(`制造商: ${car2.manufacturer}`);
console.log(`型号: ${car2.model}`);
console.log(`车辆代码: ${car2.vehicleCode}`);
```

## 第四步：配置 package.json

```json
{
  "type": "module",
  "engines": {
    "node": ">=14.0.0"
  }
}
```

## 第五步：运行

```bash
node test.js
```

输出：
```
✓ 编码了 76 字节
制造商: Toyota
型号: Camry
车辆代码: ABC123
```

## 🎉 完成！

你已经成功使用 SBE！

### 下一步

- 📖 阅读 [完整使用指南](./使用指南.md)
- 📚 查看 [示例代码](../sbe-samples/)
- 🔧 了解 [性能优化](./使用指南.md#性能优化)

### 常见数据类型

```javascript
// 基本类型
message.intField = 123;              // int32 → number
message.longField = 123456789n;      // int64 → bigint (注意 'n')

// 枚举
message.color = Color.RED;           // enum → number

// 数组
message.prices = [1, 2, 3, 4, 5];   // int32[5]
message.code = 'ABC123';             // char[6] → string

// 位集
message.flags.flag1 = true;          // bitset
message.flags.flag2 = false;

// 可变长度字符串
message.description = '描述文本';     // varData string

// 二进制数据
message.rawData = Buffer.from([0x01, 0x02]);  // varData Buffer

// 复合类型
message.engine.capacity = 2000;      // composite field

// 重复组
message.addItems(3);                 // group
message.items[0].name = 'Item 1';
```

### 提示

1. **BigInt**: 64位整数必须加 `n` 后缀: `123n`
2. **Buffer 重用**: 重用 Buffer 提高性能
3. **类型**: 查看生成代码的 JSDoc 注释了解类型
4. **错误处理**: 始终 try-catch encode/decode

需要帮助？查看 [完整文档](./使用指南.md) 或提交 [Issue](https://github.com/real-logic/simple-binary-encoding/issues)。
