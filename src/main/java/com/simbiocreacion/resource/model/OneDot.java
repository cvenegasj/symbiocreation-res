package com.simbiocreacion.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OneDot {

    @Id
    private String id;
    private String name;
    private int[][] grid;
    private List<OneDotParticipant> participants;
    private List<int[][]> screenshots;

    private Date createdAt;
    private Date lastModifiedAt;
}
