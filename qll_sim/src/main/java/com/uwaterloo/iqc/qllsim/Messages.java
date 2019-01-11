package com.uwaterloo.iqc.qllsim;

import java.io.Serializable;

public class Messages {
    public static class KeyGenReq implements Serializable {
		private static final long serialVersionUID = -8528312310961821294L;
		private String siteId;
        
        public KeyGenReq(String id) {
            this.siteId = id;
        }

        public String getId() {
            return siteId;
        }
    }

    public static class KeyGenResp implements Serializable {
		private static final long serialVersionUID = 389925866578761201L;
		private String siteId;
        private String digest;
        private long cnt;
        
        public KeyGenResp(String siteId, String digest, long cnt) {
            this.siteId = siteId;
            this.digest = digest;
            this.cnt = cnt;
        }

        public String getId() {
            return siteId;
        }
        
        public String getDigest() {
            return digest;
        }
        
        public long getCnt() {
            return cnt;
        }  
    }
}
