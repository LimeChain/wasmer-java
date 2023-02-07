#[no_mangle]
pub extern fn string() -> *const u8 {
    b"Hello, World!\0".as_ptr()
}