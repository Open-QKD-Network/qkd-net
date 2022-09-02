package com.uwaterloo.iqc.qnl.qll;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicLong;

public interface QLLReader {
    /**
     * Get the next available key ID for a key of the given length. The key data is stored locally
     * until read() is called with the same identifier.
     * @param len Length of key in bytes.
     * @return Identifier of the key that is shared on the QLL.
     */
    String getNextKeyId(int len);

    /**
     * Get the next available key for a key of the given length.
     * @param dst Buffer to write key data into. Must be exactly len bytes
     * @param len Length of the key to retrieve in bytes.
     * @return Identifier of the key that is shared on the QLL.
     */
    String readNextKey(byte[] dst, int len);

    // read key with given ID

    /**
     * Get key data of a key that has previously been allocated using getNextKeyId() or shared via the QLL.
     * @param identifier Identifier of the key that is shared on the QLL.
     * @return Raw key data.
     */
    byte[] read(String identifier);
}
