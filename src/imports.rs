use crate::{
    exception::{joption_or_throw},
    types::{jptr, Pointer},
    module::Module,
};
use jni::{
    sys::{jlong},
    objects::{JClass, JObject, ReleaseMode, AutoArray, TypeArray},
    JNIEnv,
};
use std::{collections::HashMap, panic};
use std::convert::TryFrom;
use std::sync::Arc;
use wasmer::{ImportObject, NamedResolver, ChainableNamedResolver, Exports, Function, FunctionType, Type, Value, Memory, MemoryType};
use wasmer_wasi::WasiState;
use crate::memory::{IMPORTED_MEMORY, Memory as MemoryWrapper};

pub struct Imports {
    pub(crate) import_object: Box<dyn NamedResolver>,
}

fn array2vec<'a, T: TypeArray>(array: &'a AutoArray<T>) -> Vec<&'a T> {
    let len = array.size().unwrap();
    let mut ret = Vec::with_capacity(len as usize);
    for i in 0..len {
        ret.push(unsafe { &*(array.as_ptr().offset(i as isize) as *const T) });
    }
    ret
}


#[no_mangle]
pub extern "system" fn Java_org_wasmer_Imports_nativeImportsInstantiate(
    env: JNIEnv,
    _class: JClass,
    imports: JObject,
    module: jptr,
) -> jptr {
    let output = panic::catch_unwind(|| {
        let mut namespaces = HashMap::<String, _>::new();
        let mut import_object = ImportObject::new();
        let module: &Module = Into::<Pointer<Module>>::into(module).borrow();
        let store = module.module.store();
        let imports = env.get_list(imports)?;

        for import in imports.iter()? {
            let namespace = env.get_field(import, "namespace", "Ljava/lang/String;")?.l()?;
            let namespace = env.get_string(namespace.into())?.to_str()?.to_string();
            let name = env.get_field(import, "name", "Ljava/lang/String;")?.l()?;
            let name = env.get_string(name.into())?.to_str()?.to_string();

            if name == "memory" {
                let min_pages = env.get_field(import, "minPages", "I")?.i()?;
                let max_pages = env.get_field(import, "maxPages", "Ljava/lang/Integer;")?.l()?;
                let max_pages = if max_pages.is_null() {
                    None
                } else {
                    //have to get the field again if not null as it cannot be cast to int
                    let max_pages = env.get_field(import, "maxPages", "I")?.i()?;
                    Some(u32::try_from(max_pages)?)
                };
                let shared = env.get_field(import, "shared", "Z")?.z()?;
                let memory_type = MemoryType::new(u32::try_from(min_pages)?, max_pages, shared);
                let memory = Memory::new(&store, memory_type)?;
                IMPORTED_MEMORY.lock().unwrap().replace(MemoryWrapper::new(Arc::new(memory.clone())));
                namespaces.entry(namespace).or_insert_with(|| Exports::new()).insert(name, memory)
            } else {
                let function = env.get_field(import, "function", "Ljava/util/function/Function;")?.l()?;
                let params = env.get_field(import, "argTypesInt", "[I")?.l()?;
                let returns = env.get_field(import, "retTypesInt", "[I")?.l()?;
                let params = env.get_int_array_elements(*params, ReleaseMode::NoCopyBack)?;
                let returns = env.get_int_array_elements(*returns, ReleaseMode::NoCopyBack)?;
                let i2t = |i: &i32| match i { 1 => Type::I32, 2 => Type::I64, 3 => Type::F32, 4 => Type::F64, _ => unreachable!("Unknown {}", i)};
                let params = array2vec(&params).into_iter().map(i2t).collect::<Vec<_>>();
                let returns = array2vec(&returns).into_iter().map(i2t).collect::<Vec<_>>();
                let sig = FunctionType::new(params.clone(), returns.clone());
                let function = env.new_global_ref(function)?;
                let jvm = env.get_java_vm()?;
                namespaces.entry(namespace).or_insert_with(|| Exports::new()).insert(name, Function::new(store, sig, move |argv| {
                    // There are many ways of transferring the args from wasm to java, JList being the cleanest,
                    // but probably also slowest by far (two JNI calls per argument). Benchmark?
                    let env = jvm.get_env().expect("Couldn't get JNIEnv");
                    env.ensure_local_capacity(argv.len() as i32 + 2).ok();
                    let jargv = env.new_long_array(argv.len() as i32).expect("Couldn't create array");
                    let argv = argv.into_iter().enumerate().map(|(i, arg)| match arg {
                        Value::I32(arg) => { assert_eq!(params[i], Type::I32); *arg as i64 },
                        Value::I64(arg) => { assert_eq!(params[i], Type::I64); *arg as i64 },
                        Value::F32(arg) => { assert_eq!(params[i], Type::F32); arg.to_bits() as i64 },
                        Value::F64(arg) => { assert_eq!(params[i], Type::F64); arg.to_bits() as i64 },
                        _ => panic!("Argument of unsupported type {:?}", arg)
                    }).collect::<Vec<jlong>>();
                    env.set_long_array_region(jargv, 0, &argv).expect("Couldn't set array region");
                    let jret = env.call_method(function.as_obj(), "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", &[jargv.into()])
                        .expect("Couldn't call 'apply' function").l().expect("Failed to unwrap object");
                    let ret = match returns.len() {
                        0 => vec![],
                        len => {
                            let mut ret = vec![0; len];
                            env.get_long_array_region(*jret, 0, &mut ret).expect("Couldn't get array region");
                            ret.into_iter().enumerate().map(|(i, ret)| match returns[i] {
                                Type::I32 => Value::I32(ret as i32),
                                Type::I64 => Value::I64(ret as i64),
                                Type::F32 => Value::F32(f32::from_bits(ret as u32)),
                                Type::F64 => Value::F64(f64::from_bits(ret as u64)),
                                t => panic!("Return of unsupported type {:?}", t)
                            }).collect()
                        }
                    };
                    Ok(ret)
                }));
            }
        }

        for (namespace, exports) in namespaces.into_iter() {
            import_object.register(namespace, exports);
        }
        let import_object = Box::new(import_object);

        Ok(Pointer::new(Imports { import_object }).into())
    });

    joption_or_throw(&env, output).unwrap_or(0)
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Imports_nativeImportsWasi(
    env: JNIEnv,
    _class: JClass,
    module: jptr,
) -> jptr {
    let output = panic::catch_unwind(|| {
        let module: &Module = Into::<Pointer<Module>>::into(module).borrow();
        let mut wasi = WasiState::new("").finalize()?;
        let import_object = wasi.import_object(&module.module)?;
        let import_object = Box::new(import_object);

        Ok(Pointer::new(Imports { import_object }).into())
    });

    joption_or_throw(&env, output).unwrap_or(0)
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Imports_nativeImportsChain(
    env: JNIEnv,
    _class: JClass,
    back: jptr,
    front: jptr,
) -> jptr {
    let output = panic::catch_unwind(|| {
        let back: &Imports = Into::<Pointer<Imports>>::into(back).borrow();
        let front: &Imports = Into::<Pointer<Imports>>::into(front).borrow();
        let import_object = Box::new((&back.import_object).chain_front(&front.import_object));

        Ok(Pointer::new(Imports { import_object }).into())
    });

    joption_or_throw(&env, output).unwrap_or(0)
}
