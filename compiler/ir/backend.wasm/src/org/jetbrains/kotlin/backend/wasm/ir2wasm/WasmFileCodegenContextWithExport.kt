/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.util.superClass
import org.jetbrains.kotlin.wasm.ir.WasmExport
import org.jetbrains.kotlin.wasm.ir.WasmFunction
import org.jetbrains.kotlin.wasm.ir.WasmGlobal

class WasmFileCodegenContextWithExport(
    wasmFileFragment: WasmCompiledFileFragment,
    idSignatureRetriever: IdSignatureRetriever,
    private val moduleReferencedDeclarations: ModuleReferencedDeclarations,
) : WasmFileCodegenContext(wasmFileFragment, idSignatureRetriever) {
    override fun defineFunction(irFunction: IrFunctionSymbol, wasmFunction: WasmFunction) {
        super.defineFunction(irFunction, wasmFunction)
        val owner = irFunction.owner
        if (owner.isEffectivelyPrivate()) return
        val signature = idSignatureRetriever.declarationSignature(owner)
        addExport(
            WasmExport.Function(
                field = wasmFunction,
                name = "${WasmServiceImportExportKind.FUNC.prefix}$signature"
            )
        )
    }

    override fun referenceFunction(irFunction: IrFunctionSymbol): FuncSymbol {
        moduleReferencedDeclarations.referencedFunction.add(irFunction.getReferenceKey())
        referenceFunctionType(irFunction)
        return super.referenceFunction(irFunction)
    }

    override fun referenceGlobalVTable(irClass: IrClassSymbol): VTableGlobalSymbol {
        moduleReferencedDeclarations.referencedGlobalVTable.add(irClass.getReferenceKey())
        return super.referenceGlobalVTable(irClass)
    }

    override fun referenceGlobalClassITable(irClass: IrClassSymbol): ClassITableGlobalSymbol {
        moduleReferencedDeclarations.referencedGlobalClassITable.add(irClass.getReferenceKey())
        return super.referenceGlobalClassITable(irClass)
    }

    override fun referenceRttiGlobal(irClass: IrClassSymbol): RttiGlobalSymbol {
        moduleReferencedDeclarations.referencedRttiGlobal.add(irClass.getReferenceKey())
        return super.referenceRttiGlobal(irClass)
    }

    override fun defineGlobalVTable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        super.defineGlobalVTable(irClass, wasmGlobal)
        exportDeclarationGlobal(irClass.owner, WasmServiceImportExportKind.VTABLE, wasmGlobal)
    }

    override fun defineGlobalClassITable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        super.defineGlobalClassITable(irClass, wasmGlobal)
        exportDeclarationGlobal(irClass.owner, WasmServiceImportExportKind.ITABLE, wasmGlobal)
    }

    override fun defineRttiGlobal(global: WasmGlobal, irClass: IrClassSymbol, irSuperClass: IrClassSymbol?) {
        super.defineRttiGlobal(global, irClass, irSuperClass)
        exportDeclarationGlobal(irClass.owner, WasmServiceImportExportKind.RTTI, global)
    }

    private fun addGcTypeToReferenced(irClass: IrClassSymbol) {
        val irClassOwner = irClass.owner
        val signature = idSignatureRetriever.declarationSignature(irClassOwner)!!
        if (!moduleReferencedDeclarations.referencedGcTypes.add(signature)) return

        irClassOwner.declarations.forEach {
            when (it) {
                is IrFunction -> {
                    addFunctionTypeToReferenced(it.symbol)
                }
                is IrField -> {
                    addGcTypeToReferenced(it.type.erasedUpperBound.symbol)
                }
                is IrProperty -> {}
                is IrClass -> {}
                is IrTypeAlias -> {}
                else -> {
                    error("Unexpected symbol type ${it.symbol}")
                }
            }
        }

        irClassOwner.getAllSuperclasses().forEach { addGcTypeToReferenced(it.symbol) }
    }

    private fun addFunctionTypeToReferenced(irClass: IrFunctionSymbol) {
        val irFunctionOwner = irClass.owner
        val signature = idSignatureRetriever.declarationSignature(irFunctionOwner)!!
        if (!moduleReferencedDeclarations.referencedFunctionTypes.add(signature)) return

        irFunctionOwner.parameters.forEach { p ->
            p.type.erasedUpperBound.symbol.let(::addGcTypeToReferenced)
        }
        addGcTypeToReferenced(irFunctionOwner.returnType.erasedUpperBound.symbol)
    }

    override fun referenceGcType(irClass: IrClassSymbol): GcTypeSymbol {
        addGcTypeToReferenced(irClass)
        return super.referenceGcType(irClass)
    }

    override fun referenceHeapType(irClass: IrClassSymbol): GcHeapTypeSymbol {
        addGcTypeToReferenced(irClass)
        return super.referenceHeapType(irClass)
    }

    override fun referenceVTableGcType(irClass: IrClassSymbol): VTableTypeSymbol {
        addGcTypeToReferenced(irClass)
        return super.referenceVTableGcType(irClass)
    }

    override fun referenceVTableHeapType(irClass: IrClassSymbol): VTableHeapTypeSymbol {
        addGcTypeToReferenced(irClass)
        return super.referenceVTableHeapType(irClass)
    }

    override fun referenceFunctionType(irClass: IrFunctionSymbol): FunctionTypeSymbol {
        addFunctionTypeToReferenced(irClass)
        return super.referenceFunctionType(irClass)
    }

    override fun referenceFunctionHeapType(irClass: IrFunctionSymbol): FunctionHeapTypeSymbol {
        addFunctionTypeToReferenced(irClass)
        return super.referenceFunctionHeapType(irClass)
    }

    private fun exportDeclarationGlobal(declaration: IrDeclarationWithVisibility, prefix: WasmServiceImportExportKind, global: WasmGlobal) {
        if (declaration.isEffectivelyPrivate()) return
        val signature = idSignatureRetriever.declarationSignature(declaration)
        addExport(
            WasmExport.Global(
                name = "${prefix.prefix}$signature",
                field = global
            )
        )
    }
}