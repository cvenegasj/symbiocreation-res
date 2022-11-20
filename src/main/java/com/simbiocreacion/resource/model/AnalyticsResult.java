package com.simbiocreacion.resource.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalyticsResult {

    @Id
    private String id;
    private String analysisName;
    private String serviceName;
    private Date creationDateTime;
    private String resultsFileContent;
}
