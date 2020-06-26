package com.simbiocreacion.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Idea {

    private String title;
    private String description;
    private Date lastModified;
    //private String photoURL;
    private String imgPublicId;

}
