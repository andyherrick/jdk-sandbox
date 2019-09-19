/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_MEMORY_METASPACE_METADEBUG_HPP
#define SHARE_MEMORY_METASPACE_METADEBUG_HPP

#include "memory/allocation.hpp"

namespace metaspace {

class Metadebug : AllStatic {
  // Debugging support for Metaspaces
  static int _allocation_fail_alot_count;

 public:

  static void init_allocation_fail_alot_count();
#ifdef ASSERT
  static bool test_metadata_failure();
#endif
};

#ifdef ASSERT
#define EVERY_NTH(n)          \
{ static int counter_ = 0;    \
  if (n > 0) {                \
    counter_ ++;              \
    if (counter_ > n) {       \
      counter_ = 0;           \

#define END_EVERY_NTH         } } }

#define SOMETIMES(code) \
    EVERY_NTH(VerifyMetaspaceInterval) \
    { code } \
    END_EVERY_NTH

#define ASSERT_SOMETIMES(condition, ...) \
		EVERY_NTH(VerifyMetaspaceInterval) \
		assert( (condition), __VA_ARGS__); \
		END_EVERY_NTH

#else

#define SOMETIMES(code)
#define ASSERT_SOMETIMES(condition, ...)

#endif // ASSERT



} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_METADEBUG_HPP
