use jni::{
    objects::{JClass, JString},
    JNIEnv,
};

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Native_nativePanic(
    env: JNIEnv,
    _class: JClass,
    message: JString,
) {
    let message: String = env.get_string(message).unwrap().into();
    panic!("{}", message)
}