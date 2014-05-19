/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package datafu.pig.hash;

import java.util.Random;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;

import datafu.pig.util.SimpleEvalFunc;

/**
 * Computes a <em>non-cryptographic</em> Hash value of a string and outputs it in hex (by default).
 *
 * <ul>
 * <li><code>'murmur3-32', [optional seed]</code> or <code>'murmur3-128',
 *   [optional seed]</code>: Returns a <a
 *   href="https://code.google.com/p/smhasher/">murmur3 hash</a> of the given
 *   length. Murmur3 is fast, with has exceptionally good statistical
 *   properties; it's a good choice if all you need is good mixing of the
 *   inputs. It is <em>not</em> cryptographically secure; that is, given an
 *   output value from murmur3, there are efficient algorithms to find an input
 *   yielding the same output value. Supply the seed as a string that
 *   <a href="http://docs.oracle.com/javase/7/docs/api/java/lang/Integer.html#decode(java.lang.String)">Integer.decode</a>
 *   can handle. Examples: <code>datafu.pig.hash.Hasher('murmur3-32', '0x56789abc');</code> or <code>datafu.pig.hash.Hasher('murmur3-32', '-12345678');</code>.</li>

 * <li><code>'sip24', [optional seed]</code>: Returns a <a href="https://131002.net/siphash/">64-bit
 *   SipHash-2-4</a> hash. SipHash is competitive in performance with Murmur3,
 *   and is simpler and faster than the cryptographic algorithms below. When
 *   used with a seed, it can be considered cryptographically secure: given
 *   the output from a sip24 instance but not the seed used, we cannot
 *   efficiently craft a message yielding the same output from that instance. To
 *   supply a seed, pass in a 32-character string representing the seed in
 *   hexadecimal. If none is given, k = '00010203&hellip;0e0f' is used.</li>
 * <li><code>'adler32'</code>: Returns an Adler-32 checksum (32 hash bits) by delegating to Java's Adler32 Checksum.</li>
 * <li><code>'crc32'</code>:   Returns a CRC-32 checksum (32 hash bits) by delegating to Java's CRC32 Checksum.</li>
 * <li><code>'md5'</code>:     Returns an MD5 hash (128 hash bits) using Java's MD5 MessageDigest.</li>
 * <li><code>'sha1'</code>:    Returns a SHA-1 hash (160 hash bits) using Java's SHA-1 MessageDigest.</li>
 * <li><code>'sha256'</code>:  Returns a SHA-256 hash (256 hash bits) using Java's SHA-256 MessageDigest.</li>
 * <li><code>'sha512'</code>:  Returns a SHA-512 hash (160 hash bits) using Java's SHA-512 MessageDigest.</li>
 * <li><code>'good-{integer number of bits}'</code>: Returns a general-purpose,
 *   <i>non-cryptographic-strength</i>, streaming hash function that produces
 *   hash codes of length at least minimumBits. Users without specific'
 *   compatibility requirements and who do not persist the hash codes are
 *   encouraged to choose this hash function. (Cryptographers, like dieticians
 *   and fashionistas, occasionally realize that We've Been Doing it Wrong
 *   This Whole Time. Using 'good-*' lets you track What the Experts From
 *   (Milan|NIH|IEEE) Say To (Wear|Eat|Hash With) this Fall.) Expect values
 *   returned by this hasher to change run-to-run.</li>
 * </ul>
 *
 * For hashes that accept a seed, pass in the string 'rand' for the seed to get
 * a randomly-chosen seed. This gives a well-mixed sequence of values without
 * the relying on the random number generator for each call.
 */
public class Hasher extends SimpleEvalFunc<String>
{
  protected final HashFunction hash_func;
  public static Random randgen = new Random(); // so tests can inject constant seed

  private static final String HASH_NAMES = "'murmur3-32' (with optional seed); 'murmur3-128' (with optional seed); 'sip24' (with optional seed); 'crc32', 'adler32', 'md5'; 'sha1'; 'sha256'; 'sha512';  or 'good-{number of bits}'.";

  /**
   * Hasher with a non-cryptographic-strength hash function of at least 32 bits.
   *
   * @throws IllegalArgumentException
   */
  public Hasher() throws IllegalArgumentException
  {
    this("good-32");
  }

  /**
   * Hasher with the hash function given by algorithm
   *
   * @param algorithm
   * @throws IllegalArgumentException
   */
  public Hasher(String algorithm)
  {
    if      (algorithm.startsWith("good-")) {
      int bits = Integer.parseInt(algorithm.substring(5));
      hash_func = Hashing.goodFastHash(bits);
    }
    else if (algorithm.equals("murmur3-32")) { hash_func = Hashing.murmur3_32();  }
    else if (algorithm.equals("murmur3-128")){ hash_func = Hashing.murmur3_128(); }
    else if (algorithm.equals("sip24"))      { hash_func = Hashing.sipHash24();   }
    else if (algorithm.equals("sha1"))       { hash_func = Hashing.sha1();        }
    else if (algorithm.equals("sha256"))     { hash_func = Hashing.sha256();      }
    else if (algorithm.equals("sha512"))     { hash_func = Hashing.sha512();      }
    else if (algorithm.equals("md5"))        { hash_func = Hashing.md5();         }
    else if (algorithm.equals("adler32"))    { hash_func = Hashing.adler32();     }
    else if (algorithm.equals("crc32"))      { hash_func = Hashing.crc32();       }
    else { throw new IllegalArgumentException("No hash function found for algorithm "+algorithm+". Allowed values include "+HASH_NAMES); }
  }

  public Hasher(String algorithm, String seed) throws IllegalArgumentException, RuntimeException
  {
    try {
      if      (algorithm.equals("murmur3-32") && seed.equals("rand")) {
        hash_func = Hashing.murmur3_32(randgen.nextInt());
      }
      else if (algorithm.equals("murmur3-128") && seed.equals("rand")) {
        hash_func = Hashing.murmur3_128(randgen.nextInt());
      }
      else if (algorithm.equals("murmur3-32")) {
        Integer seedint = Integer.decode(seed);
        hash_func = Hashing.murmur3_32(seedint);
      }
      else if (algorithm.equals("murmur3-128")) {
        Integer seedint = Integer.decode(seed);
        hash_func = Hashing.murmur3_128(seedint);
      }
      else if (algorithm.equals("sip24") && seed.equals("rand")) {
        Long k0 = randgen.nextLong(), k1 = randgen.nextLong();
        hash_func = Hashing.sipHash24(k0, k1);
      }
      else if (algorithm.equals("sip24")){
        if (seed.length() != 32) { throw new IllegalArgumentException("Seed for sip24 must be a 32-character string representing a 128-bit number in hexadecimal."); }
        Long k0 = Long.decode("0x"+seed.substring( 0,16));
        Long k1 = Long.decode("0x"+seed.substring(16,32));
        hash_func = Hashing.sipHash24(k0, k1);
      }
      else { throw new IllegalArgumentException("No hash function found for algorithm "+algorithm+" with a seed. Allowed values include "+HASH_NAMES); }
    }
    catch (NumberFormatException err) {
      throw new RuntimeException(err);
    }
  }

  public String call(String val)
  {
    return hash_func.hashBytes(val.getBytes()).toString();
  }
}
