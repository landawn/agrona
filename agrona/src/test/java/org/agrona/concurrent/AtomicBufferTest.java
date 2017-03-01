/*
 * Copyright 2014-2017 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona.concurrent;

import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.agrona.BitUtil;
import org.agrona.DirectBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.agrona.BufferUtil.array;
import static org.agrona.BufferUtil.arrayOffset;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Theories.class)
public class AtomicBufferTest
{
    private static final ByteOrder BYTE_ORDER = ByteOrder.nativeOrder();
    private static final int BUFFER_CAPACITY = 4096;
    private static final int INDEX = 8;

    private static final byte BYTE_VALUE = 1;
    private static final short SHORT_VALUE = Byte.MAX_VALUE + 2;
    private static final char CHAR_VALUE = '8';
    private static final int INT_VALUE = Short.MAX_VALUE + 3;
    private static final float FLOAT_VALUE = Short.MAX_VALUE + 4.0f;
    private static final long LONG_VALUE = Integer.MAX_VALUE + 5L;
    private static final double DOUBLE_VALUE = Integer.MAX_VALUE + 7.0d;

    @DataPoint
    public static final AtomicBuffer BYTE_ARRAY_BACKED =
        new UnsafeBuffer(new byte[BUFFER_CAPACITY], 0, BUFFER_CAPACITY);

    @DataPoint
    public static final AtomicBuffer HEAP_BYTE_BUFFER = new UnsafeBuffer(
        ByteBuffer.allocate(BUFFER_CAPACITY), 0, BUFFER_CAPACITY);

    @DataPoint
    public static final AtomicBuffer DIRECT_BYTE_BUFFER = new UnsafeBuffer(
        ByteBuffer.allocateDirect(BUFFER_CAPACITY), 0, BUFFER_CAPACITY);

    @DataPoint
    public static final AtomicBuffer HEAP_BYTE_BUFFER_SLICE = new UnsafeBuffer(
        ((ByteBuffer)(ByteBuffer.allocate(BUFFER_CAPACITY * 2).position(BUFFER_CAPACITY))).slice());

    @DataPoint
    public static final AtomicBuffer HEAP_BYTE_BUFFER_READ_ONLY = new UnsafeBuffer(
        ByteBuffer.allocate(BUFFER_CAPACITY).asReadOnlyBuffer(), 0, BUFFER_CAPACITY);

    @Theory
    public void shouldGetCapacity(final AtomicBuffer buffer)
    {
        assertThat(buffer.capacity(), is(BUFFER_CAPACITY));
    }

    @Theory
    @Test(expected = IndexOutOfBoundsException.class)
    public void shouldThrowExceptionForAboveCapacity(final AtomicBuffer buffer)
    {
        final int index = BUFFER_CAPACITY + 1;
        buffer.checkLimit(index);
    }

    @Theory
    @Test(expected = IndexOutOfBoundsException.class)
    public void shouldThrowExceptionWhenOutOfBounds(final AtomicBuffer buffer)
    {
        final int index = BUFFER_CAPACITY;
        buffer.getByte(index);
    }

    @Test
    public void sharedBuffer()
    {
        final ByteBuffer bb = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer ub1 = new UnsafeBuffer(bb, 0, 512);
        final UnsafeBuffer ub2 = new UnsafeBuffer(bb, 512, 512);
        ub1.putLong(INDEX, LONG_VALUE);
        ub2.putLong(INDEX, 9876543210L);

        assertThat(ub1.getLong(INDEX), is(LONG_VALUE));
    }

    @Test
    public void shouldVerifyBufferAlignment()
    {
        final AtomicBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(1024));
        try
        {
            buffer.verifyAlignment();
        }
        catch (final IllegalStateException ex)
        {
            fail("All buffers should be aligned " + ex);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenBufferNotAligned()
    {
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        byteBuffer.position(1);
        final UnsafeBuffer buffer = new UnsafeBuffer(byteBuffer.slice());

        buffer.verifyAlignment();
    }

    @Theory
    public void shouldCopyMemory(final AtomicBuffer buffer)
    {
        final byte[] testBytes = "xxxxxxxxxxx".getBytes();

        buffer.setMemory(0, testBytes.length, (byte)'x');

        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        final byte[] buff = new byte[testBytes.length];
        duplicateBuffer.get(buff);

        assertThat(buff, is(testBytes));
    }

    @Theory
    public void shouldGetLongFromBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        duplicateBuffer.putLong(INDEX, LONG_VALUE);

        assertThat(buffer.getLong(INDEX, BYTE_ORDER), is(LONG_VALUE));
    }

    @Theory
    public void shouldPutLongToBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        buffer.putLong(INDEX, LONG_VALUE, BYTE_ORDER);

        assertThat(duplicateBuffer.getLong(INDEX), is(LONG_VALUE));
    }

    @Theory
    public void shouldGetLongFromNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putLong(INDEX, LONG_VALUE);

        assertThat(buffer.getLong(INDEX), is(LONG_VALUE));
    }

    @Theory
    public void shouldPutLongToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        buffer.putLong(INDEX, LONG_VALUE);

        assertThat(duplicateBuffer.getLong(INDEX), is(LONG_VALUE));
    }

    @Theory
    public void shouldGetLongVolatileFromNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putLong(INDEX, LONG_VALUE);

        assertThat(buffer.getLongVolatile(INDEX), is(LONG_VALUE));
    }

    @Theory
    public void shouldPutLongVolatileToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        buffer.putLongVolatile(INDEX, LONG_VALUE);

        assertThat(duplicateBuffer.getLong(INDEX), is(LONG_VALUE));
    }

    @Theory
    public void shouldPutLongOrderedToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        buffer.putLongOrdered(INDEX, LONG_VALUE);

        assertThat(duplicateBuffer.getLong(INDEX), is(LONG_VALUE));
    }

    @Theory
    public void shouldAddLongOrderedToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        final long initialValue = Integer.MAX_VALUE + 7L;
        final long increment = 9L;
        buffer.putLongOrdered(INDEX, initialValue);
        buffer.addLongOrdered(INDEX, increment);

        assertThat(duplicateBuffer.getLong(INDEX), is(initialValue + increment));
    }

    @Theory
    public void shouldCompareAndSetLongToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putLong(INDEX, LONG_VALUE);

        assertTrue(buffer.compareAndSetLong(INDEX, LONG_VALUE, LONG_VALUE + 1));

        assertThat(duplicateBuffer.getLong(INDEX), is(LONG_VALUE + 1));
    }

    @Theory
    public void shouldGetAndSetLongToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putLong(INDEX, LONG_VALUE);

        final long afterValue = 1;
        final long beforeValue = buffer.getAndSetLong(INDEX, afterValue);

        assertThat(beforeValue, is(LONG_VALUE));
        assertThat(duplicateBuffer.getLong(INDEX), is(afterValue));
    }

    @Theory
    public void shouldGetAndAddLongToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putLong(INDEX, LONG_VALUE);

        final long delta = 1;
        final long beforeValue = buffer.getAndAddLong(INDEX, delta);

        assertThat(beforeValue, is(LONG_VALUE));
        assertThat(duplicateBuffer.getLong(INDEX), is(LONG_VALUE + delta));
    }

    @Theory
    public void shouldGetIntFromBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        duplicateBuffer.putInt(INDEX, INT_VALUE);

        assertThat(buffer.getInt(INDEX, BYTE_ORDER), is(INT_VALUE));
    }

    @Theory
    public void shouldPutIntToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        buffer.putInt(INDEX, INT_VALUE);

        assertThat(duplicateBuffer.getInt(INDEX), is(INT_VALUE));
    }

    @Theory
    public void shouldGetIntFromNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putInt(INDEX, INT_VALUE);

        assertThat(buffer.getInt(INDEX), is(INT_VALUE));
    }

    @Theory
    public void shouldPutIntToBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        buffer.putInt(INDEX, INT_VALUE, BYTE_ORDER);

        assertThat(duplicateBuffer.getInt(INDEX), is(INT_VALUE));
    }

    @Theory
    public void shouldGetIntVolatileFromNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putInt(INDEX, INT_VALUE);

        assertThat(buffer.getIntVolatile(INDEX), is(INT_VALUE));
    }

    @Theory
    public void shouldPutIntVolatileToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        buffer.putIntVolatile(INDEX, INT_VALUE);

        assertThat(duplicateBuffer.getInt(INDEX), is(INT_VALUE));
    }

    @Theory
    public void shouldPutIntOrderedToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        buffer.putIntOrdered(INDEX, INT_VALUE);

        assertThat(duplicateBuffer.getInt(INDEX), is(INT_VALUE));
    }

    @Theory
    public void shouldAddIntOrderedToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        final int initialValue = 7;
        final int increment = 9;
        buffer.putIntOrdered(INDEX, initialValue);
        buffer.addIntOrdered(INDEX, increment);

        assertThat(duplicateBuffer.getInt(INDEX), is(initialValue + increment));
    }

    @Theory
    public void shouldCompareAndSetIntToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putInt(INDEX, INT_VALUE);

        assertTrue(buffer.compareAndSetInt(INDEX, INT_VALUE, INT_VALUE + 1));

        assertThat(duplicateBuffer.getInt(INDEX), is(INT_VALUE + 1));
    }

    @Theory
    public void shouldGetAndSetIntToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putInt(INDEX, INT_VALUE);

        final int afterValue = 1;
        final int beforeValue = buffer.getAndSetInt(INDEX, afterValue);

        assertThat(beforeValue, is(INT_VALUE));
        assertThat(duplicateBuffer.getInt(INDEX), is(afterValue));
    }

    @Theory
    public void shouldGetAndAddIntToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putInt(INDEX, INT_VALUE);

        final int delta = 1;
        final int beforeValue = buffer.getAndAddInt(INDEX, delta);

        assertThat(beforeValue, is(INT_VALUE));
        assertThat(duplicateBuffer.getInt(INDEX), is(INT_VALUE + delta));
    }

    @Theory
    public void shouldGetShortFromBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        duplicateBuffer.putShort(INDEX, SHORT_VALUE);

        assertThat(buffer.getShort(INDEX, BYTE_ORDER), is(SHORT_VALUE));
    }

    @Theory
    public void shouldPutShortToBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        buffer.putShort(INDEX, SHORT_VALUE, BYTE_ORDER);

        assertThat(duplicateBuffer.getShort(INDEX), is(SHORT_VALUE));
    }

    @Theory
    public void shouldGetShortFromNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putShort(INDEX, SHORT_VALUE);

        assertThat(buffer.getShort(INDEX), is(SHORT_VALUE));
    }

    @Theory
    public void shouldPutShortToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        buffer.putShort(INDEX, SHORT_VALUE);

        assertThat(duplicateBuffer.getShort(INDEX), is(SHORT_VALUE));
    }

    @Theory
    public void shouldGetShortVolatileFromNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putShort(INDEX, SHORT_VALUE);

        assertThat(buffer.getShortVolatile(INDEX), is(SHORT_VALUE));
    }

    @Theory
    public void shouldPutShortVolatileToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        buffer.putShortVolatile(INDEX, SHORT_VALUE);

        assertThat(duplicateBuffer.getShort(INDEX), is(SHORT_VALUE));
    }

    @Theory
    public void shouldGetCharFromBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        duplicateBuffer.putChar(INDEX, CHAR_VALUE);

        assertThat(buffer.getChar(INDEX, BYTE_ORDER), is(CHAR_VALUE));
    }

    @Theory
    public void shouldPutCharToBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        buffer.putChar(INDEX, CHAR_VALUE, BYTE_ORDER);

        assertThat(duplicateBuffer.getChar(INDEX), is(CHAR_VALUE));
    }

    @Theory
    public void shouldGetCharFromNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putChar(INDEX, CHAR_VALUE);

        assertThat(buffer.getChar(INDEX), is(CHAR_VALUE));
    }

    @Theory
    public void shouldPutCharToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        buffer.putChar(INDEX, CHAR_VALUE);

        assertThat(duplicateBuffer.getChar(INDEX), is(CHAR_VALUE));
    }

    @Theory
    public void shouldGetCharVolatileFromNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putChar(INDEX, CHAR_VALUE);

        assertThat(buffer.getCharVolatile(INDEX), is(CHAR_VALUE));
    }

    @Theory
    public void shouldPutCharVolatileToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        buffer.putCharVolatile(INDEX, CHAR_VALUE);

        assertThat(duplicateBuffer.getChar(INDEX), is(CHAR_VALUE));
    }

    @Theory
    public void shouldGetDoubleFromBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        duplicateBuffer.putDouble(INDEX, DOUBLE_VALUE);

        assertThat(buffer.getDouble(INDEX, BYTE_ORDER), is(DOUBLE_VALUE));
    }

    @Theory
    public void shouldPutDoubleToBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        buffer.putDouble(INDEX, DOUBLE_VALUE, BYTE_ORDER);

        assertThat(duplicateBuffer.getDouble(INDEX), is(DOUBLE_VALUE));
    }

    @Theory
    public void shouldGetDoubleFromNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putDouble(INDEX, DOUBLE_VALUE);

        assertThat(buffer.getDouble(INDEX), is(DOUBLE_VALUE));
    }

    @Theory
    public void shouldPutDoubleToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        buffer.putDouble(INDEX, DOUBLE_VALUE);

        assertThat(duplicateBuffer.getDouble(INDEX), is(DOUBLE_VALUE));
    }

    @Theory
    public void shouldGetFloatFromBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        duplicateBuffer.putFloat(INDEX, FLOAT_VALUE);

        assertThat(buffer.getFloat(INDEX, BYTE_ORDER), is(FLOAT_VALUE));
    }

    @Theory
    public void shouldPutFloatToBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        buffer.putFloat(INDEX, FLOAT_VALUE, BYTE_ORDER);

        assertThat(duplicateBuffer.getFloat(INDEX), is(FLOAT_VALUE));
    }

    @Theory
    public void shouldGetFloatFromNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        duplicateBuffer.putFloat(INDEX, FLOAT_VALUE);

        assertThat(buffer.getFloat(INDEX), is(FLOAT_VALUE));
    }

    @Theory
    public void shouldPutFloatToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.order(ByteOrder.nativeOrder());

        buffer.putFloat(INDEX, FLOAT_VALUE);

        assertThat(duplicateBuffer.getFloat(INDEX), is(FLOAT_VALUE));
    }

    @Theory
    public void shouldGetByteFromBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        duplicateBuffer.put(INDEX, BYTE_VALUE);

        assertThat(buffer.getByte(INDEX), is(BYTE_VALUE));
    }

    @Theory
    public void shouldPutByteToBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        buffer.putByte(INDEX, BYTE_VALUE);

        assertThat(duplicateBuffer.get(INDEX), is(BYTE_VALUE));
    }

    @Theory
    public void shouldGetByteVolatileFromBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        duplicateBuffer.put(INDEX, BYTE_VALUE);

        assertThat(buffer.getByteVolatile(INDEX), is(BYTE_VALUE));
    }

    @Theory
    public void shouldPutByteVolatileToBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = byteBuffer(buffer);

        buffer.putByteVolatile(INDEX, BYTE_VALUE);

        assertThat(duplicateBuffer.get(INDEX), is(BYTE_VALUE));
    }

    @Theory
    public void shouldGetByteArrayFromBuffer(final AtomicBuffer buffer)
    {
        final byte[] testArray = {'H', 'e', 'l', 'l', 'o'};

        int i = INDEX;
        for (final byte v : testArray)
        {
            buffer.putByte(i, v);
            i += BitUtil.SIZE_OF_BYTE;
        }

        final byte[] result = new byte[testArray.length];
        buffer.getBytes(INDEX, result);

        assertThat(result, is(testArray));
    }

    @Theory
    public void shouldGetBytesFromBuffer(final AtomicBuffer buffer)
    {
        final byte[] testBytes = "Hello World".getBytes();

        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.position(INDEX);
        duplicateBuffer.put(testBytes);

        final byte[] buff = new byte[testBytes.length];
        buffer.getBytes(INDEX, buff);

        assertThat(buff, is(testBytes));
    }

    @Theory
    public void shouldGetBytesFromBufferToBuffer(final AtomicBuffer buffer)
    {
        final byte[] testBytes = "Hello World".getBytes();

        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.position(INDEX);
        duplicateBuffer.put(testBytes);

        final ByteBuffer dstBuffer = ByteBuffer.allocate(testBytes.length);
        buffer.getBytes(INDEX, dstBuffer, testBytes.length);

        assertThat(dstBuffer.array(), is(testBytes));
    }

    @Theory
    public void shouldGetBytesFromBufferToAtomicBuffer(final AtomicBuffer buffer)
    {
        final byte[] testBytes = "Hello World".getBytes();

        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.position(INDEX);
        duplicateBuffer.put(testBytes);

        final ByteBuffer dstBuffer = ByteBuffer.allocateDirect(testBytes.length);
        buffer.getBytes(INDEX, dstBuffer, testBytes.length);

        dstBuffer.flip();
        final byte[] result = new byte[testBytes.length];
        dstBuffer.get(result);

        assertThat(result, is(testBytes));
    }

    @Theory
    public void shouldGetBytesFromBufferToSlice(final AtomicBuffer buffer)
    {
        final byte[] testBytes = "Hello World".getBytes();

        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.position(INDEX);
        duplicateBuffer.put(testBytes);

        final ByteBuffer dstBuffer =
            ((ByteBuffer)ByteBuffer.allocate(testBytes.length * 2).position(testBytes.length)).slice();

        buffer.getBytes(INDEX, dstBuffer, testBytes.length);

        dstBuffer.flip();
        final byte[] result = new byte[testBytes.length];
        dstBuffer.get(result);

        assertThat(result, is(testBytes));
    }

    @Theory
    public void shouldPutBytesToBuffer(final AtomicBuffer buffer)
    {
        final byte[] testBytes = "Hello World".getBytes();
        buffer.putBytes(INDEX, testBytes);

        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.position(INDEX);

        final byte[] buff = new byte[testBytes.length];
        duplicateBuffer.get(buff);

        assertThat(buff, is(testBytes));
    }

    @Theory
    public void shouldPutBytesToBufferFromBuffer(final AtomicBuffer buffer)
    {
        final byte[] testBytes = "Hello World".getBytes();
        final ByteBuffer srcBuffer = ByteBuffer.wrap(testBytes);

        buffer.putBytes(INDEX, srcBuffer, testBytes.length);

        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.position(INDEX);

        final byte[] buff = new byte[testBytes.length];
        duplicateBuffer.get(buff);

        assertThat(buff, is(testBytes));
    }

    @Theory
    public void shouldPutBytesToBufferFromAtomicBuffer(final AtomicBuffer buffer)
    {
        final byte[] testBytes = "Hello World".getBytes();
        final ByteBuffer srcBuffer = ByteBuffer.allocateDirect(testBytes.length);
        srcBuffer.put(testBytes).flip();

        buffer.putBytes(INDEX, srcBuffer, testBytes.length);

        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.position(INDEX);

        final byte[] buff = new byte[testBytes.length];
        duplicateBuffer.get(buff);

        assertThat(buff, is(testBytes));
    }

    @Theory
    public void shouldPutBytesToBufferFromSlice(final AtomicBuffer buffer)
    {
        final byte[] testBytes = "Hello World".getBytes();
        final ByteBuffer srcBuffer =
            ((ByteBuffer)ByteBuffer.allocate(testBytes.length * 2).position(testBytes.length)).slice();
        srcBuffer.put(testBytes).flip();

        buffer.putBytes(INDEX, srcBuffer, testBytes.length);

        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.position(INDEX);

        final byte[] buff = new byte[testBytes.length];
        duplicateBuffer.get(buff);

        assertThat(buff, is(testBytes));
    }

    @Theory
    public void shouldPutBytesToAtomicBufferFromAtomicBuffer(final AtomicBuffer buffer)
    {
        final byte[] testBytes = "Hello World".getBytes();
        final ByteBuffer srcBuffer = ByteBuffer.allocateDirect(testBytes.length);
        srcBuffer.put(testBytes).flip();

        final UnsafeBuffer srcUnsafeBuffer = new UnsafeBuffer(srcBuffer);

        buffer.putBytes(INDEX, srcUnsafeBuffer, 0, testBytes.length);

        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.position(INDEX);

        final byte[] buff = new byte[testBytes.length];
        duplicateBuffer.get(buff);

        assertThat(buff, is(testBytes));
    }

    @Theory
    public void shouldGetBytesIntoAtomicBufferFromAtomicBuffer(final AtomicBuffer buffer)
    {
        final byte[] testBytes = "Hello World".getBytes();
        final ByteBuffer srcBuffer = ByteBuffer.allocateDirect(testBytes.length);
        srcBuffer.put(testBytes).flip();

        final UnsafeBuffer srcUnsafeBuffer = new UnsafeBuffer(srcBuffer);

        srcUnsafeBuffer.getBytes(0, buffer, INDEX, testBytes.length);

        final ByteBuffer duplicateBuffer = byteBuffer(buffer);
        duplicateBuffer.position(INDEX);

        final byte[] buff = new byte[testBytes.length];
        duplicateBuffer.get(buff);

        assertThat(buff, is(testBytes));
    }

    private static ByteBuffer byteBuffer(final DirectBuffer buffer)
    {
        ByteBuffer byteBuffer;

        final ByteBuffer bb = buffer.byteBuffer();
        if (null != bb)
        {
            if (bb.isDirect())
            {
                byteBuffer = bb.duplicate();
            }
            else
            {
                final byte[] array = array(bb);
                final int offset = arrayOffset(bb);
                final int capacity = buffer.capacity();

                byteBuffer = ByteBuffer.wrap(array);
                if (offset > 0)
                {
                    byteBuffer.limit(offset + capacity);
                    byteBuffer.position(offset);
                    byteBuffer = byteBuffer.slice();
                }
            }
        }
        else
        {
            byteBuffer = ByteBuffer.wrap(buffer.byteArray());
        }

        byteBuffer.order(BYTE_ORDER);
        byteBuffer.clear();

        return byteBuffer;
    }
}
