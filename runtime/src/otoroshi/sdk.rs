#![allow(clippy::missing_safety_doc)]

use std::os::raw::c_char;

use crate::{
    function,
    otoroshi::*,
    sdk::{ExtismFunction, ExtismVal, Size},
    CurrentPlugin, Internal, Plugin, EXTISM_ENV_MODULE,
};

use super::wasm_memory::ExtismMemory;

#[no_mangle]
pub(crate) unsafe fn wasm_otoroshi_plugin_call_native(
    plugin: *mut Plugin,
    func_name: *const c_char,
    params: Option<Vec<Val>>,
) -> Option<Vec<Val>> {
    let plugin = &mut *plugin;
    let _lock = plugin.instance.clone();
    let mut lock = _lock.lock().unwrap();

    let name = std::ffi::CStr::from_ptr(func_name);
    let name = match name.to_str() {
        Ok(name) => name,
        Err(e) => return plugin.return_error(&mut lock, e, None),
    };

    let mut results = vec![wasmtime::Val::null(); 0];

    let res = plugin.raw_call(&mut lock, name, [0; 0], false, params, Some(&mut results));

    match res {
        Err((e, _rc)) => {
            // panic!("{:#?} {} {:#?}", e, _rc, results);
            plugin.return_error(&mut lock, e, None)
            // Some(results)
        }
        Ok(_x) => Some(results),
    }
}

#[no_mangle]
pub(crate) unsafe extern "C" fn wasm_otoroshi_deallocate_results(ptr: *mut ExtismVal, len: usize) {
    let len = len as usize;
    drop(Vec::from_raw_parts(ptr, len, len));
}

#[no_mangle]
pub(crate) unsafe extern "C" fn wasm_otoroshi_call(
    plugin: *mut Plugin,
    func_name: *const c_char,
    params: *const ExtismVal,
    n_params: Size,
) -> *mut ExtismVal {
    let prs = std::slice::from_raw_parts(params, n_params as usize).to_vec();

    let p: Vec<Val> = prs
        .iter()
        .map(|x| {
            let t = match x.t {
                function::ValType::I32 => Val::I32(x.v.i32),
                function::ValType::I64 => Val::I64(x.v.i64),
                function::ValType::F32 => Val::F32(x.v.f32 as u32),
                function::ValType::F64 => Val::F64(x.v.f64 as u64),
                _ => todo!(),
            };
            t
        })
        .collect();

    let params = if n_params == 0 || params.is_null() {
        None
    } else {
        Some(p)
    };

    match wasm_otoroshi_plugin_call_native(plugin, func_name, params) {
        // None => panic!("SOMEHTING BAD HAPPENED HERE 74. sdk.rs"), //std::ptr::null_mut(),
        None => std::ptr::null_mut(),
        Some(t) => {
            // std::ptr::null_mut()
            let mut v = t
                .iter()
                .map(|x| {
                    let t = ExtismVal::from(x);
                    t
                })
                .collect::<Vec<ExtismVal>>();

            let ptr = v.as_mut_ptr() as *mut _;
            std::mem::forget(v);
            ptr
        }
    }
}

#[no_mangle]
pub(crate) unsafe extern "C" fn wasm_plugin_call_without_params(
    plugin_ptr: *mut Plugin,
    func_name: *const c_char,
) -> *mut ExtismVal {
    match wasm_otoroshi_plugin_call_native(plugin_ptr, func_name, None) {
        None => std::ptr::null_mut(),
        Some(t) => {
            // std::ptr::null_mut()
            let mut v = t
                .iter()
                .map(|x| {
                    let t = ExtismVal::from(x);
                    t
                })
                .collect::<Vec<ExtismVal>>();

            let ptr = v.as_mut_ptr() as *mut _;
            std::mem::forget(v);
            ptr
        }
    }
}

#[no_mangle]
pub(crate) unsafe extern "C" fn wasm_plugin_call_without_results(
    plugin_ptr: *mut Plugin,
    func_name: *const c_char,
    params: *const ExtismVal,
    n_params: Size,
) {
    let prs = std::slice::from_raw_parts(params, n_params as usize);

    let p: Vec<Val> = prs
        .iter()
        .map(|x| {
            let t = match x.t {
                function::ValType::I32 => Val::I32(x.v.i32),
                function::ValType::I64 => Val::I64(x.v.i64),
                function::ValType::F32 => Val::F32(x.v.f32 as u32),
                function::ValType::F64 => Val::F64(x.v.f64 as u64),
                _ => Val::I32(-1),
                // _ => todo!(),
            };
            t
        })
        .collect();

    let params = if params.is_null() || n_params == 0 {
        None
    } else {
        Some(p)
    };

    wasm_otoroshi_plugin_call_native(plugin_ptr, func_name, params);
}

