package org.extism.sdk;

import org.extism.sdk.manifest.Manifest;
import org.extism.sdk.manifest.MemoryOptions;
import org.extism.sdk.wasmotoroshi.*;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.extism.sdk.TestWasmSources.CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WasmOtoroshiTests {

    @Test
    public void shouldWorks() {
        Manifest manifest = new Manifest(Collections.singletonList(CODE.getRawAdditionPath()));

        LibExtism.ExtismValType[] parametersTypes = new LibExtism.ExtismValType[]{LibExtism.ExtismValType.I64};
        LibExtism.ExtismValType[] resultsTypes = new LibExtism.ExtismValType[]{LibExtism.ExtismValType.I64};

        ExtismFunction<HostUserData> helloWorldFunction = (plugin, params, returns, data) -> {
            System.out.println("Hello from Java Host Function!");
        };

        var f = new HostFunction<>(
                "hello_world",
                parametersTypes,
                resultsTypes,
                helloWorldFunction,
                Optional.empty()
        ).withNamespace("env");

        var functions = new HostFunction[]{f};

        List<Integer> test = new ArrayList<>(500);
        for (int i = 0; i < 500; i++) {
            test.add(i);
        }

        try(var instance = new Plugin(manifest, true, functions)) {
            test.parallelStream().forEach(number -> {
                try(var params = new Parameters(2)
                        .pushInts(2, 3)) {

                    Results result = instance.call("add", params, 1);
                    assertEquals(result.getValue(0).v.i32, 5);
                }
            });
        }
    }

    @Test
    public void shouldExistmCallWorks() throws Exception {
        var manifest = new Manifest(Collections.singletonList(CODE.pathWasmWebAssemblyFunctionSource()));

        LibExtism.ExtismValType[] parametersTypes = new LibExtism.ExtismValType[]{LibExtism.ExtismValType.I64};
        LibExtism.ExtismValType[] resultsTypes = new LibExtism.ExtismValType[]{LibExtism.ExtismValType.I64};

        var functions = new HostFunction[]{
                new HostFunction<>(
                        "hello_world",
                        parametersTypes,
                        resultsTypes,
                        (plugin, params, returns, data) -> {
                            System.out.println("Hello from Java Host Function !!");
                }, Optional.empty()
                ).withNamespace("env")
        };

        try (var instance = new Plugin(manifest, true, functions)) {
            instance.call("execute", "".getBytes(StandardCharsets.UTF_8));
        }
    }


    @Test
    public void shouldInvokeNativeFunction() {
        Manifest manifest = new Manifest(Arrays.asList(CODE.getRawAdditionPath()));
        String functionName = "add";

        Parameters params = new Parameters(2)
                .pushInts(2, 3);

        try (var instance = new Plugin(manifest, true, null, null)) {
            Results result = instance.call(functionName, params, 1);
            assertEquals(result.getValues()[0].v.i32, 5);

        }
    }

    void display(ExtismCurrentPlugin plugin) {
        var p = plugin.getLinearMemory("memory");

        System.out.println(p);
    }

    @Test
    public void shouldTestWorks() {
        var manifest = new Manifest(Collections.singletonList(CODE.pathWasmWebAssemblyFunctionSource()));

        var parametersTypes = new LibExtism.ExtismValType[]{LibExtism.ExtismValType.I64};
        var resultsTypes = new LibExtism.ExtismValType[]{LibExtism.ExtismValType.I64};

        var functions = new HostFunction[]{
                new HostFunction<>(
                        "hello_world",
                        parametersTypes,
                        resultsTypes,
                        (plugin, params, returns, data) -> {
                            display(plugin);
                            System.out.println("Hello from Java Host Function!");
                        },
                        Optional.empty()
                ).withNamespace("env")
        };

        try(var instance = new Plugin(manifest, true, functions, new LinearMemory[0])) {
            instance.call("execute", "".getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void shouldGetMemoryBounds() {
        Manifest manifest = new Manifest(Collections.singletonList(CODE.pathWasmWebAssemblyFunctionSource()), new MemoryOptions(4));

        LibExtism.ExtismValType[] parametersTypes = new LibExtism.ExtismValType[]{LibExtism.ExtismValType.I64};
        LibExtism.ExtismValType[] resultsTypes = new LibExtism.ExtismValType[]{LibExtism.ExtismValType.I64};

        HostFunction[] functions = {
                new HostFunction<>(
                        "hello_world",
                        parametersTypes,
                        resultsTypes,
                        (plugin, params, returns, data) -> {
                            System.out.println("Hello from Java Host Function!");
                        },
                        Optional.empty()
                ).withNamespace("env")
        };

        var instance = new Plugin(manifest, true, functions, new LinearMemory[0]);
        instance.call("execute", "".getBytes(StandardCharsets.UTF_8));
        instance.free();
    }

    @Test
    public void shouldCreateLinearMemory() {
        var manifest = new Manifest(Collections.singletonList(CODE.pathWasmWebAssemblyFunctionSource()), new MemoryOptions(4));
        LibExtism.ExtismValType[] parametersTypes = new LibExtism.ExtismValType[]{LibExtism.ExtismValType.I64};
        LibExtism.ExtismValType[] resultsTypes = new LibExtism.ExtismValType[]{LibExtism.ExtismValType.I64};

        var functions = new HostFunction[]{
                new HostFunction<>(
                        "hello_world",
                        parametersTypes,
                        resultsTypes,
                        (plugin, params, returns, data) -> {
                            System.out.println("Hello from Java Host Function!");
                        },
                        Optional.empty()
                ).withNamespace("env")
        };

        var memory = new LinearMemory("huge-memory", new LinearMemoryOptions(0, Optional.of(2)));

        var instance = new Plugin(manifest, true, functions, new LinearMemory[]{memory});
        instance.call("execute", "".getBytes(StandardCharsets.UTF_8));
        instance.free();
    }

    @Test
    public void shouldPluginWithNewVersionRun() {
        var manifest = new Manifest(Collections.singletonList(CODE.getMajorRelease()), new MemoryOptions(50));

        var instance = new Plugin(manifest, true, null);
        instance.call("execute", "{}".getBytes(StandardCharsets.UTF_8));
        instance.free();
    }

    @Test
    public void shouldOPAWorks() {
        var opa = new OPA(CODE.getOPA());

        var values = opa.initialize();
//        var result = opa.evaluate(
//                (int)values.toArray()[0],
//                (int)values.toArray()[1],
//                "{ \"request\" -> { \"headers\" -> { \"foo\": \"bar\" } } }");
//
//        System.out.println(result);
    }
}
