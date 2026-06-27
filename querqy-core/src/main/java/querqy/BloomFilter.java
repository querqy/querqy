/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
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
package querqy;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Bloom filter parameterised by bit count and number of hash functions.
 * Hash positions are derived from a single MurmurHash3-128 hash via double-hashing:
 * {@code position_i = (h1 + i*h2) mod bits}.
 *
 * <p>Serialised as a fixed {@code (bits/4)}-character lowercase hex string.</p>
 */
public final class BloomFilter {

    private static final long MURMUR_C1 = 0x87c37b91114253d5L;
    private static final long MURMUR_C2 = 0x4cf5ad432745937fL;

    private final int bits;
    private final int hashFunctions;
    private final long[] words;

    public BloomFilter(final int bits, final int hashFunctions) {
        if (bits <= 0 || bits % 64 != 0) {
            throw new IllegalArgumentException("bits must be a positive multiple of 64, got: " + bits);
        }
        this.bits = bits;
        this.hashFunctions = hashFunctions;
        this.words = new long[bits / 64];
    }

    /**
     * Constructs a BloomFilter by decoding a lowercase hex string produced by {@link #toHex()}.
     *
     * @param hex           hex string of length divisible by 16; each 16 chars encode one 64-bit word
     * @param hashFunctions number of hash functions used when the filter was built
     */
    public static BloomFilter fromHex(final String hex, final int hashFunctions) {
        if (hex.length() % 16 != 0) {
            throw new IllegalArgumentException("hex length must be a multiple of 16, got: " + hex.length());
        }
        final int numWords = hex.length() / 16;
        final BloomFilter bf = new BloomFilter(numWords * 64, hashFunctions);
        for (int i = 0; i < numWords; i++) {
            bf.words[i] = Long.parseUnsignedLong(hex, i * 16, i * 16 + 16, 16);
        }
        return bf;
    }

    public void add(final String term) {
        final byte[] bytes = term.getBytes(StandardCharsets.UTF_8);
        final long[] h = murmur3_128(bytes, 0, bytes.length);
        final long h1 = h[0], h2 = h[1];
        for (int i = 0; i < hashFunctions; i++) {
            final int pos = (int) ((h1 + (long) i * h2) & (bits - 1));
            words[pos >>> 6] |= 1L << (pos & 63);
        }
    }

    public boolean contains(final CharSequence term) {
        final byte[] bytes = term.toString().getBytes(StandardCharsets.UTF_8);
        final long[] h = murmur3_128(bytes, 0, bytes.length);
        final long h1 = h[0], h2 = h[1];
        for (int i = 0; i < hashFunctions; i++) {
            final int pos = (int) ((h1 + (long) i * h2) & (bits - 1));
            if ((words[pos >>> 6] & (1L << (pos & 63))) == 0) {
                return false;
            }
        }
        return true;
    }

    public void saturate() {
        Arrays.fill(words, -1L);
    }

    /** Returns a new BloomFilter whose bits are the bitwise OR of {@code a} and {@code b}. */
    public static BloomFilter or(final BloomFilter a, final BloomFilter b) {
        final BloomFilter result = new BloomFilter(a.bits, a.hashFunctions);
        for (int i = 0; i < a.words.length; i++) {
            result.words[i] = a.words[i] | b.words[i];
        }
        return result;
    }

    /** Returns a lowercase hex string of length {@code bits/4}. */
    public String toHex() {
        final StringBuilder sb = new StringBuilder(words.length * 16);
        for (final long l : words) {
            sb.append(String.format("%016x", l));
        }
        return sb.toString();
    }

    public int getBits() {
        return bits;
    }

    public int getHashFunctions() {
        return hashFunctions;
    }

    // -------------------------------------------------------------------------
    // MurmurHash3_x64_128, seed 0 — public domain, Austin Appleby
    // https://github.com/aappleby/smhasher
    // -------------------------------------------------------------------------

    private static long[] murmur3_128(final byte[] data, final int offset, final int len) {
        long h1 = 0L, h2 = 0L;

        final int nblocks = len >>> 4;
        for (int i = 0; i < nblocks; i++) {
            long k1 = readLE64(data, offset + (i << 4));
            long k2 = readLE64(data, offset + (i << 4) + 8);

            k1 *= MURMUR_C1; k1 = Long.rotateLeft(k1, 31); k1 *= MURMUR_C2; h1 ^= k1;
            h1 = Long.rotateLeft(h1, 27); h1 += h2; h1 = h1 * 5 + 0x52dce729L;

            k2 *= MURMUR_C2; k2 = Long.rotateLeft(k2, 33); k2 *= MURMUR_C1; h2 ^= k2;
            h2 = Long.rotateLeft(h2, 31); h2 += h1; h2 = h2 * 5 + 0x38495ab5L;
        }

        long k1 = 0, k2 = 0;
        final int tail = offset + (nblocks << 4);
        switch (len & 15) {
            case 15: k2 ^= ((long) (data[tail + 14] & 0xff)) << 48;
            case 14: k2 ^= ((long) (data[tail + 13] & 0xff)) << 40;
            case 13: k2 ^= ((long) (data[tail + 12] & 0xff)) << 32;
            case 12: k2 ^= ((long) (data[tail + 11] & 0xff)) << 24;
            case 11: k2 ^= ((long) (data[tail + 10] & 0xff)) << 16;
            case 10: k2 ^= ((long) (data[tail +  9] & 0xff)) <<  8;
            case  9: k2 ^= ((long) (data[tail +  8] & 0xff));
                     k2 *= MURMUR_C2; k2 = Long.rotateLeft(k2, 33); k2 *= MURMUR_C1; h2 ^= k2;
            case  8: k1 ^= ((long) (data[tail +  7] & 0xff)) << 56;
            case  7: k1 ^= ((long) (data[tail +  6] & 0xff)) << 48;
            case  6: k1 ^= ((long) (data[tail +  5] & 0xff)) << 40;
            case  5: k1 ^= ((long) (data[tail +  4] & 0xff)) << 32;
            case  4: k1 ^= ((long) (data[tail +  3] & 0xff)) << 24;
            case  3: k1 ^= ((long) (data[tail +  2] & 0xff)) << 16;
            case  2: k1 ^= ((long) (data[tail +  1] & 0xff)) <<  8;
            case  1: k1 ^= ((long) (data[tail]      & 0xff));
                     k1 *= MURMUR_C1; k1 = Long.rotateLeft(k1, 31); k1 *= MURMUR_C2; h1 ^= k1;
            default: break;
        }

        h1 ^= len; h2 ^= len;
        h1 += h2;  h2 += h1;
        h1 = fmix64(h1); h2 = fmix64(h2);
        h1 += h2;  h2 += h1;
        return new long[]{h1, h2};
    }

    private static long fmix64(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }

    private static long readLE64(final byte[] data, final int offset) {
        return   ((long) (data[offset]     & 0xff))
               | ((long) (data[offset + 1] & 0xff) <<  8)
               | ((long) (data[offset + 2] & 0xff) << 16)
               | ((long) (data[offset + 3] & 0xff) << 24)
               | ((long) (data[offset + 4] & 0xff) << 32)
               | ((long) (data[offset + 5] & 0xff) << 40)
               | ((long) (data[offset + 6] & 0xff) << 48)
               | ((long) (data[offset + 7] & 0xff) << 56);
    }
}
