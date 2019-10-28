package net.jolikit.time.sched;

import net.jolikit.time.clocks.InterfaceClock;

public class TestClock implements InterfaceClock {
    
    private long nowNs = Long.MIN_VALUE;
    
    public TestClock() {
    }
    
    /**
     * Convenience method.
     */
    public long setAndGetTimeNs(long nowNs) {
        this.nowNs = nowNs;
        return nowNs;
    }
    
    @Override
    public long getTimeNs() {
        return this.nowNs;
    }
    
    @Override
    public double getTimeS() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public double getTimeSpeed() {
        throw new UnsupportedOperationException();
    }
}