#[no_mangle]
pub(crate) unsafe extern "C" fn wasm_otoroshi_create_wasmtime_memory(
    name: *const std::ffi::c_char,
    namespace: *const std::ffi::c_char,
    min_pages: u32,
    max_pages: u32,
) -> *mut ExtismMemory {
    let name = match std::ffi::CStr::from_ptr(name).to_str() {
        Ok(x) => x.to_string(),
        Err(_) => {
            return std::ptr::null_mut();
        }
    };

    let namespace = match std::ffi::CStr::from_ptr(namespace).to_str() {
        Ok(x) => x.to_string(),
        Err(_) => {
            return std::ptr::null_mut();
        }
    };

    let mem = WasmMemory::new(name, namespace, min_pages, max_pages);

    Box::into_raw(Box::new(ExtismMemory(mem)))
}

// #[no_mangle]
// pub extern "C" fn wasm_otoroshi_free_memory(mem: *mut ExtismMemory) {
//     unsafe {
//         drop(Box::from_raw(mem));
//     }
// }

/// Remove all plugins from the registry
#[no_mangle]
pub unsafe extern "C" fn custom_memory_reset(plugin: *mut Plugin) {
    if plugin.is_null() {
        return;
    }

    let plugin = &mut *plugin;
    let plugin_memory = &mut *plugin.current_plugin().memory_export;
    plugin_memory.reset();
}

#[no_mangle]
pub unsafe extern "C" fn wasm_otoroshi_extism_memory_write_bytes(
    instance_ptr: *mut Plugin,
    data: *const u8,
    data_size: Size,
    offset: u32,
    namespace: *const c_char,
    name: *const c_char,
) -> i8 {
    let plugin = &mut *instance_ptr;

    let (linker, store) = plugin.linker_and_store();

    let data = std::slice::from_raw_parts(data, data_size as usize);

    let ns = if namespace.is_null() {
        ""
    } else {
        match std::ffi::CStr::from_ptr(namespace).to_str() {
            Ok(name) => name,
            Err(_e) => EXTISM_ENV_MODULE,
        }
    };

    let name = match std::ffi::CStr::from_ptr(name).to_str() {
        Ok(name) => name,
        Err(_e) => "memory",
    };

    match (&mut *linker).get(&mut *store, ns, name) {
        None => -1,
        Some(external) => match external.into_memory() {
            None => -1,
            Some(memory) => match memory.write(&mut *store, offset as usize, data) {
                Ok(()) => 0,
                Err(_) => -1,
            },
        },
    }
}

#[no_mangle]
pub unsafe extern "C" fn linear_memory_get(
    plugin: *mut Plugin,
    name: *const std::ffi::c_char,
    namespace: *const std::ffi::c_char,
) -> *mut u8 {
    let plugin = &mut *plugin;

    let name = match std::ffi::CStr::from_ptr(name).to_str() {
        Ok(x) => x.to_string(),
        Err(_e) => return std::ptr::null_mut(),
    };

    let namespace = match std::ffi::CStr::from_ptr(namespace).to_str() {
        Ok(x) => x.to_string(),
        Err(_e) => EXTISM_ENV_MODULE.to_string(),
    };

    let (linker, store) = plugin.linker_and_store();

    match (&mut *linker).get(&mut *store, &namespace, &name) {
        None => std::ptr::null_mut(),
        Some(external) => match external.into_memory() {
            None => std::ptr::null_mut(),
            Some(memory) => memory.data_mut(&mut *store).as_mut_ptr(),
        },
    }
}

#[no_mangle]
pub unsafe extern "C" fn linear_memory_size(
    instance_ptr: *mut Plugin,
    namespace: *const c_char,
    name: *const c_char
) -> usize {
    let plugin = &mut *instance_ptr;

    let (linker, store) = plugin.linker_and_store();

    let name = match std::ffi::CStr::from_ptr(name).to_str() {
        Ok(name) => name,
        Err(_e) => "",
    };

    let namespace = match std::ffi::CStr::from_ptr(namespace).to_str() {
        Ok(namespace) => namespace,
        Err(_e) => "",
    };

    match (&mut *linker).get(&mut *store, &namespace, &name) {
        None => 0 as usize,
        Some(external) => match external.into_memory() {
            None => 0 as usize,
            Some(memory) => memory
                .data(&mut *store)
                .iter()
                .position(|x| x.to_owned() == 0)
                .unwrap(),
        },
    }
}

