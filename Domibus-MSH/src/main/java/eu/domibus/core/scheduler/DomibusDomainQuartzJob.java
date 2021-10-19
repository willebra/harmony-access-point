package eu.domibus.core.scheduler;

import eu.domibus.api.multitenancy.Domain;

public class DomibusDomainQuartzJob {
    private Domain domain;

    private String quartzJob;

    public DomibusDomainQuartzJob(Domain domain, String quartzJob) {
        this.domain = domain;
        this.quartzJob = quartzJob;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public String getQuartzJob() {
        return quartzJob;
    }

    public void setQuartzJob(String quartzJob) {
        this.quartzJob = quartzJob;
    }
}
