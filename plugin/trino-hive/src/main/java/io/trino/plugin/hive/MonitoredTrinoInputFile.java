/*
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
package io.trino.plugin.hive;

import io.trino.filesystem.TrinoInput;
import io.trino.filesystem.TrinoInputFile;
import org.apache.iceberg.io.SeekableInputStream;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

public class MonitoredTrinoInputFile
        implements TrinoInputFile
{
    private final FileFormatDataSourceStats stats;
    private final TrinoInputFile delegate;

    public MonitoredTrinoInputFile(FileFormatDataSourceStats stats, TrinoInputFile delegate)
    {
        this.stats = requireNonNull(stats, "stats is null");
        this.delegate = requireNonNull(delegate, "delegate is null");
    }

    @Override
    public TrinoInput newInput()
            throws IOException
    {
        return new MonitoredTrinoInput(stats, delegate.newInput());
    }

    @Override
    public long length()
            throws IOException
    {
        return delegate.length();
    }

    @Override
    public long modificationTime()
            throws IOException
    {
        return delegate.modificationTime();
    }

    @Override
    public boolean exists()
            throws IOException
    {
        return delegate.exists();
    }

    @Override
    public String location()
    {
        return delegate.location();
    }

    @Override
    public String toString()
    {
        return delegate.toString();
    }

    private static final class MonitoredTrinoInput
            implements TrinoInput
    {
        private final FileFormatDataSourceStats stats;
        private final TrinoInput delegate;

        public MonitoredTrinoInput(FileFormatDataSourceStats stats, TrinoInput delegate)
        {
            this.stats = requireNonNull(stats, "stats is null");
            this.delegate = requireNonNull(delegate, "delegate is null");
        }

        @Override
        public SeekableInputStream inputStream()
        {
            return new MonitoredSeekableInputStream(stats, delegate.inputStream());
        }

        @Override
        public void readFully(long position, byte[] buffer, int bufferOffset, int bufferLength)
                throws IOException
        {
            long readStart = System.nanoTime();
            delegate.readFully(position, buffer, bufferOffset, bufferLength);
            stats.readDataBytesPerSecond(bufferLength, System.nanoTime() - readStart);
        }

        @Override
        public int readTail(byte[] buffer, int bufferOffset, int bufferLength)
                throws IOException
        {
            long readStart = System.nanoTime();
            int size = delegate.readTail(buffer, bufferOffset, bufferLength);
            stats.readDataBytesPerSecond(size, System.nanoTime() - readStart);
            return size;
        }

        @Override
        public void close()
                throws IOException
        {
            delegate.close();
        }
    }

    private static final class MonitoredSeekableInputStream
            extends SeekableInputStream
    {
        private final FileFormatDataSourceStats stats;
        private final SeekableInputStream delegate;

        public MonitoredSeekableInputStream(FileFormatDataSourceStats stats, SeekableInputStream delegate)
        {
            this.stats = requireNonNull(stats, "stats is null");
            this.delegate = requireNonNull(delegate, "delegate is null");
        }

        @Override
        public long getPos()
                throws IOException
        {
            return delegate.getPos();
        }

        @Override
        public void seek(long newPos)
                throws IOException
        {
            delegate.seek(newPos);
        }

        @Override
        public int read()
                throws IOException
        {
            long readStart = System.nanoTime();
            int value = delegate.read();
            stats.readDataBytesPerSecond(1, System.nanoTime() - readStart);
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len)
                throws IOException
        {
            long readStart = System.nanoTime();
            int size = delegate.read(b, off, len);
            stats.readDataBytesPerSecond(size, System.nanoTime() - readStart);
            return size;
        }

        @Override
        public long skip(long n)
                throws IOException
        {
            long readStart = System.nanoTime();
            long size = delegate.skip(n);
            stats.readDataBytesPerSecond(size, System.nanoTime() - readStart);
            return size;
        }

        @Override
        public void close()
                throws IOException
        {
            delegate.close();
        }
    }
}
