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
package org.apache.camel.quarkus.component.fhir.deployment.dstu2;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import ca.uhn.fhir.model.dstu2.resource.*;
import ca.uhn.fhir.model.dstu2.valueset.AccountStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ActionListEnum;
import ca.uhn.fhir.model.dstu2.valueset.AddressTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.AddressUseEnum;
import ca.uhn.fhir.model.dstu2.valueset.AdjudicationCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.AdjudicationErrorCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.AdjustmentReasonCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.AdmitSourceEnum;
import ca.uhn.fhir.model.dstu2.valueset.AggregationModeEnum;
import ca.uhn.fhir.model.dstu2.valueset.AllergyIntoleranceCategoryEnum;
import ca.uhn.fhir.model.dstu2.valueset.AllergyIntoleranceCertaintyEnum;
import ca.uhn.fhir.model.dstu2.valueset.AllergyIntoleranceCriticalityEnum;
import ca.uhn.fhir.model.dstu2.valueset.AllergyIntoleranceSeverityEnum;
import ca.uhn.fhir.model.dstu2.valueset.AllergyIntoleranceStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.AllergyIntoleranceTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.AnswerFormatEnum;
import ca.uhn.fhir.model.dstu2.valueset.AppointmentStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.AssertionDirectionTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.AssertionOperatorTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.AssertionResponseTypesEnum;
import ca.uhn.fhir.model.dstu2.valueset.AuditEventActionEnum;
import ca.uhn.fhir.model.dstu2.valueset.AuditEventObjectLifecycleEnum;
import ca.uhn.fhir.model.dstu2.valueset.AuditEventObjectRoleEnum;
import ca.uhn.fhir.model.dstu2.valueset.AuditEventObjectTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.AuditEventOutcomeEnum;
import ca.uhn.fhir.model.dstu2.valueset.AuditEventParticipantNetworkTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.AuditEventSourceTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.BindingStrengthEnum;
import ca.uhn.fhir.model.dstu2.valueset.BundleTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.CarePlanActivityStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.CarePlanRelationshipEnum;
import ca.uhn.fhir.model.dstu2.valueset.CarePlanStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ClaimTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.ClinicalImpressionStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.CommunicationRequestStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.CommunicationStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.CompositionAttestationModeEnum;
import ca.uhn.fhir.model.dstu2.valueset.CompositionStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConceptMapEquivalenceEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConditionCategoryCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConditionClinicalStatusCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConditionVerificationStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConditionalDeleteStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConformanceEventModeEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConformanceResourceStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConformanceStatementKindEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConstraintSeverityEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointSystemEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointUseEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContentTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.DataElementStringencyEnum;
import ca.uhn.fhir.model.dstu2.valueset.DaysOfWeekEnum;
import ca.uhn.fhir.model.dstu2.valueset.DetectedIssueSeverityEnum;
import ca.uhn.fhir.model.dstu2.valueset.DeviceMetricCalibrationStateEnum;
import ca.uhn.fhir.model.dstu2.valueset.DeviceMetricCalibrationTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.DeviceMetricCategoryEnum;
import ca.uhn.fhir.model.dstu2.valueset.DeviceMetricColorEnum;
import ca.uhn.fhir.model.dstu2.valueset.DeviceMetricOperationalStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.DeviceStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.DeviceUseRequestPriorityEnum;
import ca.uhn.fhir.model.dstu2.valueset.DeviceUseRequestStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticOrderPriorityEnum;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticOrderStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticReportStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.DigitalMediaTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.DocumentModeEnum;
import ca.uhn.fhir.model.dstu2.valueset.DocumentReferenceStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.DocumentRelationshipTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.EncounterClassEnum;
import ca.uhn.fhir.model.dstu2.valueset.EncounterLocationStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.EncounterStateEnum;
import ca.uhn.fhir.model.dstu2.valueset.EpisodeOfCareStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.EventTimingEnum;
import ca.uhn.fhir.model.dstu2.valueset.ExtensionContextEnum;
import ca.uhn.fhir.model.dstu2.valueset.FamilyHistoryStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.FilterOperatorEnum;
import ca.uhn.fhir.model.dstu2.valueset.FlagStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.GoalPriorityEnum;
import ca.uhn.fhir.model.dstu2.valueset.GoalStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.GroupTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.GuideDependencyTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.GuidePageKindEnum;
import ca.uhn.fhir.model.dstu2.valueset.GuideResourcePurposeEnum;
import ca.uhn.fhir.model.dstu2.valueset.HTTPVerbEnum;
import ca.uhn.fhir.model.dstu2.valueset.IdentifierTypeCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.IdentifierUseEnum;
import ca.uhn.fhir.model.dstu2.valueset.IdentityAssuranceLevelEnum;
import ca.uhn.fhir.model.dstu2.valueset.InstanceAvailabilityEnum;
import ca.uhn.fhir.model.dstu2.valueset.IssueSeverityEnum;
import ca.uhn.fhir.model.dstu2.valueset.IssueTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.KOStitleEnum;
import ca.uhn.fhir.model.dstu2.valueset.LinkTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.ListModeEnum;
import ca.uhn.fhir.model.dstu2.valueset.ListOrderCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.ListStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.LocationModeEnum;
import ca.uhn.fhir.model.dstu2.valueset.LocationStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.LocationTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.MaritalStatusCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.MeasmntPrincipleEnum;
import ca.uhn.fhir.model.dstu2.valueset.MedicationAdministrationStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.MedicationDispenseStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.MedicationOrderStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.MedicationStatementStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.MessageEventEnum;
import ca.uhn.fhir.model.dstu2.valueset.MessageSignificanceCategoryEnum;
import ca.uhn.fhir.model.dstu2.valueset.MessageTransportEnum;
import ca.uhn.fhir.model.dstu2.valueset.NameUseEnum;
import ca.uhn.fhir.model.dstu2.valueset.NamingSystemIdentifierTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.NamingSystemTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.NoteTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.NutritionOrderStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ObservationRelationshipTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.ObservationStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.OperationKindEnum;
import ca.uhn.fhir.model.dstu2.valueset.OperationParameterUseEnum;
import ca.uhn.fhir.model.dstu2.valueset.OrderStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ParticipantRequiredEnum;
import ca.uhn.fhir.model.dstu2.valueset.ParticipantStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ParticipantTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.ParticipationStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.PayeeTypeCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.ProcedureRequestPriorityEnum;
import ca.uhn.fhir.model.dstu2.valueset.ProcedureRequestStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ProcedureStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.PropertyRepresentationEnum;
import ca.uhn.fhir.model.dstu2.valueset.ProvenanceEntityRoleEnum;
import ca.uhn.fhir.model.dstu2.valueset.QuantityComparatorEnum;
import ca.uhn.fhir.model.dstu2.valueset.QuestionnaireResponseStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.QuestionnaireStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ReferralMethodEnum;
import ca.uhn.fhir.model.dstu2.valueset.ReferralStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.RemittanceOutcomeEnum;
import ca.uhn.fhir.model.dstu2.valueset.ResourceTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.ResourceVersionPolicyEnum;
import ca.uhn.fhir.model.dstu2.valueset.ResponseTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.RestfulConformanceModeEnum;
import ca.uhn.fhir.model.dstu2.valueset.RestfulSecurityServiceEnum;
import ca.uhn.fhir.model.dstu2.valueset.RulesetCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.SearchEntryModeEnum;
import ca.uhn.fhir.model.dstu2.valueset.SearchModifierCodeEnum;
import ca.uhn.fhir.model.dstu2.valueset.SearchParamTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.ServiceProvisionConditionsEnum;
import ca.uhn.fhir.model.dstu2.valueset.SignatureTypeCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.SlicingRulesEnum;
import ca.uhn.fhir.model.dstu2.valueset.SlotStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.SpecimenStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.StructureDefinitionKindEnum;
import ca.uhn.fhir.model.dstu2.valueset.SubscriptionChannelTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.SubscriptionStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.SubstanceCategoryCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.SupplyDeliveryStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.SupplyRequestStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.SystemRestfulInteractionEnum;
import ca.uhn.fhir.model.dstu2.valueset.TimingAbbreviationEnum;
import ca.uhn.fhir.model.dstu2.valueset.TransactionModeEnum;
import ca.uhn.fhir.model.dstu2.valueset.TypeRestfulInteractionEnum;
import ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum;
import ca.uhn.fhir.model.dstu2.valueset.UnknownContentCodeEnum;
import ca.uhn.fhir.model.dstu2.valueset.UseEnum;
import ca.uhn.fhir.model.dstu2.valueset.VisionBaseEnum;
import ca.uhn.fhir.model.dstu2.valueset.VisionEyesEnum;
import ca.uhn.fhir.model.dstu2.valueset.XPathUsageTypeEnum;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.component.fhir.FhirContextRecorder;
import org.apache.camel.quarkus.component.fhir.FhirFlags;

