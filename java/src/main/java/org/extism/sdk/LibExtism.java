package org.extism.sdk;

import com.sun.jna.*;

/**
 * Wrapper around the Extism library.
 */
public interface LibExtism extends Library {

    /**
     * Holds the extism library instance.
     * Resolves the extism library based on the resolution algorithm defined in {@link com.sun.jna.NativeLibrary}.
     */
    LibExtism INSTANCE = Native.load("extism", LibExtism.class);

    interface InternalExtismFunction extends Callback {
        void invoke(
                Pointer currentPlugin,
                ExtismVal inputs,
                int nInputs,
                ExtismVal outputs,
                int nOutputs,
                Pointer data
        );
    }

    @Structure.FieldOrder({"t", "v"})
    class ExtismVal extends Structure {
        public static class ByReference extends ExtismVal implements Structure.ByReference {
            public ByReference(Pointer ptr) {
                super(ptr);
            }

            public ByReference() {}
        }

        public ExtismVal() {}

        public ExtismVal(Pointer p) {
            super(p);
        }

        public int t;
        public ExtismValUnion v;

        @Override
        public String toString() {
            String typeAsString = new String[]{"int", "long", "float", "double"}[t];

            Object unionValue = new Object[]{v.i32, v.i64, v.f32, v.f64}[t];

            return String.format("ExtismVal(%s, %s)", typeAsString, unionValue);
        }
    }

    class ExtismValUnion extends Union {
        public int i32;
        public long i64;
        public float f32;
        public double f64;
    }

    enum ExtismValType {
        I32(0),
        I64(1),
        F32(2),
        F64(3),
        V128(4),
        FuncRef(5),
        ExternRef(6);

        public final int v;

        ExtismValType(int value) {
            this.v = value;
        }
    }

    Pointer extism_function_new(String name,
                                int[] inputs,
                                int nInputs,
                                int[] outputs,
                                int nOutputs,
                                InternalExtismFunction func,
                                Pointer userData,
                                Pointer freeUserData);

    void extism_function_free(Pointer function);

    Pointer extism_memory_new(String name, String namespace, int minPages, int maxPages);

    /**
     * Get the length of an allocated block
     * NOTE: this should only be called from host functions.
     */
    int extism_current_plugin_memory_length(Pointer plugin, long n);

    /**
     * Returns a pointer to the memory of the currently running plugin
     * NOTE: this should only be called from host functions.
     */
    Pointer extism_current_plugin_memory(Pointer plugin);

    /**
     * Allocate a memory block in the currently running plugin
     * NOTE: this should only be called from host functions.
     */
    int extism_current_plugin_memory_alloc(Pointer plugin, long n);

//    Pointer extism_get_lineary_memory_from_host_functions(Pointer plugin, String memoryName);

    /**
     * Free an allocated memory block
     * NOTE: this should only be called from host functions.
     */
    void extism_current_plugin_memory_free(Pointer plugin, long ptr);

    /**
     * Sets the logger to the given path with the given level of verbosity
     *
     * @param path     The file path of the logger
     * @param logLevel The level of the logger
     * @return true if successful
     */
    boolean extism_log_file(String path, String logLevel);

    /**
     * Returns the error associated with a @{@link Plugin}
     *
     * @param pluginPointer
     * @return
     */
    String extism_plugin_error(Pointer pluginPointer);

    /**
     *
     * @param pluginPointer
     * @return
     */
    String extism_error(Pointer plugiPointer);

    /**
     * Create a new plugin.
     *
     * @param wasm           is a WASM module (wat or wasm) or a JSON encoded manifest
     * @param wasmSize       the length of the `wasm` parameter
     * @param functions      host functions
     * @param nFunctions     the number of host functions
     * @param withWASI       enables/disables WASI
     * @return id of the plugin or {@literal -1} in case of error
     */

    Pointer extism_plugin_new(byte[] wasm,
                          long wasmSize,
                          Pointer[] functions,
                          int nFunctions,
                          boolean withWASI,
                          Pointer[] errmsg);

    /**
     * Free error message from `extism_plugin_new`
     */
    void extism_plugin_new_error_free(Pointer errmsg);


    /**
     * Returns the Extism version string
     */
    String extism_version();


    /**
     * Calls a function from the @{@link Plugin} at the given {@code pluginIndex}.
     *
     * @param pluginPointer
     * @param function_name  is the function to call
     * @param data           is the data input data
     * @param dataLength     is the data input data length
     * @return the result code of the plugin call. {@literal -1} in case of error, {@literal 0} otherwise.
     */
    int extism_plugin_call(Pointer pluginPointer, String function_name, byte[] data, int dataLength);

