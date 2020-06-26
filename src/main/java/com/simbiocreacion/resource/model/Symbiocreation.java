package com.simbiocreacion.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Symbiocreation {

    @Id
    private String id;
    private String name;
    private Date lastModified;
    private boolean enabled;
    private String visibility;
    private List<Participant> participants;
    private List<Node> graph;
    @BsonIgnore
    private Integer nParticipants;
}
