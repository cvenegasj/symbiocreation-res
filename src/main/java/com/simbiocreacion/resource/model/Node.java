package com.simbiocreacion.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;

import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Node {

    private String id;
    private String u_id;
    @Transient
    private User user;
    private String role;
    private String name;
    //@JsonDeserialize
    private Idea idea;
    private List<Node> children;

    @Override
    public boolean equals(Object o) {
        // self check
        if (this == o) return true;
        // null check
        if (o == null) return false;
        // type check and cast
        if (getClass() != o.getClass()) return false;

        Node node = (Node) o;
        // field comparison
        return Objects.equals(this.id, node.getId());
    }
}
