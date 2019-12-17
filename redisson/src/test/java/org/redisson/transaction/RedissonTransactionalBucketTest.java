package org.redisson.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.redisson.BaseTest;
import org.redisson.api.RBucket;
import org.redisson.api.RTransaction;
import org.redisson.api.TransactionOptions;

public class RedissonTransactionalBucketTest extends BaseTest {

    @Test
    public void testTimeout() throws InterruptedException {
        RBucket<String> b = redisson.getBucket("test");
        b.set("123");
        
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults().timeout(3, TimeUnit.SECONDS));
        RBucket<String> bucket = transaction.getBucket("test");
        bucket.set("234");
        
        Thread.sleep(3000);
        
        try {
            transaction.commit();
            Assert.fail();
        } catch (TransactionException e) {
            // skip
        }
        
        Thread.sleep(1000);
        
        assertThat(b.get()).isEqualTo("123");
    }
    
    @Test
    public void testSet() {
        RBucket<String> b = redisson.getBucket("test");
        b.set("123");
        
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        RBucket<String> bucket = transaction.getBucket("test");
        bucket.set("234");
        assertThat(bucket.get()).isEqualTo("234");
        
        transaction.commit();
        
        assertThat(redisson.getKeys().count()).isEqualTo(1);
        assertThat(b.get()).isEqualTo("234");
    }
    
    @Test
    public void testGetAndSet() {
        RBucket<String> b = redisson.getBucket("test");
        b.set("123");
        
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        RBucket<String> bucket = transaction.getBucket("test");
        assertThat(bucket.getAndSet("0")).isEqualTo("123");
        assertThat(bucket.get()).isEqualTo("0");
        assertThat(bucket.getAndSet("324")).isEqualTo("0");
        
        transaction.commit();
        
        assertThat(redisson.getKeys().count()).isEqualTo(1);
        assertThat(b.get()).isEqualTo("324");
    }
    
    @Test
    public void testCompareAndSet() {
        RBucket<String> b = redisson.getBucket("test");
        b.set("123");
        
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        RBucket<String> bucket = transaction.getBucket("test");
        assertThat(bucket.compareAndSet("0", "434")).isFalse();
        assertThat(bucket.get()).isEqualTo("123");
        assertThat(bucket.compareAndSet("123", "232")).isTrue();
        assertThat(bucket.get()).isEqualTo("232");
        
        transaction.commit();
        
        assertThat(redisson.getKeys().count()).isEqualTo(1);
        assertThat(b.get()).isEqualTo("232");
    }
    
    @Test
    public void testTrySet() {
        RBucket<String> b = redisson.getBucket("test");
        b.set("123");
        
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        RBucket<String> bucket = transaction.getBucket("test");
        assertThat(bucket.trySet("0")).isFalse();
        assertThat(bucket.delete()).isTrue();
        assertThat(bucket.trySet("324")).isTrue();
        assertThat(bucket.trySet("43")).isFalse();
        
        transaction.commit();
        
        assertThat(redisson.getKeys().count()).isEqualTo(1);
        assertThat(b.get()).isEqualTo("324");
    }
    
    @Test
    public void testGetAndRemove() {
        RBucket<String> m = redisson.getBucket("test");
        m.set("123");
        
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        RBucket<String> set = transaction.getBucket("test");
        assertThat(set.get()).isEqualTo("123");
        assertThat(set.size()).isEqualTo(10);
        assertThat(set.getAndDelete()).isEqualTo("123");
        assertThat(set.size()).isEqualTo(0);
        assertThat(set.get()).isNull();
        assertThat(set.getAndDelete()).isNull();
        
        transaction.commit();
        
        assertThat(redisson.getKeys().count()).isEqualTo(0);
        assertThat(m.get()).isNull();
    }
    
    @Test
    public void testRollback() {
        RBucket<Object> b = redisson.getBucket("test");
        b.set("1234");
        
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        RBucket<Object> bucket = transaction.getBucket("test");
        assertThat(bucket.get()).isEqualTo("1234");
        assertThat(bucket.getAndDelete()).isEqualTo("1234");
        
        assertThat(b.get()).isEqualTo("1234");
        
        transaction.rollback();
        
        assertThat(redisson.getKeys().count()).isEqualTo(1);
        
        assertThat(b.get()).isEqualTo("1234");
    }

    
}