    /**
     * Returns 
     * @return the length of the output data in bytes.
     */
    int extism_plugin_output_length(Pointer pluginPointer);

    /**
   
     * @return
     */
    Pointer extism_plugin_output_data(Pointer pluginPointer);

    /**
     * Remove a plugin from the
     */
    void extism_plugin_free(Pointer pluginPointer);

    LibExtism.ExtismVal.ByReference wasm_plugin_call(
            Pointer contextPointer,
            String function_name,
            ExtismVal.ByReference inputs,
            int nInputs,
            byte[] data,
            int dataLength);

    Pointer wasm_plugin_call_without_params(
            Pointer contextPointer,
            String function_name,
            byte[] data,
            int dataLength);

    void wasm_plugin_call_without_results(
            Pointer contextPointer,
            String function_name,
            ExtismVal.ByReference inputs,
            int nInputs);

    Pointer extism_plugin_call_native(Pointer contextPointer, String function_name, ExtismVal inputs, int nInputs);

    int extism_plugin_call_native_int(Pointer contextPointer, String function_name, ExtismVal.ByReference inputs, int nInputs, byte[] data, int dataLen);

    void deallocate_plugin_call_results(LibExtism.ExtismVal.ByReference results, int length);


    /**
     * Update plugin config values, this will merge with the existing values.
     *
     * @param pluginPointer
     * @param json
     * @param jsonLength
     * @return {@literal true} if update was successful
     */
    boolean extism_plugin_config(Pointer pluginPointer, byte[] json, int jsonLength);
    Pointer extism_plugin_cancel_handle(Pointer pluginPointer);
    int strlen(Pointer s);
    Pointer extism_plugin_cancel_handle(Pointer contextPointer, int n);
    boolean extism_plugin_cancel(Pointer contextPointer);
    void extism_function_set_namespace(Pointer p, String name);

    // void extism_reset(Pointer contextPointer);
    void extism_plugin_reset(Pointer contextPointer);
//    Pointer extism_get_lineary_memory_from_host_functions(Pointer plugin, int instanceIndex, String memoryName);


    // TODO - ADDED
    Pointer wasm_otoroshi_create_wasmtime_memory(String name, String namespace, int minPages, int maxPages);
    // TODO - ADDED
    void wasm_otoroshi_free_memory(Pointer memory);
    // TODO - ADDED
    void wasm_otoroshi_deallocate_results(LibExtism.ExtismVal.ByReference results, int length);
    // TODO - ADDED
    Pointer extism_plugin_new_with_memories(byte[] wasm,
                                            long wasmSize,
                                            Pointer[] functions,
                                            int nFunctions,
                                            Pointer[] memories,
                                            int nMemories,
                                            boolean withWASI,
                                            Pointer[] errmsg);
    // TODO - ADDED
    LibExtism.ExtismVal.ByReference wasm_otoroshi_call(Pointer pluginPointer, String functionName, LibExtism.ExtismVal.ByReference inputs, int length);
    // TODO - ADDED
    Pointer wasm_otoroshi_wasm_plugin_call_without_params(Pointer pluginPointer, String functionName);
    // TODO - ADDED
    void wasm_otoroshi_wasm_plugin_call_without_results(Pointer pluginPointer,
                                                        String functionName,
                                                        LibExtism.ExtismVal.ByReference inputs,
                                                        int nInputs);
    // TODO - ADDED
    int wasm_otoroshi_extism_memory_write_bytes(Pointer pluginPointer, byte[] data, int n, int offset, String namespace, String name);
    // TODO - ADDED
    Pointer wasm_otoroshi_extism_get_memory(Pointer instance, String memoryName, String namespace);

    Pointer wasm_otoroshi_extism_get_linear_memory(Pointer instance, String memoryName, String namespace);

    Pointer wasm_otoroshi_extism_get_linear_memory(Pointer instance);

    // TODO - ADDED
//    void extism_reset(Pointer pluginPointer);
    // TODO - ADDED
    int wasm_otoroshi_extism_memory_bytes(Pointer pluginPointer);

    // TODO - ADDED
    int wasm_otoroshi_extism_current_plugin_memory_length(Pointer plugin, long n);

    Pointer wasm_otoroshi_extism_current_plugin_memory(Pointer plugin);

    int wasm_otoroshi_extism_current_plugin_memory_alloc(Pointer plugin, long n);

    void wasm_otoroshi_extism_current_plugin_memory_free(Pointer plugin, long ptr);
}
