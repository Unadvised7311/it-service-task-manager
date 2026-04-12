package com.danny.taskmanager.model;

import jakarta.persistence.*; // [cite: 3427]

@Entity // Kennzeichnet die Klasse als Datenbank-Tabelle [cite: 3413, 3428]
public class Project {
    @Id // Primärschlüssel [cite: 3430]
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Automatische ID [cite: 3431]
    private Long id;

    private String name;
    private String description;
    private String status; // ACTIVE | ARCHIVED

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    // Getter und Setter (Wichtig für das Mapping)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
}
