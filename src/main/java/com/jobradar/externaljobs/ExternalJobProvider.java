package com.jobradar.externaljobs;

import java.util.List;

public interface ExternalJobProvider {
    String name();
    List<ExternalJobDto> fetchJobs();
}
