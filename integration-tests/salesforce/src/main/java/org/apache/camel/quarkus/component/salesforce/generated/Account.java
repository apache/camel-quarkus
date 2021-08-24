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
package org.apache.camel.quarkus.component.salesforce.generated;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import org.apache.camel.component.salesforce.api.MultiSelectPicklistConverter;
import org.apache.camel.component.salesforce.api.MultiSelectPicklistDeserializer;
import org.apache.camel.component.salesforce.api.MultiSelectPicklistSerializer;
import org.apache.camel.component.salesforce.api.PicklistEnumConverter;
import org.apache.camel.component.salesforce.api.dto.AbstractDescribedSObjectBase;
import org.apache.camel.component.salesforce.api.dto.SObjectDescription;
import org.apache.camel.component.salesforce.api.dto.SObjectDescriptionUrls;
import org.apache.camel.component.salesforce.api.dto.SObjectField;

/**
 * Salesforce DTO for SObject Account
 */
@Generated("org.apache.camel.maven.CamelSalesforceMojo")
@XStreamAlias("Account")
public class Account extends AbstractDescribedSObjectBase {

    public Account() {
        getAttributes().setType("Account");
    }

    private static final SObjectDescription DESCRIPTION = createSObjectDescription();

    private String MasterRecordId;

    @JsonProperty("MasterRecordId")
    public String getMasterRecordId() {
        return this.MasterRecordId;
    }

    @JsonProperty("MasterRecordId")
    public void setMasterRecordId(String MasterRecordId) {
        this.MasterRecordId = MasterRecordId;
    }

    @XStreamAlias("MasterRecord")
    private Account MasterRecord;

    @JsonProperty("MasterRecord")
    public Account getMasterRecord() {
        return this.MasterRecord;
    }

    @JsonProperty("MasterRecord")
    public void setMasterRecord(Account MasterRecord) {
        this.MasterRecord = MasterRecord;
    }

    @XStreamConverter(PicklistEnumConverter.class)
    private Account_TypeEnum Type;

    @JsonProperty("Type")
    public Account_TypeEnum getType() {
        return this.Type;
    }

    @JsonProperty("Type")
    public void setType(Account_TypeEnum Type) {
        this.Type = Type;
    }

    private String ParentId;

    @JsonProperty("ParentId")
    public String getParentId() {
        return this.ParentId;
    }

    @JsonProperty("ParentId")
    public void setParentId(String ParentId) {
        this.ParentId = ParentId;
    }

    @XStreamAlias("Parent")
    private Account Parent;

    @JsonProperty("Parent")
    public Account getParent() {
        return this.Parent;
    }

    @JsonProperty("Parent")
    public void setParent(Account Parent) {
        this.Parent = Parent;
    }

    private String BillingStreet;

    @JsonProperty("BillingStreet")
    public String getBillingStreet() {
        return this.BillingStreet;
    }

    @JsonProperty("BillingStreet")
    public void setBillingStreet(String BillingStreet) {
        this.BillingStreet = BillingStreet;
    }

    private String BillingCity;

    @JsonProperty("BillingCity")
    public String getBillingCity() {
        return this.BillingCity;
    }

    @JsonProperty("BillingCity")
    public void setBillingCity(String BillingCity) {
        this.BillingCity = BillingCity;
    }

    private String BillingState;

    @JsonProperty("BillingState")
    public String getBillingState() {
        return this.BillingState;
    }

    @JsonProperty("BillingState")
    public void setBillingState(String BillingState) {
        this.BillingState = BillingState;
    }

    private String BillingPostalCode;

    @JsonProperty("BillingPostalCode")
    public String getBillingPostalCode() {
        return this.BillingPostalCode;
    }

    @JsonProperty("BillingPostalCode")
    public void setBillingPostalCode(String BillingPostalCode) {
        this.BillingPostalCode = BillingPostalCode;
    }

    private String BillingCountry;

    @JsonProperty("BillingCountry")
    public String getBillingCountry() {
        return this.BillingCountry;
    }

    @JsonProperty("BillingCountry")
    public void setBillingCountry(String BillingCountry) {
        this.BillingCountry = BillingCountry;
    }

    private Double BillingLatitude;

    @JsonProperty("BillingLatitude")
    public Double getBillingLatitude() {
        return this.BillingLatitude;
    }

    @JsonProperty("BillingLatitude")
    public void setBillingLatitude(Double BillingLatitude) {
        this.BillingLatitude = BillingLatitude;
    }

