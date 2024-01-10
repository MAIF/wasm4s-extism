package org.extism.sdk.wasmotoroshi;

import com.sun.jna.Pointer;
import org.extism.sdk.HostFunction;
import org.extism.sdk.LibExtism;
import org.extism.sdk.Plugin;
import org.extism.sdk.manifest.Manifest;

import java.util.concurrent.atomic.AtomicBoolean;

public class WasmOtoroshiInstance extends Plugin {

    private AtomicBoolean closed = new AtomicBoolean(false);

    public WasmOtoroshiInstance(byte[] manifestBytes, boolean withWASI, HostFunction[] functions, LinearMemory[] memories) {
        super(manifestBytes, withWASI, functions, memories);
    }

    public WasmOtoroshiInstance(Manifest manifest, boolean withWASI, HostFunction[] functions, LinearMemory[] memories) {
        super(manifest, withWASI, functions, memories);
    }

    public WasmOtoroshiInstance(byte[] manifestBytes, boolean withWASI, HostFunction[] functions) {
        super(manifestBytes, withWASI, functions);
    }

    public WasmOtoroshiInstance(Manifest manifest, boolean withWASI, HostFunction[] functions) {
        super(manifest, withWASI, functions);
    }

    public Results call(String functionName, Parameters params, int resultsLength) {
        if(!closed.get()) {
            params.getPtr().write();

            LibExtism.ExtismVal.ByReference results = LibExtism.INSTANCE.wasm_otoroshi_call(
                    this.pluginPointer,
                    functionName,
                    params.getPtr(),
                    params.getLength());

            if (results == null) {
                return new Results(0);
            } else {
                return new Results(results, resultsLength);
            }
        }
        return null;
    }

    public Pointer callWithoutParams(String functionName, int resultsLength) {
        if(!closed.get()) {
            Pointer results = LibExtism.INSTANCE.wasm_otoroshi_wasm_plugin_call_without_params(
                    this.pluginPointer,
                    functionName);


            if (results == null) {
                if (resultsLength > 0) {
                    return null;
                } else {
                    return null;
                }
            } else {
                return results;
            }
        }
        return null;
    }

    public void callWithoutResults(String functionName, Parameters params) {
        if(!closed.get()) {
            params.getPtr().write();

            LibExtism.INSTANCE.wasm_otoroshi_wasm_plugin_call_without_results(
                    this.pluginPointer,
                    functionName,
                    params.getPtr(),
                    params.getLength());
        }
    }

    public void freeResults(Results results) {
        LibExtism.INSTANCE.wasm_otoroshi_deallocate_results(results.getPtr(), results.getLength());
    }

    public int writeBytes(byte[] data, int n, int offset) {
        if(!closed.get()) {
            return LibExtism.INSTANCE.wasm_otoroshi_extism_memory_write_bytes(this.pluginPointer, data, n, offset);
        }
        return -1;
    }

    public Pointer getMemory(String name) {
        return LibExtism.INSTANCE.wasm_otoroshi_extism_get_memory(this.pluginPointer, name);
    }

    public void reset() {
        LibExtism.INSTANCE.wasm_otoroshi_extism_reset(this.pluginPointer);
    }

    public int getMemorySize() {
        return LibExtism.INSTANCE.wasm_otoroshi_extism_memory_bytes(this.pluginPointer);
    }

    @Override
    public void close() throws Exception {
        if(!closed.compareAndSet(false, true)) {
            free();
        }
    }
}
