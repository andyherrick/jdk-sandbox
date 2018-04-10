/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_VM_GC_EPSILON_BARRIERSET_HPP
#define SHARE_VM_GC_EPSILON_BARRIERSET_HPP

#include "gc/shared/collectorPolicy.hpp"
#include "gc/shared/modRefBarrierSetAssembler.hpp"
#include "gc/shared/barrierSet.hpp"

// No interaction with application is required for Epsilon, and therefore
// the barrier set is mostly empty.
class EpsilonBarrierSet: public BarrierSet {
  friend class VMStructs;

public:
  EpsilonBarrierSet() : BarrierSet(make_barrier_set_assembler<ModRefBarrierSetAssembler>(), BarrierSet::FakeRtti(BarrierSet::Epsilon)) {};
  virtual void print_on(outputStream *st) const {}

  template <DecoratorSet decorators, typename BarrierSetT = EpsilonBarrierSet>
  class AccessBarrier: public BarrierSet::AccessBarrier<decorators, BarrierSetT> {};

protected:
  virtual void write_ref_array_work(MemRegion mr) {}
};

template<>
struct BarrierSet::GetName<EpsilonBarrierSet> {
  static const BarrierSet::Name value = BarrierSet::Epsilon;
};

template<>
struct BarrierSet::GetType<BarrierSet::Epsilon> {
  typedef EpsilonBarrierSet type;
};

#endif // SHARE_VM_GC_EPSILON_BARRIERSET_HPP
