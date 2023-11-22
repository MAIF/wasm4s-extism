package org.extism.sdk;

import com.sun.jna.Pointer;
import org.extism.sdk.manifest.Manifest;
import org.extism.sdk.support.JsonSerde;

import org.extism.sdk.Parameters;
import org.extism.sdk.Results;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Represents a Extism plugin.
 */
public class Plugin implements AutoCloseable {

    /**
     * Holds the Extism plugin pointer
     */
    private final Pointer pluginPointer;

    public Plugin(byte[] manifestBytes,
                  boolean withWASI,
                  HostFunction[] functions,
                  LinearMemory[] memories) {

        Objects.requireNonNull(manifestBytes, "manifestBytes");

        Pointer[] functionsPtr = HostFunction.arrayToPointer(functions);
        Pointer[] memoriesPtr = LinearMemory.arrayToPointer(memories);

        Pointer[] errormsg = new Pointer[1];
        Pointer p = LibExtism.INSTANCE.extism_plugin_new(manifestBytes,
                manifestBytes.length,
                functionsPtr,
                functions == null ? 0 : functions.length,
                withWASI,
                memoriesPtr,
                memories == null ? 0 : memories.length,
                errormsg
        );

        if (p == null) {
            int errlen = LibExtism.INSTANCE.strlen(errormsg[0]);
            byte[] msg = new byte[errlen];
            errormsg[0].read(0, msg, 0, errlen);
            LibExtism.INSTANCE.extism_plugin_new_error_free(errormsg[0]);
            throw new ExtismException(new String(msg));
        }

        this.pluginPointer = p;
    }

    public Plugin(Manifest manifest, boolean withWASI, HostFunction[] functions) {
        this(serialize(manifest), withWASI, functions, null);
    }


    public Plugin(Manifest manifest, boolean withWASI, HostFunction[] functions, LinearMemory[] memories) {
        this(serialize(manifest), withWASI, functions, memories);
    }

    private static byte[] serialize(Manifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        return JsonSerde.toJson(manifest).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Invoke a function with the given name and input.
     *
     * @param functionName The name of the exported function to invoke
     * @param inputData    The raw bytes representing any input data
     * @return A byte array representing the raw output data
     * @throws ExtismException if the call fails
     */
    public byte[] call(String functionName, byte[] inputData) {

        Objects.requireNonNull(functionName, "functionName");

        int inputDataLength = inputData == null ? 0 : inputData.length;
        int exitCode = LibExtism.INSTANCE.extism_plugin_call(this.pluginPointer, functionName, inputData, inputDataLength);
        if (exitCode == -1) {
            String error = this.error();
            throw new ExtismException(error);
        }

        int length = LibExtism.INSTANCE.extism_plugin_output_length(this.pluginPointer);
        Pointer output = LibExtism.INSTANCE.extism_plugin_output_data(this.pluginPointer);
        return output.getByteArray(0, length);
    }

    /**
     * Invoke a function with the given name and input.
     *
     * @param functionName The name of the exported function to invoke
     * @param input        The string representing the input data
     * @return A string representing the output data
     */
    public String call(String functionName, String input) {

        Objects.requireNonNull(functionName, "functionName");

        var inputBytes = input == null ? null : input.getBytes(StandardCharsets.UTF_8);
        var outputBytes = call(functionName, inputBytes);
        return new String(outputBytes, StandardCharsets.UTF_8);
    }
    
    /**
     * Get the error associated with a plugin
     *
     * @return the error message
     */
    protected String error() {
        return LibExtism.INSTANCE.extism_plugin_error(this.pluginPointer);
    }

    public Results call(String functionName, Parameters params, int resultsLength) {
        return call(functionName, params, resultsLength, new byte[0]);
    }

    public Results call(String functionName, Parameters params, int resultsLength, byte[] input) {
        params.getPtr().write();

        LibExtism.ExtismVal.ByReference results = LibExtism.INSTANCE.wasm_plugin_call(
                this.pluginPointer,
                functionName,
                params.getPtr(),
                params.getLength(),
                input,
                input.length);

        if (results == null && resultsLength > 0) {
            String error = error();
            throw new ExtismException(error);
        }

        if (results == null) {
            if (resultsLength > 0) {
                String error = error();
                throw new ExtismException(error);
            } else {
                return new Results(0);
            }
        } else {
            return new Results(results, resultsLength);
        }
    }
    /**
     * Frees a plugin from memory
     */
    public void free() {
        LibExtism.INSTANCE.extism_plugin_free(this.pluginPointer);
    }

    /**
     * Update plugin config values, this will merge with the existing values.
     *
     * @param json
     * @return
     */
    public boolean updateConfig(String json) {
        Objects.requireNonNull(json, "json");
        return updateConfig(json.getBytes(StandardCharsets.UTF_8));
    }


    /**
     * Update plugin config values, this will merge with the existing values.
     *
     * @param jsonBytes
     * @return {@literal true} if update was successful
     */
    public boolean updateConfig(byte[] jsonBytes) {
        Objects.requireNonNull(jsonBytes, "jsonBytes");
        return LibExtism.INSTANCE.extism_plugin_config(this.pluginPointer, jsonBytes, jsonBytes.length);
    }

    /**
     * Return a new `CancelHandle`, which can be used to cancel a running Plugin
     */
    public CancelHandle cancelHandle() {
        Pointer handle = LibExtism.INSTANCE.extism_plugin_cancel_handle(this.pluginPointer);
        return new CancelHandle(handle);
    }

    public void reset() {
        LibExtism.INSTANCE.extism_reset(this.pluginPointer);
    }

    public Pointer callWithoutParams(String functionName, int resultsLength) {
        Pointer results = LibExtism.INSTANCE.wasm_plugin_call_without_params(
                this.pluginPointer,
                functionName,
                new byte[0],
                0);


        if (results == null) {
            if (resultsLength > 0) {
                String error = error();
                throw new ExtismException(error);
            } else {
                return null;
            }
        } else {
            return results;
        }
    }

    public void callWithoutResults(String functionName, Parameters params) {
        params.getPtr().write();

        LibExtism.INSTANCE.wasm_plugin_call_without_results(
                this.pluginPointer,
                functionName,
                params.getPtr(),
                params.getLength(),
                new byte[0],
                0);
    }

    @Override
    public void close() throws Exception {
        free();
    }
}
