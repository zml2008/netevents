/**
 * Copyright (C) 2014 zml (netevents@zachsthings.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zachsthings.netevents.sec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;

/**
 * Socket wrapper that provides an encrypted channel
 */
public class AESSocketWrapper implements SocketWrapper {
    private static final byte[] SALT = new byte[]{8, 12, 16, 84, 98, 93, 92, 23, 38, 3};
    private static final int ITER_COUNT = 1024, KEY_LEN = 128;
    private final String passphrase;


    public AESSocketWrapper(String passphrase) {
        this.passphrase = passphrase;
    }

    @Override
    public SocketChannel wrapSocket(SocketChannel chan) throws IOException {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), SALT, ITER_COUNT, KEY_LEN);
            SecretKey secretKey = factory.generateSecret(spec);
            Key key = new SecretKeySpec(secretKey.getEncoded(), "AES");
            AlgorithmParameters params = AlgorithmParameters.getInstance("AES");
            params.init(new IvParameterSpec(new byte[16]));
            return new CryptSocketChannel(chan, key, params);
        } catch (NoSuchAlgorithmException
                | InvalidKeySpecException
                | NoSuchPaddingException
                | InvalidKeyException
                | InvalidAlgorithmParameterException
                | InvalidParameterSpecException e) {
            throw new IOException(e);
        }
    }

    private static class CryptSocketChannel extends WrappedSocketChannel {
        private final Object readLock = new Object(),
                writeLock = new Object();
        private ByteBuffer readTmp;
        private final Cipher enc, dec;

        public CryptSocketChannel(SocketChannel wrappee, Key k, AlgorithmParameters params) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
            super(wrappee);
            enc = Cipher.getInstance("AES/CBC/PKCS5Padding");
            enc.init(Cipher.ENCRYPT_MODE, k, params);
            dec = Cipher.getInstance("AES/CBC/PKCS5Padding");
            dec.init(Cipher.DECRYPT_MODE, k, params);
        }

        /**
         * Only call under readLock
         *
         * @param capacity required capacity
         * @return adjusted readTmp buffer
         */
        private ByteBuffer adjustReadTmp(int capacity) {
            if (readTmp == null || readTmp.capacity() < capacity
                            || (capacity > 16 && readTmp.capacity() > (4 * capacity))) { // If it's not a small buffer and readTmp is pretty large, lets shrink it
                        readTmp = ByteBuffer.allocate(capacity);
                    }
            return readTmp;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int startIdx = dst.position();
            ByteBuffer src = ByteBuffer.allocate(paddedLen(dst));
            int read = super.read(src);
            if (read <= 0) { // Nothing or closed channel
                return read;
            }

            src.flip();
            synchronized (readLock) {
                try {
                    adjustReadTmp(src.capacity());

                    try {
                        dec.doFinal(src, readTmp);
                    } catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
                        // If one of these happens, it's probably an incorrect passphrase
                        throw new IOException("Invalid data received from remote! Do passphrases match?", e);
                    }
                    readTmp.flip();
                    dst.put(readTmp);
                } finally {
                    if (readTmp != null) {
                        readTmp.clear();
                    }
                }
            }
            return dst.position() - startIdx;
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
            //return super.read(dsts, offset, length);
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            ByteBuffer dst = ByteBuffer.allocate(enc.getOutputSize(src.limit()));
            try {
                synchronized (writeLock) {
                    enc.doFinal(src, dst);
                }
            } catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
                throw new IOException(e);
            }
            dst.flip();
            return super.write(dst);
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
            //return super.write(srcs, offset, length);
        }

        private static int paddedLen(ByteBuffer test) {
            return test.capacity() + (16 - test.capacity() % 16);
        }
    }

}
