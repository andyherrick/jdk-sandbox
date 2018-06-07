/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 */


/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jvmti/PopFrame/popframe005.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that after popping a method's frame by the JVMTI
 *     function PopFrame():
 *       - lock acquired by the popped frame will be released;
 *       - no JVMTI events will be generated by the function PopFrame()
 *     To enable debug tracing for:
 *         - java code:
 *             add "-v" to the test's parameters
 *         - native code:
 *             pass "trace=all" parameter to the agent, e.g.
 *                 -agentlib:popframe005=trace=all
 * COMMENTS
 *     The test was changed due to the bug 4448675.
 *     Fixed according to the 4528859 bug.
 *     Ported from JVMDI.
 *     Fixed according to 4912302 bug.
 *       - rearranged synchronization of tested threads
 *
 * @library /vmTestbase
 *          /test/lib
 * @run driver jdk.test.lib.FileInstaller . .
 * @run main/othervm/native -agentlib:popframe005 nsk.jvmti.PopFrame.popframe005
 */