#[no_mangle]
pub unsafe extern "C" fn linear_memory_reset(
    instance_ptr: *mut Plugin,
    namespace: *const c_char,
    name: *const c_char,
) -> usize {
    let plugin = &mut *instance_ptr;

    let (linker, store) = plugin.linker_and_store();

    let name = match std::ffi::CStr::from_ptr(name).to_str() {
        Ok(name) => name,
        Err(_e) => "",
    };

    let namespace = match std::ffi::CStr::from_ptr(namespace).to_str() {
        Ok(namespace) => namespace,
        Err(_e) => "",
    };

    match (&mut *linker).get(&mut *store, namespace, &name) {
        None => 0 as usize,
        Some(external) => match external.into_memory() {
            None => 0 as usize,
            Some(memory) => memory
                .data(&mut *store)
                .iter()
                .position(|x| x.to_owned() == 0)
                .unwrap(),
        },
    }
}

#[no_mangle]
pub unsafe extern "C" fn custom_memory_get(plugin: *mut CurrentPlugin) -> *mut u8 {
    let plugin = &mut *plugin;

    let plugin = &mut *plugin;
    let plugin_memory = &mut *plugin.memory_export;

    plugin_memory
        .memory
        .data_mut(&mut *plugin_memory.store)
        .as_mut_ptr()
}

#[no_mangle]
pub unsafe extern "C" fn custom_memory_size(plugin: *mut CurrentPlugin) -> usize {
    let plugin = &mut *plugin;
    let plugin_memory = &mut *plugin.memory_export;

    plugin_memory
        .memory
        .data(&mut *plugin_memory.store)
        .iter()
        .position(|x| x.to_owned() == 0)
        .unwrap()
}

#[no_mangle]
pub unsafe extern "C" fn custom_memory_length(plugin: *mut CurrentPlugin, n: Size) -> Size {
    if plugin.is_null() {
        return 0;
    }

    let plugin = &mut *plugin;
    let plugin_memory = &mut *plugin.memory_export;

    match plugin_memory.block_length(n as usize) {
        Some(x) => x as Size,
        None => 0,
    }
}

#[no_mangle]
pub unsafe extern "C" fn custom_memory_free(plugin: *mut CurrentPlugin, ptr: u64) {
    if plugin.is_null() {
        return;
    }

    let plugin = &mut *plugin;
    let plugin_memory = &mut *plugin.memory_export;
    plugin_memory.free(ptr as usize);
}

#[no_mangle]
pub unsafe extern "C" fn custom_memory_alloc(plugin: *mut CurrentPlugin, n: Size) -> u64 {
    if plugin.is_null() {
        return 0;
    }

    let plugin = &mut *plugin;
    let plugin_memory = &mut *plugin.memory_export;
    let mem = match plugin_memory.alloc(n as usize) {
        Ok(x) => x,
        Err(_e) => return 0,
    };

    mem.offset as u64
}

#[no_mangle]
pub(crate) unsafe extern "C" fn extism_plugin_new_with_memories(
    wasm: *const u8,
    wasm_size: Size,
    functions: *mut *const ExtismFunction,
    n_functions: Size,
    memories: *mut *const ExtismMemory,
    n_memories: i8,
    with_wasi: bool,
    errmsg: *mut *mut std::ffi::c_char,
) -> *mut Plugin {
    let mut mems: Vec<&WasmMemory> = vec![];

    if !memories.is_null() {
        for i in 0..n_memories {
            unsafe {
                let f = *memories.add(i as usize);
                if f.is_null() {
                    continue;
                }
                let f = &*f;
                mems.push(&f.0);
            }
        }
    }

    // extism_plugin_new(wasm, wasm_size, functions, n_functions, mems, with_wasi, errmsg)
    let data = std::slice::from_raw_parts(wasm, wasm_size as usize);
    let mut funcs = vec![];

    if !functions.is_null() {
        for i in 0..n_functions {
            unsafe {
                let f = *functions.add(i as usize);
                if f.is_null() {
                    continue;
                }
                if let Some(f) = (*f).0.take() {
                    funcs.push(f);
                } else {
                    let e = std::ffi::CString::new(
                        "Function cannot be registered with multiple different Plugins",
                    )
                    .unwrap();
                    *errmsg = e.into_raw();
                }
            }
        }
    }

    let plugin = Plugin::new_with_memories(data, funcs, mems, with_wasi);
    match plugin {
        Err(e) => {
            if !errmsg.is_null() {
                let e = std::ffi::CString::new(format!("Unable to create Extism plugin: {}", e))
                    .unwrap();
                *errmsg = e.into_raw();
            }
            std::ptr::null_mut()
        }
        Ok(p) => Box::into_raw(Box::new(p)),
    }
}
