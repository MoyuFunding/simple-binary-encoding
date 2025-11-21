/*
 * Copyright 2013-2025 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.sbe.generation.nodejs;

import org.agrona.Verify;
import org.agrona.generation.OutputManager;
import uk.co.real_logic.sbe.PrimitiveType;
import uk.co.real_logic.sbe.PrimitiveValue;
import uk.co.real_logic.sbe.SbeTool;
import uk.co.real_logic.sbe.generation.CodeGenerator;
import uk.co.real_logic.sbe.generation.Generators;
import uk.co.real_logic.sbe.ir.*;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static uk.co.real_logic.sbe.generation.Generators.toUpperFirstChar;
import static uk.co.real_logic.sbe.generation.nodejs.NodeJsUtil.*;
import static uk.co.real_logic.sbe.ir.GenerationUtil.*;

/**
 * Codec generator for the Node.js/JavaScript programming language.
 */
@SuppressWarnings("MethodLength")
public class NodeJsGenerator implements CodeGenerator
{
    private static final String INDENT = "  ";
    private static final String BASE_INDENT = "";

    private final Ir ir;
    private final OutputManager outputManager;
    private final String byteOrderStr;
    private final boolean useUnsafeMode;

    /**
     * Create a new Node.js {@link CodeGenerator}.
     *
     * @param ir            for the messages and types.
     * @param outputManager for generating the codecs to.
     */
    public NodeJsGenerator(final Ir ir, final OutputManager outputManager)
    {
        Verify.notNull(ir, "ir");
        Verify.notNull(outputManager, "outputManager");

        this.ir = ir;
        this.outputManager = outputManager;
        this.byteOrderStr = ir.byteOrder() == ByteOrder.LITTLE_ENDIAN ? "LITTLE_ENDIAN" : "BIG_ENDIAN";
        this.useUnsafeMode = Boolean.parseBoolean(System.getProperty(SbeTool.NODEJS_UNSAFE_MODE, "false"));
    }

    /**
     * {@inheritDoc}
     */
    public void generate() throws IOException
    {
        generateMessageHeaderStub();
        generateTypeStubs();
        generateMessages();
    }

    private void generateMessageHeaderStub() throws IOException
    {
        final List<Token> tokens = ir.headerStructure().tokens();
        try (Writer out = outputManager.createOutput("MessageHeader"))
        {
            out.append(generateFileHeader());
            out.append(generateComposite("MessageHeader", tokens));
        }
    }

    private void generateTypeStubs() throws IOException
    {
        for (final List<Token> tokens : ir.types())
        {
            switch (tokens.get(0).signal())
            {
                case BEGIN_ENUM:
                    generateEnum(tokens);
                    break;

                case BEGIN_SET:
                    generateBitSet(tokens);
                    break;

                case BEGIN_COMPOSITE:
                    generateCompositeType(tokens);
                    break;

                default:
                    break;
            }
        }
    }

    private void generateMessages() throws IOException
    {
        for (final List<Token> tokens : ir.messages())
        {
            final Token msgToken = tokens.get(0);
            final String className = formatClassName(msgToken.name());

            try (Writer out = outputManager.createOutput(className))
            {
                final StringBuilder sb = new StringBuilder();
                sb.append(generateFileHeader());

                // Collect all composite type imports needed
                final Set<String> compositeImports = collectCompositeImports(tokens);
                sb.append("import { MessageHeader } from './MessageHeader.js';\n");
                for (final String importName : compositeImports)
                {
                    sb.append("import { ").append(importName).append(" } from './"
).append(importName).append(".js';\n");
                }
                sb.append("\n");

                generateMessageClass(sb, className, tokens);

                out.append(sb);
            }
        }
    }

    private Set<String> collectCompositeImports(final List<Token> tokens)
    {
        final Set<String> imports = new HashSet<>();
        final List<Token> messageBody = getMessageBody(tokens);

        // Collect imports from message fields
        Generators.forEachField(messageBody, (fieldToken, typeToken) ->
        {
            if (typeToken.signal() == Signal.BEGIN_COMPOSITE)
            {
                final String typeName = typeToken.name();
                if (typeName != null && !typeName.isEmpty() && !"messageHeader".equals(typeName))
                {
                    imports.add(formatClassName(typeName));
                }
            }
            else if (typeToken.signal() == Signal.BEGIN_ENUM || typeToken.signal() == Signal.BEGIN_SET)
            {
                final String typeName = typeToken.name();
                if (typeName != null && !typeName.isEmpty())
                {
                    imports.add(formatClassName(typeName));
                }
            }
        });

        // Collect imports from groups (including nested groups)
        final List<Token> fields = new ArrayList<>();
        final int i = collectFields(messageBody, 0, fields);
        final List<Token> groups = new ArrayList<>();
        collectGroups(messageBody, i, groups);
        collectImportsFromGroups(groups, imports);

        return imports;
    }

    private void collectImportsFromGroups(final List<Token> groups, final Set<String> imports)
    {
        for (int i = 0; i < groups.size(); )
        {
            final Token groupToken = groups.get(i);
            if (groupToken.signal() == Signal.BEGIN_GROUP)
            {
                final List<Token> groupTokens = groups.subList(i, i + groupToken.componentTokenCount());
                final Token dimensionToken = groupTokens.get(1);
                final int dimensionTokenCount = dimensionToken.componentTokenCount();
                final int groupBodyStart = 1 + dimensionTokenCount;
                final List<Token> groupBody = groupTokens.subList(groupBodyStart, groupTokens.size() - 1);

                // Collect from group fields
                Generators.forEachField(groupBody, (fieldToken, typeToken) ->
                {
                    if (typeToken.signal() == Signal.BEGIN_COMPOSITE)
                    {
                        final String typeName = typeToken.name();
                        if (typeName != null && !typeName.isEmpty() && !"messageHeader".equals(typeName))
                        {
                            imports.add(formatClassName(typeName));
                        }
                    }
                    else if (typeToken.signal() == Signal.BEGIN_ENUM || typeToken.signal() == Signal.BEGIN_SET)
                    {
                        final String typeName = typeToken.name();
                        if (typeName != null && !typeName.isEmpty())
                        {
                            imports.add(formatClassName(typeName));
                        }
                    }
                });

                // Recursively collect from nested groups
                final List<Token> nestedGroups = new ArrayList<>();
                final int j = 0;
                for (int k = 0; k < groupBody.size(); )
                {
                    final Token token = groupBody.get(k);
                    if (token.signal() == Signal.BEGIN_GROUP)
                    {
                        final int componentCount = token.componentTokenCount();
                        for (int m = k; m < k + componentCount && m < groupBody.size(); m++)
                        {
                            nestedGroups.add(groupBody.get(m));
                        }
                        k += componentCount;
                    }
                    else
                    {
                        k++;
                    }
                }

                if (!nestedGroups.isEmpty())
                {
                    collectImportsFromGroups(nestedGroups, imports);
                }
            }
            i += groupToken.componentTokenCount();
        }
    }

