package com.jobautomation.common.constants;

/**
 * Portal URL constants shared across automation services.
 */
public enum JobConstants {
    JOB_PORTAL_LINKEDIN("https://www.linkedin.com/login"),
    JOBS_LINKEDURL("https://www.linkedin.com/jobs/?"),
    JOB_PORTAL_NAUKRI("https://www.naukri.com/nlogin/login");

    private final String constantValue;

    JobConstants(String constantValue) {
        this.constantValue = constantValue;
    }

    public String getConstantValue() {
        return constantValue;
    }
}
