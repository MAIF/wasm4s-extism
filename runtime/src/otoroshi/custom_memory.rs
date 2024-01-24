use std::collections::BTreeMap;

use crate::{otoroshi::*, CurrentPlugin};

/// Handles memory for plugins
pub struct PluginMemory {
    /// wasmtime Store
    pub store: *mut Store<CurrentPlugin>,

    /// WASM memory
    pub memory: Memory,

    /// Tracks allocated blocks
    pub live_blocks: BTreeMap<usize, usize>,

    /// Tracks free blocks
    pub free: Vec<MemoryBlock>,

    /// Tracks current offset in memory
    pub position: usize,
}

/// `ToMemoryBlock` is used to convert from Rust values to blocks of WASM memory
pub trait ToMemoryBlock {
    fn to_memory_block(&self, mem: &PluginMemory) -> Result<MemoryBlock, Error>;
}

impl ToMemoryBlock for MemoryBlock {
    fn to_memory_block(&self, _mem: &PluginMemory) -> Result<MemoryBlock, Error> {
        Ok(*self)
    }
}

impl ToMemoryBlock for (usize, usize) {
    fn to_memory_block(&self, _mem: &PluginMemory) -> Result<MemoryBlock, Error> {
        Ok(MemoryBlock {
            offset: self.0,
            length: self.1,
        })
    }
}

impl ToMemoryBlock for usize {
    fn to_memory_block(&self, mem: &PluginMemory) -> Result<MemoryBlock, Error> {
        match mem.at_offset(*self) {
            Some(x) => Ok(x),
            None => Err(Error::msg(format!("Invalid memory offset: {}", self))),
        }
    }
}

const PAGE_SIZE: u32 = 65536;

// BLOCK_SIZE_THRESHOLD exists to ensure that free blocks are never split up any
// smaller than this value
const BLOCK_SIZE_THRESHOLD: usize = 32;

impl PluginMemory {
    /// Create memory for a plugin
    pub fn new(store: *mut Store<CurrentPlugin>, memory: Memory) -> Self {
        PluginMemory {
            free: Vec::new(),
            live_blocks: BTreeMap::new(),
            store,
            memory,
            position: 1,
        }
    }

    fn store(&self) -> &Store<CurrentPlugin> {
        unsafe { &*self.store }
    }

    fn as_store_mut(&mut self) -> &mut Store<CurrentPlugin> {
        unsafe { &mut *self.store }
    }

    /// Write byte to memory
    pub(crate) fn store_u8(&mut self, offs: usize, data: u8) -> Result<(), MemoryAccessError> {
        if offs >= self.size() {
            // This should raise MemoryAccessError
            let buf = &mut [0];
            self.memory.read(&self.store(), offs, buf)?;
            return Ok(());
        }
        unsafe {
            self.memory.data_mut(&mut *self.store)[offs] = data;
        }
        Ok(())
    }

    /// Read byte from memory
    pub(crate) fn load_u8(&self, offs: usize) -> Result<u8, MemoryAccessError> {
        if offs >= self.size() {
            // This should raise MemoryAccessError
            let buf = &mut [0];
            self.memory.read(&self.store(), offs, buf)?;
            return Ok(0);
        }
        Ok(self.memory.data(&self.store())[offs])
    }

    /// Write u64 to memory
    pub(crate) fn store_u64(&mut self, offs: usize, data: u64) -> Result<(), Error> {
        let handle = MemoryBlock {
            offset: offs,
            length: 8,
        };
        self.write(handle, data.to_ne_bytes())?;
        Ok(())
    }

    /// Read u64 from memory
    pub(crate) fn load_u64(&self, offs: usize) -> Result<u64, Error> {
        let mut buf = [0; 8];
        let handle = MemoryBlock {
            offset: offs,
            length: 8,
        };
        self.read(handle, &mut buf)?;
        Ok(u64::from_ne_bytes(buf))
    }

    /// Write slice to memory
    pub fn write(&mut self, pos: impl ToMemoryBlock, data: impl AsRef<[u8]>) -> Result<(), Error> {
        let pos = pos.to_memory_block(self)?;
        assert!(data.as_ref().len() <= pos.length);
        unsafe {
            self.memory
                .write(&mut *self.store, pos.offset, data.as_ref())?;
        }
        Ok(())
    }

    /// Read slice from memory
    pub fn read(&self, pos: impl ToMemoryBlock, mut data: impl AsMut<[u8]>) -> Result<(), Error> {
        let pos = pos.to_memory_block(self)?;
        assert!(data.as_mut().len() <= pos.length);
        self.memory.read(&self.store(), pos.offset, data.as_mut())?;
        Ok(())
    }

