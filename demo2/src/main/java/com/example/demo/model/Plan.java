package com.example.demo.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Plan {

    @NotNull(message = "Organization (_org) cannot be null")
    @NotBlank(message = "org cannot be empty")
    private String _org;

    @NotNull(message = "Object ID cannot be null")
    @NotBlank(message = "objectId cannot be empty")
    private String objectId;

    @NotNull(message = "Object type cannot be null")
    @NotBlank(message = "object type cannot be empty")
    private String objectType;

    @NotNull(message = "Plan type cannot be null")
    @NotBlank(message = "Plan Type cannot be empty")
    private String planType;

    @NotNull(message = "Creation date cannot be null")
    @NotBlank(message = "Creation date cannot be empty")
    private String creationDate;

    @Valid
    @NotNull(message = "Plan cost shares cannot be null")
    private PlanCostShare planCostShares;

    @Valid
    @NotNull(message = "Linked plan services cannot be null")
    private List<LinkedPlanService> linkedPlanServices;
}
