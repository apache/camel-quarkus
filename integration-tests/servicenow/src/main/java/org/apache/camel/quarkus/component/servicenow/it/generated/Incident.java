/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.servicenow.it.generated;

import java.lang.Integer;
import java.lang.String;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Generated;
import org.apache.camel.component.servicenow.annotations.ServiceNowSysParm;

@Generated("org.apache.camel.maven.CamelServiceNowGenerateMojo")
@ServiceNowSysParm(name = "sysparm_exclude_reference_link", value = "true")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Incident {
    @JsonProperty(value = "active", required = false)
    private boolean active;

    @JsonProperty(value = "activity_due", required = false)
    private LocalDateTime activityDue;

    @JsonProperty(value = "additional_assignee_list", required = false)
    private String additionalAssigneeList;

    @JsonProperty(value = "approval", required = false)
    private String approval;

    @JsonProperty(value = "approval_history", required = false)
    private String approvalHistory;

    @JsonProperty(value = "approval_set", required = false)
    private LocalDateTime approvalSet;

    @JsonProperty(value = "assigned_to", required = false)
    private String assignedTo;

    @JsonProperty(value = "assignment_group", required = false)
    private String assignmentGroup;

    @JsonProperty(value = "business_duration", required = false)
    private LocalDateTime businessDuration;

    @JsonProperty(value = "business_service", required = false)
    private String businessService;

    @JsonProperty(value = "calendar_duration", required = false)
    private LocalDateTime calendarDuration;

    @JsonProperty(value = "closed_at", required = false)
    private LocalDateTime closedAt;

    @JsonProperty(value = "closed_by", required = false)
    private String closedBy;

    @JsonProperty(value = "close_notes", required = false)
    private String closeNotes;

    @JsonProperty(value = "cmdb_ci", required = false)
    private String cmdbCi;

    @JsonProperty(value = "comments", required = false)
    private String comments;

    @JsonProperty(value = "comments_and_work_notes", required = false)
    private String commentsAndWorkNotes;

    @JsonProperty(value = "company", required = false)
    private String company;

    @JsonProperty(value = "contact_type", required = false)
    private String contactType;

    @JsonProperty(value = "correlation_display", required = false)
    private String correlationDisplay;

    @JsonProperty(value = "correlation_id", required = false)
    private String correlationId;

    @JsonProperty(value = "delivery_plan", required = false)
    private String deliveryPlan;

    @JsonProperty(value = "delivery_task", required = false)
    private String deliveryTask;

    @JsonProperty(value = "description", required = false)
    private String description;

    @JsonProperty(value = "due_date", required = false)
    private LocalDateTime dueDate;

    @JsonProperty(value = "escalation", required = false)
    private Integer escalation;

    @JsonProperty(value = "expected_start", required = false)
    private LocalDateTime expectedStart;

    @JsonProperty(value = "follow_up", required = false)
    private LocalDateTime followUp;

    @JsonProperty(value = "group_list", required = false)
    private String groupList;

    @JsonProperty(value = "impact", required = false)
    private Integer impact;

    @JsonProperty(value = "knowledge", required = false)
    private boolean knowledge;

    @JsonProperty(value = "location", required = false)
    private String location;

    @JsonProperty(value = "made_sla", required = false)
    private boolean madeSla;

    @JsonProperty(value = "number", required = false)
    private String number;

    @JsonProperty(value = "opened_at", required = false)
    private LocalDateTime openedAt;

    @JsonProperty(value = "opened_by", required = false)
    private String openedBy;

    @JsonProperty(value = "order", required = false)
    private Integer order;

    @JsonProperty(value = "parent", required = false)
    private String parent;

    @JsonProperty(value = "priority", required = false)
    private Integer priority;

    @JsonProperty(value = "reassignment_count", required = false)
    private Integer reassignmentCount;

    @JsonProperty(value = "rejection_goto", required = false)
    private String rejectionGoto;

    @JsonProperty(value = "service_offering", required = false)
    private String serviceOffering;

    @JsonProperty(value = "short_description", required = false)
    private String shortDescription;

    @JsonProperty(value = "skills", required = false)
    private String skills;

    @JsonProperty(value = "sla_due", required = false)
    private LocalDateTime slaDue;

    @JsonProperty(value = "state", required = false)
    private Integer state;

    @JsonProperty(value = "sys_class_name", required = false)
    private String sysClassName;

    @JsonProperty(value = "sys_created_by", required = false)
    private String sysCreatedBy;

    @JsonProperty(value = "sys_created_on", required = false)
    private LocalDateTime sysCreatedOn;

    @JsonProperty(value = "sys_domain", required = false)
    private String sysDomain;

    @JsonProperty(value = "sys_domain_path", required = false)
    private String sysDomainPath;

    @JsonProperty(value = "sys_id", required = false)
    private String sysId;

    @JsonProperty(value = "sys_mod_count", required = false)
    private Integer sysModCount;

    @JsonProperty(value = "sys_updated_by", required = false)
    private String sysUpdatedBy;

    @JsonProperty(value = "sys_updated_on", required = false)
    private LocalDateTime sysUpdatedOn;

    @JsonProperty(value = "time_worked", required = false)
    private String timeWorked;

    @JsonProperty(value = "upon_approval", required = false)
    private String uponApproval;

    @JsonProperty(value = "upon_reject", required = false)
    private String uponReject;

    @JsonProperty(value = "urgency", required = false)
    private Integer urgency;

    @JsonProperty(value = "user_input", required = false)
    private String userInput;

    @JsonProperty(value = "variables", required = false)
    private String variables;

    @JsonProperty(value = "watch_list", required = false)
    private String watchList;

    @JsonProperty(value = "wf_activity", required = false)
    private String wfActivity;

    @JsonProperty(value = "work_end", required = false)
    private LocalDateTime workEnd;

    @JsonProperty(value = "work_notes", required = false)
    private String workNotes;

    @JsonProperty(value = "work_notes_list", required = false)
    private String workNotesList;

    @JsonProperty(value = "work_start", required = false)
    private LocalDateTime workStart;

    @JsonProperty(value = "business_stc", required = false)
    private Integer businessStc;

    @JsonProperty(value = "calendar_stc", required = false)
    private Integer calendarStc;

    @JsonProperty(value = "caller_id", required = false)
    private String callerId;

    @JsonProperty(value = "category", required = false)
    private String category;

    @JsonProperty(value = "caused_by", required = false)
    private String causedBy;

    @JsonProperty(value = "child_incidents", required = false)
    private Integer childIncidents;

    @JsonProperty(value = "close_code", required = false)
    private String closeCode;

    @JsonProperty(value = "incident_state", required = false)
    private Integer incidentState;

    @JsonProperty(value = "notify", required = false)
    private Integer notify;

    @JsonProperty(value = "parent_incident", required = false)
    private String parentIncident;

    @JsonProperty(value = "problem_id", required = false)
    private String problemId;

    @JsonProperty(value = "reopened_by", required = false)
    private String reopenedBy;

    @JsonProperty(value = "reopened_time", required = false)
    private LocalDateTime reopenedTime;

    @JsonProperty(value = "reopen_count", required = false)
    private Integer reopenCount;

    @JsonProperty(value = "resolved_at", required = false)
    private LocalDateTime resolvedAt;

    @JsonProperty(value = "resolved_by", required = false)
    private String resolvedBy;

    @JsonProperty(value = "rfc", required = false)
    private String rfc;

    @JsonProperty(value = "severity", required = false)
    private Integer severity;

    @JsonProperty(value = "subcategory", required = false)
    private String subcategory;

    public boolean getActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getActivityDue() {
        return this.activityDue;
    }

    public void setActivityDue(LocalDateTime activityDue) {
        this.activityDue = activityDue;
    }

    public String getAdditionalAssigneeList() {
        return this.additionalAssigneeList;
    }

    public void setAdditionalAssigneeList(String additionalAssigneeList) {
        this.additionalAssigneeList = additionalAssigneeList;
    }

    public String getApproval() {
        return this.approval;
    }

    public void setApproval(String approval) {
        this.approval = approval;
    }

    public String getApprovalHistory() {
        return this.approvalHistory;
    }

    public void setApprovalHistory(String approvalHistory) {
        this.approvalHistory = approvalHistory;
    }

    public LocalDateTime getApprovalSet() {
        return this.approvalSet;
    }

    public void setApprovalSet(LocalDateTime approvalSet) {
        this.approvalSet = approvalSet;
    }

    public String getAssignedTo() {
        return this.assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getAssignmentGroup() {
        return this.assignmentGroup;
    }

    public void setAssignmentGroup(String assignmentGroup) {
        this.assignmentGroup = assignmentGroup;
    }

    public LocalDateTime getBusinessDuration() {
        return this.businessDuration;
    }

    public void setBusinessDuration(LocalDateTime businessDuration) {
        this.businessDuration = businessDuration;
    }

    public String getBusinessService() {
        return this.businessService;
    }

    public void setBusinessService(String businessService) {
        this.businessService = businessService;
    }

    public LocalDateTime getCalendarDuration() {
        return this.calendarDuration;
    }

    public void setCalendarDuration(LocalDateTime calendarDuration) {
        this.calendarDuration = calendarDuration;
    }

    public LocalDateTime getClosedAt() {
        return this.closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getClosedBy() {
        return this.closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public String getCloseNotes() {
        return this.closeNotes;
    }

    public void setCloseNotes(String closeNotes) {
        this.closeNotes = closeNotes;
    }

    public String getCmdbCi() {
        return this.cmdbCi;
    }

    public void setCmdbCi(String cmdbCi) {
        this.cmdbCi = cmdbCi;
    }

    public String getComments() {
        return this.comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getCommentsAndWorkNotes() {
        return this.commentsAndWorkNotes;
    }

    public void setCommentsAndWorkNotes(String commentsAndWorkNotes) {
        this.commentsAndWorkNotes = commentsAndWorkNotes;
    }

    public String getCompany() {
        return this.company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getContactType() {
        return this.contactType;
    }

    public void setContactType(String contactType) {
        this.contactType = contactType;
    }

    public String getCorrelationDisplay() {
        return this.correlationDisplay;
    }

    public void setCorrelationDisplay(String correlationDisplay) {
        this.correlationDisplay = correlationDisplay;
    }

    public String getCorrelationId() {
        return this.correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getDeliveryPlan() {
        return this.deliveryPlan;
    }

    public void setDeliveryPlan(String deliveryPlan) {
        this.deliveryPlan = deliveryPlan;
    }

    public String getDeliveryTask() {
        return this.deliveryTask;
    }

    public void setDeliveryTask(String deliveryTask) {
        this.deliveryTask = deliveryTask;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDueDate() {
        return this.dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getEscalation() {
        return this.escalation;
    }

    public void setEscalation(Integer escalation) {
        this.escalation = escalation;
    }

    public LocalDateTime getExpectedStart() {
        return this.expectedStart;
    }

    public void setExpectedStart(LocalDateTime expectedStart) {
        this.expectedStart = expectedStart;
    }

    public LocalDateTime getFollowUp() {
        return this.followUp;
    }

    public void setFollowUp(LocalDateTime followUp) {
        this.followUp = followUp;
    }

    public String getGroupList() {
        return this.groupList;
    }

    public void setGroupList(String groupList) {
        this.groupList = groupList;
    }

    public Integer getImpact() {
        return this.impact;
    }

    public void setImpact(Integer impact) {
        this.impact = impact;
    }

    public boolean getKnowledge() {
        return this.knowledge;
    }

    public void setKnowledge(boolean knowledge) {
        this.knowledge = knowledge;
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean getMadeSla() {
        return this.madeSla;
    }

    public void setMadeSla(boolean madeSla) {
        this.madeSla = madeSla;
    }

    public String getNumber() {
        return this.number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public LocalDateTime getOpenedAt() {
        return this.openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public String getOpenedBy() {
        return this.openedBy;
    }

    public void setOpenedBy(String openedBy) {
        this.openedBy = openedBy;
    }

    public Integer getOrder() {
        return this.order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getParent() {
        return this.parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public Integer getPriority() {
        return this.priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getReassignmentCount() {
        return this.reassignmentCount;
    }

    public void setReassignmentCount(Integer reassignmentCount) {
        this.reassignmentCount = reassignmentCount;
    }

    public String getRejectionGoto() {
        return this.rejectionGoto;
    }

    public void setRejectionGoto(String rejectionGoto) {
        this.rejectionGoto = rejectionGoto;
    }

    public String getServiceOffering() {
        return this.serviceOffering;
    }

    public void setServiceOffering(String serviceOffering) {
        this.serviceOffering = serviceOffering;
    }

    public String getShortDescription() {
        return this.shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getSkills() {
        return this.skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public LocalDateTime getSlaDue() {
        return this.slaDue;
    }

    public void setSlaDue(LocalDateTime slaDue) {
        this.slaDue = slaDue;
    }

    public Integer getState() {
        return this.state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public String getSysClassName() {
        return this.sysClassName;
    }

    public void setSysClassName(String sysClassName) {
        this.sysClassName = sysClassName;
    }

    public String getSysCreatedBy() {
        return this.sysCreatedBy;
    }

    public void setSysCreatedBy(String sysCreatedBy) {
        this.sysCreatedBy = sysCreatedBy;
    }

    public LocalDateTime getSysCreatedOn() {
        return this.sysCreatedOn;
    }

    public void setSysCreatedOn(LocalDateTime sysCreatedOn) {
        this.sysCreatedOn = sysCreatedOn;
    }

    public String getSysDomain() {
        return this.sysDomain;
    }

    public void setSysDomain(String sysDomain) {
        this.sysDomain = sysDomain;
    }

    public String getSysDomainPath() {
        return this.sysDomainPath;
    }

    public void setSysDomainPath(String sysDomainPath) {
        this.sysDomainPath = sysDomainPath;
    }

    public String getSysId() {
        return this.sysId;
    }

    public void setSysId(String sysId) {
        this.sysId = sysId;
    }

    public Integer getSysModCount() {
        return this.sysModCount;
    }

    public void setSysModCount(Integer sysModCount) {
        this.sysModCount = sysModCount;
    }

    public String getSysUpdatedBy() {
        return this.sysUpdatedBy;
    }

    public void setSysUpdatedBy(String sysUpdatedBy) {
        this.sysUpdatedBy = sysUpdatedBy;
    }

    public LocalDateTime getSysUpdatedOn() {
        return this.sysUpdatedOn;
    }

    public void setSysUpdatedOn(LocalDateTime sysUpdatedOn) {
        this.sysUpdatedOn = sysUpdatedOn;
    }

    public String getTimeWorked() {
        return this.timeWorked;
    }

    public void setTimeWorked(String timeWorked) {
        this.timeWorked = timeWorked;
    }

    public String getUponApproval() {
        return this.uponApproval;
    }

    public void setUponApproval(String uponApproval) {
        this.uponApproval = uponApproval;
    }

    public String getUponReject() {
        return this.uponReject;
    }

    public void setUponReject(String uponReject) {
        this.uponReject = uponReject;
    }

    public Integer getUrgency() {
        return this.urgency;
    }

    public void setUrgency(Integer urgency) {
        this.urgency = urgency;
    }

    public String getUserInput() {
        return this.userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public String getVariables() {
        return this.variables;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public String getWatchList() {
        return this.watchList;
    }

    public void setWatchList(String watchList) {
        this.watchList = watchList;
    }

    public String getWfActivity() {
        return this.wfActivity;
    }

    public void setWfActivity(String wfActivity) {
        this.wfActivity = wfActivity;
    }

    public LocalDateTime getWorkEnd() {
        return this.workEnd;
    }

    public void setWorkEnd(LocalDateTime workEnd) {
        this.workEnd = workEnd;
    }

    public String getWorkNotes() {
        return this.workNotes;
    }

    public void setWorkNotes(String workNotes) {
        this.workNotes = workNotes;
    }

    public String getWorkNotesList() {
        return this.workNotesList;
    }

    public void setWorkNotesList(String workNotesList) {
        this.workNotesList = workNotesList;
    }

    public LocalDateTime getWorkStart() {
        return this.workStart;
    }

    public void setWorkStart(LocalDateTime workStart) {
        this.workStart = workStart;
    }

    public Integer getBusinessStc() {
        return this.businessStc;
    }

    public void setBusinessStc(Integer businessStc) {
        this.businessStc = businessStc;
    }

    public Integer getCalendarStc() {
        return this.calendarStc;
    }

    public void setCalendarStc(Integer calendarStc) {
        this.calendarStc = calendarStc;
    }

    public String getCallerId() {
        return this.callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCausedBy() {
        return this.causedBy;
    }

    public void setCausedBy(String causedBy) {
        this.causedBy = causedBy;
    }

    public Integer getChildIncidents() {
        return this.childIncidents;
    }

    public void setChildIncidents(Integer childIncidents) {
        this.childIncidents = childIncidents;
    }

    public String getCloseCode() {
        return this.closeCode;
    }

    public void setCloseCode(String closeCode) {
        this.closeCode = closeCode;
    }

    public Integer getIncidentState() {
        return this.incidentState;
    }

    public void setIncidentState(Integer incidentState) {
        this.incidentState = incidentState;
    }

    public Integer getNotify() {
        return this.notify;
    }

    public void setNotify(Integer notify) {
        this.notify = notify;
    }

    public String getParentIncident() {
        return this.parentIncident;
    }

    public void setParentIncident(String parentIncident) {
        this.parentIncident = parentIncident;
    }

    public String getProblemId() {
        return this.problemId;
    }

    public void setProblemId(String problemId) {
        this.problemId = problemId;
    }

    public String getReopenedBy() {
        return this.reopenedBy;
    }

    public void setReopenedBy(String reopenedBy) {
        this.reopenedBy = reopenedBy;
    }

    public LocalDateTime getReopenedTime() {
        return this.reopenedTime;
    }

    public void setReopenedTime(LocalDateTime reopenedTime) {
        this.reopenedTime = reopenedTime;
    }

    public Integer getReopenCount() {
        return this.reopenCount;
    }

    public void setReopenCount(Integer reopenCount) {
        this.reopenCount = reopenCount;
    }

    public LocalDateTime getResolvedAt() {
        return this.resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolvedBy() {
        return this.resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getRfc() {
        return this.rfc;
    }

    public void setRfc(String rfc) {
        this.rfc = rfc;
    }

    public Integer getSeverity() {
        return this.severity;
    }

    public void setSeverity(Integer severity) {
        this.severity = severity;
    }

    public String getSubcategory() {
        return this.subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }
}
