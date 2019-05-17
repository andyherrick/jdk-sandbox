/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#include "precompiled.hpp"
#include "jfr/dcmd/jfrDcmds.hpp"
#include "jfr/recorder/jfrRecorder.hpp"
#include "jfr/recorder/repository/jfrChunk.hpp"
#include "jfr/recorder/repository/jfrChunkWriter.hpp"
#include "jfr/utilities/jfrTimeConverter.hpp"
#include "logging/log.hpp"
#include "runtime/os.inline.hpp"
#include "runtime/thread.inline.hpp"

static const u1 GUARD = 0xff;

static jlong nanos_now() {
  return os::javaTimeMillis() * JfrTimeConverter::NANOS_PER_MILLISEC;
}

static jlong ticks_now() {
  return JfrTicks::now();
}

JfrChunk::JfrChunk() :
  _path(NULL),
  _start_ticks(0),
  _previous_start_ticks(invalid_time),
  _start_nanos(0),
  _previous_start_nanos(invalid_time),
  _last_update_nanos(0),
  _last_checkpoint_offset(0),
  _last_metadata_offset(0),
  _generation(1) {}

JfrChunk::~JfrChunk() {
  reset();
}

void JfrChunk::reset() {
  if (_path != NULL) {
    JfrCHeapObj::free(_path, strlen(_path) + 1);
    _path = NULL;
  }
  _last_checkpoint_offset = _last_metadata_offset = 0;
  _generation = 1;
}

void JfrChunk::set_last_checkpoint_offset(int64_t offset) {
  _last_checkpoint_offset = offset;
}

int64_t JfrChunk::last_checkpoint_offset() const {
  return _last_checkpoint_offset;
}

int64_t JfrChunk::start_ticks() const {
  assert(_start_ticks != 0, "invariant");
  return _start_ticks;
}

int64_t JfrChunk::start_nanos() const {
  assert(_start_nanos != 0, "invariant");
  return _start_nanos;
}

int64_t JfrChunk::previous_start_ticks() const {
  assert(_previous_start_ticks != invalid_time, "invariant");
  return _previous_start_ticks;
}

int64_t JfrChunk::previous_start_nanos() const {
  assert(_previous_start_nanos != invalid_time, "invariant");
  return _previous_start_nanos;
}

void JfrChunk::update_start_ticks() {
  _start_ticks = ticks_now();
}

void JfrChunk::update_start_nanos() {
  _start_nanos = _last_update_nanos = nanos_now();
}

void JfrChunk::update() {
  _last_update_nanos = nanos_now();
}

void JfrChunk::save_current_and_update_start_ticks() {
  _previous_start_ticks = _start_ticks;
  update_start_ticks();
}

void JfrChunk::save_current_and_update_start_nanos() {
  _previous_start_nanos = _start_nanos;
  update_start_nanos();
}

void JfrChunk::update_time_to_now() {
  save_current_and_update_start_nanos();
  save_current_and_update_start_ticks();
}

int64_t JfrChunk::last_chunk_duration() const {
  assert(_previous_start_nanos != invalid_time, "invariant");
  return _start_nanos - _previous_start_nanos;
}

static char* copy_path(const char* path) {
  assert(path != NULL, "invariant");
  const size_t path_len = strlen(path);
  char* new_path = JfrCHeapObj::new_array<char>(path_len + 1);
  strncpy(new_path, path, path_len + 1);
  return new_path;
}

void JfrChunk::set_path(const char* path) {
  if (_path != NULL) {
    JfrCHeapObj::free(_path, strlen(_path) + 1);
    _path = NULL;
  }
  if (path != NULL) {
    _path = copy_path(path);
  }
}

const char* JfrChunk::path() const {
  return _path;
}

bool JfrChunk::is_started() const {
  return _start_nanos != 0;
}

bool JfrChunk::is_finished() const {
  return 0 == _generation;
}

bool JfrChunk::is_initial_flush() const {
  return 0 == _last_metadata_offset;
}

int64_t JfrChunk::duration() const {
  assert(_last_update_nanos >= _start_nanos, "invariant");
  return _last_update_nanos - _start_nanos;
}

int64_t JfrChunk::last_metadata_offset() const {
  return _last_metadata_offset;
}

void JfrChunk::set_last_metadata_offset(int64_t offset) {
  if (0 == offset) {
    return;
  }
  assert(offset > _last_metadata_offset, "invariant");
  _last_metadata_offset = offset;
}

bool JfrChunk::has_metadata() const {
  return 0 != _last_metadata_offset;
}

u1 JfrChunk::generation() const {
  assert(_generation > 0, "invariant");
  const u1 this_generation = _generation++;
  if (GUARD == _generation) {
    _generation = 1;
  }
  return this_generation;
}

