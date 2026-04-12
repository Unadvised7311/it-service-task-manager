package com.danny.taskmanager.dto;

import java.util.ArrayList;
import java.util.List;

public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private String status;
    private Long ownerId;
    private String ownerUsername;
    private Integer taskTotal;
    private Integer taskDone;
    private Integer progressPercent;
    private List<Long> memberIds = new ArrayList<>();
    private List<MemberAccessDto> members = new ArrayList<>();
    /** Aktueller Nutzer: NONE | READ | WRITE (für UI) */
    private String myAccess;

    public ProjectResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public Integer getTaskTotal() { return taskTotal; }
    public void setTaskTotal(Integer taskTotal) { this.taskTotal = taskTotal; }

    public Integer getTaskDone() { return taskDone; }
    public void setTaskDone(Integer taskDone) { this.taskDone = taskDone; }

    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }

    public List<Long> getMemberIds() { return memberIds; }
    public void setMemberIds(List<Long> memberIds) { this.memberIds = memberIds != null ? memberIds : new ArrayList<>(); }

    public List<MemberAccessDto> getMembers() { return members; }
    public void setMembers(List<MemberAccessDto> members) { this.members = members != null ? members : new ArrayList<>(); }

    public String getMyAccess() { return myAccess; }
    public void setMyAccess(String myAccess) { this.myAccess = myAccess; }
}
