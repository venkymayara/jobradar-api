package com.jobradar.externaljobs;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalJobProviderStatusRepository extends JpaRepository<ExternalJobProviderStatusEntity, String> { }
