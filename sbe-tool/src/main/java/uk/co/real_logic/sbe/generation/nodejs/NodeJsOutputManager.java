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

import org.agrona.generation.OutputManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * {@link OutputManager} for managing the creation of Node.js/JavaScript source files.
 */
public class NodeJsOutputManager implements OutputManager
{
    private final File outputDir;

    /**
     * Create a new {@link OutputManager} for generating Node.js/JavaScript source files.
     *
     * @param baseDirName for the generated source files.
     */
    public NodeJsOutputManager(final String baseDirName)
    {
        this.outputDir = new File(baseDirName);
        if (!outputDir.exists() && !outputDir.mkdirs())
        {
            throw new IllegalStateException("Unable to create directory: " + baseDirName);
        }
    }

    /**
     * Create a new output for a JavaScript source file.
     *
     * @param name the name of the JavaScript class.
     * @return a {@link Writer} to which the source code should be written.
     * @throws IOException if an error occurs creating the file.
     */
    public Writer createOutput(final String name) throws IOException
    {
        final File targetFile = new File(outputDir, name + ".js");
        return Files.newBufferedWriter(targetFile.toPath(), StandardCharsets.UTF_8);
    }
}
