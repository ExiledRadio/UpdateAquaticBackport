package com.exiledradio.updateaquaticbackport.selftest;

/** One stream's headless test. */
public interface SelfTest {

    /** The stream name, as passed to {@code -Pselftest=}. */
    String name();

    void run(Check check) throws Exception;

    /** @return false for a test that runs only when asked for by name, never as part of {@code all} */
    default boolean inAll() {
        return true;
    }
}
