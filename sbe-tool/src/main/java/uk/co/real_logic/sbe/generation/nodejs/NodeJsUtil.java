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

import uk.co.real_logic.sbe.PrimitiveType;
import uk.co.real_logic.sbe.SbeTool;
import uk.co.real_logic.sbe.ValidationUtil;

import java.util.EnumMap;
import java.util.Map;

import static uk.co.real_logic.sbe.generation.Generators.toLowerFirstChar;
import static uk.co.real_logic.sbe.generation.Generators.toUpperFirstChar;

/**
 * Utilities for mapping between IR and the Node.js/JavaScript language.
 */
public class NodeJsUtil
{
    private static final Map<PrimitiveType, String> PRIMITIVE_TYPE_MAP = new EnumMap<>(PrimitiveType.class);
    private static final Map<PrimitiveType, String> BUFFER_READ_METHOD_MAP = new EnumMap<>(PrimitiveType.class);
    private static final Map<PrimitiveType, String> BUFFER_WRITE_METHOD_MAP = new EnumMap<>(PrimitiveType.class);

    static
    {
        // JavaScript uses Number for most numeric types
        PRIMITIVE_TYPE_MAP.put(PrimitiveType.CHAR, "number");
        PRIMITIVE_TYPE_MAP.put(PrimitiveType.INT8, "number");
        PRIMITIVE_TYPE_MAP.put(PrimitiveType.INT16, "number");
        PRIMITIVE_TYPE_MAP.put(PrimitiveType.INT32, "number");
        PRIMITIVE_TYPE_MAP.put(PrimitiveType.INT64, "bigint");
        PRIMITIVE_TYPE_MAP.put(PrimitiveType.UINT8, "number");
        PRIMITIVE_TYPE_MAP.put(PrimitiveType.UINT16, "number");
        PRIMITIVE_TYPE_MAP.put(PrimitiveType.UINT32, "number");
        PRIMITIVE_TYPE_MAP.put(PrimitiveType.UINT64, "bigint");
        PRIMITIVE_TYPE_MAP.put(PrimitiveType.FLOAT, "number");
        PRIMITIVE_TYPE_MAP.put(PrimitiveType.DOUBLE, "number");
    }

    static
    {
        // Node.js Buffer read methods
        BUFFER_READ_METHOD_MAP.put(PrimitiveType.CHAR, "readUInt8");
        BUFFER_READ_METHOD_MAP.put(PrimitiveType.INT8, "readInt8");
        BUFFER_READ_METHOD_MAP.put(PrimitiveType.INT16, "readInt16LE");
        BUFFER_READ_METHOD_MAP.put(PrimitiveType.INT32, "readInt32LE");
        BUFFER_READ_METHOD_MAP.put(PrimitiveType.INT64, "readBigInt64LE");
        BUFFER_READ_METHOD_MAP.put(PrimitiveType.UINT8, "readUInt8");
        BUFFER_READ_METHOD_MAP.put(PrimitiveType.UINT16, "readUInt16LE");
        BUFFER_READ_METHOD_MAP.put(PrimitiveType.UINT32, "readUInt32LE");
        BUFFER_READ_METHOD_MAP.put(PrimitiveType.UINT64, "readBigUInt64LE");
        BUFFER_READ_METHOD_MAP.put(PrimitiveType.FLOAT, "readFloatLE");
        BUFFER_READ_METHOD_MAP.put(PrimitiveType.DOUBLE, "readDoubleLE");
    }

    static
    {
        // Node.js Buffer write methods
        BUFFER_WRITE_METHOD_MAP.put(PrimitiveType.CHAR, "writeUInt8");
        BUFFER_WRITE_METHOD_MAP.put(PrimitiveType.INT8, "writeInt8");
        BUFFER_WRITE_METHOD_MAP.put(PrimitiveType.INT16, "writeInt16LE");
        BUFFER_WRITE_METHOD_MAP.put(PrimitiveType.INT32, "writeInt32LE");
        BUFFER_WRITE_METHOD_MAP.put(PrimitiveType.INT64, "writeBigInt64LE");
        BUFFER_WRITE_METHOD_MAP.put(PrimitiveType.UINT8, "writeUInt8");
        BUFFER_WRITE_METHOD_MAP.put(PrimitiveType.UINT16, "writeUInt16LE");
        BUFFER_WRITE_METHOD_MAP.put(PrimitiveType.UINT32, "writeUInt32LE");
        BUFFER_WRITE_METHOD_MAP.put(PrimitiveType.UINT64, "writeBigUInt64LE");
        BUFFER_WRITE_METHOD_MAP.put(PrimitiveType.FLOAT, "writeFloatLE");
        BUFFER_WRITE_METHOD_MAP.put(PrimitiveType.DOUBLE, "writeDoubleLE");
    }

    /**
     * Map a {@link PrimitiveType} to a JavaScript type name (for JSDoc annotations).
     *
     * @param primitiveType to map.
     * @return the JavaScript type name.
     */
    public static String typescriptTypeName(final PrimitiveType primitiveType)
    {
        return PRIMITIVE_TYPE_MAP.get(primitiveType);
    }

