package com.simbiocreacion.resource.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Node {

    private String id;
    private String u_id;
    @Transient
    private User user;
    private String name;
    //@JsonDeserialize
    private Idea idea;
    private List<Node> children;
}