    private Double BillingLongitude;

    @JsonProperty("BillingLongitude")
    public Double getBillingLongitude() {
        return this.BillingLongitude;
    }

    @JsonProperty("BillingLongitude")
    public void setBillingLongitude(Double BillingLongitude) {
        this.BillingLongitude = BillingLongitude;
    }

    @XStreamConverter(PicklistEnumConverter.class)
    private Account_BillingGeocodeAccuracyEnum BillingGeocodeAccuracy;

    @JsonProperty("BillingGeocodeAccuracy")
    public Account_BillingGeocodeAccuracyEnum getBillingGeocodeAccuracy() {
        return this.BillingGeocodeAccuracy;
    }

    @JsonProperty("BillingGeocodeAccuracy")
    public void setBillingGeocodeAccuracy(Account_BillingGeocodeAccuracyEnum BillingGeocodeAccuracy) {
        this.BillingGeocodeAccuracy = BillingGeocodeAccuracy;
    }

    private org.apache.camel.component.salesforce.api.dto.Address BillingAddress;

    @JsonProperty("BillingAddress")
    public org.apache.camel.component.salesforce.api.dto.Address getBillingAddress() {
        return this.BillingAddress;
    }

    @JsonProperty("BillingAddress")
    public void setBillingAddress(org.apache.camel.component.salesforce.api.dto.Address BillingAddress) {
        this.BillingAddress = BillingAddress;
    }

    private String ShippingStreet;

    @JsonProperty("ShippingStreet")
    public String getShippingStreet() {
        return this.ShippingStreet;
    }

    @JsonProperty("ShippingStreet")
    public void setShippingStreet(String ShippingStreet) {
        this.ShippingStreet = ShippingStreet;
    }

    private String ShippingCity;

    @JsonProperty("ShippingCity")
    public String getShippingCity() {
        return this.ShippingCity;
    }

    @JsonProperty("ShippingCity")
    public void setShippingCity(String ShippingCity) {
        this.ShippingCity = ShippingCity;
    }

    private String ShippingState;

    @JsonProperty("ShippingState")
    public String getShippingState() {
        return this.ShippingState;
    }

    @JsonProperty("ShippingState")
    public void setShippingState(String ShippingState) {
        this.ShippingState = ShippingState;
    }

    private String ShippingPostalCode;

    @JsonProperty("ShippingPostalCode")
    public String getShippingPostalCode() {
        return this.ShippingPostalCode;
    }

    @JsonProperty("ShippingPostalCode")
    public void setShippingPostalCode(String ShippingPostalCode) {
        this.ShippingPostalCode = ShippingPostalCode;
    }

    private String ShippingCountry;

    @JsonProperty("ShippingCountry")
    public String getShippingCountry() {
        return this.ShippingCountry;
    }

    @JsonProperty("ShippingCountry")
    public void setShippingCountry(String ShippingCountry) {
        this.ShippingCountry = ShippingCountry;
    }

    private Double ShippingLatitude;

    @JsonProperty("ShippingLatitude")
    public Double getShippingLatitude() {
        return this.ShippingLatitude;
    }

    @JsonProperty("ShippingLatitude")
    public void setShippingLatitude(Double ShippingLatitude) {
        this.ShippingLatitude = ShippingLatitude;
    }

    private Double ShippingLongitude;

    @JsonProperty("ShippingLongitude")
    public Double getShippingLongitude() {
        return this.ShippingLongitude;
    }

    @JsonProperty("ShippingLongitude")
    public void setShippingLongitude(Double ShippingLongitude) {
        this.ShippingLongitude = ShippingLongitude;
    }

    @XStreamConverter(PicklistEnumConverter.class)
    private Account_ShippingGeocodeAccuracyEnum ShippingGeocodeAccuracy;

    @JsonProperty("ShippingGeocodeAccuracy")
    public Account_ShippingGeocodeAccuracyEnum getShippingGeocodeAccuracy() {
        return this.ShippingGeocodeAccuracy;
    }

    @JsonProperty("ShippingGeocodeAccuracy")
    public void setShippingGeocodeAccuracy(Account_ShippingGeocodeAccuracyEnum ShippingGeocodeAccuracy) {
        this.ShippingGeocodeAccuracy = ShippingGeocodeAccuracy;
    }

    private org.apache.camel.component.salesforce.api.dto.Address ShippingAddress;

    @JsonProperty("ShippingAddress")
    public org.apache.camel.component.salesforce.api.dto.Address getShippingAddress() {
        return this.ShippingAddress;
    }

