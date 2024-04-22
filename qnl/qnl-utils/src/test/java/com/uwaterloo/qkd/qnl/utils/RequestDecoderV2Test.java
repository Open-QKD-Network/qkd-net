package com.uwaterloo.qkd.qnl.utils;

import com.uwaterloo.qkd.qnl.utils.QNLConstants;
import com.uwaterloo.qkd.qnl.utils.QNLRequest;
import com.uwaterloo.qkd.qnl.utils.RequestDecoderV2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

import org.junit.Test;
import static org.junit.Assert.*;

import java.nio.charset.Charset;

public class RequestDecoderV2Test {
    @Test
    public void testReqGetAllocKpBlock() {
        // Step 1: create ByteBuf to decode
        ByteBuf buf = Unpooled.buffer();
        
        // REQ_GET_ALLOC_KP_BLOCK has: frameSz, opId, srcSite, destSiteIdInBytes
        Charset utf8 = Charset.forName("UTF-8");
        int frameSz = 0;
        short opID = QNLConstants.REQ_GET_ALLOC_KP_BLOCK;
        String srcSiteId = "alpha";
        byte[] srcSiteIdInBytes = srcSiteId.getBytes(utf8);
        String destSiteId = "beta";
        byte[] destSiteIdInBytes = destSiteId.getBytes(utf8);
        
        frameSz = (Integer.BYTES 
                    + Short.BYTES 
                    + Short.BYTES   // short for encoding length of srcSiteIdInBytes
                    + srcSiteIdInBytes.length
                    + Short.BYTES  // short for encoding length of destSiteIdInBytes
                    + destSiteIdInBytes.length);
        
        buf.writeInt(frameSz);
        buf.writeShort(opID);
        buf.writeShort(srcSiteIdInBytes.length);
        buf.writeBytes(srcSiteIdInBytes, 0, srcSiteIdInBytes.length);
        buf.writeShort(destSiteIdInBytes.length);
        buf.writeBytes(destSiteIdInBytes);
        
        ByteBuf input = buf.duplicate();
        // step 2: create channel & write inbound
            // used kpBlockByteSz = 0 because this msg type does not use a kpBlockByteSz in the first place.
        EmbeddedChannel channel = new EmbeddedChannel(new RequestDecoderV2(0));
        assertTrue(channel.writeInbound(input.retain()));
        assertTrue(channel.finish());
        
        // read messages
        QNLRequest readRequest = (QNLRequest) channel.readInbound();
        assertEquals(readRequest.getOpId(), opID);
        assertEquals(readRequest.getSrcSiteId(), srcSiteId);
        assertEquals(readRequest.getDstSiteId(), destSiteId);

        // release resources
        
    }
}
