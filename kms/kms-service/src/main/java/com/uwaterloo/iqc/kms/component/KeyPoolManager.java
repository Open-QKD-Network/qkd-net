package com.uwaterloo.iqc.kms.component;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;

import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

@Configuration
@PropertySource(value = "file:${SITE_PROPERTIES}", ignoreResourceNotFound = true)
public class KeyPoolManager {

    class PoolLock {
        boolean inProgress;
        Lock lock;
    }
    private static final Logger logger = LoggerFactory.getLogger(KeyPoolManager.class);
    private ConcurrentMap<String, KeyPool> keyPools = new ConcurrentHashMap<>();
    private ConcurrentMap<String, PoolLock> poolLocks = new ConcurrentHashMap<>();
    private ReentrantLock initPoolLock=  new ReentrantLock();
    private SubnetSiteIDConfig ssidCfg;
    Map <String, SubnetUtils> subnetMap = new HashMap<String, SubnetUtils>();
    @Value("${kms.keys.bytesize}") private int byteSz;
    @Value("${kms.keys.blocksize}") private long blockSz;
    @Value("${kms.site.id}") private String localSiteId;
    @Value("${kms.keys.dir}") private String poolsDir;
    @Value("${kms.remote.subnet.siteid.prop.location}") private String subnet_site;
    @Value("${qnl.ip}") private String qnlIP;
    @Value("${qnl.port}") private int qnlPort;
    
    @Autowired	private QNLKeyReader keyReader; 

    @PostConstruct
    public void init() {
      try {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(subnet_site));
        ssidCfg = gson.fromJson(reader, SubnetSiteIDConfig.class);
        reader.close();
        for (String k : ssidCfg.remote_subnet_siteid.keySet()) {
        	subnetMap.put(k, new SubnetUtils(k));
        }
      } catch(Exception e) {
        e.printStackTrace();
      }
    }    
    
    @Bean
    public KeyPoolManager keyPoolMgr() {
        KeyPoolManager kp = new  KeyPoolManager();
        return  kp;
    }
    
    public String getSiteId() {
    	return localSiteId;
    }
    
    public String getPeerSiteID(String ip) {
    	for (Map.Entry<String, SubnetUtils> e: subnetMap.entrySet()) {
    		SubnetUtils s = e.getValue();
    		String mask = e.getKey();
    		boolean isOk = s.getInfo().isInRange(ip);
    		if (isOk) {
    			return ssidCfg.remote_subnet_siteid.get(mask);
    		}
    	}
    	return null;
    }

    public Key newKey(String siteId) {
        Key key = null;
        if (siteId == null)
            return null;
        try {
            key = fetchKey(siteId, null, -1L);
        } catch(Exception e) {
        }
        if (key == null)
            key = new Key();
        return key;
    }


    public Key getKey(String siteId, String block, long index) {
        Key key;
        if (siteId == null || block == null || index < 0)
            return null;
        try {
            key = fetchKey(siteId, block, index);
        } catch(Exception e) {
            key = new Key();
        }
        return key;
    }

    private Key fetchKey(String siteId, String inBlockId, long ind) throws Exception {
        String blockId = inBlockId;
        Key cipherKey = null;
        String srcSiteId;
        String dstSiteId;
        PoolLock poolLock;
        String poolName;
        int index = (int)ind;

        if (index < 0) {
            srcSiteId = localSiteId;
            dstSiteId = siteId;
        } else {
            srcSiteId = siteId;
            dstSiteId = localSiteId;
        }

        poolName = srcSiteId + dstSiteId;

        if (containsPool(poolName)) {
            cipherKey = key(poolName, index);
        } else if (containsPoolLock(poolName) &&
                   keyPoolLock(poolName).inProgress) {
            poolLock = keyPoolLock(poolName);
            synchronized(poolLock.lock) {
                try {
                    while (poolLock.inProgress) {
                        poolLock.lock.wait();
                    }
                } catch(Exception e) {
                	logger.error("Intention was to be notified ", e);
                } finally {}
            }
            cipherKey = key(poolName, index);
        } else {
            initPoolLock.lock();  // block until condition holds
            try {
                if (containsPool(poolName)) {
                    cipherKey = key(poolName, index);
                    initPoolLock.unlock();
                } else if (containsPoolLock(poolName) &&
                           keyPoolLock(poolName).inProgress) {
                    poolLock = keyPoolLock(poolName);
                    synchronized(poolLock.lock) {
                        initPoolLock.unlock();
                        try {
                            while (poolLock.inProgress) {
                                poolLock.lock.wait();
                            }
                        } catch(Exception e) {
                        	logger.error("Intention was to be notified while waiting for the key block ", e);
                        } finally {}
                    }
                    cipherKey = key(poolName, index);
                } else {
                    if (containsPoolLock(poolName)) {
                        poolLock = keyPoolLock(poolName);
                        poolLock.inProgress = true;
                        initPoolLock.unlock();
                    } else {
                        poolLock = new PoolLock();
                        poolLock.inProgress = true;
                        poolLock.lock = new ReentrantLock();
                        poolLocks.put(poolName, poolLock);
                        initPoolLock.unlock();
                    }
                    //do the fetching work
                    Vector<String> keyBlockDst = new Vector<>((int)blockSz);
                    try {
                        if (blockId == null && index == -1L)
                            blockId = keyReader.read(localSiteId,
                                                     siteId, keyBlockDst,
                                                     qnlIP, qnlPort, poolsDir, (int)blockSz, byteSz);
                        else
                            keyReader.read(siteId, localSiteId,
                                           blockId, keyBlockDst, poolsDir, blockSz);
                    } catch(Exception e) {
                        logger.error("Problem occurred while fetching key block ", e);
                        throw e;
                    }

                    keyPools.remove(poolName);
                    KeyPool kp = new KeyPool(blockId, blockSz, keyBlockDst);
                    keyPools.put(poolName, kp);

                    synchronized(poolLock.lock) {
                        poolLock.inProgress = false;
                        poolLock.lock.notifyAll();
                    }
                    cipherKey = key(poolName, index);
                }
            } catch(Exception e) {
            	logger.error("Problem occurred while fetching key block ", e);
            }	 finally {
            }
        }
        return cipherKey;
    }

    private boolean containsPool(String poolName)  {
        boolean isPool = keyPools.containsKey(poolName);
        boolean validKeys = false;
        KeyPool kp;

        if (isPool) {
            kp = keyPools.get(poolName);
            validKeys = kp.isValid();
        }
        return (isPool && validKeys);
    }

    private Key key(String poolName, int index) {
        if (index < 0)
            return keyPools.get(poolName).getKey();
        else
            return keyPools.get(poolName).getKey(index);
    }

    private boolean containsPoolLock(String poolName) {
        return poolLocks.containsKey(poolName);
    }


    private PoolLock keyPoolLock(String poolName) {
        return poolLocks.get(poolName);
    }


    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