    @JsonProperty("ShippingAddress")
    public void setShippingAddress(org.apache.camel.component.salesforce.api.dto.Address ShippingAddress) {
        this.ShippingAddress = ShippingAddress;
    }

    private String Phone;

    @JsonProperty("Phone")
    public String getPhone() {
        return this.Phone;
    }

    @JsonProperty("Phone")
    public void setPhone(String Phone) {
        this.Phone = Phone;
    }

    private String Fax;

    @JsonProperty("Fax")
    public String getFax() {
        return this.Fax;
    }

    @JsonProperty("Fax")
    public void setFax(String Fax) {
        this.Fax = Fax;
    }

    private String AccountNumber;

    @JsonProperty("AccountNumber")
    public String getAccountNumber() {
        return this.AccountNumber;
    }

    @JsonProperty("AccountNumber")
    public void setAccountNumber(String AccountNumber) {
        this.AccountNumber = AccountNumber;
    }

    private String Website;

    @JsonProperty("Website")
    public String getWebsite() {
        return this.Website;
    }

    @JsonProperty("Website")
    public void setWebsite(String Website) {
        this.Website = Website;
    }

    private String PhotoUrl;

    @JsonProperty("PhotoUrl")
    public String getPhotoUrl() {
        return this.PhotoUrl;
    }

    @JsonProperty("PhotoUrl")
    public void setPhotoUrl(String PhotoUrl) {
        this.PhotoUrl = PhotoUrl;
    }

    private String Sic;

    @JsonProperty("Sic")
    public String getSic() {
        return this.Sic;
    }

    @JsonProperty("Sic")
    public void setSic(String Sic) {
        this.Sic = Sic;
    }

    @XStreamConverter(PicklistEnumConverter.class)
    private Account_IndustryEnum Industry;

    @JsonProperty("Industry")
    public Account_IndustryEnum getIndustry() {
        return this.Industry;
    }

    @JsonProperty("Industry")
    public void setIndustry(Account_IndustryEnum Industry) {
        this.Industry = Industry;
    }

    private Double AnnualRevenue;

    @JsonProperty("AnnualRevenue")
    public Double getAnnualRevenue() {
        return this.AnnualRevenue;
    }

    @JsonProperty("AnnualRevenue")
    public void setAnnualRevenue(Double AnnualRevenue) {
        this.AnnualRevenue = AnnualRevenue;
    }

    private Integer NumberOfEmployees;

    @JsonProperty("NumberOfEmployees")
    public Integer getNumberOfEmployees() {
        return this.NumberOfEmployees;
    }

    @JsonProperty("NumberOfEmployees")
    public void setNumberOfEmployees(Integer NumberOfEmployees) {
        this.NumberOfEmployees = NumberOfEmployees;
    }

    @XStreamConverter(PicklistEnumConverter.class)
    private Account_OwnershipEnum Ownership;

    @JsonProperty("Ownership")
    public Account_OwnershipEnum getOwnership() {
        return this.Ownership;
    }

    @JsonProperty("Ownership")
    public void setOwnership(Account_OwnershipEnum Ownership) {
        this.Ownership = Ownership;
    }

    private String TickerSymbol;

    @JsonProperty("TickerSymbol")
    public String getTickerSymbol() {
        return this.TickerSymbol;
    }

    @JsonProperty("TickerSymbol")
    public void setTickerSymbol(String TickerSymbol) {
        this.TickerSymbol = TickerSymbol;
    }

    private String Description;

    @JsonProperty("Description")
    public String getDescription() {
        return this.Description;
    }

    @JsonProperty("Description")
    public void setDescription(String Description) {
        this.Description = Description;
    }

    @XStreamConverter(PicklistEnumConverter.class)
    private Account_RatingEnum Rating;

    @JsonProperty("Rating")
    public Account_RatingEnum getRating() {
        return this.Rating;
    }

    @JsonProperty("Rating")
    public void setRating(Account_RatingEnum Rating) {
        this.Rating = Rating;
    }

    private String Site;

    @JsonProperty("Site")
    public String getSite() {
        return this.Site;
    }

    @JsonProperty("Site")
    public void setSite(String Site) {
        this.Site = Site;
    }

    private String Jigsaw;

    @JsonProperty("Jigsaw")
    public String getJigsaw() {
        return this.Jigsaw;
    }

    @JsonProperty("Jigsaw")
    public void setJigsaw(String Jigsaw) {
        this.Jigsaw = Jigsaw;
    }

