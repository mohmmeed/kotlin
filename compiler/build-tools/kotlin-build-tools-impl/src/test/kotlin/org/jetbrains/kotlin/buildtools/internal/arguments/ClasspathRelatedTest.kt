/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ClasspathRelatedTest {
    @Test
    fun `when both classpath and classpath entries are provided, they are merged`() {
        val entries = listOf(Path("a"), Path("b"))
        val args = createArgs("c${File.pathSeparator}d")

        args.applyClasspathEntries(entries)

        assertEquals("a${File.pathSeparator}b${File.pathSeparator}c${File.pathSeparator}d", args.classpath)
    }

    @Test
    fun `when classpath and classpath entries data overlap, duplicates are removed`() {
        val entries = listOf(Path("a"), Path("b"), Path("c"))
        val args = createArgs("b${File.pathSeparator}c${File.pathSeparator}d")

        args.applyClasspathEntries(entries)

        assertEquals("a${File.pathSeparator}b${File.pathSeparator}c${File.pathSeparator}d", args.classpath)
    }

    @Test
    fun `when classpath is null, entries are set as the classpath`() {
        val entries = listOf(Path("a"), Path("b"))
        val args = createArgs(null)

        args.applyClasspathEntries(entries)

        assertEquals("a${File.pathSeparator}b", args.classpath)
    }

    @Test
    fun `when classpath is empty, entries are set as the classpath`() {
        val entries = listOf(Path("a"), Path("b"))
        val args = createArgs("")

        args.applyClasspathEntries(entries)

        assertEquals("a${File.pathSeparator}b", args.classpath)
    }

    @Test
    fun `when classpath entry list is empty, classpath remains unchanged`() {
        val entries = emptyList<Path>()
        val args = createArgs("a${File.pathSeparator}b")

        args.applyClasspathEntries(entries)

        assertEquals("a${File.pathSeparator}b", args.classpath)
    }

    private fun createArgs(classpath: String?) = K2JVMCompilerArguments().apply {
        this.classpath = classpath
    }
}
