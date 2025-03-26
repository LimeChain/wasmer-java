use std::num::TryFromIntError;
use std::str::Utf8Error;
use jni::JNIEnv;
use std::thread;
use jni::errors::Error as JNIError;
use wasmer::MemoryError;
use wasmer_wasi::{WasiError, WasiStateCreationError};

#[derive(Debug)]
pub enum Error {
    JNIError(JNIError),
    Message(String),
    WasiError(WasiError),
    WasiStateCreationError(WasiStateCreationError),
    MemoryError(MemoryError),
    TryFromIntError(TryFromIntError),
    Utf8Error(Utf8Error)
}

impl From<JNIError> for Error {
    fn from(err: JNIError) -> Self { Self::JNIError(err) }
}

impl From<WasiError> for Error {
    fn from(err: WasiError) -> Self { Self::WasiError(err) }
}

impl From<WasiStateCreationError> for Error {
    fn from(err: WasiStateCreationError) -> Self { Self::WasiStateCreationError(err) }
}

impl From<MemoryError> for Error {
    fn from(err: MemoryError) -> Self { Self::MemoryError(err) }
}

impl From<TryFromIntError> for Error {
    fn from(err: TryFromIntError) -> Self { Self::TryFromIntError(err) }
}

impl From<Utf8Error> for Error {
    fn from(err: Utf8Error) -> Self { Self::Utf8Error(err) }
}

impl std::fmt::Display for Error {
    fn fmt(&self, fmt: &mut std::fmt::Formatter<'_>) -> Result<(), std::fmt::Error> {
        match self {
            Error::JNIError(err) => err.fmt(fmt),
            Error::Message(msg) => msg.fmt(fmt),
            Error::WasiError(err) => err.fmt(fmt),
            Error::WasiStateCreationError(err) => err.fmt(fmt),
            Error::MemoryError(err) => err.fmt(fmt),
            Error::TryFromIntError(err) => err.fmt(fmt),
            Error::Utf8Error(err) => err.fmt(fmt),
        }
    }
}

pub fn runtime_error(message: String) -> Error {
    Error::Message(message)
}

#[derive(Debug)]
pub enum JOption<T> {
    Some(T),
    None,
}

impl<T> JOption<T> {
    pub fn unwrap_or(self, default: T) -> T {
        match self {
            JOption::Some(result) => result,
            JOption::None => default,
        }
    }
}

pub fn joption_or_throw<T>(env: &JNIEnv, result: thread::Result<Result<T, Error>>) -> JOption<T> {
    match result {
        Ok(result) => match result {
            Ok(result) => JOption::Some(result),
            Err(error) => {
                if !env.exception_check().unwrap() {
                    env.throw_new("java/lang/RuntimeException", &error.to_string())
                        .expect("Cannot throw a `java/lang/RuntimeException` exception.");
                }

                JOption::None
            }
        },
        Err(ref error) => {
            env.throw_new("java/lang/RuntimeException", format!("{:?}", error))
                .expect("Cannot throw a `java/lang/RuntimeException` exception.");

            JOption::None
        }
    }
}
