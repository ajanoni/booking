package com.ajanoni.repository.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Builder
@Accessors(fluent = true)
@RegisterForReflection
public class Customer {

    private String id;
    private String email;
    private String fullName;

}
