#![allow(clippy::missing_safety_doc)]

use std::os::raw::c_char;

use crate::{otoroshi::*, Plugin, sdk::{ExtismVal, Size, ExtismFunction, extism_plugin_new}, function};

use super::types::ExtismMemory;

#[no_mangle]
pub(crate) unsafe fn wasm_otoroshi_plugin_call_native(
    plugin: *mut Plugin,
    func_name: *const c_char,
    params: Vec<Val>,
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

    let res = plugin.raw_call(&mut lock, name, 
        [0; 0], 
        false, 
        Some(params),
        Some(&mut results)
    );

    match res {
        Err((e, rc)) => plugin.return_error(&mut lock, e, None),
        Ok(x) => Some(results),
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
    

    match wasm_otoroshi_plugin_call_native(plugin, func_name, p) {
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

// #[no_mangle]
// pub(crate) unsafe extern "C" fn wasm_otoroshi_wasm_plugin_call_without_params(
//     plugin_ptr: *mut WasmPlugin,
//     func_name: *const c_char,
// ) -> *mut ExtismVal {
//     match wasm_otoroshi_plugin_call_native(plugin_ptr, func_name, Vec::new()) {
//         None => std::ptr::null_mut(),
//         Some(t) => {
//             // std::ptr::null_mut()
//             let mut v = t
//                 .iter()
//                 .map(|x| {
//                     let t = ExtismVal::from(x);
//                     t
//                 })
//                 .collect::<Vec<ExtismVal>>();

//             let ptr = v.as_mut_ptr() as *mut _;
//             std::mem::forget(v);
//             ptr
//         }
//     }
// }

// #[no_mangle]
// pub(crate) unsafe extern "C" fn wasm_otoroshi_wasm_plugin_call_without_results(
//     plugin_ptr: *mut WasmPlugin,
//     func_name: *const c_char,
//     params: *const ExtismVal,
//     n_params: Size,
// ) {
//     let prs = std::slice::from_raw_parts(params, n_params as usize);

//     let p: Vec<Val> = prs
//         .iter()
//         .map(|x| {
//             let t = match x.t {
//                 ValType::I32 => Val::I32(x.v.i32),
//                 ValType::I64 => Val::I64(x.v.i64),
//                 ValType::F32 => Val::F32(x.v.f32 as u32),
//                 ValType::F64 => Val::F64(x.v.f64 as u64),
//                 _ => todo!(),
//             };
//             t
//         })
//         .collect();

//     wasm_otoroshi_plugin_call_native(plugin_ptr, func_name, p);
// }

// #[no_mangle]
// pub(crate) unsafe extern "C" fn wasm_otoroshi_create_wasmtime_memory(
//     name: *const std::ffi::c_char,
//     namespace: *const std::ffi::c_char,
//     min_pages: u32,
//     max_pages: u32,
// ) -> *mut ExtismMemory {
//     let name = match std::ffi::CStr::from_ptr(name).to_str() {
//         Ok(x) => x.to_string(),
//         Err(_) => {
//             return std::ptr::null_mut();
//         }
//     };

//     let namespace = match std::ffi::CStr::from_ptr(namespace).to_str() {
//         Ok(x) => x.to_string(),
//         Err(_) => {
//             return std::ptr::null_mut();
//         }
//     };

//     let mem = WasmMemory::new(name, namespace, min_pages, max_pages);

//     Box::into_raw(Box::new(ExtismMemory(mem)))
// }

// #[no_mangle]
// pub extern "C" fn wasm_otoroshi_free_memory(mem: *mut ExtismMemory) {
//     unsafe {
//         drop(Box::from_raw(mem));
//     }
// }

/// Remove all plugins from the registry
#[no_mangle]
pub unsafe extern "C" fn wasm_otoroshi_extism_reset(
    plugin: *mut Plugin
) {
    let plugin = &mut *plugin;
    let lock = plugin.instance.clone();
    let mut lock = lock.lock().unwrap();

    let _ = plugin.reset_store(&mut lock);
}

// #[no_mangle]
// pub unsafe extern "C" fn wasm_otoroshi_extism_memory_write_bytes(
//     instance_ptr: *mut WasmPlugin,
//     data: *const u8,
//     data_size: Size,
//     offset: u32,
// ) -> i8 {
//     let plugin = &mut *instance_ptr;

//     let memory = match plugin.get_memory("memory") {
//         Some(x) => x,
//         None => return plugin.error(format!("Memory not found: memory"), -1),
//     };

//     let data = std::slice::from_raw_parts(data, data_size as usize);
//     match memory.write(&mut plugin.memory.store, offset as usize, data) {
//         Ok(()) => 0,
//         Err(_) => -1,
//     }
// }

#[no_mangle]
pub unsafe extern "C" fn wasm_otoroshi_extism_get_memory(
    plugin: *mut Plugin,
    name: *const std::ffi::c_char,
) -> *mut u8 {
    let plugin = &mut *plugin;
    let lock = plugin.instance.clone();
    let mut lock = lock.lock().unwrap();

    let name = match std::ffi::CStr::from_ptr(name).to_str() {
        Ok(x) => x.to_string(),
        Err(e) => return plugin.return_error(&mut lock, e, std::ptr::null_mut()),
    };
    
    plugin.get_memory(&mut lock, name)
}

// #[no_mangle]
// pub unsafe extern "C" fn wasm_otoroshi_extism_memory_bytes(
//     instance_ptr: *mut WasmPlugin
// ) -> usize {
//     let plugin = &mut *instance_ptr;

//     match plugin.instance.get_memory(&mut plugin.memory.store, "memory") {
//         Some(mem) => mem.data(&plugin.memory.store).len(),
//         None => 0 as usize
//     }
// }



#[no_mangle]
pub(crate) unsafe extern "C" fn extism_plugin_new_with_memories(
    wasm: *const u8,
    wasm_size: Size,
    functions: *mut *const ExtismFunction,
    n_functions: Size,
    memories: *mut *const ExtismMemory,
    n_memories: i8,
    with_wasi: bool,
    errmsg: *mut *mut std::ffi::c_char
) -> *mut Plugin {

    let plugin = extism_plugin_new(wasm, wasm_size, functions, n_functions, with_wasi, errmsg);

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

    for m in mems {
        let name = m.name.to_string();
        let ns = m.namespace.to_string();


        let mem = wasmtime::Memory::new(
            &mut (*plugin).store, 
            m.ty.clone()).unwrap(); // TODO - dont do that
        (*plugin).linker.define(&mut (*plugin).store, &ns, &name, mem).unwrap();
    }

    plugin
 
    // match template.instantiate(engine, funcs, mems, with_wasi) {
    //     Err(err) => panic!("{}", err), //std::ptr::null_mut(),
    //     Ok(instance) => {
    //         let inst = Box::into_raw(Box::new(instance));
    //         let plugin = unsafe { &mut *inst };
    //         plugin.borrow_mut().set_wasm_plugin();
    //         inst
    //     }
    // }
}