    private String JigsawCompanyId;

    @JsonProperty("JigsawCompanyId")
    public String getJigsawCompanyId() {
        return this.JigsawCompanyId;
    }

    @JsonProperty("JigsawCompanyId")
    public void setJigsawCompanyId(String JigsawCompanyId) {
        this.JigsawCompanyId = JigsawCompanyId;
    }

    @XStreamConverter(PicklistEnumConverter.class)
    private Account_CleanStatusEnum CleanStatus;

    @JsonProperty("CleanStatus")
    public Account_CleanStatusEnum getCleanStatus() {
        return this.CleanStatus;
    }

    @JsonProperty("CleanStatus")
    public void setCleanStatus(Account_CleanStatusEnum CleanStatus) {
        this.CleanStatus = CleanStatus;
    }

    @XStreamConverter(PicklistEnumConverter.class)
    private Account_AccountSourceEnum AccountSource;

    @JsonProperty("AccountSource")
    public Account_AccountSourceEnum getAccountSource() {
        return this.AccountSource;
    }

    @JsonProperty("AccountSource")
    public void setAccountSource(Account_AccountSourceEnum AccountSource) {
        this.AccountSource = AccountSource;
    }

    private String DunsNumber;

    @JsonProperty("DunsNumber")
    public String getDunsNumber() {
        return this.DunsNumber;
    }

    @JsonProperty("DunsNumber")
    public void setDunsNumber(String DunsNumber) {
        this.DunsNumber = DunsNumber;
    }

    private String Tradestyle;

    @JsonProperty("Tradestyle")
    public String getTradestyle() {
        return this.Tradestyle;
    }

    @JsonProperty("Tradestyle")
    public void setTradestyle(String Tradestyle) {
        this.Tradestyle = Tradestyle;
    }

    private String NaicsCode;

    @JsonProperty("NaicsCode")
    public String getNaicsCode() {
        return this.NaicsCode;
    }

    @JsonProperty("NaicsCode")
    public void setNaicsCode(String NaicsCode) {
        this.NaicsCode = NaicsCode;
    }

    private String NaicsDesc;

    @JsonProperty("NaicsDesc")
    public String getNaicsDesc() {
        return this.NaicsDesc;
    }

    @JsonProperty("NaicsDesc")
    public void setNaicsDesc(String NaicsDesc) {
        this.NaicsDesc = NaicsDesc;
    }

    private String YearStarted;

    @JsonProperty("YearStarted")
    public String getYearStarted() {
        return this.YearStarted;
    }

    @JsonProperty("YearStarted")
    public void setYearStarted(String YearStarted) {
        this.YearStarted = YearStarted;
    }

    private String SicDesc;

    @JsonProperty("SicDesc")
    public String getSicDesc() {
        return this.SicDesc;
    }

    @JsonProperty("SicDesc")
    public void setSicDesc(String SicDesc) {
        this.SicDesc = SicDesc;
    }

    private String DandbCompanyId;

    @JsonProperty("DandbCompanyId")
    public String getDandbCompanyId() {
        return this.DandbCompanyId;
    }

    @JsonProperty("DandbCompanyId")
    public void setDandbCompanyId(String DandbCompanyId) {
        this.DandbCompanyId = DandbCompanyId;
    }

    @XStreamConverter(PicklistEnumConverter.class)
    private Account_CustomerPriorityEnum CustomerPriority__c;

    @JsonProperty("CustomerPriority__c")
    public Account_CustomerPriorityEnum getCustomerPriority__c() {
        return this.CustomerPriority__c;
    }

    @JsonProperty("CustomerPriority__c")
    public void setCustomerPriority__c(Account_CustomerPriorityEnum CustomerPriority__c) {
        this.CustomerPriority__c = CustomerPriority__c;
    }

    @XStreamConverter(PicklistEnumConverter.class)
    private Account_SLAEnum SLA__c;

    @JsonProperty("SLA__c")
    public Account_SLAEnum getSLA__c() {
        return this.SLA__c;
    }

    @JsonProperty("SLA__c")
    public void setSLA__c(Account_SLAEnum SLA__c) {
        this.SLA__c = SLA__c;
    }

    @XStreamConverter(PicklistEnumConverter.class)
    private Account_ActiveEnum Active__c;

    @JsonProperty("Active__c")
    public Account_ActiveEnum getActive__c() {
        return this.Active__c;
    }

    @JsonProperty("Active__c")
    public void setActive__c(Account_ActiveEnum Active__c) {
        this.Active__c = Active__c;
    }

