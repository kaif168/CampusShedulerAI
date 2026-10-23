package com.campusscheduler.ai.models;

import java.io.Serializable;

public class ResourceItem implements Serializable {
    private String id;
    private String resourceId;
    private String collegeId;
    private String departmentId;
    private String teacherUid;
    private String uploadedByFacultyId;
    private String subjectId;
    private String subjectName;
    private String semester;
    private String section;
    private String title;
    private String description;
    private String fileUrl;
    private String url;
    private String resourceType;
    private String module;
    private String fileName;
    private long fileSizeBytes;
    private long createdAt;

    public ResourceItem() {
        this.createdAt = System.currentTimeMillis();
    }

    public ResourceItem(String id, String collegeId, String departmentId, String subjectId, String uploadedByFacultyId, String title, String fileUrl, String resourceType) {
        this();
        this.id = id;
        this.resourceId = id;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.subjectId = subjectId;
        this.uploadedByFacultyId = uploadedByFacultyId;
        this.teacherUid = uploadedByFacultyId;
        this.title = title;
        this.fileUrl = fileUrl;
        this.url = fileUrl;
        this.resourceType = resourceType;
    }

    public String getId() { return id != null ? id : resourceId; }
    public void setId(String id) { this.id = id; this.resourceId = id; }

    public String getResourceId() { return resourceId != null ? resourceId : id; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; this.id = resourceId; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getTeacherUid() { return teacherUid != null ? teacherUid : uploadedByFacultyId; }
    public void setTeacherUid(String teacherUid) { this.teacherUid = teacherUid; this.uploadedByFacultyId = teacherUid; }

    public String getUploadedByFacultyId() { return uploadedByFacultyId != null ? uploadedByFacultyId : teacherUid; }
    public void setUploadedByFacultyId(String uploadedByFacultyId) { this.uploadedByFacultyId = uploadedByFacultyId; this.teacherUid = uploadedByFacultyId; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName != null ? subjectName : "General Subject"; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description != null ? description : fileUrl; }
    public void setDescription(String description) { this.description = description; }

    public String getFileUrl() { return fileUrl != null ? fileUrl : url; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; this.url = fileUrl; }

    public String getUrl() { return url != null ? url : fileUrl; }
    public void setUrl(String url) { this.url = url; this.fileUrl = url; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
