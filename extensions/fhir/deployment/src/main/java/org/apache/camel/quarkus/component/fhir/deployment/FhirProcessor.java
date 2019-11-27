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
package org.apache.camel.quarkus.component.fhir.deployment;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import ca.uhn.fhir.model.dstu2.FhirDstu2;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.valueset.*;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.util.jar.DependencyLogImpl;
import ca.uhn.fhir.validation.schematron.SchematronBaseValidator;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.component.fhir.FhirCapabilitiesEndpointConfiguration;
import org.apache.camel.component.fhir.FhirConfiguration;
import org.apache.camel.component.fhir.FhirCreateEndpointConfiguration;
import org.apache.camel.component.fhir.FhirDeleteEndpointConfiguration;
import org.apache.camel.component.fhir.FhirHistoryEndpointConfiguration;
import org.apache.camel.component.fhir.FhirLoadPageEndpointConfiguration;
import org.apache.camel.component.fhir.FhirMetaEndpointConfiguration;
import org.apache.camel.component.fhir.FhirOperationEndpointConfiguration;
import org.apache.camel.component.fhir.FhirPatchEndpointConfiguration;
import org.apache.camel.component.fhir.FhirReadEndpointConfiguration;
import org.apache.camel.component.fhir.FhirSearchEndpointConfiguration;
import org.apache.camel.component.fhir.FhirTransactionEndpointConfiguration;
import org.apache.camel.component.fhir.FhirUpdateEndpointConfiguration;
import org.apache.camel.component.fhir.FhirValidateEndpointConfiguration;
import org.apache.camel.quarkus.component.fhir.FhirFlags;
import org.hl7.fhir.dstu3.hapi.ctx.FhirDstu3;
import org.hl7.fhir.dstu3.model.Base;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.MetadataResource;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.r4.hapi.ctx.FhirR4;
import org.hl7.fhir.r5.hapi.ctx.FhirR5;

class FhirProcessor {
    private static final String FEATURE = "camel-fhir";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep()
    ReflectiveClassBuildItem fhirEndpointConfiguration() {
        return new ReflectiveClassBuildItem(true, true,
                FhirCreateEndpointConfiguration.class,
                FhirCapabilitiesEndpointConfiguration.class,
                FhirDeleteEndpointConfiguration.class,
                FhirHistoryEndpointConfiguration.class,
                FhirLoadPageEndpointConfiguration.class,
                FhirMetaEndpointConfiguration.class,
                FhirOperationEndpointConfiguration.class,
                FhirPatchEndpointConfiguration.class,
                FhirReadEndpointConfiguration.class,
                FhirSearchEndpointConfiguration.class,
                FhirTransactionEndpointConfiguration.class,
                FhirUpdateEndpointConfiguration.class,
                FhirValidateEndpointConfiguration.class,
                FhirConfiguration.class);
    }

    @BuildStep()
    NativeImageResourceBundleBuildItem hapiMessages() {
        return new NativeImageResourceBundleBuildItem("ca.uhn.fhir.i18n.hapi-messages");
    }