    private Double NumberofLocations__c;

    @JsonProperty("NumberofLocations__c")
    public Double getNumberofLocations__c() {
        return this.NumberofLocations__c;
    }

    @JsonProperty("NumberofLocations__c")
    public void setNumberofLocations__c(Double NumberofLocations__c) {
        this.NumberofLocations__c = NumberofLocations__c;
    }

    @XStreamConverter(PicklistEnumConverter.class)
    private Account_UpsellOpportunityEnum UpsellOpportunity__c;

    @JsonProperty("UpsellOpportunity__c")
    public Account_UpsellOpportunityEnum getUpsellOpportunity__c() {
        return this.UpsellOpportunity__c;
    }

    @JsonProperty("UpsellOpportunity__c")
    public void setUpsellOpportunity__c(Account_UpsellOpportunityEnum UpsellOpportunity__c) {
        this.UpsellOpportunity__c = UpsellOpportunity__c;
    }

    private String SLASerialNumber__c;

    @JsonProperty("SLASerialNumber__c")
    public String getSLASerialNumber__c() {
        return this.SLASerialNumber__c;
    }

    @JsonProperty("SLASerialNumber__c")
    public void setSLASerialNumber__c(String SLASerialNumber__c) {
        this.SLASerialNumber__c = SLASerialNumber__c;
    }

    private java.time.LocalDate SLAExpirationDate__c;

    @JsonProperty("SLAExpirationDate__c")
    public java.time.LocalDate getSLAExpirationDate__c() {
        return this.SLAExpirationDate__c;
    }

    @JsonProperty("SLAExpirationDate__c")
    public void setSLAExpirationDate__c(java.time.LocalDate SLAExpirationDate__c) {
        this.SLAExpirationDate__c = SLAExpirationDate__c;
    }

    @XStreamConverter(MultiSelectPicklistConverter.class)
    private Account_MyMultiselectEnum[] MyMultiselect__c;

    @JsonProperty("MyMultiselect__c")
    @JsonSerialize(using = MultiSelectPicklistSerializer.class)
    public Account_MyMultiselectEnum[] getMyMultiselect__c() {
        return this.MyMultiselect__c;
    }

    @JsonProperty("MyMultiselect__c")
    @JsonDeserialize(using = MultiSelectPicklistDeserializer.class)
    public void setMyMultiselect__c(Account_MyMultiselectEnum[] MyMultiselect__c) {
        this.MyMultiselect__c = MyMultiselect__c;
    }

    private QueryRecordsAccount ChildAccounts;

    @JsonProperty("ChildAccounts")
    public QueryRecordsAccount getChildAccounts() {
        return ChildAccounts;
    }

    @JsonProperty("ChildAccounts")
    public void setChildAccounts(QueryRecordsAccount ChildAccounts) {
        this.ChildAccounts = ChildAccounts;
    }

    @Override
    public final SObjectDescription description() {
        return DESCRIPTION;
    }