import static org.apache.camel.quarkus.component.fhir.deployment.FhirUtil.getModelClasses;
import static org.apache.camel.quarkus.component.fhir.deployment.FhirUtil.getResourceDefinitions;
import static org.apache.camel.quarkus.component.fhir.deployment.FhirUtil.loadProperties;

public class FhirDstu2Processor {

    @BuildStep(onlyIf = FhirFlags.Dstu2Enabled.class)
    Dstu2PropertiesBuildItem properties(BuildProducer<NativeImageResourceBuildItem> resource) {
        Properties properties = loadProperties("/ca/uhn/fhir/model/dstu2/fhirversion.properties");
        resource.produce(new NativeImageResourceBuildItem("ca/uhn/fhir/model/dstu2/fhirversion.properties"));
        return new Dstu2PropertiesBuildItem(properties);
    }

    @BuildStep(onlyIf = FhirFlags.Dstu2Enabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    void recordContext(FhirContextRecorder fhirContextRecorder, BeanContainerBuildItem beanContainer,
            Dstu2PropertiesBuildItem propertiesBuildItem) {
        fhirContextRecorder.createDstu2FhirContext(beanContainer.getValue(),
                getResourceDefinitions(propertiesBuildItem.getProperties()));
    }

    @BuildStep(onlyIf = FhirFlags.Dstu2Enabled.class)
    void enableReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, Dstu2PropertiesBuildItem buildItem) {
        Set<String> classes = new HashSet<>();
        classes.add(BaseResource.class.getCanonicalName());
        classes.addAll(getModelClasses(buildItem.getProperties()));

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, classes.toArray(new String[0])));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, getDstu2Enums()));
    }

    private String[] getDstu2Enums() {
        return new String[] {
                AccountStatusEnum.class.getCanonicalName(),
                ActionListEnum.class.getCanonicalName(),
                AddressTypeEnum.class.getCanonicalName(),
                AddressUseEnum.class.getCanonicalName(),
                AdjudicationCodesEnum.class.getCanonicalName(),
                AdjudicationErrorCodesEnum.class.getCanonicalName(),
                AdjustmentReasonCodesEnum.class.getCanonicalName(),
                AdministrativeGenderEnum.class.getCanonicalName(),
                AdmitSourceEnum.class.getCanonicalName(),
                AggregationModeEnum.class.getCanonicalName(),
                AllergyIntoleranceCategoryEnum.class.getCanonicalName(),
                AllergyIntoleranceCertaintyEnum.class.getCanonicalName(),
                AllergyIntoleranceCriticalityEnum.class.getCanonicalName(),
                AllergyIntoleranceSeverityEnum.class.getCanonicalName(),
                AllergyIntoleranceStatusEnum.class.getCanonicalName(),
                AllergyIntoleranceTypeEnum.class.getCanonicalName(),
                AnswerFormatEnum.class.getCanonicalName(),
                AppointmentStatusEnum.class.getCanonicalName(),
                AssertionDirectionTypeEnum.class.getCanonicalName(),
                AssertionOperatorTypeEnum.class.getCanonicalName(),
                AssertionResponseTypesEnum.class.getCanonicalName(),
                AuditEventActionEnum.class.getCanonicalName(),
                AuditEventObjectLifecycleEnum.class.getCanonicalName(),
                AuditEventObjectRoleEnum.class.getCanonicalName(),
                AuditEventObjectTypeEnum.class.getCanonicalName(),
                AuditEventOutcomeEnum.class.getCanonicalName(),
                AuditEventParticipantNetworkTypeEnum.class.getCanonicalName(),
                AuditEventSourceTypeEnum.class.getCanonicalName(),
                BindingStrengthEnum.class.getCanonicalName(),
                BundleTypeEnum.class.getCanonicalName(),
                CarePlanActivityStatusEnum.class.getCanonicalName(),
                CarePlanRelationshipEnum.class.getCanonicalName(),
                CarePlanStatusEnum.class.getCanonicalName(),
                ClaimTypeEnum.class.getCanonicalName(),
                ClinicalImpressionStatusEnum.class.getCanonicalName(),
                CommunicationRequestStatusEnum.class.getCanonicalName(),
                CommunicationStatusEnum.class.getCanonicalName(),
                CompositionAttestationModeEnum.class.getCanonicalName(),
                CompositionStatusEnum.class.getCanonicalName(),
                ConceptMapEquivalenceEnum.class.getCanonicalName(),
                ConditionalDeleteStatusEnum.class.getCanonicalName(),
                ConditionCategoryCodesEnum.class.getCanonicalName(),
                ConditionClinicalStatusCodesEnum.class.getCanonicalName(),
                ConditionVerificationStatusEnum.class.getCanonicalName(),
                ConformanceEventModeEnum.class.getCanonicalName(),
                ConformanceResourceStatusEnum.class.getCanonicalName(),
                ConformanceStatementKindEnum.class.getCanonicalName(),
                ConstraintSeverityEnum.class.getCanonicalName(),
                ContactPointSystemEnum.class.getCanonicalName(),
                ContactPointUseEnum.class.getCanonicalName(),
                ContentTypeEnum.class.getCanonicalName(),
                DataElementStringencyEnum.class.getCanonicalName(),
                DaysOfWeekEnum.class.getCanonicalName(),
                DetectedIssueSeverityEnum.class.getCanonicalName(),
                DeviceMetricCalibrationStateEnum.class.getCanonicalName(),
                DeviceMetricCalibrationTypeEnum.class.getCanonicalName(),
                DeviceMetricCategoryEnum.class.getCanonicalName(),
                DeviceMetricColorEnum.class.getCanonicalName(),
                DeviceMetricOperationalStatusEnum.class.getCanonicalName(),
                DeviceStatusEnum.class.getCanonicalName(),
                DeviceUseRequestPriorityEnum.class.getCanonicalName(),
                DeviceUseRequestStatusEnum.class.getCanonicalName(),
                DiagnosticOrderPriorityEnum.class.getCanonicalName(),
                DiagnosticOrderStatusEnum.class.getCanonicalName(),
                DiagnosticReportStatusEnum.class.getCanonicalName(),
                DigitalMediaTypeEnum.class.getCanonicalName(),
                DocumentModeEnum.class.getCanonicalName(),
                DocumentReferenceStatusEnum.class.getCanonicalName(),
                DocumentRelationshipTypeEnum.class.getCanonicalName(),
                EncounterClassEnum.class.getCanonicalName(),
                EncounterLocationStatusEnum.class.getCanonicalName(),
                EncounterStateEnum.class.getCanonicalName(),
                EpisodeOfCareStatusEnum.class.getCanonicalName(),
                EventTimingEnum.class.getCanonicalName(),
                ExtensionContextEnum.class.getCanonicalName(),
                FamilyHistoryStatusEnum.class.getCanonicalName(),
                FilterOperatorEnum.class.getCanonicalName(),
                FlagStatusEnum.class.getCanonicalName(),
                GoalPriorityEnum.class.getCanonicalName(),
                GoalStatusEnum.class.getCanonicalName(),
                GroupTypeEnum.class.getCanonicalName(),
                GuideDependencyTypeEnum.class.getCanonicalName(),
                GuidePageKindEnum.class.getCanonicalName(),
                GuideResourcePurposeEnum.class.getCanonicalName(),
                HTTPVerbEnum.class.getCanonicalName(),
                IdentifierTypeCodesEnum.class.getCanonicalName(),
                IdentifierUseEnum.class.getCanonicalName(),
                IdentityAssuranceLevelEnum.class.getCanonicalName(),
                InstanceAvailabilityEnum.class.getCanonicalName(),
                IssueSeverityEnum.class.getCanonicalName(),
                IssueTypeEnum.class.getCanonicalName(),
                KOStitleEnum.class.getCanonicalName(),
                LinkTypeEnum.class.getCanonicalName(),
                ListModeEnum.class.getCanonicalName(),
                ListOrderCodesEnum.class.getCanonicalName(),
                ListStatusEnum.class.getCanonicalName(),
                LocationModeEnum.class.getCanonicalName(),
                LocationStatusEnum.class.getCanonicalName(),
                LocationTypeEnum.class.getCanonicalName(),
                MaritalStatusCodesEnum.class.getCanonicalName(),
                MeasmntPrincipleEnum.class.getCanonicalName(),
                MedicationAdministrationStatusEnum.class.getCanonicalName(),
                MedicationDispenseStatusEnum.class.getCanonicalName(),
                MedicationOrderStatusEnum.class.getCanonicalName(),
                MedicationStatementStatusEnum.class.getCanonicalName(),
                MessageEventEnum.class.getCanonicalName(),
                MessageSignificanceCategoryEnum.class.getCanonicalName(),
                MessageTransportEnum.class.getCanonicalName(),
                NameUseEnum.class.getCanonicalName(),
                NamingSystemIdentifierTypeEnum.class.getCanonicalName(),
                NamingSystemTypeEnum.class.getCanonicalName(),
                NarrativeStatusEnum.class.getCanonicalName(),
                NoteTypeEnum.class.getCanonicalName(),
                NutritionOrderStatusEnum.class.getCanonicalName(),
                ObservationRelationshipTypeEnum.class.getCanonicalName(),
                ObservationStatusEnum.class.getCanonicalName(),
                OperationKindEnum.class.getCanonicalName(),
                OperationParameterUseEnum.class.getCanonicalName(),
                OrderStatusEnum.class.getCanonicalName(),
                ParticipantRequiredEnum.class.getCanonicalName(),
                ParticipantStatusEnum.class.getCanonicalName(),
                ParticipantTypeEnum.class.getCanonicalName(),
                ParticipationStatusEnum.class.getCanonicalName(),
                PayeeTypeCodesEnum.class.getCanonicalName(),
                ProcedureRequestPriorityEnum.class.getCanonicalName(),
                ProcedureRequestStatusEnum.class.getCanonicalName(),
                ProcedureStatusEnum.class.getCanonicalName(),
                PropertyRepresentationEnum.class.getCanonicalName(),
                ProvenanceEntityRoleEnum.class.getCanonicalName(),
                QuantityComparatorEnum.class.getCanonicalName(),
                QuestionnaireResponseStatusEnum.class.getCanonicalName(),
                QuestionnaireStatusEnum.class.getCanonicalName(),
                ReferralMethodEnum.class.getCanonicalName(),
                ReferralStatusEnum.class.getCanonicalName(),
                RemittanceOutcomeEnum.class.getCanonicalName(),
                ResourceTypeEnum.class.getCanonicalName(),
                ResourceVersionPolicyEnum.class.getCanonicalName(),
                ResponseTypeEnum.class.getCanonicalName(),
                RestfulConformanceModeEnum.class.getCanonicalName(),
                RestfulSecurityServiceEnum.class.getCanonicalName(),
                RulesetCodesEnum.class.getCanonicalName(),
                SearchEntryModeEnum.class.getCanonicalName(),
                SearchModifierCodeEnum.class.getCanonicalName(),
                SearchParamTypeEnum.class.getCanonicalName(),
                ServiceProvisionConditionsEnum.class.getCanonicalName(),
                SignatureTypeCodesEnum.class.getCanonicalName(),
                SlicingRulesEnum.class.getCanonicalName(),
                SlotStatusEnum.class.getCanonicalName(),
                SpecimenStatusEnum.class.getCanonicalName(),
                StructureDefinitionKindEnum.class.getCanonicalName(),
                SubscriptionChannelTypeEnum.class.getCanonicalName(),
                SubscriptionStatusEnum.class.getCanonicalName(),
                SubstanceCategoryCodesEnum.class.getCanonicalName(),
                SupplyDeliveryStatusEnum.class.getCanonicalName(),
                SupplyRequestStatusEnum.class.getCanonicalName(),
                SystemRestfulInteractionEnum.class.getCanonicalName(),
                TimingAbbreviationEnum.class.getCanonicalName(),
                TransactionModeEnum.class.getCanonicalName(),
                TypeRestfulInteractionEnum.class.getCanonicalName(),
                UnitsOfTimeEnum.class.getCanonicalName(),
                UnknownContentCodeEnum.class.getCanonicalName(),
                UseEnum.class.getCanonicalName(),
                VisionBaseEnum.class.getCanonicalName(),
                VisionEyesEnum.class.getCanonicalName(),
                XPathUsageTypeEnum.class.getCanonicalName()
        };
    }

}
