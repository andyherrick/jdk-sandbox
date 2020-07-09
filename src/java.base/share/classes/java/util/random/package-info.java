
/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

 /**
  * Classes and interfaces that support the definition and use of "random
  * generators", a term that is meant to cover what have traditionally been
  * called "random number generators" as well as generators of other sorts of
  * randomly chosen values, and also to cover not only deterministic
  * (pseudorandom) algorithms but also generators of values that use some "truly
  * random" physical source (perhaps making use of thermal noise, for example,
  * or quantum-mechanical effects).
  *
  * <p>The principal interface is {@link java.util.random.RandomGenerator},
  * which provides methods for requesting individual values of type {@code int},
  * {@code long}, {@code float}, {@code double}, or {@code boolean} chosen
  * (pseudo)randomly from a uniform distribution; methods for requesting values
  * of type {@code double} chosen (pseudo)randomly from a normal distribution or
  * from an exponential distribution; and methods for creating streams of values
  * of type {@code int}, {@code long}, or {@code double} chosen (pseudo)randomly
  * from a uniform distribution (such streams are spliterator-based, allowing
  * for parallel processing of their elements).  There are also static factory
  * methods for creating an instance of a specific random number generator
  * algorithm given its name.
  *
  * <p>An important subsidiary interface is {@link java.util.random.RandomGenerator.StreamableGenerator},
  * which provides methods for creating spliterator-based streams of {@code RandomGenerator} objects,
  * allowing for allowing for parallel processing of these objects using multiple threads.
  * Unlike {@link java.util.Random}, most implementations of {@code java.util.random.RandomGenerator}
  * are <i>not</i> thread-safe.  The intent is that instances should not be shared among threads;
  * rather, each thread should have its own random generator(s) to use.  The various pseudorandom
  * algorithms provided by this package are designed so that multiple instances will (with very
  * high probability) behave as if statistically independent.
  *
  * <p>For many purposes, these are the only two interfaces that a consumer of
  * (pseudo)random values will need.  There are also some more specialized
  * interfaces that describe more specialized categories of random number
  * generators ({@link java.util.random.RandomGenerator.SplittableGenerator},
  * {@link java.util.random.RandomGenerator.JumpableGenerator},
  * {@link java.util.random.RandomGenerator.LeapableGenerator}, and
  * {@link java.util.random.RandomGenerator.ArbitrarilyJumpableGenerator}) that have
  * specific strategies for creating statistically independent instances.
  *
  * <p>The class {@link java.util.random.RandomSupport} provides utility methods, constants, and
  * abstract classes frequently useful in the implementation of pseudorandom number generators
  * that satisfy the interface {@link RandomGenerator}.
  *
  *
  * <h2>Using the Random Number Generator Interfaces</h2>
  *
  * <p>To get started, an application should first create one instance of a generator class.
  * Assume that the contents of the package {@link java.util.random} have been imported:
  *
  * {@code import java.util.random.*;}
  *
  * <p>Then one can choose a specific implementation class and use {@code new},
  * providing either a 64-bit seed value, an array of seed bytes, or no argument at all:
  *
  * <blockquote>{@code RandomGenerator g = new L64X128MixRandom();}</blockquote>
  *
  * <p>or one can supply (or compute) the name of a generator class, in which case
  * no arguments are given to the constructor:
  *
  * <blockquote>{@code RandomGenerator g = RandomGenerator.of("L64X128MixRandom");}</blockquote>
  *
  * <p>or one can supply (or compute) a value of the enumeration type
  * {@link RandomGenerator.Algorithm}, in which case no arguments are given to the constructor:
  *
  * <blockquote>{@code import static java.util.random.RandomGenerator.Algorithm.*;}</blockquote>
  *
  * <blockquote>{@code RandomGenerator g = RandomGenerator.of(L64X128MixRandom);}</blockquote>
  *
  * <p>or one can call the {@code instance} method for a value of the enumeration type,
  * in which case no arguments are given to the constructor:
  *
  * <blockquote>{@code import static java.util.random.RandomGenerator.Algorithm.*;}</blockquote>
  *
  * <blockquote>{@code RandomGenerator g = L64X128MixRandom.instance();}</blockquote>
  *
  * <p>For a single-threaded application, this is all that is needed.  One can then invoke
  * methods of {@code g} such as {@code nextLong()}, {@code nextInt()}, {@code nextFloat()},
  * {@code nextDouble()}, and {@code nextBoolean()} to generate individual randomly chosen values.
  * One can also use the methods {@code ints()}, {@code longs()}, and {@code doubles()}
  * to create streams of randomly chosen values.  The methods {@code nextGaussian()} and
  * {@code nextExponential()} draw floating-point values from nonuniform distributions.
  * The method {@code period()} returns information about the period of the generator instance.
  *
  * <p>For a multi-threaded application, one can repeat the preceding steps to create
  * additional {@code RandomGenerator} values, but often it is preferable to use methods
  * of the one single initially created generator to create others like it.  (One reason
  * is that some generator algorithms, if asked to create a new set of generators all at once,
  * can make a special effort to ensure that the new generators are statistically independent.)
  * If the initial generator implements the interface {@code RandomGenerator.StreamableGenerator},
  * then the method {@code rngs()} can be used to create a stream of generators.  If this is
  * a parallel stream, then it is easy to get parallel execution by using the {@code map(...)}
  * method on the stream.
  *
  * <p>For a multi-threaded application that forks new threads dynamically, another approach is to
  * use an initial generator that implements the interface {@code RandomGenerator.SplittableGenerator},
  * which is then considered to "belong" to the initial thread for its exclusive use;
  * then whenever any thread needs to fork a new thread, it first uses the {@code split()}
  * method of its own generator to create a new generator, which is then passed to the newly
  * created thread for exclusive use by that new thread.
  *
  *
  * <h2>Choosing a Random Number Generator Algorithm</h2>
  *
  * <p>If an application requires a random number generator algorithm that is cryptographically secure,
  * then it should use an instance of the class {@link java.security.SecureRandom}.
  *
  * <p>For applications (such as physical simulation, machine learning, and games) that do not require
  * a cryptographically secure algorithm, this package provides multiple implementations of
  * interface {@link java.util.random.RandomGenerator} that provide trade-offs among
  * speed, space, period, accidental correlation, and equidistribution properties.
  *
  * <p>For applications with no special requirements, {@link java.util.random.L64X128MixRandom} has
  * a good balance among speed, space, and period, and is suitable for both single-threaded
  * and multi-threaded applications when used properly (a separate instance for each thread).
  *
  * <p>If the application uses only a single thread, then {@link java.util.random.Xoroshiro128PlusPlus}
  * is even smaller and faster, and certainly has a sufficiently long period.
  *
  * <p>For an application running in a 32-bit hardware environment and using only one or a small number of
  * threads, {@link java.util.random.L32X64StarStarRandom} or {@link java.util.random.L32X64MixRandom}
  * may be a good choice.
  *
  * <p>For an application that uses many threads that are allocated in one batch at the start of the
  * computation, either a "jumpable" generator such as {@link java.util.random.Xoroshiro128PlusPlus}
  * or {@link java.util.random.Xoshiro256PlusPlus} may be used, or a "splittable" generator such as
  * {@link java.util.random.L64X128MixRandom} or {@link java.util.random.L64X256MixRandom} may be used.
  * If furthermore the application uses only floating-point values from a uniform distribution
  * no more than 32 bits of floating-point precision are required, and exact equidistribution is not
  * required, then {@link java.util.random.MRG32k3a} (a classic and well-studied algorithm) may be appropriate.
  *
  * <p>For an application that creates many threads dynamically, perhaps through the use of spliterators,
  * a "splittable" generator such as
  * {@link java.util.random.L64X128MixRandom} or {@link java.util.random.L64X256MixRandom} is recommended
  * (the original {@link java.util.SplittableRandom} algorithm may also be used, but it is now known to have certain
  * minor mathematical statistical weaknesses that may or may not matter in practice).
  * If the number of generators created dynamically may be very large (millions or more), then using generators
  * such as {@link java.util.random.L128X128MixRandom} or {@link java.util.random.L128X256MixRandom},
  * which use a 128-bit parameter rather than a 64-bit parameter for their LCG subgenerator,
  * will make it much less likely that two instances use the same state cycle.
  *
  * <p>For an application that uses tuples of consecutively generated values, it may be desirable to use a generator
  * that is <i>k</i>-equidistributed such that <i>k</i> is at least as large as the length of the tuples being generated.
  * The generator {@link java.util.random.L64X256MixRandom} is provably 4-equidistributed,
  * and {@link java.util.random.L64X1024MixRandom} is provably 16-equidistributed.
  *
  * <p>For applications that generate large permutations, it may be best to use a generator whose period is much larger than
  * the total number of possible permutations; otherwise it will be impossible to generate some of the intended permutations.
  * For example, if the goal is to shuffle a deck of 52 cards,
  * the number of possible permutations is 52! (52 factorial), which is approximately 2<sup>225.58</sup>,
  * so it may be best to use a generator whose period is roughly 2<sup>256</sup> or larger, such as
  * {@link java.util.random.L64X256MixRandom} or {@link java.util.random.L64X1024MixRandom} or
  * {@link java.util.random.L128X256MixRandom} or {@link java.util.random.L128X1024MixRandom}.
  * (It is of course also necessary to provide sufficiently many seed bits when the generator
  * is initialized, or else it will still be impossible to generate some of the intended permutations.)
  *
  *
  * <h2>Categories of Random Number Generator Algorithms</h2>
  *
  * <p>Historically, most pseudorandom generator algorithms have been based on some sort of
  * finite-state machine with a single, large cycle of states; when it is necessary to have
  * multiple threads use the same algorithm simultaneously, the usual technique is to arrange for
  * each thread to traverse a different region of the state cycle.  These regions may be doled
  * out to threads by starting with a single initial state and then using a "jump function" that
  * travels a long distance around the cycle (perhaps 2<sup>64</sup> steps or more); the jump
  * function is applied repeatedly and sequentially, to identify widely spaced states that are
  * then doled out, one to each thread, to serve as the initial state for the generator to be
  * used by that thread.  This strategy is supported by the interface
  * {@link java.util.random.RandomGenerator.JumpableGenerator}.
  * Sometimes it is desirable to support two levels of jumping (by long distances and
  * by <i>really</i> long distances); this strategy is supported by the interface
  * {@link java.util.random.RandomGenerator.LeapableGenerator}.  There is also an interface
  * {@link java.util.random.RandomGenerator.ArbitrarilyJumpableGenerator} for algorithms that
  * allow jumping along the state cycle by any user-specified distance.
  * In this package, implementations of these interfaces include
  * {@link java.util.random.Xoroshiro128PlusPlus},
  * {@link java.util.random.Xoshiro256PlusPlus},
  * and {@link java.util.random.MRG32K3A}.
  *
  * <p>A more recent category of "splittable" pseudorandom generator algorithms uses a large family
  * of state cycles and makes some attempt to ensure that distinct instances use different state
  * cycles; but even if two instances "accidentally" use the same state cycle, they are highly
  * likely to traverse different regions parts of that shared state cycle.  This strategy is
  * supported by the interface {@link java.util.random.RandomGenerator.SplittableGenerator}.
  * In this package, implementations of this interface include
  * {@link java.util.random.L32X64StarStarRandom},
  * {@link java.util.random.L32X64MixRandom},
  * {@link java.util.random.L64X128StarStarRandom},
  * {@link java.util.random.L64X128MixRandom},
  * {@link java.util.random.L64X256MixRandom},
  * {@link java.util.random.L64X1024MixRandom},
  * {@link java.util.random.L128X128MixRandom},
  * {@link java.util.random.L128X256MixRandom},
  * {@link java.util.random.L128X1024MixRandom},
  * and {@link java.util.SplittableRandom}.
  *
  *
  * <h2>The LXM Family of Random Number Generator Algorithms</h2>
  *
  * <p>Each class with a name of the form {@code LmmmXnnnSomethingRandom} uses some
  * specific member of the LXM family of random number algorithms; "LXM" is short
  * for "LCG, Xorshift, Mixing function".  Every LXM generator consists of two
  * subgenerators; one is an LCG (Linear Congruential Generator) and the other is
  * an Xorshift generator.  Each output of an LXM generator is the result of
  * combining state from the LCG with state from the Xorshift generator by
  * using a Mixing function (and then the state of the LCG and the state of the
  * Xorshift generator are advanced).
  *
  * <p>The LCG subgenerator has an update step of the form {@code s = m * s + a},
  * where {@code s}, {@code m}, and {@code a} are all binary integers of the same
  * size, each having {@code mmm} bits; {@code s} is the mutable state, the
  * multiplier {@code m} is fixed (the same for all instances of a class) and the
  * addend {@code a} is a parameter (a final field of the instance).  The
  * parameter {@code a} is required to be odd (this allows the LCG to have the
  * maximal period, namely 2<sup>mmm</sup>); therefore there are
  * 2<sup>mmm-1</sup> distinct choices of parameter.  (When the size of {@code s}
  * is 128 bits, then we use the name "{@code sh}" below to refer to the high half
  * of {@code s}, that is, the high-order 64 bits of {@code s}.)
  *
  * <p>The Xorshift subgenerator can in principle be any one of a wide variety of
  * xorshift algorithms; in this package it is always either {@code xoroshiro128},
  * {@code xoshiro256}, or {@code xoroshiro1024}, in each case without any final
  * scrambler such as "+" or "**".  Its state consists of some fixed number of
  * {@code int} or {@code long} fields, generally named {@code x0}, {@code x1},
  * and so on, which can take on any values provided that they are not all zero.
  * The collective total size of these fields is {@code nnn} bits; therefore the
  * period of this subgenerator is 2<sup>nnn</sup>-1.
  *
  * <p>Because the periods 2<sup>mmm</sup> and 2<sup>nnn</sup>-1 of the two subgenerators
  * are relatively prime, the <em>period</em> of any single instance of an LXM algorithm
  * (the length of the series of generated values before it repeats) is the product
  * of the periods of the subgenerators, that is, 2<sup>mmm</sup>(2<sup>nnn</sup>-1),
  * which is just slightly smaller than 2<sup>(mmm+nnn)</sup>.  Moreover, if two distinct
  * instances of the same LXM algorithm have different {@code a} parameters, then their
  * cycles of produced values will be different.
  *
  * <p>Generally speaking, among the "{@code LmmmXnnn}" generators, the memory
  * required for an instance is {@code (2 * mmm + nnn)} bits.  (If {@code nnn} is 1024
  * or larger, the Xorshift state is represented as an array, so additional bits are
  * needed for the array object header, and another 32 bits are used for an array index.)
  *
  * <p>Larger values of "{@code mmm}" imply a lower probability that two distinct instances will
  * traverse the same state cycle, and larger values of "{@code nnn}" imply that the generator is
  * equidistributed in a larger number of dimensions (this is provably true when {@code mmm} is
  * 64, and conjectured to be approximately true when {@code mmm} is 128).  A class with
  * "{@code Mix}" in its name uses a fairly strong mixing function with excellent avalanche
  * characteristics; a class with "{@code StarStar}" in its name uses a
  * weaker but faster mixing function.
  *
  * <p>The specific LXM algorithms used in this package are all chosen so that
  * the 64-bit values produced by the {@code nextLong()} method are exactly equidistributed
  * (for example, for any specific instance of {@link L64X128MixRandom}, over the course of its cycle each
  * of the 2<sup>64</sup> possible {@code long} values will be produced 2<sup>128</sup>-1 times).
  * The values produced by the {@code nextInt()}, {@code nextFloat()}, and {@code nextDouble()}
  * methods are likewise exactly equidistributed.  Some algorithms provide a further guarantee of
  * <i>k</i>-equidistribution for some <i>k</i> greater than 1, meaning that successive
  * non-overlapping <i>k</i>-tuples of 64-bit values produced by the {@code nextLong()} method
  * are exactly equidistributed (equally likely to occur).
  *
  * <p> The following table gives the period, state size (in bits), parameter size (in bits,
  * including the low-order bit that is required always to be a 1-bit), and equidistribution
  * property for each of the specific LXM algorithms used in this package.
  *
  * <table>
  * <thead>
  *   <tr><th>Class</th><th>Period</th><th>State size</th><th>Parameter size</th><th>{@code nextLong} values are</th></tr>
  * </thead>
  * <tbody>
  *   <tr><td>{@link java.util.random.L32X64StarStarRandom}<td>2<sup>32</sup>(2<sup>64</sup>-1)</td><td>96 bits</td><td>32 bits</td><td></td></tr>
  *   <tr><td>{@link java.util.random.L32X64MixRandom}<td>2<sup>32</sup>(2<sup>64</sup>-1)</td><td>96 bits</td><td>32 bits</td><td></td></tr>
  *   <tr><td>{@link java.util.random.L64X128StarStarRandom}<td>2<sup>64</sup>(2<sup>128</sup>-1)</td><td>192 bits</td><td>64 bits</td><td>2-equidistributed and exactly equidistributed</td></tr>
  *   <tr><td>{@link java.util.random.L64X128MixRandom}<td>2<sup>64</sup>(2<sup>128</sup>-1)</td><td>192 bits</td><td>64 bits</td><td>2-equidistributed and exactly equidistributed</td></tr>
  *   <tr><td>{@link java.util.random.L64X256MixRandom}<td>2<sup>64</sup>(2<sup>256</sup>-1)</td><td>320 bits</td><td>64 bits</td><td>4-equidistributed and exactly equidistributed</td></tr>
  *   <tr><td>{@link java.util.random.L64X1024MixRandom}<td>2<sup>64</sup>(2<sup>1024</sup>-1)</td><td>1088 bits</td><td>64 bits</td><td>16-equidistributed and exactly equidistributed</td></tr>
  *   <tr><td>{@link java.util.random.L128X128MixRandom}<td>2<sup>128</sup>(2<sup>128</sup>-1)</td><td>256 bits</td><td>128 bits</td><td>exactly equidistributed</td></tr>
  *   <tr><td>{@link java.util.random.L128X256MixRandom}<td>2<sup>128</sup>(2<sup>256</sup>-1)</td><td>384 bits</td><td>128 bits</td><td>exactly equidistributed</td></tr>
  *   <tr><td>{@link java.util.random.L128X1024MixRandom}<td>2<sup>128</sup>(2<sup>1024</sup>-1)</td><td>1152 bits</td><td>128 bits</td><td>exactly equidistributed</td></tr>
  * </tbody>
  * </table>
  *
  * <p>For the algorithms listed above whose names begin with {@code L32},
  * the 32-bit values produced by the {@code nextInt()} method are exactly equidistributed,
  * but the 64-bit values produced by the {@code nextLong()} method are not exactly equidistributed.
  *
  * <p>For the algorithms listed above whose names begin with {@code L64} or {@code L128},
  * the 64-bit values produced by the {@code nextLong()} method are <i>exactly equidistributed</i>:
  * every instance, over the course of its cycle, will produce each
  * of the 2<sup>64</sup> possible {@code long} values exactly the same number of times.
  * For example, any specific instance of {@link L64X256MixRandom}, over the course of its cycle each
  * of the 2<sup>64</sup> possible {@code long} values will be produced 2<sup>256</sup>-1 times.
  * The values produced by the {@code nextInt()}, {@code nextFloat()}, and {@code nextDouble()}
  * methods are likewise exactly equidistributed.
  *
  * <p>In addition, for the algorithms listed above whose names begin with {@code L64},
  * the 64-bit values produced by the {@code nextLong()} method are <i>k</i>-equidistributed
  * (but not exactly <i>k</i>-equidistributed).
  * To be precise, and taking {@link L64X256MixRandom} as an example:
  * for any specific instance of {@link L64X256MixRandom}, consider
  * the (overlapping) length-4 subsequences of the cycle of 64-bit values produced by
  * {@code nextLong()} (assuming no other methods are called that would affect the state).
  * There are 2<sup>64</sup>(2<sup>256</sup>-1) such subsequences, and each subsequence,
  * which consists of 4 64-bit values, can have one of 2<sup>256</sup> values. Of those
  * 2<sup>256</sup> subsequence values, nearly all of them (2<sup>256</sup>-2<sup>64</sup>)
  * occur 2<sup>64</sup> times over the course of the entire cycle, and the other
  * 2<sup>64</sup> subsequence values occur only 2<sup>64</sup>-1 times.  So the ratio
  * of the probability of getting any specific one of the less common subsequence values and the
  * probability of getting any specific one of the more common subsequence values is 1-2<sup>-64</sup>.
  * (Note that the set of 2<sup>64</sup> less-common subsequence values will differ from
  * one instance of {@link L64X256MixRandom} to another, as a function of the additive
  * parameter of the LCG.)  The values produced by the {@code nextInt()}, {@code nextFloat()},
  * and {@code nextDouble()} methods are likewise 4-equidistributed (but not exactly 4-equidistributed).
  *
  * <p> The next table gives the LCG multiplier value, the name of the specific Xorshift
  * algorithm used, the specific numeric parameters for that Xorshift algorithm, and the mixing
  * function for each of the specific LXM algorithms used in this package.  (Note that the
  * multiplier used for the 128-bit LCG cases is 65 bits wide, so the constant
  * {@code 0x1d605bbb58c8abbfdL} shown in the table cannot actually be used in code; instead,
  * only the 64 low-order bits {@code 0xd605bbb58c8abbfdL} are represented in the source code,
  * and the missing 1-bit is handled through special coding of the multiply-add algorithm used in
  * the LCG.)
  *
  * <table>
  * <thead>
  *   <tr><th>Class</th><th>LCG multiplier {@code m}</th><th>Xorshift algorithm</th><th>Xorshift parameters</th><th>Mixing function</th></tr>
  * </thead>
  * <tbody>
  *   <tr><td>{@link java.util.random.L32X64StarStarRandom}<td>{@code 0xadb4a92d}</td><td>{@code xoroshiro64}, version 1.0</td><td>(26, 9, 13)}</td><td>Integer.rotateLeft((s + x0) * 5, 7) * 9</td></tr>
  *   <tr><td>{@link java.util.random.L32X64MixRandom}<td>{@code 0xadb4a92d}</td><td>{@code xoroshiro64}, version 1.0</td><td>(26, 9, 13)}</td><td>{@link RandomSupport.mixLea32}{@code (s+x0)}</td></tr>
  *   <tr><td>{@link java.util.random.L64X128StarStarRandom}<td>{@code 0xd1342543de82ef95L}</td><td>{@code xoroshiro128}, version 1.0</td><td>(24, 16, 37)}</td><td>Long.rotateLeft((s + x0) * 5, 7) * 9</td></tr>
  *   <tr><td>{@link java.util.random.L64X128MixRandom}<td>{@code 0xd1342543de82ef95L}</td><td>{@code xoroshiro128}, version 1.0</td><td>(24, 16, 37)}</td><td>{@link RandomSupport.mixLea64}{@code (s+x0)}</td></tr>
  *   <tr><td>{@link java.util.random.L64X256MixRandom}<td>{@code 0xd1342543de82ef95L}</td><td>{@code xoshiro256}, version 1.0</td><td>(17, 45)}</td><td>{@link RandomSupport.mixLea64}{@code (s+x0)}</td></tr>
  *   <tr><td>{@link java.util.random.L64X1024MixRandom}<td>{@code 0xd1342543de82ef95L}</td><td>{@code xoroshiro1024}, version 1.0</td><td>(25, 27, 36)}</td><td>{@link RandomSupport.mixLea64}{@code (s+x0)}</td></tr>
  *   <tr><td>{@link java.util.random.L128X128MixRandom}<td>{@code 0x1d605bbb58c8abbfdL}</td><td>{@code xoroshiro128}, version 1.0</td><td>(24, 16, 37)}</td><td>{@link RandomSupport.mixLea64}{@code (sh+x0)}</td></tr>
  *   <tr><td>{@link java.util.random.L128X256MixRandom}<td>{@code 0x1d605bbb58c8abbfdL}</td><td>{@code xoshiro256}, version 1.0</td><td>(17, 45)}</td><td>{@link RandomSupport.mixLea64}{@code (sh+x0)}</td></tr>
  *   <tr><td>{@link java.util.random.L128X1024MixRandom}<td>{@code 0x1d605bbb58c8abbfdL}</td><td>{@code xoroshiro1024}, version 1.0</td><td>(25, 27, 36)}</td><td>{@link RandomSupport.mixLea64}{@code (sh+x0)}</td></tr>
  * </tbody>
  * </table>
  *
  * @since   16
  */

 package java.util.random;
