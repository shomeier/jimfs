/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.jimfs.internal;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import com.google.common.primitives.UnsignedBytes;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Colin Decker
 */
public class RealFileByteStore extends ByteStore implements Closeable {

  private final Path file;
  private final FileChannel channel;

  public RealFileByteStore() {
    try {
      this.file = Files.createTempFile("RealFileByteStore", "tmp");
      this.channel = FileChannel.open(file, READ, WRITE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long currentSize() {
    try {
      return channel.size();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected ByteStore createCopy() {
    RealFileByteStore copy = new RealFileByteStore();
    try {
      transferTo(0, currentSize(), copy.channel);
      return copy;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean truncate(long size) {
    if (size < currentSize()) {
      try {
        channel.truncate(size);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return false;
  }

  @Override
  public int write(long pos, byte b) {
    return write(pos, new byte[] {b}, 0, 1);
  }

  @Override
  public int write(long pos, byte[] b, int off, int len) {
    return write(pos, ByteBuffer.wrap(b, off, len));
  }

  @Override
  public int write(long pos, ByteBuffer buf) {
    try {
      channel.position(pos);
      return channel.write(buf);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long transferFrom(ReadableByteChannel src, long pos, long count) throws IOException {
    return channel.transferFrom(src, pos, count);
  }

  @Override
  public int read(long pos) {
    byte[] b = new byte[1];
    int read = read(pos, b, 0, 1);
    return read == -1 ? -1 : UnsignedBytes.toInt(b[0]);
  }

  @Override
  public int read(long pos, byte[] b, int off, int len) {
    return read(pos, ByteBuffer.wrap(b, off, len));
  }

  @Override
  public int read(long pos, ByteBuffer buf) {
    try {
      channel.position(pos);
      return channel.read(buf);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long transferTo(long pos, long count, WritableByteChannel dest) throws IOException {
    return channel.transferTo(pos, count, dest);
  }

  @Override
  protected void deleteContents() {
    try {
      close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      channel.close();
    } finally {
      Files.deleteIfExists(file);
    }
  }
}