    private String generateFileHeader()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("/**\n");
        sb.append(" * Generated SBE (Simple Binary Encoding) message codec.\n");
        sb.append(" * WARNING: This code is auto-generated. Do not edit!\n");
        sb.append(" */\n\n");
        return sb.toString();
    }

    private void generateEnum(final List<Token> tokens) throws IOException
    {
        final Token beginToken = tokens.get(0);
        final String enumName = formatClassName(beginToken.name());
        final PrimitiveType encodingType = beginToken.encoding().primitiveType();
        final boolean isBigInt = !useUnsafeMode &&
            (encodingType == PrimitiveType.UINT64 || encodingType == PrimitiveType.INT64);

        try (Writer out = outputManager.createOutput(enumName))
        {
            final StringBuilder sb = new StringBuilder();
            sb.append(generateFileHeader());

            final String description = beginToken.description();
            if (null != description && !description.isEmpty())
            {
                sb.append(generateJsDoc(description, BASE_INDENT));
            }

            sb.append("const ").append(enumName).append(" = {\n");

            for (int i = 1; i < tokens.size() - 1; i++)
            {
                final Token token = tokens.get(i);
                final String name = formatConstantName(token.name());
                final String tokenDesc = token.description();

                if (null != tokenDesc && !tokenDesc.isEmpty())
                {
                    sb.append(INDENT).append("/** ").append(tokenDesc).append(" */\n");
                }

                // Add 'n' suffix for BigInt enum values
                final String value = token.encoding().constValue().toString();
                sb.append(INDENT).append(name).append(": ").append(value);
                if (isBigInt)
                {
                    sb.append("n");
                }

                if (i < tokens.size() - 2)
                {
                    sb.append(",");
                }
                sb.append("\n");
            }

            sb.append("};\n\n");
            sb.append("export { ").append(enumName).append(" };\n");
            out.append(sb);
        }
    }

    private void generateBitSet(final List<Token> tokens) throws IOException
    {
        final Token beginToken = tokens.get(0);
        final String setName = formatClassName(beginToken.name());
        final PrimitiveType encodingType = beginToken.encoding().primitiveType();
        final boolean isBigInt = !useUnsafeMode &&
            (encodingType == PrimitiveType.UINT64 || encodingType == PrimitiveType.INT64);
        final String jsType = isBigInt ? "bigint" : "number";
        final String suffix = isBigInt ? "n" : "";
        final String zero = "0" + suffix;

        try (Writer out = outputManager.createOutput(setName))
        {
            final StringBuilder sb = new StringBuilder();
            sb.append(generateFileHeader());

            final String description = beginToken.description();
            if (null != description && !description.isEmpty())
            {
                sb.append(generateJsDoc(description, BASE_INDENT));
            }

            sb.append("class ").append(setName).append(" {\n");
            sb.append(INDENT).append("/**\n");
            sb.append(INDENT).append(" * @param {").append(jsType).append("} [value=").append(zero).append("]\n");
            sb.append(INDENT).append(" */\n");
            sb.append(INDENT).append("constructor(value = ").append(zero).append(") {\n");
            sb.append(INDENT).append(INDENT).append("this.value = value;\n");
            sb.append(INDENT).append("}\n\n");

            // getValue method
            sb.append(INDENT).append("/**\n");
            sb.append(INDENT).append(" * @returns {").append(jsType).append("}\n");
            sb.append(INDENT).append(" */\n");
            sb.append(INDENT).append("getValue() {\n");
            sb.append(INDENT).append(INDENT).append("return this.value;\n");
            sb.append(INDENT).append("}\n\n");

            // Generate bit accessors
            for (int i = 1; i < tokens.size() - 1; i++)
            {
                final Token token = tokens.get(i);
                final String choiceName = formatPropertyName(token.name());
                final long bitIndex = token.encoding().constValue().longValue();

                // For BigInt, use BigInt(1) << BigInt(bitIndex) at runtime
                // For number, compute mask at compile time
                final String bitMask;
                if (isBigInt)
                {
                    // Generate: (1n << 40n) for example
                    bitMask = "(1" + suffix + " << " + bitIndex + suffix + ")";
                }
                else
                {
                    // Compute mask at Java compile time for 32-bit
                    final long mask = 1L << bitIndex;
                    bitMask = String.valueOf(mask);
                }

                sb.append(INDENT).append("/**\n");
                sb.append(INDENT).append(" * @returns {boolean}\n");
                sb.append(INDENT).append(" */\n");
                sb.append(INDENT).append("get ").append(choiceName).append("() {\n");
                sb.append(INDENT).append(INDENT).append("return (this.value & ").append(bitMask)
                    .append(") !== ").append(zero).append(";\n");
                sb.append(INDENT).append("}\n\n");

                sb.append(INDENT).append("/**\n");
                sb.append(INDENT).append(" * @param {boolean} value\n");
                sb.append(INDENT).append(" */\n");
                sb.append(INDENT).append("set ").append(choiceName).append("(value) {\n");
                sb.append(INDENT).append(INDENT).append("if (value) {\n");
                sb.append(INDENT).append(INDENT).append(INDENT).append("this.value |= ").append(bitMask)
                    .append(";\n");
                sb.append(INDENT).append(INDENT).append("} else {\n");
                sb.append(INDENT).append(INDENT).append(INDENT).append("this.value &= ~").append(bitMask)
                    .append(";\n");
                sb.append(INDENT).append(INDENT).append("}\n");
                sb.append(INDENT).append("}\n\n");
            }

            sb.append("}\n\n");
            sb.append("export { ").append(setName).append(" };\n");
            out.append(sb);
        }
    }

    private void generateCompositeType(final List<Token> tokens) throws IOException
    {
        final Token beginToken = tokens.get(0);
        final String compositeName = formatClassName(beginToken.name());

        try (Writer out = outputManager.createOutput(compositeName))
        {
            out.append(generateFileHeader());
            out.append(generateComposite(compositeName, tokens));
        }
    }

    private String generateComposite(final String compositeName, final List<Token> tokens)
    {
        final StringBuilder sb = new StringBuilder();
        final Token beginToken = tokens.get(0);
        final String description = beginToken.description();

        if (null != description && !description.isEmpty())
        {
            sb.append(generateJsDoc(description, BASE_INDENT));
        }

        sb.append("class ").append(compositeName).append(" {\n");
        sb.append(INDENT).append("static SIZE = ")
            .append(calculateCompositeSize(tokens)).append(";\n\n");

        // Constructor
        sb.append(INDENT).append("constructor() {\n");
        for (int i = 1; i < tokens.size() - 1; i++)
        {
            final Token token = tokens.get(i);
            if (token.signal() == Signal.ENCODING)
            {
                final Encoding encoding = token.encoding();
                final String propName = formatPropertyName(token.name());

                // Skip constants
                if (encoding.presence() == Encoding.Presence.CONSTANT)
                {
                    continue;
                }

                final int arrayLength = token.arrayLength();
                final PrimitiveType primitiveType = encoding.primitiveType();

                if (arrayLength > 1)
                {
                    if (primitiveType == PrimitiveType.CHAR)
                    {
                        sb.append(INDENT).append(INDENT).append("this._").append(propName).append(" = '';\n");
                    }
                    else
                    {
                        sb.append(INDENT).append(INDENT).append("this._").append(propName)
                            .append(" = new Array(").append(arrayLength).append(").fill(");
                        if (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64)
                        {
                            sb.append("0n");
                        }
                        else
                        {
                            sb.append("0");
                        }
                        sb.append(");\n");
                    }
                }
                else
                {
                    sb.append(INDENT).append(INDENT).append("this._").append(propName).append(" = ");
                    if (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64)
                    {
                        sb.append("0n");
                    }
                    else
                    {
                        sb.append("0");
                    }
                    sb.append(";\n");
                }
            }
        }
        sb.append(INDENT).append("}\n\n");

        // Encode method
        sb.append(INDENT).append("/**\n");
        sb.append(INDENT).append(" * @param {Buffer} buffer\n");
        sb.append(INDENT).append(" * @param {number} offset\n");
        sb.append(INDENT).append(" * @returns {number}\n");
        sb.append(INDENT).append(" */\n");
        sb.append(INDENT).append("encode(buffer, offset) {\n");
        int compositeOffset = 0;
        for (int i = 1; i < tokens.size() - 1; i++)
        {
            final Token token = tokens.get(i);
            if (token.signal() == Signal.ENCODING)
            {
                final Encoding encoding = token.encoding();
                final String propName = formatPropertyName(token.name());
                final PrimitiveType primitiveType = encoding.primitiveType();
                final int arrayLength = token.arrayLength();

                // Skip constants
                if (encoding.presence() == Encoding.Presence.CONSTANT)
                {
                    continue;
                }

                if (arrayLength > 1)
                {
                    if (primitiveType == PrimitiveType.CHAR)
                    {
                        // char array
                        sb.append(INDENT).append(INDENT).append("for (let i = 0; i < ").append(arrayLength)
                            .append("; i++) {\n");
                        sb.append(INDENT).append(INDENT).append(INDENT)
                            .append("buffer.writeUInt8(i < this._").append(propName)
                            .append(".length ? this._").append(propName)
                            .append(".charCodeAt(i) : 0, offset + ").append(compositeOffset).append(" + i);\n");
                        sb.append(INDENT).append(INDENT).append("}\n");
                        compositeOffset += arrayLength;
                    }
                    else
                    {
                        // numeric array
                        final String writeMethod = bufferWriteMethod(primitiveType, byteOrderStr, useUnsafeMode);
                        final int elementSize = primitiveTypeSize(primitiveType);
                        sb.append(INDENT).append(INDENT).append("for (let i = 0; i < ").append(arrayLength)
                            .append("; i++) {\n");
                        sb.append(INDENT).append(INDENT).append(INDENT).append("buffer.").append(writeMethod)
                            .append("(this._").append(propName).append("[i], offset + ")
                            .append(compositeOffset).append(" + i * ").append(elementSize).append(");\n");
                        sb.append(INDENT).append(INDENT).append("}\n");
                        compositeOffset += arrayLength * elementSize;
                    }
                }
                else
                {
                    // scalar
                    final String writeMethod = bufferWriteMethod(primitiveType, byteOrderStr, useUnsafeMode);
                    sb.append(INDENT).append(INDENT).append("buffer.").append(writeMethod)
                        .append("(this._").append(propName).append(", offset + ")
                        .append(compositeOffset).append(");\n");
                    compositeOffset += primitiveTypeSize(primitiveType);
                }
            }
        }
        sb.append(INDENT).append(INDENT).append("return ").append(compositeOffset).append(";\n");
        sb.append(INDENT).append("}\n\n");

        // Decode method
        sb.append(INDENT).append("/**\n");
        sb.append(INDENT).append(" * @param {Buffer} buffer\n");
        sb.append(INDENT).append(" * @param {number} offset\n");
        sb.append(INDENT).append(" * @returns {number}\n");
        sb.append(INDENT).append(" */\n");
        sb.append(INDENT).append("decode(buffer, offset) {\n");
        compositeOffset = 0;
        for (int i = 1; i < tokens.size() - 1; i++)
        {
            final Token token = tokens.get(i);
            if (token.signal() == Signal.ENCODING)
            {
                final Encoding encoding = token.encoding();
                final String propName = formatPropertyName(token.name());
                final PrimitiveType primitiveType = encoding.primitiveType();
                final int arrayLength = token.arrayLength();

                // Skip constants
                if (encoding.presence() == Encoding.Presence.CONSTANT)
                {
                    continue;
                }

                if (arrayLength > 1)
                {
                    if (primitiveType == PrimitiveType.CHAR)
                    {
                        // char array
                        sb.append(INDENT).append(INDENT).append("let ").append(propName).append("Chars = '';\n");
                        sb.append(INDENT).append(INDENT).append("for (let i = 0; i < ").append(arrayLength)
                            .append("; i++) {\n");
                        sb.append(INDENT).append(INDENT).append(INDENT)
                            .append("const charCode = buffer.readUInt8(offset + ").append(compositeOffset)
                            .append(" + i);\n");
                        sb.append(INDENT).append(INDENT).append(INDENT).append("if (charCode === 0) break;\n");
                        sb.append(INDENT).append(INDENT).append(INDENT).append(propName)
                            .append("Chars += String.fromCharCode(charCode);\n");
                        sb.append(INDENT).append(INDENT).append("}\n");
                        sb.append(INDENT).append(INDENT).append("this._").append(propName).append(" = ")
                            .append(propName).append("Chars;\n");
                        compositeOffset += arrayLength;
                    }
                    else
                    {
                        // numeric array
                        final String readMethod = bufferReadMethod(primitiveType, byteOrderStr, useUnsafeMode);
                        final int elementSize = primitiveTypeSize(primitiveType);
                        sb.append(INDENT).append(INDENT).append("for (let i = 0; i < ").append(arrayLength)
                            .append("; i++) {\n");
                        sb.append(INDENT).append(INDENT).append(INDENT).append("this._").append(propName)
                            .append("[i] = buffer.").append(readMethod).append("(offset + ")
                            .append(compositeOffset).append(" + i * ").append(elementSize).append(");\n");
                        sb.append(INDENT).append(INDENT).append("}\n");
                        compositeOffset += arrayLength * elementSize;
                    }
                }
                else
                {
                    // scalar
                    final String readMethod = bufferReadMethod(primitiveType, byteOrderStr, useUnsafeMode);
                    sb.append(INDENT).append(INDENT).append("this._").append(propName).append(" = buffer.")
                        .append(readMethod).append("(offset + ").append(compositeOffset).append(");\n");
                    compositeOffset += primitiveTypeSize(primitiveType);
                }
            }
        }
        sb.append(INDENT).append(INDENT).append("return ").append(compositeOffset).append(";\n");
        sb.append(INDENT).append("}\n\n");

        // Getters and setters
        for (int i = 1; i < tokens.size() - 1; i++)
        {
            final Token token = tokens.get(i);
            if (token.signal() == Signal.ENCODING)
            {
                final Encoding encoding = token.encoding();
                final String propName = formatPropertyName(token.name());
                final PrimitiveType primitiveType = encoding.primitiveType();
                final int arrayLength = token.arrayLength();

                // Constants - static getter
                if (encoding.presence() == Encoding.Presence.CONSTANT)
                {
                    final PrimitiveValue constValue = encoding.constValue();
                    if (constValue != null)
                    {
                        final String jsValue = formatConstantValue(constValue, primitiveType);
                        final String jsType = primitiveType == PrimitiveType.CHAR ? "string" :
                            (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64) ?
                            "bigint" : "number";
                        sb.append(INDENT).append("/** @returns {").append(jsType).append("} */\n");
                        sb.append(INDENT).append("static get ").append(propName).append("() {\n");
                        sb.append(INDENT).append(INDENT).append("return ").append(jsValue).append(";\n");
                        sb.append(INDENT).append("}\n\n");
                    }
                    continue;
                }

                // Regular fields
                if (arrayLength > 1)
                {
                    if (primitiveType == PrimitiveType.CHAR)
                    {
                        sb.append(INDENT).append("/** @returns {string} */\n");
                        sb.append(INDENT).append("get ").append(propName).append("() {\n");
                        sb.append(INDENT).append(INDENT).append("return this._").append(propName).append(";\n");
                        sb.append(INDENT).append("}\n\n");

                        sb.append(INDENT).append("/** @param {string} value */\n");
                        sb.append(INDENT).append("set ").append(propName).append("(value) {\n");
                        sb.append(INDENT).append(INDENT).append("this._").append(propName).append(" = value;\n");
                        sb.append(INDENT).append("}\n\n");
                    }
                    else
                    {
                        final String typeName = jsTypeName(primitiveType);
                        sb.append(INDENT).append("/** @returns {").append(typeName).append("[]} */\n");
                        sb.append(INDENT).append("get ").append(propName).append("() {\n");
                        sb.append(INDENT).append(INDENT).append("return this._").append(propName).append(";\n");
                        sb.append(INDENT).append("}\n\n");

                        sb.append(INDENT).append("/** @param {").append(typeName).append("[]} value */\n");
                        sb.append(INDENT).append("set ").append(propName).append("(value) {\n");
                        sb.append(INDENT).append(INDENT).append("this._").append(propName).append(" = value;\n");
                        sb.append(INDENT).append("}\n\n");
                    }
                }
                else
                {
                    final String typeName = jsTypeName(primitiveType);
                    sb.append(INDENT).append("/** @returns {").append(typeName).append("} */\n");
                    sb.append(INDENT).append("get ").append(propName).append("() {\n");
                    sb.append(INDENT).append(INDENT).append("return this._").append(propName).append(";\n");
                    sb.append(INDENT).append("}\n\n");

                    sb.append(INDENT).append("/** @param {").append(typeName).append("} value */\n");
                    sb.append(INDENT).append("set ").append(propName).append("(value) {\n");
                    sb.append(INDENT).append(INDENT).append("this._").append(propName).append(" = value;\n");
                    sb.append(INDENT).append("}\n\n");
                }
            }
        }

        sb.append("}\n\n");
        sb.append("export { ").append(compositeName).append(" };\n");
        return sb.toString();
    }

    private void generateMessageClass(final StringBuilder sb, final String className, final List<Token> tokens)
    {
        final Token msgToken = tokens.get(0);
        final String description = msgToken.description();

        if (null != description && !description.isEmpty())
        {
            sb.append(generateJsDoc(description, BASE_INDENT));
        }

        // Extract message body
        final List<Token> messageBody = getMessageBody(tokens);
        final List<Token> fields = new ArrayList<>();
        int i = 0;
        i = collectFields(messageBody, i, fields);

        final List<Token> groups = new ArrayList<>();
        i = collectGroups(messageBody, i, groups);

        final List<Token> varData = new ArrayList<>();
        collectVarData(messageBody, i, varData);

        sb.append("class ").append(className).append(" {\n");
        sb.append(INDENT).append("static TEMPLATE_ID = ").append(msgToken.id()).append(";\n");
        sb.append(INDENT).append("static SCHEMA_ID = ").append(ir.id()).append(";\n");
        sb.append(INDENT).append("static SCHEMA_VERSION = ").append(ir.version()).append(";\n");
        sb.append(INDENT).append("static BLOCK_LENGTH = ").append(msgToken.encodedLength())
            .append(";\n\n");

        // Generate constructor with field initialization
        generateConstructor(sb, fields, groups, varData);

        // Generate encode method
        generateEncodeMethod(sb, className, fields, groups, varData);

        // Generate decode method
        generateDecodeMethod(sb, className, fields, groups, varData);

        // Generate field accessors
        generateFieldAccessors(sb, fields);

        // Generate group accessors
        generateGroupAccessors(sb, groups);

        // Generate varData accessors
        generateVarDataAccessors(sb, varData);

        // Generate nested group classes
        generateGroupClasses(sb, groups);

        sb.append("}\n\n");
        sb.append("export { ").append(className).append(" };\n");
    }

    private void generateConstructor(
        final StringBuilder sb,
        final List<Token> fields,
        final List<Token> groups,
        final List<Token> varData)
    {
        sb.append(INDENT).append("constructor() {\n");

        // Initialize fields
        Generators.forEachField(fields, (fieldToken, encodingToken) ->
        {
            final String fieldName = formatPropertyName(fieldToken.name());

            // Skip constant fields - they don't need instance variables
            if (encodingToken.encoding().presence() == Encoding.Presence.CONSTANT)
            {
                return;
            }

            if (encodingToken.signal() == Signal.BEGIN_COMPOSITE)
            {
                final String typeName = encodingToken.name();
                if (typeName != null && !typeName.isEmpty())
                {
                    final String compositeName = formatClassName(typeName);
                    sb.append(INDENT).append(INDENT).append("this._").append(fieldName)
                        .append(" = new ").append(compositeName).append("();\n");
                }
            }
            else if (encodingToken.signal() == Signal.BEGIN_ENUM)
            {
                // Enum fields: initialize to 0 (will be read/written as underlying integer)
                sb.append(INDENT).append(INDENT).append("this._").append(fieldName).append(" = 0;\n");
            }
            else if (encodingToken.signal() == Signal.BEGIN_SET)
            {
                // Bitset fields: initialize as instance of the bitset class
                final String setTypeName = formatClassName(encodingToken.name());
                sb.append(INDENT).append(INDENT).append("this._").append(fieldName)
                    .append(" = new ").append(setTypeName).append("();\n");
            }
            else if (encodingToken.signal() == Signal.ENCODING)
            {
                final int arrayLength = encodingToken.arrayLength();
                final PrimitiveType primitiveType = encodingToken.encoding().primitiveType();

                if (arrayLength > 1)
                {
                    // Array field: initialize as array
                    if (primitiveType == PrimitiveType.CHAR)
                    {
                        // char array: initialize as empty string or buffer
                        sb.append(INDENT).append(INDENT).append("this._").append(fieldName)
                            .append(" = '';\n");
                    }
                    else
                    {
                        // Numeric array: initialize as array of zeros
                        sb.append(INDENT).append(INDENT).append("this._").append(fieldName)
                            .append(" = new Array(").append(arrayLength).append(").fill(");

                        // Use BigInt for 64-bit types (only in safe mode)
                        if (!useUnsafeMode &&
                            (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64))
                        {
                            sb.append("0n");
                        }
                        else
                        {
                            sb.append("0");
                        }
                        sb.append(");\n");
                    }
                }
                else
                {
                    // Scalar field: initialize to 0 or 0n (only in safe mode)
                    sb.append(INDENT).append(INDENT).append("this._").append(fieldName).append(" = ");
                    if (!useUnsafeMode &&
                        (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64))
                    {
                        sb.append("0n");
                    }
                    else
                    {
                        sb.append("0");
                    }
                    sb.append(";\n");
                }
            }
        });

        // Initialize groups
        for (int i = 0; i < groups.size(); )
        {
            final Token groupToken = groups.get(i);
            if (groupToken.signal() == Signal.BEGIN_GROUP)
            {
                final String groupName = formatPropertyName(groupToken.name());
                sb.append(INDENT).append(INDENT).append("this._").append(groupName).append(" = [];\n");
            }
            i += groupToken.componentTokenCount();
        }

        // Initialize varData
        for (int i = 0; i < varData.size(); )
        {
            final Token varDataToken = varData.get(i);
            if (varDataToken.signal() == Signal.BEGIN_VAR_DATA)
            {
                final String varDataName = formatPropertyName(varDataToken.name());

                // Check if this varData has character encoding (string) or not (Buffer)
                final List<Token> varDataTokens = varData.subList(i, i + varDataToken.componentTokenCount());
                boolean hasEncoding = false;
                for (final Token token : varDataTokens)
                {
                    if (token.signal() == Signal.ENCODING && token.encoding().characterEncoding() != null)
                    {
                        hasEncoding = true;
                        break;
                    }
                }

                if (hasEncoding)
                {
                    sb.append(INDENT).append(INDENT).append("this._").append(varDataName).append(" = '';\n");
                }
                else
                {
                    sb.append(INDENT).append(INDENT).append("this._").append(varDataName)
                        .append(" = Buffer.allocUnsafe(0);\n");
                }
            }
            i += varDataToken.componentTokenCount();
        }

        sb.append(INDENT).append("}\n\n");
    }

    private void generateEncodeMethod(
        final StringBuilder sb,
        final String className,
        final List<Token> fields,
        final List<Token> groups,
        final List<Token> varData)
    {
        sb.append(INDENT).append("/**\n");
        sb.append(INDENT).append(" * @param {Buffer} buffer\n");
        sb.append(INDENT).append(" * @param {number} [offset=0]\n");
        sb.append(INDENT).append(" * @returns {number}\n");
        sb.append(INDENT).append(" */\n");
        sb.append(INDENT).append("encode(buffer, offset = 0) {\n");
        sb.append(INDENT).append(INDENT).append("const header = new MessageHeader();\n");
        sb.append(INDENT).append(INDENT).append("header.blockLength = ").append(className)
            .append(".BLOCK_LENGTH;\n");
        sb.append(INDENT).append(INDENT).append("header.templateId = ").append(className).append(".TEMPLATE_ID;\n");
        sb.append(INDENT).append(INDENT).append("header.schemaId = ").append(className).append(".SCHEMA_ID;\n");
        sb.append(INDENT).append(INDENT).append("header.version = ").append(className)
            .append(".SCHEMA_VERSION;\n");
        sb.append(INDENT).append(INDENT).append("let pos = offset + header.encode(buffer, offset);\n\n");

        // Encode fields
        Generators.forEachField(fields, (fieldToken, encodingToken) ->
        {
            final String fieldName = formatPropertyName(fieldToken.name());
            final Encoding encoding = encodingToken.encoding();

            // Skip constant fields - they are not encoded
            if (encoding.presence() == Encoding.Presence.CONSTANT)
            {
                return;
            }

            if (encodingToken.signal() == Signal.BEGIN_COMPOSITE)
            {
                sb.append(INDENT).append(INDENT).append("pos += this._").append(fieldName)
                    .append(".encode(buffer, pos);\n");
            }
            else if (encodingToken.signal() == Signal.BEGIN_ENUM)
            {
                // Enum: encode as underlying integer type
                final PrimitiveType primitiveType = encodingToken.encoding().primitiveType();
                final String writeMethod = bufferWriteMethod(primitiveType, byteOrderStr, useUnsafeMode);
                final boolean needsBigIntConversion = useUnsafeMode &&
                    (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64);
                sb.append(INDENT).append(INDENT).append("buffer.").append(writeMethod).append("(");
                if (needsBigIntConversion)
                {
                    sb.append("BigInt(this._").append(fieldName).append(" || 0)");
                }
                else
                {
                    sb.append("this._").append(fieldName);
                }
                sb.append(", pos);\n");
                sb.append(INDENT).append(INDENT).append("pos += ").append(primitiveTypeSize(primitiveType))
                    .append(";\n");
            }
            else if (encodingToken.signal() == Signal.BEGIN_SET)
            {
                // Bitset: encode the value property
                final PrimitiveType primitiveType = encodingToken.encoding().primitiveType();
                final String writeMethod = bufferWriteMethod(primitiveType, byteOrderStr, useUnsafeMode);
                final boolean needsBigIntConversion = useUnsafeMode &&
                    (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64);
                sb.append(INDENT).append(INDENT).append("buffer.").append(writeMethod).append("(");
                if (needsBigIntConversion)
                {
                    sb.append("BigInt(this._").append(fieldName).append(".value || 0)");
                }
                else
                {
                    sb.append("this._").append(fieldName).append(".value");
                }
                sb.append(", pos);\n");
                sb.append(INDENT).append(INDENT).append("pos += ").append(primitiveTypeSize(primitiveType))
                    .append(";\n");
            }
            else if (encodingToken.signal() == Signal.ENCODING)
            {
                final int arrayLength = encodingToken.arrayLength();
                final PrimitiveType primitiveType = encoding.primitiveType();

                if (arrayLength > 1)
                {
                    // Array field
                    if (primitiveType == PrimitiveType.CHAR)
                    {
                        // char array: encode as fixed-length string
                        sb.append(INDENT).append(INDENT).append("// Encode char array: ")
                            .append(fieldName).append("\n");
                        sb.append(INDENT).append(INDENT).append("for (let i = 0; i < ").append(arrayLength)
                            .append("; i++) {\n");
                        sb.append(INDENT).append(INDENT).append(INDENT)
                            .append("buffer.writeUInt8(i < this._").append(fieldName)
                            .append(".length ? this._").append(fieldName).append(".charCodeAt(i) : 0, pos + i);\n");
                        sb.append(INDENT).append(INDENT).append("}\n");
                        sb.append(INDENT).append(INDENT).append("pos += ").append(arrayLength).append(";\n");
                    }
                    else
                    {
                        // Numeric array: encode each element
                        final String writeMethod = bufferWriteMethod(primitiveType, byteOrderStr, useUnsafeMode);
                        final int elementSize = primitiveTypeSize(primitiveType);
                        final boolean needsBigIntConversion = useUnsafeMode &&
                            (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64);
                        sb.append(INDENT).append(INDENT).append("// Encode array: ").append(fieldName).append("\n");
                        sb.append(INDENT).append(INDENT).append("for (let i = 0; i < ").append(arrayLength)
                            .append("; i++) {\n");
                        sb.append(INDENT).append(INDENT).append(INDENT).append("buffer.").append(writeMethod).append("(");
                        if (needsBigIntConversion)
                        {
                            sb.append("BigInt(this._").append(fieldName).append("[i] || 0)");
                        }
                        else
                        {
                            sb.append("this._").append(fieldName).append("[i]");
                        }
                        sb.append(", pos + i * ").append(elementSize).append(");\n");
                        sb.append(INDENT).append(INDENT).append("}\n");
                        sb.append(INDENT).append(INDENT).append("pos += ").append(arrayLength * elementSize)
                            .append(";\n");
                    }
                }
                else
                {
                    // Scalar field
                    final String writeMethod = bufferWriteMethod(primitiveType, byteOrderStr, useUnsafeMode);
                    final boolean needsBigIntConversion = useUnsafeMode &&
                        (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64);
                    sb.append(INDENT).append(INDENT).append("buffer.").append(writeMethod).append("(");
                    if (needsBigIntConversion)
                    {
                        sb.append("BigInt(this._").append(fieldName).append(" || 0)");
                    }
                    else
                    {
                        sb.append("this._").append(fieldName);
                    }
                    sb.append(", pos);\n");
                    sb.append(INDENT).append(INDENT).append("pos += ").append(primitiveTypeSize(primitiveType))
                        .append(";\n");
                }
            }
        });

        // Encode groups
        for (int i = 0; i < groups.size(); )
        {
            final Token groupToken = groups.get(i);
            if (groupToken.signal() == Signal.BEGIN_GROUP)
            {
                final String groupName = formatPropertyName(groupToken.name());
                final Token dimensionToken = groups.get(i + 1);
                final int blockLength = groupToken.encodedLength();

                // Extract dimension types using helper method
                final List<Token> dimensionTokens = groups.subList(i + 1, i + 1 + dimensionToken.componentTokenCount());
                final DimensionTypes dimTypes = extractDimensionTypes(dimensionTokens);

                sb.append(INDENT).append(INDENT).append("// Encode group: ").append(groupName).append("\n");

                if (dimTypes.blockLengthToken != null)
                {
                    final String writeMethod = bufferWriteMethod(
                        dimTypes.blockLengthToken.encoding().primitiveType(), byteOrderStr);
                    sb.append(INDENT).append(INDENT).append("buffer.").append(writeMethod)
                        .append("(").append(blockLength).append(", pos);\n");
                    sb.append(INDENT).append(INDENT).append("pos += ")
                        .append(primitiveTypeSize(dimTypes.blockLengthToken.encoding().primitiveType())).append(";\n");
                }

                if (dimTypes.numInGroupToken != null)
                {
                    final String writeMethod = bufferWriteMethod(
                        dimTypes.numInGroupToken.encoding().primitiveType(), byteOrderStr);
                    sb.append(INDENT).append(INDENT).append("buffer.").append(writeMethod)
                        .append("(this._").append(groupName).append(".length, pos);\n");
                    sb.append(INDENT).append(INDENT).append("pos += ")
                        .append(primitiveTypeSize(dimTypes.numInGroupToken.encoding().primitiveType())).append(";\n");
                }

                sb.append(INDENT).append(INDENT).append("for (const entry of this._").append(groupName).append(") {\n");
                sb.append(INDENT).append(INDENT).append(INDENT).append("pos += entry.encode(buffer, pos);\n");
                sb.append(INDENT).append(INDENT).append("}\n\n");
            }
            i += groupToken.componentTokenCount();
        }

        // Encode varData
        for (int i = 0; i < varData.size(); )
        {
            final Token varDataToken = varData.get(i);
            if (varDataToken.signal() == Signal.BEGIN_VAR_DATA)
            {
                final String varDataName = formatPropertyName(varDataToken.name());

                // Extract varData types using helper method
                final List<Token> varDataTokens = varData.subList(i, i + varDataToken.componentTokenCount());
                final VarDataTypes varTypes = extractVarDataTypes(varDataTokens);

                sb.append(INDENT).append(INDENT).append("// Encode varData: ").append(varDataName).append("\n");

                // Check if this varData has character encoding
                final boolean hasEncoding = varTypes.dataToken != null &&
                    varTypes.dataToken.encoding().characterEncoding() != null;

                if (hasEncoding)
                {
                    // Optimized path: avoid temporary Buffer allocation
                    final String encoding = mapCharacterEncoding(
                        varTypes.dataToken.encoding().characterEncoding());
                    sb.append(INDENT).append(INDENT).append("const ").append(varDataName)
                        .append("Length = Buffer.byteLength(this._").append(varDataName).append(", '")
                        .append(encoding).append("');\n");

                    if (varTypes.lengthToken != null)
                    {
                        final String writeMethod = bufferWriteMethod(
                            varTypes.lengthToken.encoding().primitiveType(), byteOrderStr);
                        sb.append(INDENT).append(INDENT).append("buffer.").append(writeMethod)
                            .append("(").append(varDataName).append("Length, pos);\n");
                        sb.append(INDENT).append(INDENT).append("pos += ")
                            .append(primitiveTypeSize(varTypes.lengthToken.encoding().primitiveType())).append(";\n");
                    }

                    sb.append(INDENT).append(INDENT).append("buffer.write(this._").append(varDataName)
                        .append(", pos, ").append(varDataName).append("Length, '").append(encoding).append("');\n");
                    sb.append(INDENT).append(INDENT).append("pos += ").append(varDataName).append("Length;\n\n");
                }
                else
                {
                    // Raw bytes - this._varDataName is already a Buffer
                    if (varTypes.lengthToken != null)
                    {
                        final String writeMethod = bufferWriteMethod(
                            varTypes.lengthToken.encoding().primitiveType(), byteOrderStr);
                        sb.append(INDENT).append(INDENT).append("buffer.").append(writeMethod)
                            .append("(this._").append(varDataName).append(".length, pos);\n");
                        sb.append(INDENT).append(INDENT).append("pos += ")
                            .append(primitiveTypeSize(varTypes.lengthToken.encoding().primitiveType())).append(";\n");
                    }

                    sb.append(INDENT).append(INDENT).append("this._").append(varDataName)
                        .append(".copy(buffer, pos);\n");
                    sb.append(INDENT).append(INDENT).append("pos += this._").append(varDataName)
                        .append(".length;\n\n");
                }
            }
            i += varDataToken.componentTokenCount();
        }

        sb.append(INDENT).append(INDENT).append("return pos - offset;\n");
        sb.append(INDENT).append("}\n\n");
    }

    private void generateDecodeMethod(
        final StringBuilder sb,
        final String className,
        final List<Token> fields,
        final List<Token> groups,
        final List<Token> varData)
    {
        sb.append(INDENT).append("/**\n");
        sb.append(INDENT).append(" * @param {Buffer} buffer\n");
        sb.append(INDENT).append(" * @param {number} [offset=0]\n");
        sb.append(INDENT).append(" * @returns {number}\n");
        sb.append(INDENT).append(" */\n");
        sb.append(INDENT).append("decode(buffer, offset = 0) {\n");
        sb.append(INDENT).append(INDENT).append("const header = new MessageHeader();\n");
        sb.append(INDENT).append(INDENT).append("let pos = offset + header.decode(buffer, offset);\n\n");

        // Decode fields
        Generators.forEachField(fields, (fieldToken, encodingToken) ->
        {
            final String fieldName = formatPropertyName(fieldToken.name());
            final Encoding encoding = encodingToken.encoding();

            // Skip constant fields - they are not decoded
            if (encoding.presence() == Encoding.Presence.CONSTANT)
            {
                return;
            }

            if (encodingToken.signal() == Signal.BEGIN_COMPOSITE)
            {
                sb.append(INDENT).append(INDENT).append("pos += this._").append(fieldName)
                    .append(".decode(buffer, pos);\n");
            }
            else if (encodingToken.signal() == Signal.BEGIN_ENUM)
            {
                // Enum: decode as underlying integer type
                final PrimitiveType primitiveType = encodingToken.encoding().primitiveType();
                final String readMethod = bufferReadMethod(primitiveType, byteOrderStr, useUnsafeMode);
                final boolean needsNumberConversion = useUnsafeMode &&
                    (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64);
                sb.append(INDENT).append(INDENT).append("this._").append(fieldName).append(" = ");
                if (needsNumberConversion)
                {
                    sb.append("Number(");
                }
                sb.append("buffer.").append(readMethod).append("(pos)");
                if (needsNumberConversion)
                {
                    sb.append(")");
                }
                sb.append(";\n");
                sb.append(INDENT).append(INDENT).append("pos += ").append(primitiveTypeSize(primitiveType))
                    .append(";\n");
            }
            else if (encodingToken.signal() == Signal.BEGIN_SET)
            {
                // Bitset: decode into value property
                final PrimitiveType primitiveType = encodingToken.encoding().primitiveType();
                final String readMethod = bufferReadMethod(primitiveType, byteOrderStr, useUnsafeMode);
                final boolean needsNumberConversion = useUnsafeMode &&
                    (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64);
                sb.append(INDENT).append(INDENT).append("this._").append(fieldName).append(".value = ");
                if (needsNumberConversion)
                {
                    sb.append("Number(");
                }
                sb.append("buffer.").append(readMethod).append("(pos)");
                if (needsNumberConversion)
                {
                    sb.append(")");
                }
                sb.append(";\n");
                sb.append(INDENT).append(INDENT).append("pos += ").append(primitiveTypeSize(primitiveType))
                    .append(";\n");
            }
            else if (encodingToken.signal() == Signal.ENCODING)
            {
                final int arrayLength = encodingToken.arrayLength();
                final PrimitiveType primitiveType = encoding.primitiveType();

                if (arrayLength > 1)
                {
                    // Array field
                    if (primitiveType == PrimitiveType.CHAR)
                    {
                        // char array: decode as fixed-length string
                        sb.append(INDENT).append(INDENT).append("// Decode char array: ")
                            .append(fieldName).append("\n");
                        sb.append(INDENT).append(INDENT).append("let ").append(fieldName).append("Chars = '';\n");
                        sb.append(INDENT).append(INDENT).append("for (let i = 0; i < ").append(arrayLength)
                            .append("; i++) {\n");
                        sb.append(INDENT).append(INDENT).append(INDENT)
                            .append("const charCode = buffer.readUInt8(pos + i);\n");
                        sb.append(INDENT).append(INDENT).append(INDENT).append("if (charCode === 0) break;\n");
                        sb.append(INDENT).append(INDENT).append(INDENT).append(fieldName)
                            .append("Chars += String.fromCharCode(charCode);\n");
                        sb.append(INDENT).append(INDENT).append("}\n");
                        sb.append(INDENT).append(INDENT).append("this._").append(fieldName).append(" = ")
                            .append(fieldName).append("Chars;\n");
                        sb.append(INDENT).append(INDENT).append("pos += ").append(arrayLength).append(";\n");
                    }
                    else
                    {
                        // Numeric array: decode each element
                        final String readMethod = bufferReadMethod(primitiveType, byteOrderStr, useUnsafeMode);
                        final int elementSize = primitiveTypeSize(primitiveType);
                        final boolean needsNumberConversion = useUnsafeMode &&
                            (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64);
                        sb.append(INDENT).append(INDENT).append("// Decode array: ").append(fieldName).append("\n");
                        sb.append(INDENT).append(INDENT).append("for (let i = 0; i < ").append(arrayLength)
                            .append("; i++) {\n");
                        sb.append(INDENT).append(INDENT).append(INDENT).append("this._").append(fieldName)
                            .append("[i] = ");
                        if (needsNumberConversion)
                        {
                            sb.append("Number(");
                        }
                        sb.append("buffer.").append(readMethod).append("(pos + i * ").append(elementSize).append(")");
                        if (needsNumberConversion)
                        {
                            sb.append(")");
                        }
                        sb.append(";\n");
                        sb.append(INDENT).append(INDENT).append("}\n");
                        sb.append(INDENT).append(INDENT).append("pos += ").append(arrayLength * elementSize)
                            .append(";\n");
                    }
                }
                else
                {
                    // Scalar field
                    final String readMethod = bufferReadMethod(primitiveType, byteOrderStr, useUnsafeMode);
                    final boolean needsNumberConversion = useUnsafeMode &&
                        (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64);
                    sb.append(INDENT).append(INDENT).append("this._").append(fieldName).append(" = ");
                    if (needsNumberConversion)
                    {
                        sb.append("Number(");
                    }
                    sb.append("buffer.").append(readMethod).append("(pos)");
                    if (needsNumberConversion)
                    {
                        sb.append(")");
                    }
                    sb.append(";\n");
                    sb.append(INDENT).append(INDENT).append("pos += ").append(primitiveTypeSize(primitiveType))
                        .append(";\n");
                }
            }
        });

        // Decode groups
        for (int i = 0; i < groups.size(); )
        {
            final Token groupToken = groups.get(i);
            if (groupToken.signal() == Signal.BEGIN_GROUP)
            {
                final String groupName = formatPropertyName(groupToken.name());
                final String groupClassName = toUpperFirstChar(groupName);
                final Token dimensionToken = groups.get(i + 1);

                // Extract dimension types using helper method
                final List<Token> dimensionTokens = groups.subList(i + 1, i + 1 + dimensionToken.componentTokenCount());
                final DimensionTypes dimTypes = extractDimensionTypes(dimensionTokens);

                sb.append(INDENT).append(INDENT).append("// Decode group: ").append(groupName).append("\n");

                if (dimTypes.blockLengthToken != null)
                {
                    final String readMethod = bufferReadMethod(
                        dimTypes.blockLengthToken.encoding().primitiveType(), byteOrderStr);
                    sb.append(INDENT).append(INDENT).append("const ").append(groupName)
                        .append("BlockLength = buffer.").append(readMethod).append("(pos);\n");
                    sb.append(INDENT).append(INDENT).append("pos += ")
                        .append(primitiveTypeSize(dimTypes.blockLengthToken.encoding().primitiveType())).append(";\n");
                }

                if (dimTypes.numInGroupToken != null)
                {
                    final String readMethod = bufferReadMethod(
                        dimTypes.numInGroupToken.encoding().primitiveType(), byteOrderStr);
                    sb.append(INDENT).append(INDENT).append("const ").append(groupName)
                        .append("NumInGroup = buffer.").append(readMethod).append("(pos);\n");
                    sb.append(INDENT).append(INDENT).append("pos += ")
                        .append(primitiveTypeSize(dimTypes.numInGroupToken.encoding().primitiveType())).append(";\n");
                }

                sb.append(INDENT).append(INDENT).append("this._").append(groupName).append(" = [];\n");
                sb.append(INDENT).append(INDENT).append("for (let i = 0; i < ").append(groupName)
                    .append("NumInGroup; i++) {\n");
                sb.append(INDENT).append(INDENT).append(INDENT).append("const entry = new ").append(groupClassName)
                    .append("();\n");
                sb.append(INDENT).append(INDENT).append(INDENT).append("pos += entry.decode(buffer, pos);\n");
                sb.append(INDENT).append(INDENT).append(INDENT).append("this._").append(groupName)
                    .append(".push(entry);\n");
                sb.append(INDENT).append(INDENT).append("}\n\n");
            }
            i += groupToken.componentTokenCount();
        }

        // Decode varData
        for (int i = 0; i < varData.size(); )
        {
            final Token varDataToken = varData.get(i);
            if (varDataToken.signal() == Signal.BEGIN_VAR_DATA)
            {
                final String varDataName = formatPropertyName(varDataToken.name());

                // Extract varData types using helper method
                final List<Token> varDataTokens = varData.subList(i, i + varDataToken.componentTokenCount());
                final VarDataTypes varTypes = extractVarDataTypes(varDataTokens);

                sb.append(INDENT).append(INDENT).append("// Decode varData: ").append(varDataName).append("\n");

                if (varTypes.lengthToken != null)
                {
                    final String readMethod = bufferReadMethod(
                        varTypes.lengthToken.encoding().primitiveType(), byteOrderStr);
                    sb.append(INDENT).append(INDENT).append("let ").append(varDataName)
                        .append("Length = buffer.").append(readMethod).append("(pos);\n");
                    sb.append(INDENT).append(INDENT).append("pos += ")
                        .append(primitiveTypeSize(varTypes.lengthToken.encoding().primitiveType())).append(";\n");
                }

                // Check if this varData has character encoding
                final boolean hasEncoding = varTypes.dataToken != null &&
                    varTypes.dataToken.encoding().characterEncoding() != null;

                if (hasEncoding)
                {
                    final String encoding = mapCharacterEncoding(
                        varTypes.dataToken.encoding().characterEncoding());
                    sb.append(INDENT).append(INDENT).append("this._").append(varDataName)
                        .append(" = buffer.toString('").append(encoding).append("', pos, pos + ")
                        .append(varDataName).append("Length);\n");
                }
                else
                {
                    // Raw bytes - return a Buffer slice
                    sb.append(INDENT).append(INDENT).append("this._").append(varDataName)
                        .append(" = buffer.subarray(pos, pos + ").append(varDataName).append("Length);\n");
                }
                sb.append(INDENT).append(INDENT).append("pos += ").append(varDataName).append("Length;\n\n");
            }
            i += varDataToken.componentTokenCount();
        }

        sb.append(INDENT).append(INDENT).append("return pos - offset;\n");
        sb.append(INDENT).append("}\n\n");
    }

    private void generateFieldAccessors(final StringBuilder sb, final List<Token> fields)
    {
        Generators.forEachField(fields, (fieldToken, encodingToken) ->
        {
            final String fieldName = formatPropertyName(fieldToken.name());
            final Encoding encoding = encodingToken.encoding();

            // Handle constant fields - generate static getter
            if (encoding.presence() == Encoding.Presence.CONSTANT)
            {
                final PrimitiveValue constValue = encoding.constValue();
                if (constValue != null)
                {
                    final PrimitiveType primitiveType = encoding.primitiveType();
                    final String jsValue = formatConstantValue(constValue, primitiveType);
                    final String jsType = primitiveType == PrimitiveType.CHAR ? "string" :
                        (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64) ?
                        "bigint" : "number";
                    sb.append(INDENT).append("/** @returns {").append(jsType).append("} */\n");
                    sb.append(INDENT).append("static get ").append(fieldName).append("() {\n");
                    sb.append(INDENT).append(INDENT).append("return ").append(jsValue).append(";\n");
                    sb.append(INDENT).append("}\n\n");
                }
                return;
            }

            final String typeName;
            final int arrayLength = encodingToken.arrayLength();

            if (encodingToken.signal() == Signal.BEGIN_COMPOSITE)
            {
                final String compositeTypeName = encodingToken.name();
                if (compositeTypeName == null || compositeTypeName.isEmpty())
                {
                    return; // Skip if no type name
                }
                typeName = formatClassName(compositeTypeName);

                sb.append(INDENT).append("/** @returns {").append(typeName).append("} */\n");
                sb.append(INDENT).append("get ").append(fieldName).append("() {\n");
                sb.append(INDENT).append(INDENT).append("return this._").append(fieldName).append(";\n");
                sb.append(INDENT).append("}\n\n");
            }
            else if (encodingToken.signal() == Signal.BEGIN_ENUM)
            {
                // Enum field: type is number
                sb.append(INDENT).append("/** @returns {number} */\n");
                sb.append(INDENT).append("get ").append(fieldName).append("() {\n");
                sb.append(INDENT).append(INDENT).append("return this._").append(fieldName).append(";\n");
                sb.append(INDENT).append("}\n\n");

                sb.append(INDENT).append("/** @param {number} value */\n");
                sb.append(INDENT).append("set ").append(fieldName).append("(value) {\n");
                sb.append(INDENT).append(INDENT).append("this._").append(fieldName).append(" = value;\n");
                sb.append(INDENT).append("}\n\n");
            }
            else if (encodingToken.signal() == Signal.BEGIN_SET)
            {
                // Bitset field: type is the bitset class
                typeName = formatClassName(encodingToken.name());
                sb.append(INDENT).append("/** @returns {").append(typeName).append("} */\n");
                sb.append(INDENT).append("get ").append(fieldName).append("() {\n");
                sb.append(INDENT).append(INDENT).append("return this._").append(fieldName).append(";\n");
                sb.append(INDENT).append("}\n\n");
            }
            else if (encodingToken.signal() == Signal.ENCODING)
            {
                if (arrayLength > 1)
                {
                    // Array field
                    if (encoding.primitiveType() == PrimitiveType.CHAR)
                    {
                        // char array: string
                        sb.append(INDENT).append("/** @returns {string} */\n");
                        sb.append(INDENT).append("get ").append(fieldName).append("() {\n");
                        sb.append(INDENT).append(INDENT).append("return this._").append(fieldName).append(";\n");
                        sb.append(INDENT).append("}\n\n");

                        sb.append(INDENT).append("/** @param {string} value */\n");
                        sb.append(INDENT).append("set ").append(fieldName).append("(value) {\n");
                        sb.append(INDENT).append(INDENT).append("this._").append(fieldName).append(" = value;\n");
                        sb.append(INDENT).append("}\n\n");
                    }
                    else
                    {
                        // Numeric array
                        typeName = jsTypeName(encoding.primitiveType());
                        sb.append(INDENT).append("/** @returns {").append(typeName).append("[]} */\n");
                        sb.append(INDENT).append("get ").append(fieldName).append("() {\n");
                        sb.append(INDENT).append(INDENT).append("return this._").append(fieldName).append(";\n");
                        sb.append(INDENT).append("}\n\n");

                        sb.append(INDENT).append("/** @param {").append(typeName).append("[]} value */\n");
                        sb.append(INDENT).append("set ").append(fieldName).append("(value) {\n");
                        sb.append(INDENT).append(INDENT).append("this._").append(fieldName).append(" = value;\n");
                        sb.append(INDENT).append("}\n\n");
                    }
                }
                else
                {
                    // Scalar field
                    typeName = jsTypeName(encoding.primitiveType());
                    sb.append(INDENT).append("/** @returns {").append(typeName).append("} */\n");
                    sb.append(INDENT).append("get ").append(fieldName).append("() {\n");
                    sb.append(INDENT).append(INDENT).append("return this._").append(fieldName).append(";\n");
                    sb.append(INDENT).append("}\n\n");

                    sb.append(INDENT).append("/** @param {").append(typeName).append("} value */\n");
                    sb.append(INDENT).append("set ").append(fieldName).append("(value) {\n");
                    sb.append(INDENT).append(INDENT).append("this._").append(fieldName).append(" = value;\n");
                    sb.append(INDENT).append("}\n\n");
                }
            }
        });
    }

    private void generateGroupAccessors(final StringBuilder sb, final List<Token> groups)
    {
        generateGroupAccessors(sb, groups, "");
    }

    private void generateGroupAccessors(final StringBuilder sb, final List<Token> groups, final String parentPrefix)
    {
        for (int i = 0; i < groups.size(); )
        {
            final Token groupToken = groups.get(i);
            if (groupToken.signal() == Signal.BEGIN_GROUP)
            {
                final String groupName = formatPropertyName(groupToken.name());
                final String groupClassName = parentPrefix + toUpperFirstChar(groupName);

                sb.append(INDENT).append("/** @returns {").append(groupClassName).append("[]} */\n");
                sb.append(INDENT).append("get ").append(groupName).append("() {\n");
                sb.append(INDENT).append(INDENT).append("return this._").append(groupName).append(";\n");
                sb.append(INDENT).append("}\n\n");

                sb.append(INDENT).append("/** @param {number} count */\n");
                sb.append(INDENT).append("add").append(groupClassName).append("(count) {\n");
                sb.append(INDENT).append(INDENT).append("for (let i = 0; i < count; i++) {\n");
                sb.append(INDENT).append(INDENT).append(INDENT).append("this._").append(groupName)
                    .append(".push(new ").append(groupClassName).append("());\n");
                sb.append(INDENT).append(INDENT).append("}\n");
                sb.append(INDENT).append("}\n\n");
            }
            i += groupToken.componentTokenCount();
        }
    }

    private void generateVarDataAccessors(final StringBuilder sb, final List<Token> varData)
    {
        for (int i = 0; i < varData.size(); )
        {
            final Token varDataToken = varData.get(i);
            if (varDataToken.signal() == Signal.BEGIN_VAR_DATA)
            {
                final String varDataName = formatPropertyName(varDataToken.name());

                // Check if varData has characterEncoding (string) or not (Buffer)
                final List<Token> varDataTokens = varData.subList(i, i + varDataToken.componentTokenCount());
                boolean hasEncoding = false;
                for (final Token token : varDataTokens)
                {
                    if (token.signal() == Signal.ENCODING && token.encoding().characterEncoding() != null)
                    {
                        hasEncoding = true;
                        break;
                    }
                }

                final String jsType = hasEncoding ? "string" : "Buffer";

                sb.append(INDENT).append("/** @returns {").append(jsType).append("} */\n");
                sb.append(INDENT).append("get ").append(varDataName).append("() {\n");
                sb.append(INDENT).append(INDENT).append("return this._").append(varDataName).append(";\n");
                sb.append(INDENT).append("}\n\n");

                sb.append(INDENT).append("/** @param {").append(jsType).append("} value */\n");
                sb.append(INDENT).append("set ").append(varDataName).append("(value) {\n");
                sb.append(INDENT).append(INDENT).append("this._").append(varDataName).append(" = value;\n");
                sb.append(INDENT).append("}\n\n");
            }
            i += varDataToken.componentTokenCount();
        }
    }

    private void generateGroupClasses(final StringBuilder sb, final List<Token> groups)
    {
        generateGroupClasses(sb, groups, "");
    }

    private void generateGroupClasses(final StringBuilder sb, final List<Token> groups, final String parentPrefix)
    {
        for (int i = 0; i < groups.size(); )
        {
            final Token groupToken = groups.get(i);
            if (groupToken.signal() == Signal.BEGIN_GROUP)
            {
                final String groupName = formatPropertyName(groupToken.name());
                final String groupClassName = parentPrefix + toUpperFirstChar(groupName);

                final List<Token> groupTokens = groups.subList(i, i + groupToken.componentTokenCount());

                // Skip BEGIN_GROUP (token 0), dimension encoding composite (token 1 + its subtokens)
                final Token dimensionToken = groupTokens.get(1);
                final int dimensionTokenCount = dimensionToken.componentTokenCount();
                final int groupBodyStart = 1 + dimensionTokenCount;
                final List<Token> groupBody = groupTokens.subList(groupBodyStart, groupTokens.size() - 1);

                final List<Token> groupFields = new ArrayList<>();
                int j = 0;
                j = collectFields(groupBody, j, groupFields);

                final List<Token> nestedGroups = new ArrayList<>();
                j = collectGroups(groupBody, j, nestedGroups);

                final List<Token> groupVarData = new ArrayList<>();
                collectVarData(groupBody, j, groupVarData);

                sb.append("}\n\n");
                sb.append("class ").append(groupClassName).append(" {\n");

                // Constructor
                sb.append(INDENT).append("constructor() {\n");
                Generators.forEachField(groupFields, (fieldToken, encodingToken) ->
                {
                    final String fieldName = formatPropertyName(fieldToken.name());
                    if (encodingToken.signal() == Signal.BEGIN_COMPOSITE)
                    {
                        final String typeName = encodingToken.name();
                        if (typeName != null && !typeName.isEmpty())
                        {
                            final String compositeName = formatClassName(typeName);
                            sb.append(INDENT).append(INDENT).append("this._").append(fieldName)
                                .append(" = new ").append(compositeName).append("();\n");
                        }
                    }
                    else if (encodingToken.signal() == Signal.ENCODING)
                    {
                        sb.append(INDENT).append(INDENT).append("this._").append(fieldName).append(" = 0;\n");
                    }
                });

                for (int k = 0; k < nestedGroups.size(); )
                {
                    final Token nestedGroupToken = nestedGroups.get(k);
                    if (nestedGroupToken.signal() == Signal.BEGIN_GROUP)
                    {
                        final String nestedGroupName = formatPropertyName(nestedGroupToken.name());
                        sb.append(INDENT).append(INDENT).append("this._").append(nestedGroupName).append(" = [];\n");
                    }
                    k += nestedGroupToken.componentTokenCount();
                }

                for (int k = 0; k < groupVarData.size(); )
                {
                    final Token varDataToken = groupVarData.get(k);
                    if (varDataToken.signal() == Signal.BEGIN_VAR_DATA)
                    {
                        final String varDataName = formatPropertyName(varDataToken.name());

                        // Check if varData has characterEncoding (string) or not (Buffer)
                        final List<Token> varDataTokens =
                            groupVarData.subList(k, k + varDataToken.componentTokenCount());
                        boolean hasEncoding = false;
                        for (final Token token : varDataTokens)
                        {
                            if (token.signal() == Signal.ENCODING && token.encoding().characterEncoding() != null)
                            {
                                hasEncoding = true;
                                break;
                            }
                        }

                        if (hasEncoding)
                        {
                            sb.append(INDENT).append(INDENT).append("this._").append(varDataName).append(" = '';\n");
                        }
                        else
                        {
                            sb.append(INDENT).append(INDENT).append("this._").append(varDataName)
                                .append(" = Buffer.allocUnsafe(0);\n");
                        }
                    }
                    k += varDataToken.componentTokenCount();
                }
                sb.append(INDENT).append("}\n\n");

                // Encode method
                generateGroupEncodeMethod(sb, groupFields, nestedGroups, groupVarData, groupClassName);

                // Decode method
                generateGroupDecodeMethod(sb, groupFields, nestedGroups, groupVarData, groupClassName);

                // Accessors
                generateFieldAccessors(sb, groupFields);
                generateGroupAccessors(sb, nestedGroups, groupClassName);
                generateVarDataAccessors(sb, groupVarData);

                // Nested groups
                if (!nestedGroups.isEmpty())
                {
                    generateGroupClasses(sb, nestedGroups, groupClassName);
                }
            }
            i += groupToken.componentTokenCount();
        }
    }

    private void generateGroupEncodeMethod(
        final StringBuilder sb,
        final List<Token> fields,
        final List<Token> groups,
        final List<Token> varData,
        final String parentPrefix)
    {
        sb.append(INDENT).append("/**\n");
        sb.append(INDENT).append(" * @param {Buffer} buffer\n");
        sb.append(INDENT).append(" * @param {number} offset\n");
        sb.append(INDENT).append(" * @returns {number}\n");
        sb.append(INDENT).append(" */\n");
        sb.append(INDENT).append("encode(buffer, offset) {\n");
        sb.append(INDENT).append(INDENT).append("let pos = offset;\n\n");

        Generators.forEachField(fields, (fieldToken, encodingToken) ->
        {
            final String fieldName = formatPropertyName(fieldToken.name());
            if (encodingToken.signal() == Signal.BEGIN_COMPOSITE)
            {
                sb.append(INDENT).append(INDENT).append("pos += this._").append(fieldName)
                    .append(".encode(buffer, pos);\n");
            }
            else if (encodingToken.signal() == Signal.ENCODING)
            {
                final PrimitiveType primitiveType = encodingToken.encoding().primitiveType();
                final String writeMethod = bufferWriteMethod(primitiveType, byteOrderStr, useUnsafeMode);
                final boolean needsBigIntConversion = useUnsafeMode &&
                    (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64);
                sb.append(INDENT).append(INDENT).append("buffer.").append(writeMethod).append("(");
                if (needsBigIntConversion)
                {
                    sb.append("BigInt(this._").append(fieldName).append(" || 0)");
                }
                else
                {
                    sb.append("this._").append(fieldName);
                }
                sb.append(", pos);\n");
                sb.append(INDENT).append(INDENT).append("pos += ").append(primitiveTypeSize(primitiveType))
                    .append(";\n");
            }
        });

        for (int i = 0; i < groups.size(); )
        {
            final Token groupToken = groups.get(i);
            if (groupToken.signal() == Signal.BEGIN_GROUP)
            {
                final String groupName = formatPropertyName(groupToken.name());
                final Token dimensionToken = groups.get(i + 1);
                final int blockLength = groupToken.encodedLength();

                // Extract dimension types using helper method
                final List<Token> dimensionTokens = groups.subList(i + 1, i + 1 + dimensionToken.componentTokenCount());
                final DimensionTypes dimTypes = extractDimensionTypes(dimensionTokens);

                if (dimTypes.blockLengthToken != null)
                {
                    final String writeMethod = bufferWriteMethod(
                        dimTypes.blockLengthToken.encoding().primitiveType(), byteOrderStr);
                    sb.append(INDENT).append(INDENT).append("buffer.").append(writeMethod)
                        .append("(").append(blockLength).append(", pos);\n");
                    sb.append(INDENT).append(INDENT).append("pos += ")
                        .append(primitiveTypeSize(dimTypes.blockLengthToken.encoding().primitiveType())).append(";\n");
                }

                if (dimTypes.numInGroupToken != null)
                {
                    final String writeMethod = bufferWriteMethod(
                        dimTypes.numInGroupToken.encoding().primitiveType(), byteOrderStr);
                    sb.append(INDENT).append(INDENT).append("buffer.").append(writeMethod)
                        .append("(this._").append(groupName).append(".length, pos);\n");
                    sb.append(INDENT).append(INDENT).append("pos += ")
                        .append(primitiveTypeSize(dimTypes.numInGroupToken.encoding().primitiveType())).append(";\n");
                }

                sb.append(INDENT).append(INDENT).append("for (const entry of this._").append(groupName).append(") {\n");
                sb.append(INDENT).append(INDENT).append(INDENT).append("pos += entry.encode(buffer, pos);\n");
                sb.append(INDENT).append(INDENT).append("}\n");
            }
            i += groupToken.componentTokenCount();
        }

        for (int i = 0; i < varData.size(); )
        {
            final Token varDataToken = varData.get(i);
            if (varDataToken.signal() == Signal.BEGIN_VAR_DATA)
            {
                final String varDataName = formatPropertyName(varDataToken.name());

                // Extract varData types using helper method
                final List<Token> varDataTokens = varData.subList(i, i + varDataToken.componentTokenCount());
                final VarDataTypes varTypes = extractVarDataTypes(varDataTokens);

                // Check if varData has characterEncoding (string) or not (Buffer)
                final boolean hasEncoding = varTypes.dataToken != null &&
                    varTypes.dataToken.encoding().characterEncoding() != null;

                if (hasEncoding)
                {
                    // Optimized path: avoid temporary Buffer allocation
                    final String encoding = mapCharacterEncoding(
                        varTypes.dataToken.encoding().characterEncoding());
                    sb.append(INDENT).append(INDENT).append("const ").append(varDataName)
                        .append("Length = Buffer.byteLength(this._").append(varDataName).append(", '")
                        .append(encoding).append("');\n");

                    if (varTypes.lengthToken != null)
                    {
                        final String writeMethod = bufferWriteMethod(
                            varTypes.lengthToken.encoding().primitiveType(), byteOrderStr);
                        sb.append(INDENT).append(INDENT).append("buffer.").append(writeMethod)
                            .append("(").append(varDataName).append("Length, pos);\n");
                        sb.append(INDENT).append(INDENT).append("pos += ")
                            .append(primitiveTypeSize(varTypes.lengthToken.encoding().primitiveType())).append(";\n");
                    }

                    sb.append(INDENT).append(INDENT).append("buffer.write(this._").append(varDataName)
                        .append(", pos, ").append(varDataName).append("Length, '").append(encoding).append("');\n");
                    sb.append(INDENT).append(INDENT).append("pos += ").append(varDataName).append("Length;\n");
                }
                else
                {
                    // Raw bytes - this._varDataName is already a Buffer
                    if (varTypes.lengthToken != null)
                    {
                        final String writeMethod = bufferWriteMethod(
                            varTypes.lengthToken.encoding().primitiveType(), byteOrderStr);
                        sb.append(INDENT).append(INDENT).append("buffer.").append(writeMethod)
                            .append("(this._").append(varDataName).append(".length, pos);\n");
                        sb.append(INDENT).append(INDENT).append("pos += ")
                            .append(primitiveTypeSize(varTypes.lengthToken.encoding().primitiveType())).append(";\n");
                    }

                    sb.append(INDENT).append(INDENT).append("this._").append(varDataName)
                        .append(".copy(buffer, pos);\n");
                    sb.append(INDENT).append(INDENT).append("pos += this._").append(varDataName)
                        .append(".length;\n");
                }
            }
            i += varDataToken.componentTokenCount();
        }

        sb.append(INDENT).append(INDENT).append("return pos - offset;\n");
        sb.append(INDENT).append("}\n\n");
    }

    private void generateGroupDecodeMethod(
        final StringBuilder sb,
        final List<Token> fields,
        final List<Token> groups,
        final List<Token> varData,
        final String parentPrefix)
    {
        sb.append(INDENT).append("/**\n");
        sb.append(INDENT).append(" * @param {Buffer} buffer\n");
        sb.append(INDENT).append(" * @param {number} offset\n");
        sb.append(INDENT).append(" * @returns {number}\n");
        sb.append(INDENT).append(" */\n");
        sb.append(INDENT).append("decode(buffer, offset) {\n");
        sb.append(INDENT).append(INDENT).append("let pos = offset;\n\n");

        Generators.forEachField(fields, (fieldToken, encodingToken) ->
        {
            final String fieldName = formatPropertyName(fieldToken.name());
            if (encodingToken.signal() == Signal.BEGIN_COMPOSITE)
            {
                sb.append(INDENT).append(INDENT).append("pos += this._").append(fieldName)
                    .append(".decode(buffer, pos);\n");
            }
            else if (encodingToken.signal() == Signal.ENCODING)
            {
                final PrimitiveType primitiveType = encodingToken.encoding().primitiveType();
                final String readMethod = bufferReadMethod(primitiveType, byteOrderStr, useUnsafeMode);
                final boolean needsNumberConversion = useUnsafeMode &&
                    (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64);
                sb.append(INDENT).append(INDENT).append("this._").append(fieldName).append(" = ");
                if (needsNumberConversion)
                {
                    sb.append("Number(");
                }
                sb.append("buffer.").append(readMethod).append("(pos)");
                if (needsNumberConversion)
                {
                    sb.append(")");
                }
                sb.append(";\n");
                sb.append(INDENT).append(INDENT).append("pos += ").append(primitiveTypeSize(primitiveType))
                    .append(";\n");
            }
        });

        for (int i = 0; i < groups.size(); )
        {
            final Token groupToken = groups.get(i);
            if (groupToken.signal() == Signal.BEGIN_GROUP)
            {
                final String groupName = formatPropertyName(groupToken.name());
                final String groupClassName = parentPrefix + toUpperFirstChar(groupName);

                // Extract dimension types using helper method
                final Token dimensionToken = groups.get(i + 1);
                final List<Token> dimensionTokens = groups.subList(i + 1, i + 1 + dimensionToken.componentTokenCount());
                final DimensionTypes dimTypes = extractDimensionTypes(dimensionTokens);

                if (dimTypes.blockLengthToken != null)
                {
                    final String readMethod = bufferReadMethod(
                        dimTypes.blockLengthToken.encoding().primitiveType(), byteOrderStr);
                    sb.append(INDENT).append(INDENT).append("const ").append(groupName)
                        .append("BlockLength = buffer.").append(readMethod).append("(pos);\n");
                    sb.append(INDENT).append(INDENT).append("pos += ")
                        .append(primitiveTypeSize(dimTypes.blockLengthToken.encoding().primitiveType())).append(";\n");
                }

                if (dimTypes.numInGroupToken != null)
                {
                    final String readMethod = bufferReadMethod(
                        dimTypes.numInGroupToken.encoding().primitiveType(), byteOrderStr);
                    sb.append(INDENT).append(INDENT).append("const ").append(groupName)
                        .append("NumInGroup = buffer.").append(readMethod).append("(pos);\n");
                    sb.append(INDENT).append(INDENT).append("pos += ")
                        .append(primitiveTypeSize(dimTypes.numInGroupToken.encoding().primitiveType())).append(";\n");
                }

                sb.append(INDENT).append(INDENT).append("this._").append(groupName).append(" = [];\n");
                sb.append(INDENT).append(INDENT).append("for (let i = 0; i < ").append(groupName)
                    .append("NumInGroup; i++) {\n");
                sb.append(INDENT).append(INDENT).append(INDENT).append("const entry = new ").append(groupClassName)
                    .append("();\n");
                sb.append(INDENT).append(INDENT).append(INDENT).append("pos += entry.decode(buffer, pos);\n");
                sb.append(INDENT).append(INDENT).append(INDENT).append("this._").append(groupName)
                    .append(".push(entry);\n");
                sb.append(INDENT).append(INDENT).append("}\n");
            }
            i += groupToken.componentTokenCount();
        }

        for (int i = 0; i < varData.size(); )
        {
            final Token varDataToken = varData.get(i);
            if (varDataToken.signal() == Signal.BEGIN_VAR_DATA)
            {
                final String varDataName = formatPropertyName(varDataToken.name());

                // Extract varData types using helper method
                final List<Token> varDataTokens = varData.subList(i, i + varDataToken.componentTokenCount());
                final VarDataTypes varTypes = extractVarDataTypes(varDataTokens);

                // Check if varData has characterEncoding (string) or not (Buffer)
                final boolean hasEncoding = varTypes.dataToken != null &&
                    varTypes.dataToken.encoding().characterEncoding() != null;

                if (varTypes.lengthToken != null)
                {
                    final String readMethod = bufferReadMethod(
                        varTypes.lengthToken.encoding().primitiveType(), byteOrderStr);
                    sb.append(INDENT).append(INDENT).append("let ").append(varDataName)
                        .append("Length = buffer.").append(readMethod).append("(pos);\n");
                    sb.append(INDENT).append(INDENT).append("pos += ")
                        .append(primitiveTypeSize(varTypes.lengthToken.encoding().primitiveType())).append(";\n");
                }

                if (hasEncoding)
                {
                    final String encoding = mapCharacterEncoding(
                        varTypes.dataToken.encoding().characterEncoding());
                    sb.append(INDENT).append(INDENT).append("this._").append(varDataName)
                        .append(" = buffer.toString('").append(encoding).append("', pos, pos + ")
                        .append(varDataName).append("Length);\n");
                }
                else
                {
                    sb.append(INDENT).append(INDENT).append("this._").append(varDataName)
                        .append(" = buffer.subarray(pos, pos + ").append(varDataName).append("Length);\n");
                }

                sb.append(INDENT).append(INDENT).append("pos += ").append(varDataName).append("Length;\n");
            }
            i += varDataToken.componentTokenCount();
        }

        sb.append(INDENT).append(INDENT).append("return pos - offset;\n");
        sb.append(INDENT).append("}\n\n");
    }

    private int calculateCompositeSize(final List<Token> tokens)
    {
        int size = 0;
        for (int i = 1; i < tokens.size() - 1; i++)
        {
            final Token token = tokens.get(i);
            if (token.signal() == Signal.ENCODING)
            {
                final Encoding encoding = token.encoding();

                // Skip constants - they don't occupy wire format space
                if (encoding.presence() == Encoding.Presence.CONSTANT)
                {
                    continue;
                }

                final PrimitiveType primitiveType = encoding.primitiveType();
                final int typeSize = primitiveTypeSize(primitiveType);
                final int arrayLength = token.arrayLength();

                // Handle arrays
                if (arrayLength > 1)
                {
                    if (primitiveType == PrimitiveType.CHAR)
                    {
                        size += arrayLength;  // char array: 1 byte per element
                    }
                    else
                    {
                        size += arrayLength * typeSize;  // numeric array
                    }
                }
                else
                {
                    size += typeSize;  // scalar field
                }
            }
        }
        return size;
    }

    /**
     * Helper class to hold dimension type information.
     */
    private static final class DimensionTypes
    {
        Token blockLengthToken;
        Token numInGroupToken;
    }

    /**
     * Helper class to hold varData type information.
     */
    private static final class VarDataTypes
    {
        Token lengthToken;
        Token dataToken;
    }

    /**
     * Extract blockLength and numInGroup types from dimension composite.
     * @param dimensionTokens the list of tokens representing the dimension composite
     * @return DimensionTypes containing blockLength and numInGroup tokens
     * @throws IllegalStateException if required fields are missing
     */
    private DimensionTypes extractDimensionTypes(final List<Token> dimensionTokens)
    {
        final DimensionTypes types = new DimensionTypes();

        for (int j = 1; j < dimensionTokens.size() - 1; j++)
        {
            final Token token = dimensionTokens.get(j);
            if (token.signal() == Signal.ENCODING)
            {
                if ("blockLength".equals(token.name()))
                {
                    types.blockLengthToken = token;
                }
                else if ("numInGroup".equals(token.name()))
                {
                    types.numInGroupToken = token;
                }
            }
        }

        // Validate required fields
        if (types.blockLengthToken == null)
        {
            throw new IllegalStateException(
                "Dimension type composite must contain 'blockLength' field. Composite tokens: " + dimensionTokens);
        }
        if (types.numInGroupToken == null)
        {
            throw new IllegalStateException(
                "Dimension type composite must contain 'numInGroup' field. Composite tokens: " + dimensionTokens);
        }

        return types;
    }

    /**
     * Extract length and data types from varData composite.
     * @param varDataTokens the list of tokens representing the varData composite
     * @return VarDataTypes containing length and data tokens
     * @throws IllegalStateException if required fields are missing
     */
    private VarDataTypes extractVarDataTypes(final List<Token> varDataTokens)
    {
        final VarDataTypes types = new VarDataTypes();

        for (int j = 1; j < varDataTokens.size() - 1; j++)
        {
            final Token token = varDataTokens.get(j);
            if (token.signal() == Signal.ENCODING)
            {
                if ("length".equals(token.name()))
                {
                    types.lengthToken = token;
                }
                else if ("varData".equals(token.name()))
                {
                    types.dataToken = token;
                }
            }
        }

        // Validate required fields
        if (types.lengthToken == null)
        {
            throw new IllegalStateException(
                "VarData type composite must contain 'length' field. Composite tokens: " + varDataTokens);
        }
        if (types.dataToken == null)
        {
            throw new IllegalStateException(
                "VarData type composite must contain 'varData' field. Composite tokens: " + varDataTokens);
        }

        return types;
    }

    /**
     * Format a constant value for JavaScript output.
     * @param constValue the primitive value to format
     * @param primitiveType the type of the primitive value
     * @return formatted string representation for JavaScript
     */
    private String formatConstantValue(final PrimitiveValue constValue, final PrimitiveType primitiveType)
    {
        final String valueStr = constValue.toString();

        if (primitiveType == PrimitiveType.CHAR)
        {
            // Character constant - return as string
            return "'" + valueStr + "'";
        }
        else if (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64)
        {
            // 64-bit integer - use BigInt literal
            return valueStr + "n";
        }
        else
        {
            // Other numeric types
            return valueStr;
        }
    }

    /**
     * Get JavaScript type name for primitives (for JSDoc).
     * @param primitiveType the primitive type to convert
     * @return JavaScript type name string
     */
    private String jsTypeName(final PrimitiveType primitiveType)
    {
        if (primitiveType == PrimitiveType.INT64 || primitiveType == PrimitiveType.UINT64)
        {
            return "bigint";
        }
        else
        {
            return "number";
        }
    }
}
