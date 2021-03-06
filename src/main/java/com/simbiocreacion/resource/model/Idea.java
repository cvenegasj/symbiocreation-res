package com.simbiocreacion.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Idea {

    private String title;
    private String description;
    private Date lastModified;
    private List<String> imgPublicIds;
    private List<String> externalUrls;
    private List<Comment> comments;
}
