package com.patternrun.execution;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How code execution is allowed to work here.
 *
 * {@code enabled} defaults to false, and that default is the security model rather than an
 * oversight. Running a learner's program requires a runtime that is genuinely isolated from this
 * process; if none is configured, the honest answer is "Run is unavailable", and a learner is told
 * that. The alternative — falling back to running it in-process because the sandbox is missing —
 * is exactly the failure mode this whole feature exists to avoid, so it is not implemented at all.
 */
@ConfigurationProperties(prefix = "patternrun.execution")
public class ExecutionProperties {

    /**
     * Whether submissions may be executed.
     *
     * Off by default. Turning it on asserts that {@link #docker()} points at a runtime that
     * isolates submissions from the API.
     */
    private boolean enabled;

    private final Docker docker = new Docker();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Docker docker() {
        return docker;
    }

    public static class Docker {

        /** The image submissions run in. Must contain a Python 3 interpreter and nothing else of value. */
        private String image = "patternrun-executor:py3";

        /** The docker CLI. Overridable so a test can point at a stub script. */
        private String binary = "docker";

        /**
         * Wall-clock ceiling for the whole run, including container start.
         *
         * Distinct from {@link #perCaseTimeout()}: this is the outer safety net for a runtime that
         * never comes back at all. When it fires the container is killed and the learner is told
         * the runner failed, not that their code was too slow.
         */
        private Duration wallClockTimeout = Duration.ofSeconds(20);

        /**
         * How long the whole program may take, across every case.
         *
         * Enforced inside the sandbox by the runner, so it produces a real
         * {@link ExecutionOutcome#TIME_LIMIT_EXCEEDED} rather than a container kill that loses
         * everything. One budget for the run rather than one per case, because "the solution did
         * not finish" is the useful thing to tell someone; "case three of seven was slow" is not.
         *
         * A linear solution over the seeded cases takes single-digit milliseconds, so a two-second
         * budget is generous enough not to fail correct answers and short enough that an accidental
         * {@code while True} is reported rather than waited on.
         */
        private Duration totalTimeout = Duration.ofSeconds(2);

        /** Hard memory ceiling for the container. The runner reports {@code MEMORY_LIMIT_EXCEEDED}. */
        private String memory = "256m";

        /** Fraction of a CPU. Deliberately below 1: a runaway loop should be slow to notice, not fatal. */
        private String cpus = "0.5";

        /** Maximum processes. A fork bomb inside a 256m container is stopped here. */
        private int pids = 32;

        /** Size of the writable scratch area. The root filesystem is read-only. */
        private String tmpfsSize = "16m";

        /** Bytes of the learner's stdout and stderr kept for debugging. */
        private int outputLimitBytes = 64 * 1024;

        /** Maximum accepted source length, in characters. Mirrors the stored code column. */
        private int maxSourceChars = 20_000;

        /**
         * How many submissions may execute at once.
         *
         * Each one is a container, so this is a load decision and not a security one. The container
         * limits are what keep one submission from affecting another.
         */
        private int maxConcurrent = 4;

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public String getBinary() {
            return binary;
        }

        public void setBinary(String binary) {
            this.binary = binary;
        }

        public Duration getWallClockTimeout() {
            return wallClockTimeout;
        }

        public void setWallClockTimeout(Duration wallClockTimeout) {
            this.wallClockTimeout = wallClockTimeout;
        }

        public Duration getTotalTimeout() {
            return totalTimeout;
        }

        public void setTotalTimeout(Duration totalTimeout) {
            this.totalTimeout = totalTimeout;
        }

        public String getMemory() {
            return memory;
        }

        public void setMemory(String memory) {
            this.memory = memory;
        }

        public String getCpus() {
            return cpus;
        }

        public void setCpus(String cpus) {
            this.cpus = cpus;
        }

        public int getPids() {
            return pids;
        }

        public void setPids(int pids) {
            this.pids = pids;
        }

        public String getTmpfsSize() {
            return tmpfsSize;
        }

        public void setTmpfsSize(String tmpfsSize) {
            this.tmpfsSize = tmpfsSize;
        }

        public int getOutputLimitBytes() {
            return outputLimitBytes;
        }

        public void setOutputLimitBytes(int outputLimitBytes) {
            this.outputLimitBytes = outputLimitBytes;
        }

        public int getMaxSourceChars() {
            return maxSourceChars;
        }

        public void setMaxSourceChars(int maxSourceChars) {
            this.maxSourceChars = maxSourceChars;
        }

        public int getMaxConcurrent() {
            return maxConcurrent;
        }

        public void setMaxConcurrent(int maxConcurrent) {
            this.maxConcurrent = maxConcurrent;
        }
    }
}