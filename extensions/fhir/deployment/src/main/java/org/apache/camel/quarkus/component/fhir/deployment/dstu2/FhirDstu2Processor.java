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
import java.util.Set;

import javax.inject.Singleton;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.valueset.*;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
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

public class FhirDstu2Processor {

    private static final String FHIR_VERSION_PROPERTIES = "ca/uhn/fhir/model/dstu2/fhirversion.properties";

    @BuildStep(onlyIf = FhirFlags.Dstu2Enabled.class)
    Dstu2PropertiesBuildItem properties(BuildProducer<NativeImageResourceBuildItem> resource) {
        resource.produce(new NativeImageResourceBuildItem(FHIR_VERSION_PROPERTIES));
        return new Dstu2PropertiesBuildItem(FHIR_VERSION_PROPERTIES);
    }

    @BuildStep(onlyIf = FhirFlags.Dstu2Enabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem recordContext(FhirContextRecorder fhirContextRecorder,
            Dstu2PropertiesBuildItem propertiesBuildItem) {
        return SyntheticBeanBuildItem.configure(FhirContext.class)
                .scope(Singleton.class)
                .named("DSTU2")
                .runtimeValue(fhirContextRecorder.createDstu2FhirContext(
                        getResourceDefinitions(propertiesBuildItem.getProperties())))
                .done();
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
