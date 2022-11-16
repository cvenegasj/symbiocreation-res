package com.simbiocreacion.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    private String id;
    private String name;
    private String firstName;
    private String lastName;
    private String email;
    private String pictureUrl;
    private Boolean isGridViewOn;
    private String role;
    private Date creationDateTime;
}
