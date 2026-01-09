/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

internal fun K2JVMCompilerArguments.applyClasspathEntries(classpathEntries: List<Path>) {
    val rawValue = classpath?.split(File.pathSeparator)?.map(::Path) ?: emptyList()

    classpath = (classpathEntries + rawValue).distinct().joinToString(File.pathSeparator)
}

internal fun applyClasspathEntries(
    currentValue: List<Path>,
    compilerArgs: K2JVMCompilerArguments,
): List<Path> {
    val rawValue = compilerArgs.classpath?.split(File.pathSeparator)?.map(::Path) ?: emptyList()

    return (currentValue + rawValue).distinct()
}