    /// Size of memory in bytes
    pub fn size(&self) -> usize {
        self.memory.data_size(&self.store())
    }

    /// Reserve `n` bytes of memory
    pub fn alloc(&mut self, n: usize) -> Result<MemoryBlock, Error> {
        for (i, block) in self.free.iter_mut().enumerate() {
            if block.length == n {
                let block = self.free.swap_remove(i);
                self.live_blocks.insert(block.offset, block.length);

                return Ok(block);
            } else if block.length.saturating_sub(n) >= BLOCK_SIZE_THRESHOLD {
                let handle = MemoryBlock {
                    offset: block.offset,
                    length: n,
                };

                block.offset += n;
                block.length -= n;
                self.live_blocks.insert(handle.offset, handle.length);
                return Ok(handle);
            }
        }

        let new_offset = self.position.saturating_add(n);

        // If there aren't enough bytes, try to grow the memory size
        if new_offset >= self.size() {
            let bytes_needed = (new_offset as f64 - self.size() as f64) / PAGE_SIZE as f64;
            let mut pages_needed = bytes_needed.ceil() as u64;
            if pages_needed == 0 {
                pages_needed = 1
            }

            // This will fail if we've already allocated the maximum amount of memory allowed
            //let st = self.as_store_mut();
            unsafe {
                self.memory.grow(&mut *self.store, pages_needed)?;
            }
        }

        let mem = MemoryBlock {
            offset: self.position,
            length: n,
        };

        self.live_blocks.insert(mem.offset, mem.length);
        self.position += n;
        Ok(mem)
    }

    /// Allocate and copy `data` into the wasm memory
    pub fn alloc_bytes(&mut self, data: impl AsRef<[u8]>) -> Result<MemoryBlock, Error> {
        let handle = self.alloc(data.as_ref().len())?;
        self.write(handle, data)?;
        Ok(handle)
    }

    /// Free the block allocated at `offset`
    pub fn free(&mut self, offset: usize) {
        if let Some(length) = self.live_blocks.remove(&offset) {
            self.free.push(MemoryBlock { offset, length });
        } else {
            return;
        }

        let free_size: usize = self.free.iter().map(|x| x.length).sum();

        // Perform compaction if there is at least 1kb of free memory available
        if free_size >= 1024 {
            let mut last: Option<MemoryBlock> = None;
            let mut free = Vec::new();
            for block in self.free.iter() {
                match last {
                    None => {
                        free.push(*block);
                    }
                    Some(last) => {
                        if last.offset + last.length == block.offset {
                            free.push(MemoryBlock {
                                offset: last.offset,
                                length: last.length + block.length,
                            });
                        }
                    }
                }
                last = Some(*block);
            }
            self.free = free;
        }
    }

    /// Reset memory - clears free-list and live blocks and resets position
    pub fn reset(&mut self) {
        self.free.clear();
        self.live_blocks.clear();
        self.position = 1;
    }

    /// Get bytes occupied by the provided memory handle
    pub fn get(&self, handle: impl ToMemoryBlock) -> Result<&[u8], Error> {
        let handle = handle.to_memory_block(self)?;
        unsafe { Ok(&self.memory.data(&*self.store)[handle.offset..handle.offset + handle.length]) }
    }

    /// Get str occupied by the provided memory handle
    pub fn get_str(&self, handle: impl ToMemoryBlock) -> Result<&str, Error> {
        let handle = handle.to_memory_block(self)?;
        unsafe {
            Ok(std::str::from_utf8(
                &self.memory.data(&*self.store)[handle.offset..handle.offset + handle.length],
            )?)
        }
    }

    /// Pointer to the provided memory handle
    pub fn ptr(&self, handle: impl ToMemoryBlock) -> Result<*mut u8, Error> {
        let handle = handle.to_memory_block(self)?;
        Ok(unsafe { self.memory.data_ptr(&self.store()).add(handle.offset) })
    }

    /// Get the length of the block starting at `offs`
    pub fn block_length(&self, offs: usize) -> Option<usize> {
        self.live_blocks.get(&offs).cloned()
    }

    /// Get the block at the specified offset
    pub fn at_offset(&self, offset: usize) -> Option<MemoryBlock> {
        let block_length = self.block_length(offset);
        block_length.map(|length| MemoryBlock { offset, length })
    }
}

#[derive(Clone, Copy)]
pub struct MemoryBlock {
    pub offset: usize,
    pub length: usize,
}

impl From<(usize, usize)> for MemoryBlock {
    fn from(x: (usize, usize)) -> Self {
        MemoryBlock {
            offset: x.0,
            length: x.1,
        }
    }
}

impl MemoryBlock {
    pub fn new(offset: usize, length: usize) -> Self {
        MemoryBlock { offset, length }
    }
}