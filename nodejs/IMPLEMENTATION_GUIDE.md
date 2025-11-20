# Node.js Generator - 完整功能实现指南

## 当前状态

当前的 Node.js 生成器支持：
- ✅ 基本原始类型字段 (int8, int16, int32, int64, uint8, uint16, uint32, uint64, float, double)
- ✅ 枚举类型 (Enums)
- ✅ 位集类型 (BitSets/Sets)
- ✅ 独立的复合类型 (Composite types)
- ✅ Message Header
- ✅ ES6 模块导出
- ✅ JSDoc 类型注释

## 缺失功能

需要添加以下功能以支持完整的 SBE schema：

### 1. 复合类型作为消息字段 (Composite Fields)

**问题**: 当前只处理 `Signal.ENCODING`，但复合类型字段是 `Signal.BEGIN_COMPOSITE`

**示例**:
```xml
<field name="engine" id="8" type="Engine"/>
```

**需要的改动**:
```javascript
// 在 generateMessageClass 中，需要检测 BEGIN_COMPOSITE
if (encodingToken.signal() == Signal.BEGIN_COMPOSITE) {
    // 生成复合类型字段
    // 导入复合类型类
    // 在构造函数中初始化: this._engine = new Engine();
    // encode: pos += this._engine.encode(buffer, pos);
    // decode: pos += this._engine.decode(buffer, pos);
}
```

**生成的代码应该是**:
```javascript
import { Engine } from './Engine.js';

class Car {
  constructor() {
    this._engine = new Engine();
  }

  encode(buffer, offset = 0) {
    // ... 其他字段
    pos += this._engine.encode(buffer, pos);
    // ...
  }

  get engine() {
    return this._engine;
  }
}
```

### 2. 重复组 (Repeating Groups)

**问题**: 完全未实现

**示例**:
```xml
<group name="fuelFigures" id="9" dimensionType="groupSizeEncoding">
    <field name="speed" id="10" type="uint16"/>
    <field name="mpg" id="11" type="float"/>
</group>
```

**需要的改动**:

1. 在消息生成后收集 groups:
```java
final List<Token> groups = new ArrayList<>();
i = collectGroups(messageBody, i, groups);
```

2. 为每个 group 生成嵌套类:
```javascript
class FuelFiguresGroup {
  constructor() {
    this._speed = 0;
    this._mpg = 0;
  }

  encode(buffer, offset) {
    let pos = offset;
    buffer.writeUInt16LE(this._speed, pos);
    pos += 2;
    buffer.writeFloatLE(this._mpg, pos);
    pos += 4;
    return pos - offset;
  }

  decode(buffer, offset) {
    let pos = offset;
    this._speed = buffer.readUInt16LE(pos);
    pos += 2;
    this._mpg = buffer.readFloatLE(pos);
    pos += 4;
    return pos - offset;
  }

  get speed() { return this._speed; }
  set speed(value) { this._speed = value; }

  get mpg() { return this._mpg; }
  set mpg(value) { this._mpg = value; }
}
```

3. 在主消息类中:
```javascript
class Car {
  constructor() {
    this._fuelFigures = [];
  }

  /**
   * @returns {FuelFiguresGroup[]}
   */
  get fuelFigures() {
    return this._fuelFigures;
  }

  /**
   * @param {number} count
   */
  setFuelFiguresCount(count) {
    this._fuelFigures = new Array(count);
    for (let i = 0; i < count; i++) {
      this._fuelFigures[i] = new FuelFiguresGroup();
    }
  }

  encode(buffer, offset = 0) {
    let pos = offset;
    // ... 编码头部和固定字段

    // 编码 group header (blockLength + numInGroup)
    buffer.writeUInt16LE(6, pos); // FuelFiguresGroup 的 blockLength
    pos += 2;
    buffer.writeUInt16LE(this._fuelFigures.length, pos); // numInGroup
    pos += 2;

    // 编码每个 group entry
    for (const entry of this._fuelFigures) {
      pos += entry.encode(buffer, pos);
    }

    return pos - offset;
  }

  decode(buffer, offset = 0) {
    let pos = offset;
    // ... 解码头部和固定字段

    // 解码 group header
    const blockLength = buffer.readUInt16LE(pos);
    pos += 2;
    const numInGroup = buffer.readUInt16LE(pos);
    pos += 2;

    // 解码每个 group entry
    this._fuelFigures = new Array(numInGroup);
    for (let i = 0; i < numInGroup; i++) {
      this._fuelFigures[i] = new FuelFiguresGroup();
      pos += this._fuelFigures[i].decode(buffer, pos);
    }

    return pos - offset;
  }
}
```

### 3. 可变长度数据 (Variable-Length Data)

**问题**: 完全未实现

**示例**:
```xml
<data name="manufacturer" id="17" type="varStringEncoding"/>
```

