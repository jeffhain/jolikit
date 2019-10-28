package net.jolikit.time.sched;

import net.jolikit.time.sched.InterfaceScheduler;

public class TestScheduler implements InterfaceScheduler {
    
    private final TestClock clock = new TestClock();
    
    private int execute_callCount;
    private Runnable execute_runnable;
    
    private int executeAtNs_callCount;
    private Runnable executeAtNs_runnable;
    private long executeAtNs_timeNs;
    
    private int executeAfterNs_callCount;
    private Runnable executeAfterNs_runnable;
    private long executeAfterNs_delayNs;
    
    public TestScheduler() {
        this.reset();
    }
    
    public final void reset() {
        this.execute_callCount = 0;
        this.execute_runnable = null;
        
        this.executeAtNs_callCount = 0;
        this.executeAtNs_runnable = null;
        this.executeAtNs_timeNs = Long.MIN_VALUE;
        
        this.executeAfterNs_callCount = 0;
        this.executeAfterNs_runnable = null;
        this.executeAfterNs_delayNs = Long.MIN_VALUE;
    }

    public int get_execute_callCount() {
        return this.execute_callCount;
    }

    public Runnable get_execute_runnable() {
        return this.execute_runnable;
    }

    public int get_executeAtNs_callCount() {
        return this.executeAtNs_callCount;
    }

    public Runnable get_executeAtNs_runnable() {
        return this.executeAtNs_runnable;
    }

    public long get_executeAtNs_timeNs() {
        return this.executeAtNs_timeNs;
    }

    public int get_executeAfterNs_callCount() {
        return this.executeAfterNs_callCount;
    }

    public Runnable get_executeAfterNs_runnable() {
        return this.executeAfterNs_runnable;
    }

    public long get_executeAfterNs_delayNs() {
        return this.executeAfterNs_delayNs;
    }
    
    /**
     * Convenience method.
     */
    public long setAndGetTimeNs(long nowNs) {
        return this.clock.setAndGetTimeNs(nowNs);
    }
    
    @Override
    public TestClock getClock() {
        return this.clock;
    }
    
    @Override
    public void execute(Runnable runnable) {
        this.execute_callCount++;
        this.execute_runnable = runnable;
    }
    
    @Override
    public void executeAtNs(Runnable runnable, long timeNs) {
        this.executeAtNs_callCount++;
        this.executeAtNs_runnable = runnable;
        this.executeAtNs_timeNs = timeNs;
    }
    
    @Override
    public void executeAfterNs(Runnable runnable, long delayNs) {
        this.executeAfterNs_callCount++;
        this.executeAfterNs_runnable = runnable;
        this.executeAfterNs_delayNs = delayNs;
    }
    
    @Override
    public void executeAtS(Runnable runnable, double timeS) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void executeAfterS(Runnable runnable, double delayS) {
        throw new UnsupportedOperationException();
    }
}