    private static SObjectDescription createSObjectDescription() {
        final SObjectDescription description = new SObjectDescription();

        final List<SObjectField> fields1 = new ArrayList<>();
        description.setFields(fields1);

        final SObjectField sObjectField1 = createField("Id", "Account ID", "id", "tns:ID", 18, false, false, false, false,
                false, false, true);
        fields1.add(sObjectField1);
        final SObjectField sObjectField2 = createField("IsDeleted", "Deleted", "boolean", "xsd:boolean", 0, false, false, false,
                false, false, false, false);
        fields1.add(sObjectField2);
        final SObjectField sObjectField3 = createField("MasterRecordId", "Master Record ID", "reference", "tns:ID", 18, false,
                true, false, false, false, false, false);
        fields1.add(sObjectField3);
        final SObjectField sObjectField4 = createField("Name", "Account Name", "string", "xsd:string", 255, false, false, true,
                false, false, false, false);
        fields1.add(sObjectField4);
        final SObjectField sObjectField5 = createField("Type", "Account Type", "picklist", "xsd:string", 255, false, true,
                false, false, false, false, false);
        fields1.add(sObjectField5);
        final SObjectField sObjectField6 = createField("ParentId", "Parent Account ID", "reference", "tns:ID", 18, false, true,
                false, false, false, false, false);
        fields1.add(sObjectField6);
        final SObjectField sObjectField7 = createField("BillingStreet", "Billing Street", "textarea", "xsd:string", 255, false,
                true, false, false, false, false, false);
        fields1.add(sObjectField7);
        final SObjectField sObjectField8 = createField("BillingCity", "Billing City", "string", "xsd:string", 40, false, true,
                false, false, false, false, false);
        fields1.add(sObjectField8);
        final SObjectField sObjectField9 = createField("BillingState", "Billing State/Province", "string", "xsd:string", 80,
                false, true, false, false, false, false, false);
        fields1.add(sObjectField9);
        final SObjectField sObjectField10 = createField("BillingPostalCode", "Billing Zip/Postal Code", "string", "xsd:string",
                20, false, true, false, false, false, false, false);
        fields1.add(sObjectField10);
        final SObjectField sObjectField11 = createField("BillingCountry", "Billing Country", "string", "xsd:string", 80, false,
                true, false, false, false, false, false);
        fields1.add(sObjectField11);
        final SObjectField sObjectField12 = createField("BillingLatitude", "Billing Latitude", "double", "xsd:double", 0, false,
                true, false, false, false, false, false);
        fields1.add(sObjectField12);
        final SObjectField sObjectField13 = createField("BillingLongitude", "Billing Longitude", "double", "xsd:double", 0,
                false, true, false, false, false, false, false);
        fields1.add(sObjectField13);
        final SObjectField sObjectField14 = createField("BillingGeocodeAccuracy", "Billing Geocode Accuracy", "picklist",
                "xsd:string", 40, false, true, false, false, false, false, false);
        fields1.add(sObjectField14);
        final SObjectField sObjectField15 = createField("BillingAddress", "Billing Address", "address", "urn:address", 0, false,
                true, false, false, false, false, false);
        fields1.add(sObjectField15);
        final SObjectField sObjectField16 = createField("ShippingStreet", "Shipping Street", "textarea", "xsd:string", 255,
                false, true, false, false, false, false, false);
        fields1.add(sObjectField16);
        final SObjectField sObjectField17 = createField("ShippingCity", "Shipping City", "string", "xsd:string", 40, false,
                true, false, false, false, false, false);
        fields1.add(sObjectField17);
        final SObjectField sObjectField18 = createField("ShippingState", "Shipping State/Province", "string", "xsd:string", 80,
                false, true, false, false, false, false, false);
        fields1.add(sObjectField18);
        final SObjectField sObjectField19 = createField("ShippingPostalCode", "Shipping Zip/Postal Code", "string",
                "xsd:string", 20, false, true, false, false, false, false, false);
        fields1.add(sObjectField19);
        final SObjectField sObjectField20 = createField("ShippingCountry", "Shipping Country", "string", "xsd:string", 80,
                false, true, false, false, false, false, false);
        fields1.add(sObjectField20);
        final SObjectField sObjectField21 = createField("ShippingLatitude", "Shipping Latitude", "double", "xsd:double", 0,
                false, true, false, false, false, false, false);
        fields1.add(sObjectField21);
        final SObjectField sObjectField22 = createField("ShippingLongitude", "Shipping Longitude", "double", "xsd:double", 0,
                false, true, false, false, false, false, false);
        fields1.add(sObjectField22);
        final SObjectField sObjectField23 = createField("ShippingGeocodeAccuracy", "Shipping Geocode Accuracy", "picklist",
                "xsd:string", 40, false, true, false, false, false, false, false);
        fields1.add(sObjectField23);
        final SObjectField sObjectField24 = createField("ShippingAddress", "Shipping Address", "address", "urn:address", 0,
                false, true, false, false, false, false, false);
        fields1.add(sObjectField24);
        final SObjectField sObjectField25 = createField("Phone", "Account Phone", "phone", "xsd:string", 40, false, true, false,
                false, false, false, false);
        fields1.add(sObjectField25);
        final SObjectField sObjectField26 = createField("Fax", "Account Fax", "phone", "xsd:string", 40, false, true, false,
                false, false, false, false);
        fields1.add(sObjectField26);
        final SObjectField sObjectField27 = createField("AccountNumber", "Account Number", "string", "xsd:string", 40, false,
                true, false, false, false, false, false);
        fields1.add(sObjectField27);
        final SObjectField sObjectField28 = createField("Website", "Website", "url", "xsd:string", 255, false, true, false,
                false, false, false, false);
        fields1.add(sObjectField28);
        final SObjectField sObjectField29 = createField("PhotoUrl", "Photo URL", "url", "xsd:string", 255, false, true, false,
                false, false, false, false);
        fields1.add(sObjectField29);
        final SObjectField sObjectField30 = createField("Sic", "SIC Code", "string", "xsd:string", 20, false, true, false,
                false, false, false, false);
        fields1.add(sObjectField30);
        final SObjectField sObjectField31 = createField("Industry", "Industry", "picklist", "xsd:string", 255, false, true,
                false, false, false, false, false);
        fields1.add(sObjectField31);
        final SObjectField sObjectField32 = createField("AnnualRevenue", "Annual Revenue", "currency", "xsd:double", 0, false,
                true, false, false, false, false, false);
        fields1.add(sObjectField32);
        final SObjectField sObjectField33 = createField("NumberOfEmployees", "Employees", "int", "xsd:int", 0, false, true,
                false, false, false, false, false);
        fields1.add(sObjectField33);
        final SObjectField sObjectField34 = createField("Ownership", "Ownership", "picklist", "xsd:string", 255, false, true,
                false, false, false, false, false);
        fields1.add(sObjectField34);
        final SObjectField sObjectField35 = createField("TickerSymbol", "Ticker Symbol", "string", "xsd:string", 20, false,
                true, false, false, false, false, false);
        fields1.add(sObjectField35);
        final SObjectField sObjectField36 = createField("Description", "Account Description", "textarea", "xsd:string", 32000,
                false, true, false, false, false, false, false);
        fields1.add(sObjectField36);
        final SObjectField sObjectField37 = createField("Rating", "Account Rating", "picklist", "xsd:string", 255, false, true,
                false, false, false, false, false);
        fields1.add(sObjectField37);
        final SObjectField sObjectField38 = createField("Site", "Account Site", "string", "xsd:string", 80, false, true, false,
                false, false, false, false);
        fields1.add(sObjectField38);
        final SObjectField sObjectField39 = createField("OwnerId", "Owner ID", "reference", "tns:ID", 18, false, false, false,
                false, false, false, false);
        fields1.add(sObjectField39);
        final SObjectField sObjectField40 = createField("CreatedDate", "Created Date", "datetime", "xsd:dateTime", 0, false,
                false, false, false, false, false, false);
        fields1.add(sObjectField40);
        final SObjectField sObjectField41 = createField("CreatedById", "Created By ID", "reference", "tns:ID", 18, false, false,
                false, false, false, false, false);
        fields1.add(sObjectField41);
        final SObjectField sObjectField42 = createField("LastModifiedDate", "Last Modified Date", "datetime", "xsd:dateTime", 0,
                false, false, false, false, false, false, false);
        fields1.add(sObjectField42);
        final SObjectField sObjectField43 = createField("LastModifiedById", "Last Modified By ID", "reference", "tns:ID", 18,
                false, false, false, false, false, false, false);
        fields1.add(sObjectField43);
        final SObjectField sObjectField44 = createField("SystemModstamp", "System Modstamp", "datetime", "xsd:dateTime", 0,
                false, false, false, false, false, false, false);
        fields1.add(sObjectField44);
        final SObjectField sObjectField45 = createField("LastActivityDate", "Last Activity", "date", "xsd:date", 0, false, true,
                false, false, false, false, false);
        fields1.add(sObjectField45);
        final SObjectField sObjectField46 = createField("LastViewedDate", "Last Viewed Date", "datetime", "xsd:dateTime", 0,
                false, true, false, false, false, false, false);
        fields1.add(sObjectField46);
        final SObjectField sObjectField47 = createField("LastReferencedDate", "Last Referenced Date", "datetime",
                "xsd:dateTime", 0, false, true, false, false, false, false, false);
        fields1.add(sObjectField47);
        final SObjectField sObjectField48 = createField("Jigsaw", "Data.com Key", "string", "xsd:string", 20, false, true,
                false, false, false, false, false);
        fields1.add(sObjectField48);
        final SObjectField sObjectField49 = createField("JigsawCompanyId", "Jigsaw Company ID", "string", "xsd:string", 20,
                false, true, false, false, false, false, false);
        fields1.add(sObjectField49);
        final SObjectField sObjectField50 = createField("CleanStatus", "Clean Status", "picklist", "xsd:string", 40, false,
                true, false, false, false, false, false);
        fields1.add(sObjectField50);
        final SObjectField sObjectField51 = createField("AccountSource", "Account Source", "picklist", "xsd:string", 255, false,
                true, false, false, false, false, false);
        fields1.add(sObjectField51);
        final SObjectField sObjectField52 = createField("DunsNumber", "D-U-N-S Number", "string", "xsd:string", 9, false, true,
                false, false, false, false, false);
        fields1.add(sObjectField52);
        final SObjectField sObjectField53 = createField("Tradestyle", "Tradestyle", "string", "xsd:string", 255, false, true,
                false, false, false, false, false);
        fields1.add(sObjectField53);
        final SObjectField sObjectField54 = createField("NaicsCode", "NAICS Code", "string", "xsd:string", 8, false, true,
                false, false, false, false, false);
        fields1.add(sObjectField54);
        final SObjectField sObjectField55 = createField("NaicsDesc", "NAICS Description", "string", "xsd:string", 120, false,
                true, false, false, false, false, false);
        fields1.add(sObjectField55);
        final SObjectField sObjectField56 = createField("YearStarted", "Year Started", "string", "xsd:string", 4, false, true,
                false, false, false, false, false);
        fields1.add(sObjectField56);
        final SObjectField sObjectField57 = createField("SicDesc", "SIC Description", "string", "xsd:string", 80, false, true,
                false, false, false, false, false);
        fields1.add(sObjectField57);
        final SObjectField sObjectField58 = createField("DandbCompanyId", "D&B Company ID", "reference", "tns:ID", 18, false,
                true, false, false, false, false, false);
        fields1.add(sObjectField58);
        final SObjectField sObjectField59 = createField("CustomerPriority__c", "Customer Priority", "picklist", "xsd:string",
                255, false, true, false, false, true, false, false);
        fields1.add(sObjectField59);
        final SObjectField sObjectField60 = createField("SLA__c", "SLA", "picklist", "xsd:string", 255, false, true, false,
                false, true, false, false);
        fields1.add(sObjectField60);
        final SObjectField sObjectField61 = createField("Active__c", "Active", "picklist", "xsd:string", 255, false, true,
                false, false, true, false, false);
        fields1.add(sObjectField61);
        final SObjectField sObjectField62 = createField("NumberofLocations__c", "Number of Locations", "double", "xsd:double",
                0, false, true, false, false, true, false, false);
        fields1.add(sObjectField62);
        final SObjectField sObjectField63 = createField("UpsellOpportunity__c", "Upsell Opportunity", "picklist", "xsd:string",
                255, false, true, false, false, true, false, false);
        fields1.add(sObjectField63);
        final SObjectField sObjectField64 = createField("SLASerialNumber__c", "SLA Serial Number", "string", "xsd:string", 10,
                false, true, false, false, true, false, false);
        fields1.add(sObjectField64);
        final SObjectField sObjectField65 = createField("SLAExpirationDate__c", "SLA Expiration Date", "date", "xsd:date", 0,
                false, true, false, false, true, false, false);
        fields1.add(sObjectField65);
        final SObjectField sObjectField66 = createField("MyMultiselect__c", "MyMultiselect", "multipicklist", "xsd:string",
                4099, false, true, false, false, true, false, false);
        fields1.add(sObjectField66);

        description.setLabel("Account");
        description.setLabelPlural("Accounts");
        description.setName("Account");

        final SObjectDescriptionUrls sObjectDescriptionUrls1 = new SObjectDescriptionUrls();
        sObjectDescriptionUrls1.setApprovalLayouts("/services/data/v50.0/sobjects/Account/describe/approvalLayouts");
        sObjectDescriptionUrls1.setCompactLayouts("/services/data/v50.0/sobjects/Account/describe/compactLayouts");
        sObjectDescriptionUrls1.setDescribe("/services/data/v50.0/sobjects/Account/describe");
        sObjectDescriptionUrls1.setLayouts("/services/data/v50.0/sobjects/Account/describe/layouts");
        sObjectDescriptionUrls1.setListviews("/services/data/v50.0/sobjects/Account/listviews");
        sObjectDescriptionUrls1.setQuickActions("/services/data/v50.0/sobjects/Account/quickActions");
        sObjectDescriptionUrls1.setRowTemplate("/services/data/v50.0/sobjects/Account/{ID}");
        sObjectDescriptionUrls1.setSobject("/services/data/v50.0/sobjects/Account");
        sObjectDescriptionUrls1.setUiDetailTemplate("https://eu37.salesforce.com/{ID}");
        sObjectDescriptionUrls1.setUiEditTemplate("https://eu37.salesforce.com/{ID}/e");
        sObjectDescriptionUrls1.setUiNewRecord("https://eu37.salesforce.com/001/e");
        description.setUrls(sObjectDescriptionUrls1);

        return description;
    }
}