**需要的改动**:

1. 收集 varData 字段:
```java
final List<Token> varData = new ArrayList<>();
collectVarData(messageBody, i, varData);
```

2. 生成代码:
```javascript
class Car {
  constructor() {
    this._manufacturer = '';
    this._model = '';
  }

  /**
   * @returns {string}
   */
  get manufacturer() {
    return this._manufacturer;
  }

  /**
   * @param {string} value
   */
  set manufacturer(value) {
    this._manufacturer = value;
  }

  encode(buffer, offset = 0) {
    let pos = offset;
    // ... 编码固定字段和 groups

    // 编码 varData (manufacturer)
    const manufacturerBytes = Buffer.from(this._manufacturer, 'utf8');
    buffer.writeUInt32LE(manufacturerBytes.length, pos);
    pos += 4;
    manufacturerBytes.copy(buffer, pos);
    pos += manufacturerBytes.length;

    // 编码 varData (model)
    const modelBytes = Buffer.from(this._model, 'utf8');
    buffer.writeUInt32LE(modelBytes.length, pos);
    pos += 4;
    modelBytes.copy(buffer, pos);
    pos += modelBytes.length;

    return pos - offset;
  }

  decode(buffer, offset = 0) {
    let pos = offset;
    // ... 解码固定字段和 groups

    // 解码 varData (manufacturer)
    let length = buffer.readUInt32LE(pos);
    pos += 4;
    this._manufacturer = buffer.toString('utf8', pos, pos + length);
    pos += length;

    // 解码 varData (model)
    length = buffer.readUInt32LE(pos);
    pos += 4;
    this._model = buffer.toString('utf8', pos, pos + length);
    pos += length;

    return pos - offset;
  }
}
```

## 实现步骤

### 步骤 1: 重构 generateMessageClass

修改以正确区分和处理所有类型的字段：

```java
private void generateMessageClass(final StringBuilder sb, final String className, final List<Token> tokens)
{
    // 1. 收集所有类型的字段
    final List<Token> messageBody = tokens.subList(1, tokens.size() - 1);
    int i = 0;

    final List<Token> fields = new ArrayList<>();
    i = collectFields(messageBody, i, fields);

    final List<Token> groups = new ArrayList<>();
    i = collectGroups(messageBody, i, groups);

    final List<Token> varData = new ArrayList<>();
    collectVarData(messageBody, i, varData);

    // 2. 生成类声明和静态常量
    // 3. 生成构造函数（初始化所有字段类型）
    // 4. 生成 encode 方法（处理所有字段、groups、varData）
    // 5. 生成 decode 方法（处理所有字段、groups、varData）
    // 6. 生成字段访问器
    // 7. 生成嵌套的 group 类
}
```

### 步骤 2: 添加辅助方法

```java
private void generateGroupClasses(final StringBuilder sb, final List<Token> groups,
    final String parentClassName)
{
    // 为每个 group 生成嵌套类
}

private void generateVarDataAccessors(final StringBuilder sb, final List<Token> varData)
{
    // 生成字符串或字节数组的 getter/setter
}

private void handleCompositeField(/* parameters */)
{
    // 处理复合类型字段的编码/解码
}
```

### 步骤 3: 更新 encode/decode 方法

需要按顺序处理：
1. Message Header
2. 固定长度字段（primitives 和 composites）
3. 重复组（每个组都有自己的 header）
4. 可变长度数据

### 步骤 4: 处理嵌套组

```java
private void generateNestedGroups(final StringBuilder sb, final List<Token> tokens,
    final String groupName)
{
    // 递归处理嵌套的 groups
}
```

## 测试验证

完成后，使用 car.xml 生成代码并验证：

```bash
./gradlew :sbe-tool:compileJava
./gradlew generateNodeJsTestCodecs
```

检查生成的 `nodejs/generated/Car.js` 应该包含：
- ✅ Engine 复合类型字段
- ✅ FuelFiguresGroup 嵌套类
- ✅ PerformanceFiguresGroup 嵌套类（包含嵌套的 AccelerationGroup）
- ✅ manufacturer 和 model 字符串字段
- ✅ 完整的 encode/decode 方法

## 参考实现

可以参考以下文件的实现：
- `GolangGenerator.java` - generateGroups, generateVarData 方法
- `JavaGenerator.java` - 完整的消息生成逻辑
- `CppGenerator.java` - 组和可变长度数据的处理

## 当前限制

由于时间限制，当前实现提供了基础功能。要支持完整的 SBE 功能，需要额外 300-500 行代码来实现上述所有特性。

建议：
1. 如果只需要简单消息（无 groups/varData），当前实现已足够
2. 如果需要完整功能，可以参考本指南逐步实现
3. 可以先实现 composite fields 支持（最简单）
4. 然后实现 groups（中等复杂度）
5. 最后实现 varData（相对简单）
