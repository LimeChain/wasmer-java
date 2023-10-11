pub extern "system" fn Java_org_wasmer_Native_nativePanic(
    env: JNIEnv,
    _class: JClass,
    message: JString,
) {
    panic!(message)
}