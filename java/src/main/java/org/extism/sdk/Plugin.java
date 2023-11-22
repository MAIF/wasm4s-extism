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
     * Holds the Extism {@link Context} that the plugin belongs to.
     */
    private final Context context;

    /**
     * Holds the index of the plugin
     */
    private final int index;

    /**
     * Constructor for a Plugin. Only expose internally. Plugins should be created and
     * managed from {@link org.extism.sdk.Context}.
     *
     * @param context       The context to manage the plugin
     * @param manifestBytes The manifest for the plugin
     * @param functions     The Host functions for th eplugin
     * @param withWASI      Set to true to enable WASI
     */
    public Plugin(Context context,
                  byte[] manifestBytes,
                  boolean withWASI,
                  HostFunction[] functions,
                  LinearMemory[] memories) {

        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(manifestBytes, "manifestBytes");

        Pointer[] functionsPtr = HostFunction.arrayToPointer(functions);
        Pointer[] memoriesPtr = LinearMemory.arrayToPointer(memories);

        Pointer contextPointer = context.getPointer();

        int index = LibExtism.INSTANCE.extism_plugin_new(contextPointer,
                manifestBytes,
                manifestBytes.length,
                functionsPtr,
                functions == null ? 0 : functions.length,
                withWASI,
                memoriesPtr,
                memories == null ? 0 : memories.length
        );
        if (index == -1) {
            String error = context.error(this);
            throw new ExtismException(error);
        }

        this.index= index;
        this.context = context;
    }


    public Plugin(Context context, Manifest manifest, boolean withWASI, HostFunction[] functions, LinearMemory[] memories) {
        this(context, serialize(manifest), withWASI, functions, memories);
    }

    private static byte[] serialize(Manifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        return JsonSerde.toJson(manifest).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Getter for the internal index pointer to this plugin.
     *
     * @return the plugin index
     */
    public int getIndex() {
        return index;
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

        Pointer contextPointer = context.getPointer();
        int inputDataLength = inputData == null ? 0 : inputData.length;
        int exitCode = LibExtism.INSTANCE.extism_plugin_call(contextPointer, index, functionName, inputData, inputDataLength);
        if (exitCode == -1) {
            String error = context.error(this);
            throw new ExtismException(error);
        }

        int length = LibExtism.INSTANCE.extism_plugin_output_length(contextPointer, index);
        Pointer output = LibExtism.INSTANCE.extism_plugin_output_data(contextPointer, index);
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

    public Results call(String functionName, Parameters params, int resultsLength) {
        return call(functionName, params, resultsLength, new byte[0]);
    }

    public Results call(String functionName, Parameters params, int resultsLength, byte[] input) {
        Pointer contextPointer = context.getPointer();
        params.getPtr().write();

        LibExtism.ExtismVal.ByReference results = LibExtism.INSTANCE.wasm_plugin_call(
                contextPointer,
                index,
                functionName,
                params.getPtr(),
                params.getLength(),
                input,
                input.length);

        if (results == null && resultsLength > 0) {
            String error = context.error(this);
            throw new ExtismException(error);
        }

        if (results == null) {
            if (resultsLength > 0) {
                String error = context.error(this);
                throw new ExtismException(error);
            } else {
                return new Results(0);
            }
        } else {
            return new Results(results, resultsLength);
        }
    }

    public Context getContext() {
        return context;
    }

    /**
     * Update the plugin code given manifest changes
     *
     * @param manifest The manifest for the plugin
     * @param withWASI Set to true to enable WASI
     * @return {@literal true} if update was successful
     */
    public boolean update(Manifest manifest, boolean withWASI, HostFunction[] functions, LinearMemory[] memories) {
        return update(serialize(manifest), withWASI, functions, memories);
    }

    /**
     * Update the plugin code given manifest changes
     *
     * @param manifestBytes The manifest for the plugin
     * @param withWASI      Set to true to enable WASI
     * @return {@literal true} if update was successful
     */
    public boolean update(byte[] manifestBytes, boolean withWASI, HostFunction[] functions, LinearMemory[] memories) {
        Objects.requireNonNull(manifestBytes, "manifestBytes");

        Pointer[] ptrArr = HostFunction.arrayToPointer(functions);
        Pointer[] ptrMem = LinearMemory.arrayToPointer(memories);


        return LibExtism.INSTANCE.extism_plugin_update(context.getPointer(), index, manifestBytes, manifestBytes.length,
                ptrArr,
                functions == null ? 0 : functions.length,
                withWASI,
                ptrMem,
                memories == null ? 0 : memories.length
        );
    }

    /**
     * Frees a plugin from memory. Plugins will be automatically cleaned up
     * if you free their parent Context using {@link org.extism.sdk.Context#free() free()} or {@link org.extism.sdk.Context#reset() reset()}
     */
    public void free() {
        LibExtism.INSTANCE.extism_plugin_free(context.getPointer(), index);
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
        return LibExtism.INSTANCE.extism_plugin_config(context.getPointer(), index, jsonBytes, jsonBytes.length);
    }

    /**
     * Calls {@link #free()} if used in the context of a TWR block.
     */
    @Override
    public void close() {
        free();
    }

    public Pointer getPointer() {
        return context.getPointer();
    }
    /**
     * Return a new `CancelHandle`, which can be used to cancel a running Plugin
     */
    public CancelHandle cancelHandle() {
        if (this.context.getPointer() == null) {
            throw new ExtismException("No Context set");
        }
        Pointer handle = LibExtism.INSTANCE.extism_plugin_cancel_handle(this.context.getPointer(), this.index);
        return new CancelHandle(handle);
    }

    public void reset() {
        LibExtism.INSTANCE.extism_reset(this.context.getPointer(), this.index);
    }

    public Pointer callWithoutParams(String functionName, int resultsLength) {
        Pointer results = LibExtism.INSTANCE.wasm_plugin_call_without_params(
                context.getPointer(),
                index,
                functionName,
                new byte[0],
                0);


        if (results == null) {
            if (resultsLength > 0) {
                String error = context.error(this);
                throw new ExtismException(error);
            } else {
                return null;
            }
        } else {
            return results;
        }
    }

    public void callWithoutResults(String functionName, Parameters params) {
        Pointer contextPointer = context.getPointer();
        params.getPtr().write();

        LibExtism.INSTANCE.wasm_plugin_call_without_results(
                contextPointer,
                index,
                functionName,
                params.getPtr(),
                params.getLength(),
                new byte[0],
                0);
    }
}
