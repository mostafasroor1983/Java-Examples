package com.revolut.buildit.loadbalancer;

import java.util.List;

/** Picks an index into the healthy backend list; may be stateful (round robin) or not (random). */
public interface Strategy {
    int pick(List<String> healthy);
}
