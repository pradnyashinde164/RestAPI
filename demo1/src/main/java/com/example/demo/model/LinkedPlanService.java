package com.example.demo.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class    LinkedPlanService {
    @Valid
    @NotNull
    private LinkedService linkedService;

    @Valid
    @NotNull
    private PlanCostShare planserviceCostShares;

    @NotNull
    @NotBlank
    private String _org;

    @NotNull
    @NotBlank
    private String objectId;

    @NotNull
    @NotBlank
    private String objectType;
}
