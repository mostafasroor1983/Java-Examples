package com.revolut.interview.ratelimiter;

public interface RateLimiter {
    /** @return true if the request identified by key is allowed now. */
    boolean allow(String key);
}