    /**
     * Get the Buffer read method for a primitive type.
     *
     * @param primitiveType to map.
     * @param byteOrder for the encoding.
     * @return the Buffer read method name.
     */
    public static String bufferReadMethod(final PrimitiveType primitiveType, final String byteOrder)
    {
        String method = BUFFER_READ_METHOD_MAP.get(primitiveType);
        if (method == null)
        {
            throw new IllegalArgumentException("No Buffer read method for primitive type: " + primitiveType);
        }

        // INT8 and UINT8 don't have byte order variants
        if (primitiveType == PrimitiveType.INT8 || primitiveType == PrimitiveType.UINT8 ||
            primitiveType == PrimitiveType.CHAR)
        {
            return method;
        }

        // For multi-byte types, handle byte order
        if ("BIG_ENDIAN".equals(byteOrder))
        {
            if (method.endsWith("LE"))
            {
                return method.substring(0, method.length() - 2) + "BE";
            }
            // If method doesn't end with LE, it might already be the correct variant
            return method;
        }

        return method;
    }

    /**
     * Get the Buffer write method for a primitive type.
     *
     * @param primitiveType to map.
     * @param byteOrder for the encoding.
     * @return the Buffer write method name.
     */
    public static String bufferWriteMethod(final PrimitiveType primitiveType, final String byteOrder)
    {
        String method = BUFFER_WRITE_METHOD_MAP.get(primitiveType);
        if (method == null)
        {
            throw new IllegalArgumentException("No Buffer write method for primitive type: " + primitiveType);
        }

        // INT8 and UINT8 don't have byte order variants
        if (primitiveType == PrimitiveType.INT8 || primitiveType == PrimitiveType.UINT8 ||
            primitiveType == PrimitiveType.CHAR)
        {
            return method;
        }

        // For multi-byte types, handle byte order
        if ("BIG_ENDIAN".equals(byteOrder))
        {
            if (method.endsWith("LE"))
            {
                return method.substring(0, method.length() - 2) + "BE";
            }
            // If method doesn't end with LE, it might already be the correct variant
            return method;
        }

        return method;
    }

    /**
     * Format a string as a property name (camelCase).
     *
     * @param value to be formatted.
     * @return the string formatted as a property name.
     */
    public static String formatPropertyName(final String value)
    {
        String formattedValue = toLowerFirstChar(value);

        if (ValidationUtil.isJavaScriptKeyword(formattedValue))
        {
            final String keywordAppendToken = System.getProperty(SbeTool.KEYWORD_APPEND_TOKEN);
            if (null == keywordAppendToken)
            {
                throw new IllegalStateException(
                    "Invalid property name='" + formattedValue +
                    "' please correct the schema or consider setting system property: " + SbeTool.KEYWORD_APPEND_TOKEN);
            }

            formattedValue += keywordAppendToken;
        }

        return formattedValue;
    }

    /**
     * Format a string as a class/type name (PascalCase).
     *
     * @param value to be formatted.
     * @return the string formatted as a type name.
     */
    public static String formatClassName(final String value)
    {
        return toUpperFirstChar(value);
    }

    /**
     * Format a string as a constant name (UPPER_SNAKE_CASE).
     *
     * @param value to be formatted.
     * @return the string formatted as a constant name.
     */
    public static String formatConstantName(final String value)
    {
        return value.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

    /**
     * Get the size in bytes for a primitive type.
     *
     * @param primitiveType to get size for.
     * @return the size in bytes.
     */
    public static int primitiveTypeSize(final PrimitiveType primitiveType)
    {
        switch (primitiveType)
        {
            case CHAR:
            case INT8:
            case UINT8:
                return 1;
            case INT16:
            case UINT16:
                return 2;
            case INT32:
            case UINT32:
            case FLOAT:
                return 4;
            case INT64:
            case UINT64:
            case DOUBLE:
                return 8;
            default:
                throw new IllegalArgumentException("Unknown primitive type: " + primitiveType);
        }
    }

    /**
     * Map SBE character encoding to Node.js Buffer encoding name.
     *
     * @param sbeEncoding the SBE character encoding name.
     * @return the Node.js Buffer encoding name.
     */
    public static String mapCharacterEncoding(final String sbeEncoding)
    {
        if (sbeEncoding == null)
        {
            return "utf8";
        }

        final String lower = sbeEncoding.toLowerCase();
        switch (lower)
        {
            case "iso-8859-1":
            case "iso_8859_1":
                return "latin1";
            case "us-ascii":
            case "ascii":
                return "ascii";
            case "utf-8":
            case "utf8":
                return "utf8";
            case "utf-16":
            case "utf16":
            case "utf-16le":
            case "utf16le":
                return "utf16le";
            default:
                return lower;
        }
    }

    /**
     * Generate JSDoc comment for a field or type.
     *
     * @param description of the element.
     * @param indent the indentation level.
     * @return the JSDoc comment string.
     */
    public static String generateJsDoc(final String description, final String indent)
    {
        if (null == description || description.isEmpty())
        {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(indent).append("/**\n");
        sb.append(indent).append(" * ").append(description).append("\n");
        sb.append(indent).append(" */\n");
        return sb.toString();
    }
}