    @BuildStep(applicationArchiveMarkers = { "org/hl7/fhir", "ca/uhn/fhir" })
    void processFhir(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, true, SchematronBaseValidator.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, true, DependencyLogImpl.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true, ApacheRestfulClientFactory.class));
    }

    @BuildStep(onlyIf = FhirFlags.Dstu2Enabled.class, applicationArchiveMarkers = { "org/hl7/fhir", "ca/uhn/fhir" })
    void processDstu2(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource) {
        Set<String> classes = new HashSet<>();
        classes.add(FhirDstu2.class.getCanonicalName());
        classes.add(BaseResource.class.getCanonicalName());
        classes.addAll(getModelClasses("/ca/uhn/fhir/model/dstu2/fhirversion.properties"));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, true, classes.toArray(new String[0])));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, true, getDstu2Enums()));
        resource.produce(new NativeImageResourceBuildItem("ca/uhn/fhir/model/dstu2/fhirversion.properties"));
    }

    @BuildStep(onlyIf = FhirFlags.Dstu3Enabled.class, applicationArchiveMarkers = { "org/hl7/fhir", "ca/uhn/fhir" })
    void processDstu3(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource) {
        Set<String> classes = new HashSet<>();
        classes.add(FhirDstu3.class.getCanonicalName());
        classes.add(DomainResource.class.getCanonicalName());
        classes.add(Resource.class.getCanonicalName());
        classes.add(org.hl7.fhir.dstu3.model.BaseResource.class.getCanonicalName());
        classes.add(Base.class.getCanonicalName());
        classes.addAll(getModelClasses("/org/hl7/fhir/dstu3/model/fhirversion.properties"));
        classes.addAll(getInnerClasses(Enumerations.class.getCanonicalName()));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true, Meta.class.getCanonicalName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true, MetadataResource.class.getCanonicalName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, true, classes.toArray(new String[0])));
        resource.produce(new NativeImageResourceBuildItem("org/hl7/fhir/dstu3/model/fhirversion.properties"));
    }

    @BuildStep(onlyIf = FhirFlags.R4Enabled.class, applicationArchiveMarkers = { "org/hl7/fhir", "ca/uhn/fhir" })
    void processR4(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource) {
        Set<String> classes = new HashSet<>();
        classes.add(FhirR4.class.getCanonicalName());
        classes.add(org.hl7.fhir.r4.model.DomainResource.class.getCanonicalName());
        classes.add(org.hl7.fhir.r4.model.Resource.class.getCanonicalName());
        classes.add(org.hl7.fhir.r4.model.BaseResource.class.getCanonicalName());
        classes.add(org.hl7.fhir.r4.model.Base.class.getCanonicalName());
        classes.addAll(getModelClasses("/org/hl7/fhir/r4/model/fhirversion.properties"));
        classes.addAll(getInnerClasses(org.hl7.fhir.r4.model.Enumerations.class.getCanonicalName()));

        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, true, org.hl7.fhir.r4.model.Meta.class.getCanonicalName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true,
                org.hl7.fhir.r4.model.MetadataResource.class.getCanonicalName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, true, classes.toArray(new String[0])));
        resource.produce(new NativeImageResourceBuildItem("org/hl7/fhir/r4/model/fhirversion.properties"));
    }

    @BuildStep(onlyIf = FhirFlags.R5Enabled.class, applicationArchiveMarkers = { "org/hl7/fhir", "ca/uhn/fhir" })
    void processR5(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource) {
        Set<String> classes = new HashSet<>();
        classes.add(FhirR5.class.getCanonicalName());
        classes.add(org.hl7.fhir.r5.model.DomainResource.class.getCanonicalName());
        classes.add(org.hl7.fhir.r5.model.Resource.class.getCanonicalName());
        classes.add(org.hl7.fhir.r5.model.BaseResource.class.getCanonicalName());
        classes.add(org.hl7.fhir.r5.model.Base.class.getCanonicalName());
        classes.addAll(getModelClasses("/org/hl7/fhir/r5/model/fhirversion.properties"));
        classes.addAll(getInnerClasses(org.hl7.fhir.r5.model.Enumerations.class.getCanonicalName()));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, true, org.hl7.fhir.r5.model.Meta.class.getCanonicalName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true,
                org.hl7.fhir.r5.model.MetadataResource.class.getCanonicalName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, true, classes.toArray(new String[0])));
        resource.produce(new NativeImageResourceBuildItem("org/hl7/fhir/r5/model/fhirversion.properties"));
    }

    private Collection<String> getModelClasses(String model) {
        try (InputStream str = FhirDstu3.class.getResourceAsStream(model)) {
            Properties prop = new Properties();
            prop.load(str);
            return getInnerClasses(prop.values().toArray(new String[0]));
        } catch (Exception e) {
            throw new RuntimeException("Please ensure FHIR is on the classpath", e);
        }
    }

    private Collection<String> getInnerClasses(String... classList) {
        try {
            Set<String> classes = new HashSet<>();
            for (Object value : classList) {
                String clazz = (String) value;
                final Class[] parent = Class.forName(clazz).getClasses();
                for (Class aClass : parent) {
                    String name = aClass.getName();
                    classes.add(name);
                }
                classes.add(clazz);
            }
            return classes;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Please ensure FHIR is on the classpath", e);
        }
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
