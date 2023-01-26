use core::ops::Range;

/// Error that can happen in the memory allocator.
#[derive(Debug)]
pub enum Error {
    /// Someone tried to allocate more memory than the allowed maximum per allocation.
    RequestedAllocationTooLarge,
    /// Allocator run out of space.
    AllocatorOutOfSpace,
    /// The client passed a memory instance which is smaller than previously observed.
    MemoryShrinked,
    // TODO: wtf is "Other"?
    Other(&'static str),
}

pub trait Memory {
    /// Read a `u64` from the heap in LE form. Returns an error if any of the bytes read are out of
    /// bounds.
    fn read_le_u64(&self, ptr: u32) -> Result<u64, Error>;
    /// Write a `u64` to the heap in LE form. Returns an error if any of the bytes written are out of
    /// bounds.
    fn write_le_u64(&mut self, ptr: u32, val: u64) -> Result<(), Error>;
    /// Returns the full size of the memory in bytes.
    fn size(&self) -> u32;
}

fn heap_range(offset: u32, length: u32, heap_len: usize) -> Option<Range<usize>> {
    let start = offset as usize;
    let end = offset.checked_add(length)? as usize;
    if end <= heap_len {
        Some(start..end)
    } else {
        None
    }
}

/// Create an allocator error.
fn error(msg: &'static str) -> Error {
    Error::Other(msg)
}

impl Memory for [u8] {
    fn read_le_u64(&self, ptr: u32) -> Result<u64, Error> {
        let range =
            heap_range(ptr, 8, self.len()).ok_or_else(|| error("read out of heap bounds"))?;
        let bytes = self[range]
            .try_into()
            .expect("[u8] slice of length 8 must be convertible to [u8; 8]");
        Ok(u64::from_le_bytes(bytes))
    }
    fn write_le_u64(&mut self, ptr: u32, val: u64) -> Result<(), Error> {
        let range =
            heap_range(ptr, 8, self.len()).ok_or_else(|| error("write out of heap bounds"))?;
        let bytes = val.to_le_bytes();
        self[range].copy_from_slice(&bytes[..]);
        Ok(())
    }
    fn size(&self) -> u32 {
        u32::try_from(self.len()).expect("size of Wasm linear memory is <2^32; qed")
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_works() {
        let result = add(2, 2);
        assert_eq!(result, 4);
    }
}
